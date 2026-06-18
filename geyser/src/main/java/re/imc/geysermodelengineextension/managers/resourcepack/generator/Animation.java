package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

        boolean bakeCatmullrom = GeyserModelEngineExtension.getExtension().getConfigManager()
                .getConfig().getBoolean("options.resource-pack.bake-catmullrom-to-linear", false);
        double sampleStep = GeyserModelEngineExtension.getExtension().getConfigManager()
                .getConfig().getDouble("options.resource-pack.catmullrom-sample-step");
        if (sampleStep <= 0) sampleStep = 0.05;

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

            if (bakeCatmullrom) {
                bakeCatmullromToLinear(animation, sampleStep);
            }

            newAnimations.add("animation." + modelId + "." + element.getKey().replace(" ", "_"), element.getValue());
        }

        json.add("animations", newAnimations);
    }

    private void bakeCatmullromToLinear(JsonObject animation, double step) {
        if (!animation.has("bones")) return;

        boolean isLooping = false;
        if (animation.has("loop")) {
            JsonElement loopElem = animation.get("loop");
            if (loopElem.isJsonPrimitive() && loopElem.getAsJsonPrimitive().isBoolean()) {
                isLooping = loopElem.getAsBoolean();
            }
        }

        double maxTime = 0;
        if (animation.has("animation_length")) {
            maxTime = animation.get("animation_length").getAsDouble();
        }

        JsonObject bones = animation.get("bones").getAsJsonObject();
        for (Map.Entry<String, JsonElement> boneEntry : bones.entrySet()) {
            if (!boneEntry.getValue().isJsonObject()) continue;
            JsonObject bone = boneEntry.getValue().getAsJsonObject();

            for (String channelName : new String[]{"position", "rotation", "scale"}) {
                if (!bone.has(channelName)) continue;
                JsonElement channelElem = bone.get(channelName);
                if (!channelElem.isJsonObject()) continue;

                JsonObject channel = channelElem.getAsJsonObject();
                if (!hasCatmullromKeyframes(channel)) continue;

                List<double[]> keyframes = extractKeyframes(channel);
                if (keyframes == null) continue;

                if (keyframes.size() < 2) {
                    stripCatmullrom(channel);
                    continue;
                }

                double channelMaxTime = maxTime;
                if (channelMaxTime <= 0) {
                    channelMaxTime = keyframes.get(keyframes.size() - 1)[0];
                }
                if (channelMaxTime <= 0) continue;

                bone.add(channelName, bakeChannel(keyframes, channelMaxTime, isLooping, step));
            }
        }
    }

    private void stripCatmullrom(JsonObject channel) {
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                JsonObject kf = entry.getValue().getAsJsonObject();
                if (kf.has("lerp_mode") && "catmullrom".equals(kf.get("lerp_mode").getAsString())) {
                    double[] values = extractValues(kf);
                    if (values != null) {
                        JsonArray arr = new JsonArray();
                        arr.add(values[0]);
                        arr.add(values[1]);
                        arr.add(values[2]);
                        channel.add(entry.getKey(), arr);
                    } else {
                        kf.remove("lerp_mode");
                    }
                }
            }
        }
    }

    private boolean hasCatmullromKeyframes(JsonObject channel) {
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                JsonObject kf = entry.getValue().getAsJsonObject();
                if (kf.has("lerp_mode") && "catmullrom".equals(kf.get("lerp_mode").getAsString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<double[]> extractKeyframes(JsonObject channel) {
        List<double[]> keyframes = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : channel.entrySet()) {
            double time;
            try {
                time = Double.parseDouble(entry.getKey());
            } catch (NumberFormatException e) {
                return null;
            }

            double[] values = extractValues(entry.getValue());
            if (values == null) return null;

            keyframes.add(new double[]{time, values[0], values[1], values[2]});
        }
        keyframes.sort(Comparator.comparingDouble(a -> a[0]));
        return keyframes;
    }

    private double[] extractValues(JsonElement elem) {
        if (elem.isJsonArray()) {
            return jsonArrayToDoubles(elem.getAsJsonArray());
        }
        if (elem.isJsonObject()) {
            JsonObject obj = elem.getAsJsonObject();
            if (obj.has("post")) {
                return extractValues(obj.get("post"));
            }
            if (obj.has("vector")) {
                return extractValues(obj.get("vector"));
            }
        }
        if (elem.isJsonPrimitive()) {
            if (elem.getAsJsonPrimitive().isNumber()) {
                double v = elem.getAsDouble();
                return new double[]{v, v, v};
            }
            return null;
        }
        return null;
    }

    private double[] jsonArrayToDoubles(JsonArray arr) {
        if (arr.size() < 3) return null;
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            JsonElement e = arr.get(i);
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) {
                return null;
            }
            result[i] = e.getAsDouble();
        }
        return result;
    }

    private JsonObject bakeChannel(List<double[]> keyframes, double maxTime, boolean isLooping, double step) {
        double firstTime = keyframes.get(0)[0];
        double lastTime = keyframes.get(keyframes.size() - 1)[0];

        boolean shouldWrap = isLooping && firstTime < 0.01 && Math.abs(lastTime - maxTime) < 0.01;

        List<double[]> samples = new ArrayList<>();
        for (double t = 0; t <= maxTime + 0.001; t += step) {
            double time = Math.min(t, maxTime);
            double[] interpolated;

            if (time <= firstTime) {
                interpolated = new double[]{keyframes.get(0)[1], keyframes.get(0)[2], keyframes.get(0)[3]};
            } else if (time >= lastTime) {
                interpolated = new double[]{keyframes.get(keyframes.size() - 1)[1], keyframes.get(keyframes.size() - 1)[2], keyframes.get(keyframes.size() - 1)[3]};
            } else {
                int p1Idx = 0;
                for (int i = 0; i < keyframes.size(); i++) {
                    if (keyframes.get(i)[0] <= time) {
                        p1Idx = i;
                    }
                }
                int p2Idx = p1Idx + 1;

                double segStart = keyframes.get(p1Idx)[0];
                double segEnd = keyframes.get(p2Idx)[0];
                double segLen = segEnd - segStart;
                double localT = segLen > 0 ? (time - segStart) / segLen : 0;

                double[] p0, p1, p2, p3;
                p1 = new double[]{keyframes.get(p1Idx)[1], keyframes.get(p1Idx)[2], keyframes.get(p1Idx)[3]};
                p2 = new double[]{keyframes.get(p2Idx)[1], keyframes.get(p2Idx)[2], keyframes.get(p2Idx)[3]};

                if (p1Idx > 0) {
                    p0 = new double[]{keyframes.get(p1Idx - 1)[1], keyframes.get(p1Idx - 1)[2], keyframes.get(p1Idx - 1)[3]};
                } else if (shouldWrap) {
                    int lastIdx = keyframes.size() - 1;
                    p0 = new double[]{keyframes.get(lastIdx)[1], keyframes.get(lastIdx)[2], keyframes.get(lastIdx)[3]};
                } else {
                    p0 = p1.clone();
                }

                if (p2Idx < keyframes.size() - 1) {
                    p3 = new double[]{keyframes.get(p2Idx + 1)[1], keyframes.get(p2Idx + 1)[2], keyframes.get(p2Idx + 1)[3]};
                } else if (shouldWrap) {
                    p3 = new double[]{keyframes.get(0)[1], keyframes.get(0)[2], keyframes.get(0)[3]};
                } else {
                    p3 = p2.clone();
                }

                interpolated = catmullromInterpolate(p0, p1, p2, p3, localT);
            }

            samples.add(new double[]{
                Math.round(time * 10000.0) / 10000.0,
                Math.round(interpolated[0] * 10000.0) / 10000.0,
                Math.round(interpolated[1] * 10000.0) / 10000.0,
                Math.round(interpolated[2] * 10000.0) / 10000.0
            });
        }

        double tolerance = 0.05;
        List<double[]> reduced = new ArrayList<>();
        reduced.add(samples.get(0));

        for (int i = 1; i < samples.size() - 1; i++) {
            double[] prev = reduced.get(reduced.size() - 1);
            double[] curr = samples.get(i);
            double[] next = samples.get(i + 1);

            double dt = next[0] - prev[0];
            if (dt <= 0) continue;
            double ratio = (curr[0] - prev[0]) / dt;

            boolean needed = false;
            for (int c = 1; c <= 3; c++) {
                double linear = prev[c] + (next[c] - prev[c]) * ratio;
                if (Math.abs(linear - curr[c]) > tolerance) {
                    needed = true;
                    break;
                }
            }
            if (needed) {
                reduced.add(curr);
            }
        }

        reduced.add(samples.get(samples.size() - 1));

        JsonObject result = new JsonObject();
        for (double[] s : reduced) {
            String timeKey = String.format(Locale.ROOT, "%.4f", s[0]).replaceAll("0+$", "").replaceAll("\\.$", "");
            JsonArray values = new JsonArray();
            values.add(s[1]);
            values.add(s[2]);
            values.add(s[3]);
            result.add(timeKey, values);
        }

        return result;
    }

    private double[] catmullromInterpolate(double[] p0, double[] p1, double[] p2, double[] p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = 0.5 * (
                (2 * p1[i]) +
                (-p0[i] + p2[i]) * t +
                (2 * p0[i] - 5 * p1[i] + 4 * p2[i] - p3[i]) * t2 +
                (-p0[i] + 3 * p1[i] - 3 * p2[i] + p3[i]) * t3
            );
        }
        return result;
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
