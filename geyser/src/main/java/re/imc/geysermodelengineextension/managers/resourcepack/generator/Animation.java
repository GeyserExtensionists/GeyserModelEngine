package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Animation {

    private String modelId;
    private JsonObject json;
    private Set<String> animationIds = new HashSet<>();

    private String path;

    public static final String HEAD_TEMPLATE = """
             {
               "relative_to" : {
                 "rotation" : "entity"
               },
               "rotation" : [ "query.target_x_rotation - this", "query.target_y_rotation - this", 0.0 ]
            }
            """;

    public void load(String string) {
        this.json = JsonParser.parseString(string).getAsJsonObject();
        JsonObject newAnimations = new JsonObject();

        for (Map.Entry<String, JsonElement> element : json.get("animations").getAsJsonObject().entrySet()) {
            animationIds.add(element.getKey());
            JsonObject animation = element.getValue().getAsJsonObject();

            if (animation.has("override_previous_animation")) {
                if (animation.get("override_previous_animation").getAsBoolean()) {
                    if (!animation.has("loop")) {
                        animation.addProperty("loop", "hold_on_last_frame");
                        // play once but override must use this to avoid strange anim
                    }
                }

                animation.remove("override_previous_animation");
            }

            if (animation.has("loop")) {
                if (animation.get("loop").getAsJsonPrimitive().isString()) {
                    if (animation.get("loop").getAsString().equals("hold_on_last_frame")) {
                        if (!animation.has("bones")) {
                            continue;
                        }
                        for (Map.Entry<String, JsonElement> bone : animation.get("bones").getAsJsonObject().entrySet()) {

                            for (Map.Entry<String, JsonElement> anim : bone.getValue().getAsJsonObject().entrySet()) {
                                float max = -1;
                                JsonObject end = null;
                                if (!anim.getValue().isJsonObject()) {
                                    continue;
                                }
                                try {
                                    for (Map.Entry<String, JsonElement> timeline : anim.getValue().getAsJsonObject().entrySet()) {
                                        float time = Float.parseFloat(timeline.getKey());
                                        if (time > max) {
                                            max = time;
                                            if (timeline.getValue().isJsonObject()) {
                                                end = timeline.getValue().getAsJsonObject();
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                                if (end != null && end.has("lerp_mode") && end.get("lerp_mode").getAsString().equals("catmullrom")) {
                                    end.addProperty("lerp_mode", "linear");
                                }
                            }
                        }
                    }
                }
            }

            newAnimations.add("animation." + modelId + "." + element.getKey().replace(" ", "_"), element.getValue());
        }

        json.add("animations", newAnimations);
    }

    public void addHeadBind(Geometry geometry) {
        JsonObject object = new JsonObject();
        object.addProperty("loop", true);
        JsonObject bones = new JsonObject();
        JsonArray array = geometry.getInternal().get("bones").getAsJsonArray();

        int i = 0;

        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                String name = element.getAsJsonObject().get("name").getAsString();

                String parent = "";
                if (element.getAsJsonObject().has("parent")) parent = element.getAsJsonObject().get("parent").getAsString();
                if (parent.startsWith("h_") || parent.startsWith("hi_")) continue;

                if (name.startsWith("h_") || name.startsWith("hi_")) {
                    bones.add(name, JsonParser.parseString(HEAD_TEMPLATE));
                    i++;
                }
            }
        }

        if (i == 0) return;

        GeyserModelEngineExtension.getExtension().getResourcePackManager().getEntityCache().get(modelId).setHasHeadAnimation(true);

        object.add("bones", bones);
        json.get("animations").getAsJsonObject().add("animation." + modelId + ".look_at_target", object);
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

    public void setAnimationIds(Set<String> animationIds) {
        this.animationIds = animationIds;
    }

    public String getModelId() {
        return modelId;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public JsonObject getJson() {
        return json;
    }

    public Set<String> getAnimationIds() {
        return animationIds;
    }

    public String getPath() {
        return path;
    }
}
