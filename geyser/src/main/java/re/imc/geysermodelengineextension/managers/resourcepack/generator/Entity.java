package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.zimzaza4.geyserutils.geyser.GeyserUtils;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.TextureData;
import re.imc.geysermodelengineextension.util.ShortHashUtil;

import java.util.*;

public class Entity {

    public static final Set<String> REGISTERED_ENTITIES = new HashSet<>();

    private static final int MAX_PROPERTIES = 32;

    private String modelId;
    private JsonObject json;
    private boolean hasHeadAnimation = false;
    private Animation animation;
    private Geometry geometry;
    private RenderController renderController;
    private String path;
    private Map<String, TextureData> textureMap = new HashMap<>();
    private ModelConfig modelConfig;
    private final Map<String, List<String[]>> translucentSplits = new HashMap<>();

    public static final String TEMPLATE = """
            {
              "format_version": "1.10.0",
              "minecraft:client_entity": {
                "description": {
                  "identifier": "%namespace%:%entity_id%",
                  "materials": {
                    "default": "%material%",
                    "anim": "entity_alphatest_anim_change_color_one_sided",
                    "blend": "entity_alphablend_change_color_one_sided",
                    "tintblend": "entity_alphablend"
                  },
                  "textures": {
                  },
                  "geometry": {
            
                  },
                  "animations": {
                    "look_at_target": "%look_at_target%"
                  },
                  "scripts": {
                    "animate": [
                      "look_at_target"
                    ]
                  },
                  "render_controllers": [
                  ]
                }
              }
            }
            """;

    public Entity(String modelId) {
        this.modelId = modelId;
    }

    public void modify(String namespace, boolean hashEnabled) {
        String modelShortName = hashEnabled ? ShortHashUtil.hashModelId(modelId) : modelId;
        String geometryId = hashEnabled ? "geometry." + modelShortName : "geometry.meg_" + modelShortName;
        String defaultTexturePath = hashEnabled ? "textures/entity/" + ShortHashUtil.hashTextureName(modelId, modelId) : "textures/entity/" + modelId;

        this.json = JsonParser.parseString(TEMPLATE.replace("%namespace%", namespace)
                .replace("%entity_id%", modelId)
                .replace("%geometry%", geometryId)
                .replace("%texture%", defaultTexturePath)
                .replace("%look_at_target%", (modelConfig.isEnableHeadRotation() && hasHeadAnimation) ? "animation." + modelId + ".look_at_target" : "animation.common.look_at_target")
                .replace("%material%", modelConfig.getMaterial())).getAsJsonObject();

        JsonObject description = json.get("minecraft:client_entity").getAsJsonObject().get("description").getAsJsonObject();
        JsonObject jsonAnimations = description.get("animations").getAsJsonObject();
        JsonObject jsonTextures = description.get("textures").getAsJsonObject();
        JsonObject jsonGeometry = description.get("geometry").getAsJsonObject();
        JsonObject jsonMaterials = description.get("materials").getAsJsonObject();

        JsonArray jsonRenderControllers = description.get("render_controllers").getAsJsonArray();

        Map<String, String> materials = modelConfig.getTextureMaterials();
        materials.forEach(jsonMaterials::addProperty);

        if (modelConfig.getPerTextureUvSize().isEmpty()) {
            jsonGeometry.addProperty(modelShortName, geometryId);
            jsonTextures.addProperty("default", defaultTexturePath);

            if (hasTints()) {
                jsonGeometry.addProperty(modelShortName + Geometry.TINT_SUFFIX, geometryId + Geometry.TINT_SUFFIX);
                jsonTextures.addProperty("default" + TextureData.TINT_SUFFIX, defaultTexturePath + TextureData.TINT_SUFFIX);
            }
        }

        boolean singleTexture = textureMap.size() == 1 && modelConfig.getPerTextureUvSize().isEmpty();

        for (String name : textureMap.keySet()) {
            if (name.endsWith("_e")) continue;
            if (modelConfig.getBingingBones().get(name) == null && !singleTexture) continue;

            if (modelConfig.getPerTextureUvSize().containsKey(name)) {
                Integer[] size = modelConfig.getPerTextureUvSize().getOrDefault(name, new Integer[]{16, 16});
                String suffix = size[0] + "_" + size[1];

                String geoName = hashEnabled ? ShortHashUtil.hashModelId(modelId + "_" + suffix) : modelShortName + "_" + suffix;
                String geoId = hashEnabled ? "geometry." + geoName : "geometry.meg_" + modelId + "_" + suffix;
                String textureName = hashEnabled ? ShortHashUtil.hashTextureName(modelId, name) : name;

                jsonGeometry.addProperty(geoName, geoId);
                jsonTextures.addProperty(textureName, "textures/entity/" + textureName);

                if (hasTints()) {
                    jsonGeometry.addProperty(geoName + Geometry.TINT_SUFFIX, geoId + Geometry.TINT_SUFFIX);
                    jsonTextures.addProperty(textureName + TextureData.TINT_SUFFIX, "textures/entity/" + textureName + TextureData.TINT_SUFFIX);
                }
            }

            String controllerName = name.equals(modelId) ? "controller.render.meg_" + modelId : "controller.render.meg_" + modelId + "_" + name;
            jsonRenderControllers.add(controllerName);
            if (hasTints()) {
                jsonRenderControllers.add(controllerName + "_tint");
                jsonRenderControllers.add(controllerName + "_tintblend");
            }
        }

        JsonArray animate = description.get("scripts").getAsJsonObject().get("animate").getAsJsonArray();

        if (animation != null) {
            for (String animation : animation.getAnimationIds()) {
                animation = animation.replace(" ", "_");
                String controller = "controller.animation." + modelId + "." + animation;
                animate.add(animation + "_control");
                jsonAnimations.addProperty(animation, "animation." + modelId + "." + animation);
                jsonAnimations.addProperty(animation + "_control", controller);
            }
        }
    }

    public void register(String namespace) {
        String id = namespace + ":" + modelId;

        boolean registered = REGISTERED_ENTITIES.contains(id);
        if (registered) return;

        REGISTERED_ENTITIES.add(id);
        GeyserUtils.addCustomEntity(id);
        if (geometry == null) return;

        if (!modelConfig.isDisablePartVisibility()) {
            for (int i = 0; i < Math.ceil(geometry.getBones().size() / 24f); i++) {
                GeyserUtils.addProperty(id, namespace + ":" + "bone" + i, Integer.class);
            }
        }

        if (animation != null) {
            for (int i = 0; i < Math.ceil(animation.getAnimationIds().size() / 24f); i++) {
                GeyserUtils.addProperty(id, namespace + ":" + "anim" + i, Integer.class);
            }
        }

        if (hasTints()) {
            for (int i = 0; i < Math.ceil(geometry.getBones().size() / 24f); i++) {
                GeyserUtils.addProperty(id, namespace + ":" + "tintmask" + i, Integer.class);
            }
            GeyserUtils.addProperty(id, namespace + ":" + "tintcolor", Integer.class);
        } else if (tintsEnabled()) {
            GeyserModelEngineExtension.getExtension().logger().warning("Bone tints disabled for " + modelId + ": it would need more than " + MAX_PROPERTIES + " entity properties.");
        }

        GeyserUtils.registerProperties(id);
    }

    /**
     * Tinted bones are drawn by extra "_tint" and "_tintblend" render controllers (see RenderController), because
     * Bedrock can't colour single bones. That needs one mask property per 24 bones plus a colour property, and Bedrock allows
     * at most 32 properties per entity, so very large models go without tints.
     */
    public boolean hasTints() {
        return tintsEnabled() && geometry != null && countProperties(true) <= MAX_PROPERTIES;
    }

    private boolean tintsEnabled() {
        return GeyserModelEngineExtension.getExtension().getConfigManager().getConfig().getBoolean("options.resource-pack.bone-tints", true);
    }

    private int countProperties(boolean withTints) {
        if (geometry == null) return 0;

        int boneProperties = (int) Math.ceil(geometry.getBones().size() / 24f);
        int count = 0;

        if (!modelConfig.isDisablePartVisibility()) count += boneProperties;
        if (animation != null) count += (int) Math.ceil(animation.getAnimationIds().size() / 24f);
        if (withTints) count += boneProperties + 1;

        return count;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

    public void setHasHeadAnimation(boolean hasHeadAnimation) {
        this.hasHeadAnimation = hasHeadAnimation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public void setRenderController(RenderController renderController) {
        this.renderController = renderController;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setTextureMap(Map<String, TextureData> textureMap) {
        this.textureMap = textureMap;
    }

    public void setModelConfig(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    public String getModelId() {
        return modelId;
    }

    public JsonObject getJson() {
        return json;
    }

    public boolean isHasHeadAnimation() {
        return hasHeadAnimation;
    }

    public Animation getAnimation() {
        return animation;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public RenderController getRenderController() {
        return renderController;
    }

    public String getPath() {
        return path;
    }

    public Map<String, TextureData> getTextureMap() {
        return textureMap;
    }

    public ModelConfig getModelConfig() {
        return modelConfig;
    }

    public void addTranslucentSplit(String texture, String tBone, String cloneFrom) {
        translucentSplits.computeIfAbsent(texture, k -> new ArrayList<>()).add(new String[]{tBone, cloneFrom});
    }

    public Map<String, List<String[]>> getTranslucentSplits() {
        return translucentSplits;
    }
}
