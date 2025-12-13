package re.imc.geysermodelengine.managers.model.propertyhandler;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Player;
import org.joml.Vector3fc;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;

public class ModelEnginePropertyHandler implements PropertyHandler {

    private final GeyserModelEngine plugin;

    public ModelEnginePropertyHandler(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendScale(EntityData modelData, Collection<Player> players, float lastScale, boolean firstSend) {
        try {
            if (players.isEmpty()) return;

            ModelEngineEntityData modelEngineEntityData = (ModelEngineEntityData) modelData;

            Vector3fc scale = modelEngineEntityData.getActiveModel().getScale();

            float average = (scale.x() + scale.y() + scale.z()) / 3;

            if (!firstSend) {
                if (average == lastScale) return;
            }

            players.forEach(player -> EntityUtils.sendCustomScale(player, modelEngineEntityData.getEntity().getEntityId(), average));
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void sendColor(EntityData entityData, Collection<Player> players, Color lastColor, boolean firstSend) {
        if (players.isEmpty()) return;

        ModelEngineEntityData data = (ModelEngineEntityData) entityData;
        Color color = calculateCurrentColor(data);

        if (!firstSend && color.equals(lastColor)) return;

        players.forEach(player -> EntityUtils.sendCustomColor(player, data.getEntity().getEntityId(), color));
    }

    @Override
    public void sendHitBox(EntityData entityData, Player player) {
        ModelEngineEntityData modelEngineEntityData = (ModelEngineEntityData) entityData;

        float w = 0;

        if (modelEngineEntityData.getActiveModel().isShadowVisible()) {
            if (modelEngineEntityData.getActiveModel().getModelRenderer() instanceof DisplayRenderer displayRenderer) {
                //    w = displayRenderer.getHitbox().getShadowRadius().get();
            }
        }

        EntityUtils.sendCustomHitBox(player, modelEngineEntityData.getEntity().getEntityId(), 0.02f, w);
    }

    @Override
    public void updateEntityProperties(EntityData entityData, Collection<Player> players, boolean firstSend, String... forceAnims) {
        ModelEngineEntityData model = (ModelEngineEntityData) entityData;

        int entity = model.getEntity().getEntityId();
        Set<String> forceAnimSet = Set.of(forceAnims);

        Map<String, Boolean> boneUpdates = new LinkedHashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();
        Set<String> anims = new HashSet<>();

        model.getActiveModel().getBlueprint().getBones().forEach((s, bone) -> processBone(model, bone, boneUpdates));

        AnimationHandler handler = model.getActiveModel().getAnimationHandler();
        Set<String> priority = model.getActiveModel().getBlueprint().getAnimationDescendingPriority();
        for (String animId : priority) {
            if (handler.isPlayingAnimation(animId)) {
                BlueprintAnimation anim = model.getActiveModel().getBlueprint().getAnimations().get(animId);

                anims.add(animId);
                if (anim.isOverride() && anim.getLoopMode() == BlueprintAnimation.LoopMode.ONCE) {
                    break;
                }

                plugin.getLogger().info(animId);
            }
        }

        for (String id : priority) {
            if (anims.contains(id)) {
                animUpdates.put(id, true);
            } else {
                animUpdates.put(id, false);
            }
        }

        Set<String> lastPlayed = new HashSet<>(model.getEntityTask().getLastPlayedAnim().asMap().keySet());

        for (Map.Entry<String, Boolean> anim : animUpdates.entrySet()) {
            if (anim.getValue()) {
                model.getEntityTask().getLastPlayedAnim().put(anim.getKey(), true);
            }
        }

        for (String anim : lastPlayed) {
            animUpdates.put(anim, true);
        }

        if (boneUpdates.isEmpty() && animUpdates.isEmpty()) return;

        Map<String, Integer> intUpdates = new HashMap<>();

        int i = 0;
        for (Integer integer : BooleanPacker.mapBooleansToInts(boneUpdates)) {
            intUpdates.put(plugin.getConfigManager().getConfig().getString("models.namespace") + ":bone" + i, integer);
            i++;
        }

        i = 0;
        for (Integer integer : BooleanPacker.mapBooleansToInts(animUpdates)) {
            intUpdates.put(plugin.getConfigManager().getConfig().getString("models.namespace") + ":anim" + i, integer);
            i++;
        }

        if (!firstSend) {
            if (intUpdates.equals(model.getEntityTask().getLastIntSet())) {
                return;
            } else {
                model.getEntityTask().getLastIntSet().clear();
                model.getEntityTask().getLastIntSet().putAll(intUpdates);
            }
        }

        if (plugin.getConfigManager().getConfig().getBoolean("options.debug")) plugin.getLogger().info(animUpdates.toString());

        players.forEach(player -> EntityUtils.sendIntProperties(player, entity, intUpdates));
    }

    private void processBone(ModelEngineEntityData model, BlueprintBone bone, Map<String, Boolean> map) {
        String name = unstripName(bone).toLowerCase();
        if (name.equals("hitbox") || name.equals("shadow") || name.equals("mount") || name.startsWith("p_") || name.startsWith("b_") || name.startsWith("ob_")) return;

        bone.getChildren().values().forEach(child -> processBone(model, child, map));

        ModelBone activeBone = model.getActiveModel().getBones().get(bone.getName());

        boolean visible = false;
        if (activeBone != null) visible = activeBone.isVisible();

        map.put(name, visible);
    }

    public String unstripName(BlueprintBone bone) {
        String name = bone.getName();
        if (bone.getBehaviors().get("head") != null) {
            if (!bone.getBehaviors().get("head").isEmpty()) return "hi_" + name;
            return "h_" + name;
        }

        return name;
    }

    private Color calculateCurrentColor(ModelEngineEntityData modelEngineEntityData) {
        if (modelEngineEntityData.getActiveModel().isMarkedHurt()) return new Color(modelEngineEntityData.getActiveModel().getDamageTint().asARGB());
        return new Color(modelEngineEntityData.getActiveModel().getDefaultTint().asARGB());
    }
}
