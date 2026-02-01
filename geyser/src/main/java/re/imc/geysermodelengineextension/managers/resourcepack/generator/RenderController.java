package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.BoneData;

import java.util.*;

public class RenderController {

    public static final Set<String> NEED_REMOVE_WHEN_SORT = Set.of("pbody_", "plarm_", "prarm_", "plleg_", "prleg_", "phead_", "p_");

    private final String modelId;
    private final Map<String, BoneData> bones;
    private final Entity entity;

    public RenderController(String modelId, Map<String, BoneData> bones, Entity entity) {
        this.modelId = modelId;
        this.bones = bones;
        this.entity = entity;
    }

    // look, I'm fine with your other code and stuff, but I ain't using templates for JSON lmao
    public String generate(String namespace) {
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
                controller.addProperty("geometry", "Geometry." + modelId + "_" + suffix);
            } else {
                controller.addProperty("geometry", "Geometry." + modelId);
            }

            JsonArray materials = new JsonArray();
            String material = entity.getModelConfig().getTextureMaterials().get(key);

            JsonObject materialItem = new JsonObject();
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
            } else {
                materialItem.addProperty("*", "Material.default");
            }
            materials.add(materialItem);
            controller.add("materials", materials);

            JsonArray textures = new JsonArray();
            if (singleTexture) {
                textures.add("Texture.default");
            } else {
                textures.add("Texture." + key);
            }

            controller.add("textures", textures);

            // if (enable) {
            JsonArray partVisibility = new JsonArray();
            JsonObject visibilityDefault = new JsonObject();
            visibilityDefault.addProperty("*", false);
            partVisibility.add(visibilityDefault);
            int i = 0;
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
                    if (!uvBonesId.contains("*")) {
                        processedBones.add(bone);
                    }
                }
                if (!boneName.startsWith("uv_")) {
                    i++;
                }
            }
            controller.add("part_visibility", partVisibility);
            //}
        }

        return root.toString();
    }

}
