package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Bedrock side of ModelEngine's manual bone animator (ModelBone#setManualAnimator, e.g. the MythicMobs
 * animatemodel mechanic) on a model's root bone. The plugin sends the pose as int properties:
 * manual_on (0/1), manual_rx/ry/rz (Bedrock euler degrees x100) and manual_px/py/pz (Bedrock pixels x100).
 * This animation applies them to the root bone and replaces the bone's other animations while manual_on is 1,
 * like ModelEngine's override.
 */
public class ManualAnimator {

    public static final String BONE = "root";
    public static final String ANIMATION = "manual_root";
    public static final List<String> PROPERTIES = List.of("manual_on", "manual_rx", "manual_ry", "manual_rz", "manual_px", "manual_py", "manual_pz");

    public static String animationId(String modelId) {
        return "animation." + modelId + "." + ANIMATION;
    }

    public static String active(String namespace) {
        return "query.property('" + namespace + ":manual_on') == 1";
    }

    public static JsonObject generate(String namespace, String modelId) {
        JsonObject bone = new JsonObject();
        bone.add("rotation", vector(namespace, "manual_rx", "manual_ry", "manual_rz"));
        bone.add("position", vector(namespace, "manual_px", "manual_py", "manual_pz"));

        JsonObject bones = new JsonObject();
        bones.add(BONE, bone);

        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.addProperty("override_previous_animation", true);
        animation.add("bones", bones);

        JsonObject animations = new JsonObject();
        animations.add(animationId(modelId), animation);

        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", animations);
        return root;
    }

    private static JsonArray vector(String namespace, String x, String y, String z) {
        JsonArray vector = new JsonArray();
        vector.add("query.property('" + namespace + ":" + x + "') / 100");
        vector.add("query.property('" + namespace + ":" + y + "') / 100");
        vector.add("query.property('" + namespace + ":" + z + "') / 100");
        return vector;
    }
}
