package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.BoneData;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.TextureData;
import re.imc.geysermodelengineextension.util.ShortHashUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class RenderController {

    public static final Set<String> NEED_REMOVE_WHEN_SORT = Set.of("pbody_", "plarm_", "prarm_", "plleg_", "prleg_", "phead_", "p_");

    private final String modelId;
    private final Map<String, BoneData> bones;
    private final Entity entity;
    private final Geometry geometry;
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    public RenderController(String modelId, Geometry geometry, Entity entity) {
        this.modelId = modelId;
        this.geometry = geometry;
        this.bones = geometry.getBones();
        this.entity = entity;
    }

    // look, I'm fine with your other code and stuff, but I ain't using templates for JSON lmao
    public String generate(String namespace, boolean hashEnabled) {
        boolean translucency = GeyserModelEngineExtension.getExtension().getConfigManager()
                .getConfig().getBoolean("options.resource-pack.translucent-materials", true);
        double translucentThreshold = GeyserModelEngineExtension.getExtension().getConfigManager()
                .getConfig().getDouble("options.resource-pack.translucent-threshold");
        if (translucentThreshold <= 0) translucentThreshold = 0.5;

        List<String> se = new ArrayList<>(bones.keySet());
        Collections.sort(se);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");

        JsonObject renderControllers = new JsonObject();
        root.add("render_controllers", renderControllers);

        Set<BoneData> processedBones = new HashSet<>();
        boolean singleTexture = entity.getTextureMap().size() == 1 && entity.getModelConfig().getPerTextureUvSize().isEmpty();
        for (String key : entity.getTextureMap().keySet()) {
            if (key.endsWith("_e")) continue;

            // Texture texture = entity.textureMap.get(key);
            Set<String> uvBonesId = entity.getModelConfig().getBingingBones().get(key);

            if (uvBonesId == null) {
                if (!singleTexture) {
                    continue;
                } else {
                    uvBonesId = new HashSet<>();
                    uvBonesId.add("*");
                }
            }
            ModelConfig.AnimTextureOptions anim = entity.getModelConfig().getAnimTextures().get(key);

            JsonObject controller = new JsonObject();

            String controllerName = key.equals(modelId) ? "controller.render.meg_" + modelId : "controller.render.meg_" + modelId + "_" + key;
            renderControllers.add(controllerName, controller);

            if (!entity.getModelConfig().getPerTextureUvSize().isEmpty()) {
                Integer[] size = entity.getModelConfig().getPerTextureUvSize().getOrDefault(key, new Integer[]{16, 16});
                String suffix = size[0] + "_" + size[1];
                if (hashEnabled) {
                    controller.addProperty("geometry", "Geometry." + ShortHashUtil.hashModelId(modelId + "_" + suffix));
                } else {
                    controller.addProperty("geometry", "Geometry." + modelId + "_" + suffix);
                }
            } else {
                if (hashEnabled) {
                    controller.addProperty("geometry", "Geometry." + ShortHashUtil.hashModelId(modelId));
                } else {
                    controller.addProperty("geometry", "Geometry." + modelId);
                }
            }

            JsonArray materials = new JsonArray();
            String material = entity.getModelConfig().getTextureMaterials().get(key);

            JsonObject materialItem = new JsonObject();
            boolean defaultMaterial = false;
            if (material != null) {
                materialItem.addProperty("*", "Material." + material);
            } else if (anim != null) {
                materialItem.addProperty("*", "Material.anim");
                JsonObject uvAnim = new JsonObject();
                controller.add("uv_anim", uvAnim);
                JsonArray offset = new JsonArray();
                offset.add(0.0);
                // Cap fps at reasonable value (7.0 is standard for Bedrock animations)
                double animFps = anim.fps > 60 ? 7.0 : anim.fps;
                offset.add("math.mod(math.floor(q.life_time * " + animFps + ")," + anim.frames + ") / " + anim.frames);
                uvAnim.add("offset", offset);
                JsonArray scale = new JsonArray();
                scale.add(1.0);
                scale.add("1 / " + anim.frames);
                uvAnim.add("scale", scale);
            } else if (translucency && semiFraction(key) >= 0.999) {
                materialItem.addProperty("*", "Material.blend");
            } else {
                materialItem.addProperty("*", "Material.default");
                defaultMaterial = true;
            }
            materials.add(materialItem);
            if (translucency && defaultMaterial) {
                for (String tBone : translucentBones(key, uvBonesId, translucentThreshold)) {
                    JsonObject boneMaterial = new JsonObject();
                    boneMaterial.addProperty(tBone, "Material.blend");
                    materials.add(boneMaterial);
                }
                for (String[] split : entity.getTranslucentSplits().getOrDefault(key, Collections.emptyList())) {
                    JsonObject boneMaterial = new JsonObject();
                    boneMaterial.addProperty(split[0], "Material.blend");
                    materials.add(boneMaterial);
                }
            }
            controller.add("materials", materials);

            JsonArray textures = new JsonArray();
            if (singleTexture) {
                textures.add("Texture.default");
            } else {
                textures.add("Texture." + (hashEnabled ? ShortHashUtil.hashTextureName(modelId, key) : key));
            }

            controller.add("textures", textures);

            // if (enable) {
            JsonArray partVisibility = new JsonArray();
            JsonObject visibilityDefault = new JsonObject();
            visibilityDefault.addProperty("*", false);
            partVisibility.add(visibilityDefault);
            int i = 0;
            Map<String, JsonElement> capturedVisibility = new HashMap<>();
            List<String> sorted = new ArrayList<>(bones.keySet());
            Map<String, String> originalId = new HashMap<>();
            ListIterator<String> iterator = sorted.listIterator();
            while (iterator.hasNext()) {
                String s = iterator.next();
                String o = s;
                for (String r : NEED_REMOVE_WHEN_SORT) {
                    s = s.replace(r, "");
                }
                iterator.set(s);
                originalId.put(s, o);
            }
            Collections.sort(sorted);

            Set<String> uvAllBones = new HashSet<>();
            for (String uvBone : uvBonesId) {
                if (uvBone.equals("*")) {
                    uvAllBones.addAll(bones.keySet());
                }

                if (!bones.containsKey(uvBone.toLowerCase())) continue;

                uvAllBones.add(uvBone.toLowerCase());
            }

            for (String boneName : sorted) {
                boneName = originalId.get(boneName);
                JsonObject visibilityItem = new JsonObject();
                BoneData bone = bones.get(boneName);
                boolean uvParent = false;

                for (BoneData child : bone.getAllChildren()) {
                    if (child.getName().startsWith("uv_")) {
                        if (uvAllBones.contains(child.getName())) {
                            uvParent = true;
                        }
                    }
                }


                for (Map.Entry<String, Set<String>> entry : entity.getModelConfig().getBingingBones().entrySet()) {
                    if (entry.getKey().equals(key)) continue;

                    if (entry.getValue().stream().anyMatch(boneName::equalsIgnoreCase)) {
                        uvParent = false;
                        break;
                    }
                }
                if (!processedBones.contains(bone) && (uvParent || uvAllBones.contains(boneName) || uvBonesId.contains("*"))) {
                    int index = i;
                    if (boneName.startsWith("uv_")) {
                        index = sorted.indexOf(bone.getParent());
                    }

                    int n = (int) Math.pow(2, (index % 24));
                    if (entity.getModelConfig().isDisablePartVisibility()) {
                        visibilityItem.addProperty(boneName, true);
                    } else {
                        visibilityItem.addProperty(boneName, "math.mod(math.floor(query.property('" + namespace + ":bone" + index / 24 + "') / " + n + "), 2) == 1");
                    }
                    partVisibility.add(visibilityItem);
                    capturedVisibility.put(boneName, visibilityItem.get(boneName));
                    if (!uvBonesId.contains("*")) {
                        processedBones.add(bone);
                    }
                }
                if (!boneName.startsWith("uv_")) {
                    i++;
                }
            }
            for (String[] split : entity.getTranslucentSplits().getOrDefault(key, Collections.emptyList())) {
                JsonElement vis = capturedVisibility.get(split[1]);
                if (vis != null) {
                    JsonObject splitVisibility = new JsonObject();
                    splitVisibility.add(split[0], vis.deepCopy());
                    partVisibility.add(splitVisibility);
                }
            }
            controller.add("part_visibility", partVisibility);
            //}
        }

        return root.toString();
    }

    private BufferedImage image(String key) {
        if (imageCache.containsKey(key)) return imageCache.get(key);
        BufferedImage img = null;
        TextureData texture = entity.getTextureMap().get(key);
        if (texture != null && texture.getImage() != null) {
            try {
                img = ImageIO.read(new ByteArrayInputStream(texture.getImage()));
                if (img != null && !img.getColorModel().hasAlpha()) img = null;
            } catch (IOException ignored) {
            }
        }
        imageCache.put(key, img);
        return img;
    }

    private double semiFraction(String key) {
        BufferedImage img = image(key);
        if (img == null) return 0;
        int w = img.getWidth(), h = img.getHeight();
        long semi = 0, visible = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a == 0) continue;
                visible++;
                if (a < 255) semi++;
            }
        }
        return visible == 0 ? 0 : (double) semi / visible;
    }

    private Set<String> translucentBones(String key, Set<String> uvBonesId, double threshold) {
        BufferedImage img = image(key);
        if (img == null) return Collections.emptySet();

        JsonObject description = geometry.getInternal().get("description").getAsJsonObject();
        Integer[] uvSize = entity.getModelConfig().getPerTextureUvSize().get(key);
        double[] sc = TranslucencySplitter.scale(description, uvSize, img);
        double sx = sc[0], sy = sc[1];

        boolean allBones = uvBonesId.contains("*");
        Set<String> bound = new HashSet<>();
        for (String b : uvBonesId) bound.add(b.toLowerCase());

        Set<String> result = new LinkedHashSet<>();
        for (JsonElement element : geometry.getInternal().get("bones").getAsJsonArray()) {
            JsonObject bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            if (name.endsWith("_t")) continue;
            if (!allBones && !bound.contains(name.toLowerCase())) continue;
            if (!bone.has("cubes")) continue;

            boolean anyGlass = false, anyOpaque = false;
            for (JsonElement cube : bone.get("cubes").getAsJsonArray()) {
                int type = TranslucencySplitter.cubeClass(cube.getAsJsonObject(), img, sx, sy, threshold);
                if (type == TranslucencySplitter.GLASS) anyGlass = true;
                else if (type == TranslucencySplitter.OPAQUE) anyOpaque = true;
            }
            if (anyGlass && !anyOpaque) result.add(name);
        }
        return result;
    }

}
