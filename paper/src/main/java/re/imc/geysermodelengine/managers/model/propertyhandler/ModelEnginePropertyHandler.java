package re.imc.geysermodelengine.managers.model.propertyhandler;

import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import com.ticxo.modelengine.api.model.bone.SimpleManualAnimator;
import com.ticxo.modelengine.api.model.render.DisplayRenderer;
import me.zimzaza4.geyserutils.spigot.api.EntityUtils;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import re.imc.geysermodelengine.GeyserModelEngine;
import re.imc.geysermodelengine.managers.model.entity.EntityData;
import re.imc.geysermodelengine.managers.model.entity.ModelEngineEntityData;
import re.imc.geysermodelengine.util.BooleanPacker;

import java.awt.*;
import java.util.*;

public class ModelEnginePropertyHandler implements PropertyHandler {

    private static final int UNTINTED = 0xFFFFFF;
    private static final String MANUAL_ANIMATOR_BONE = "root";

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

        Map<String, Boolean> boneUpdates = new LinkedHashMap<>();
        Map<String, Integer> boneTints = new HashMap<>();
        Map<String, Boolean> animUpdates = new HashMap<>();
        Set<String> anims = new HashSet<>();

        model.getActiveModel().getBlueprint().getBones().forEach((s, bone) -> processBone(model, bone, boneUpdates, boneTints));

        AnimationHandler handler = model.getActiveModel().getAnimationHandler();
        Set<String> priority = model.getActiveModel().getBlueprint().getAnimationDescendingPriority();
        for (String animId : priority) {
            if (handler.isPlayingAnimation(animId)) {
                BlueprintAnimation anim = model.getActiveModel().getBlueprint().getAnimations().get(animId);

                anims.add(animId);
                if (anim.isOverride() && anim.getLoopMode() == BlueprintAnimation.LoopMode.ONCE) {
                    break;
                }
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

        putTintProperties(model, boneTints, intUpdates);
        putManualAnimatorProperties(model, intUpdates);

        if (!firstSend) {
            if (intUpdates.equals(model.getEntityTask().getLastIntSet())) {
                return;
            } else {
                model.getEntityTask().getLastIntSet().clear();
                model.getEntityTask().getLastIntSet().putAll(intUpdates);
            }
        }

        if (plugin.getConfigManager().getConfig().getBoolean("options.debug.animations")) plugin.getLogger().info(animUpdates.toString());

        players.forEach(player -> EntityUtils.sendIntProperties(player, entity, intUpdates));
    }

    private void processBone(ModelEngineEntityData model, BlueprintBone bone, Map<String, Boolean> map, Map<String, Integer> tintMap) {
        String name = unstripName(bone).toLowerCase();
        if (name.equals("hitbox") || name.equals("shadow") || name.equals("mount") || name.startsWith("p_") || name.startsWith("b_") || name.startsWith("ob_")) return;

        bone.getChildren().values().forEach(child -> processBone(model, child, map, tintMap));

        ModelBone activeBone = model.getActiveModel().getBones().get(bone.getName());

        boolean visible = false;
        int tint = UNTINTED;
        if (activeBone != null) {
            visible = activeBone.isVisible();
            tint = activeBone.getDefaultTint().asRGB();
        }

        map.put(name, visible);
        tintMap.put(name, tint);
    }

    /**
     * Bedrock can only colour a whole render controller, so the resource pack draws tinted bones a second time
     * through a "_tint" controller. This sends which bones it should draw (one bit per bone, same order as the
     * bone visibility properties) and the colour to draw them in.
     * Bedrock shows one tint colour per model: if bones use different colours, the most common one wins.
     */
    private void putTintProperties(ModelEngineEntityData model, Map<String, Integer> boneTints, Map<String, Integer> intUpdates) {
        String namespace = plugin.getConfigManager().getConfig().getString("models.namespace");
        String colorKey = namespace + ":tintcolor";

        Map<Integer, Integer> tintCounts = new HashMap<>();
        boneTints.values().forEach(tint -> {
            if (tint != UNTINTED) tintCounts.merge(tint, 1, Integer::sum);
        });

        // Untinted models never send tint properties, unless they need resetting after being tinted
        if (tintCounts.isEmpty() && !model.getEntityTask().getLastIntSet().containsKey(colorKey)) return;

        int tint = tintCounts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(UNTINTED);

        Map<String, Boolean> tintUpdates = new HashMap<>();
        boneTints.forEach((bone, boneTint) -> tintUpdates.put(bone, tint != UNTINTED && boneTint == tint));

        int i = 0;
        for (Integer integer : BooleanPacker.mapBooleansToInts(tintUpdates)) {
            intUpdates.put(namespace + ":tintmask" + i, integer);
            i++;
        }

        intUpdates.put(colorKey, packTint(tint));

        if (plugin.getConfigManager().getConfig().getBoolean("options.debug.tints")) {
            boolean changed = intUpdates.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(namespace + ":tint"))
                    .anyMatch(entry -> !entry.getValue().equals(model.getEntityTask().getLastIntSet().get(entry.getKey())));
            if (changed) {
                Set<String> tintedBones = new TreeSet<>();
                tintUpdates.forEach((bone, tinted) -> {
                    if (tinted) tintedBones.add(bone);
                });
                plugin.getLogger().info("Tint " + model.getActiveModel().getBlueprint().getName() + ": #" + String.format("%06X", tint) + " on " + tintedBones + " of " + new TreeSet<>(boneTints.keySet()));
            }
        }
    }

    /**
     * Packs an RGB colour into 18 bits (6 per channel). GeyserUtils caps int properties at ±1,000,000, so 24-bit
     * RGB doesn't fit. The resource pack unpacks it with Molang.
     */
    private int packTint(int rgb) {
        int r = Math.round(((rgb >> 16) & 0xFF) * 63 / 255f);
        int g = Math.round(((rgb >> 8) & 0xFF) * 63 / 255f);
        int b = Math.round((rgb & 0xFF) * 63 / 255f);
        return (r << 12) | (g << 6) | b;
    }

    /**
     * Forwards a {@link SimpleManualAnimator} on the model's root bone (e.g. the MythicMobs animatemodel mechanic),
     * which Bedrock otherwise never sees because only playing animations are sent. The resource pack applies it with
     * an animation on the root bone that replaces the bone's other animations while manual_on is 1.
     * Rotation is sent as Bedrock euler angles (degrees x100), position in Bedrock pixels (x100).
     */
    private void putManualAnimatorProperties(ModelEngineEntityData model, Map<String, Integer> intUpdates) {
        String namespace = plugin.getConfigManager().getConfig().getString("models.namespace");
        String activeKey = namespace + ":manual_on";

        ModelBone root = model.getActiveModel().getBones().get(MANUAL_ANIMATOR_BONE);
        boolean active = root != null && root.getManualAnimator() instanceof SimpleManualAnimator;

        // Models without a manual animator never send these, unless they need resetting after having one
        if (!active && !model.getEntityTask().getLastIntSet().containsKey(activeKey)) return;

        Vector3f rotation = new Vector3f();
        Vector3f position = new Vector3f();
        if (active) {
            SimpleManualAnimator animator = (SimpleManualAnimator) root.getManualAnimator();
            // ModelEngine builds the rotation as Rz * Ry * Rx; Bedrock bones use the same order
            animator.getRotation().getEulerAnglesZYX(rotation).mul((float) Math.toDegrees(1));
            position.set(animator.getPosition()).mul(16);
        }

        // ModelEngine's model space is Blockbench's turned 180 degrees about Y, and Bedrock flips Blockbench's
        // rotation x/y and position x on export, which leaves these signs
        intUpdates.put(activeKey, active ? 1 : 0);
        intUpdates.put(namespace + ":manual_rx", Math.round(rotation.x * 100));
        intUpdates.put(namespace + ":manual_ry", Math.round(-rotation.y * 100));
        intUpdates.put(namespace + ":manual_rz", Math.round(-rotation.z * 100));
        intUpdates.put(namespace + ":manual_px", Math.round(position.x * 100));
        intUpdates.put(namespace + ":manual_py", Math.round(position.y * 100));
        intUpdates.put(namespace + ":manual_pz", Math.round(-position.z * 100));
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
