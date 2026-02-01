package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.zimzaza4.geyserutils.geyser.GeyserUtils;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.TextureData;

import java.util.*;

public class Entity {

    public static final Set<String> REGISTERED_ENTITIES = new HashSet<>();

    private String modelId;
    private JsonObject json;
    private boolean hasHeadAnimation = false;
    private Animation animation;
    private Geometry geometry;
    private RenderController renderController;
    private String path;
    private Map<String, TextureData> textureMap = new HashMap<>();
    private ModelConfig modelConfig;

    public static final String TEMPLATE = """
            {
              "format_version": "1.10.0",
              "minecraft:client_entity": {
                "description": {
                  "identifier": "%namespace%:%entity_id%",
                  "materials": {
                    "default": "%material%",
                    "anim": "entity_alphatest_anim_change_color_one_sided"
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

    public void modify(String namespace) {
        this.json = JsonParser.parseString(TEMPLATE.replace("%namespace%", namespace)
                .replace("%entity_id%", modelId)
                .replace("%geometry%", "geometry.meg_" + modelId)
                .replace("%texture%", "textures/entity/" + modelId)
                .replace("%look_at_target%", modelConfig.isEnableHeadRotation() ? "animation." + modelId + ".look_at_target" : "animation.none")
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
            jsonGeometry.addProperty(modelId, "geometry.meg_" + modelId);
            jsonTextures.addProperty("default", "textures/entity/" + modelId);
        }

        for (String name : textureMap.keySet()) {
            if (name.endsWith("_e")) continue;

            if (modelConfig.getPerTextureUvSize().containsKey(name)) {
                Integer[] size = modelConfig.getPerTextureUvSize().getOrDefault(name, new Integer[]{16, 16});
                String suffix = size[0] + "_" + size[1];

                jsonGeometry.addProperty(modelId + "_" + suffix, "geometry.meg_" + modelId + "_" + suffix);
                jsonTextures.addProperty(name, "textures/entity/" + name);

            }

            String controllerName = name.equals(modelId) ? "controller.render.meg_" + modelId : "controller.render.meg_" + modelId + "_" + name;
            jsonRenderControllers.add(controllerName);
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

        GeyserUtils.registerProperties(id);
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
}
