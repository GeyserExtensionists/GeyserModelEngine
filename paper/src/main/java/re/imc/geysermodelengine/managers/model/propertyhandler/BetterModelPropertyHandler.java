package re.imc.geysermodelengine.managers.model.propertyhandler;

import kr.toxicity.model.api.animation.AnimationIterator;
import kr.toxicity.model.api.bone.RenderedBone;
import kr.toxicity.model.api.data.blueprint.BlueprintAnimation;
import kr.toxicity.model.api.data.renderer.RenderPipeline;
import kr.toxicity.model.api.nms.ModelDisplay;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.BetterModelEntityData;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BetterModelPropertyHandler implements PropertyHandler {

    private final GeyserModelEngine plugin;

    public BetterModelPropertyHandler(GeyserModelEngine plugin) {
        this.plugin = plugin;
    }

    // Figure out on how to get the scale from BetterModel
    @Override
    public void sendScale(EntityData entityData, Collection<Player> players, float lastScale, boolean firstSend) {
        BetterModelEntityData betterModelEntityData = (BetterModelEntityData) entityData;
    }

    @Override
    public void sendColor(EntityData entityData, Collection<Player> players, Color lastColor, boolean firstSend) {
        if (players.isEmpty()) return;

        BetterModelEntityData betterModelEntityData = (BetterModelEntityData) entityData;

        Color color = new Color(0xFFFFFF);
        if (betterModelEntityData.isHurt()) color = new Color(betterModelEntityData.getEntityTracker().damageTintValue());

        if (firstSend) {
            if (color.equals(lastColor)) return;
        }

        for (Player player : players) {
            EntityUtils.sendCustomColor(player, betterModelEntityData.getEntity().getEntityId(), color);
        }

        betterModelEntityData.setHurt(false);
    }

    @Override
    public void sendHitBox(EntityData entityData, Player player) {
        BetterModelEntityData betterModelEntityData = (BetterModelEntityData) entityData;

        float w = 0;

        EntityUtils.sendCustomHitBox(player, betterModelEntityData.getEntity().getEntityId(), 0.02f, w);
    }

    @Override
    public void updateEntityProperties(EntityData entityData, Collection<Player> players, boolean firstSend, String... forceAnims) {
        BetterModelEntityData model = (BetterModelEntityData) entityData;

        int entity = model.getEntity().getEntityId();
        Set<String> forceAnimSet = Set.of(forceAnims);

        Map<String, Boolean> boneUpdates = new HashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();
        Set<String> anims = new HashSet<>();

        model.getTracker().bones().forEach(bone -> processBone(model, bone, boneUpdates));

        RenderPipeline handler = model.getTracker().getPipeline();

        for (RenderedBone renderedBone : handler.bones()) {
            if (model.getTracker().bone(renderedBone.name()).runningAnimation() != null) {
                BlueprintAnimation anim = model.getTracker().renderer().animations().get(renderedBone.runningAnimation().name());

                anims.add(renderedBone.runningAnimation().name());
                if (anim.override() && anim.loop() == AnimationIterator.Type.PLAY_ONCE) {
                    break;
                }
            }
        }

        for (String id : handler.getParent().animations().keySet()) {
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

        for (String anim : lastPlayed) animUpdates.put(anim, true);

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

        List<String> list = new ArrayList<>(boneUpdates.keySet());
        Collections.sort(list);

        players.forEach(player -> EntityUtils.sendIntProperties(player, entity, intUpdates));
    }

    public String unstripName(RenderedBone bone) {
        @NotNull String name = bone.name().rawName();

        if (name.equals("head")) {
            if (!bone.getChildren().isEmpty()) return "hi_" + name;
            return "h_" + name;
        }

        return name;
    }

    private void processBone(BetterModelEntityData entityData, RenderedBone bone, Map<String, Boolean> map) {
        String name = unstripName(bone).toLowerCase();
        if (name.equals("hitbox") || name.equals("shadow") || name.equals("mount") || name.startsWith("p_") || name.startsWith("b_") || name.startsWith("ob_")) return;

        for (RenderedBone renderedBone : bone.getChildren().values()) {
            processBone(entityData, renderedBone, map);
        }

        RenderedBone activeBone = entityData.getTracker().bone(bone.name());

        ModelDisplay modelDisplay = activeBone.getDisplay();
        if (modelDisplay == null) return;
        boolean visible = activeBone.getDisplay().invisible();

        map.put(name, visible);
    }
}
