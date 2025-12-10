package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnimationController {

    private JsonObject json;
    private Entity entity;

    public static final String CONTROLLER_TEMPLATE =
            """
                   {
                       "initial_state": "stop",
                        "states": {
                             "play": {
                                  "animations": [
                                       "%anim%"
                                  ],
                                  "blend_transition": 0.1,
                                  "transitions": [{ "stop": "%query% == 0"}]
                             },
                             "stop": {
                                  "blend_transition": 0.1,
                                  "transitions": [{ "play": "%query% != 0"}]
                             }
                        }
                     }
                   """;

    public void load(GeyserModelEngineExtension extension, Animation animation, Entity entity) {
        JsonObject root = new JsonObject();
        json = root;
        root.addProperty("format_version", "1.10.0");

        JsonObject animationControllers = new JsonObject();
        root.add("animation_controllers", animationControllers);

        List<String> sorted = new ArrayList<>(animation.getAnimationIds());
        int i = 0;

        Collections.sort(sorted);
        for (String id : sorted) {
            id = id.replace(" ", "_");
            int n = (int) Math.pow(2, (i % 24));

            JsonObject controller = JsonParser.parseString(CONTROLLER_TEMPLATE.replace("%anim%", id).replace("%query%", "math.mod(math.floor(query.property('" +  extension.getConfigManager().getConfig().getString("models.namespace") + ":anim" + i / 24 + "') / " + n + "), 2)")).getAsJsonObject();
            animationControllers.add("controller.animation." + animation.getModelId() + "." + id, controller);
            i++;
            if (entity != null) {
                boolean blend = entity.getModelConfig().isEnableBlendTransition();

                if (!blend) {
                    for (Map.Entry<String, JsonElement> states : controller.get("states").getAsJsonObject().entrySet()) {
                        states.getValue().getAsJsonObject().remove("blend_transition");
                    }
                }
            }
        }
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public JsonObject getJson() {
        return json;
    }

    public Entity getEntity() {
        return entity;
    }
}
