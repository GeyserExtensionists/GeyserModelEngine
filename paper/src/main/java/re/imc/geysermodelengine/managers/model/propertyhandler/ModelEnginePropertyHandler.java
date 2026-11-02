package re.imc.geysermodelengine.managers.model.propertyhandler;

import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.handler.IStateMachineHandler;
import com.ticxo.modelengine.api.animation.keyframe.KeyframeType;
import com.ticxo.modelengine.api.animation.keyframe.KeyframeTypes;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.generator.blueprint.BlueprintBone;
import com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint;
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
import java.util.List;

public class ModelEnginePropertyHandler implements PropertyHandler {

    private static final int UNTINTED = 0xFFFFFF;
    private static final String MANUAL_ANIMATOR_BONE = "root";
    private static final List<KeyframeType<?, ?>> BONE_CHANNELS = List.of(KeyframeTypes.POSITION, KeyframeTypes.ROTATION, KeyframeTypes.SCALE);

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

            modelEngineEntityData.getEntityTask().setLastScale(average);
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

        data.getEntityTask().setLastColor(color);
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

        model.getActiveModel().getBlueprint().getBones().forEach((s, bone) -> processBone(model, bone, boneUpdates, boneTints));

        ModelBlueprint blueprint = model.getActiveModel().getBlueprint();
        Set<String> anims = shownAnimations(blueprint, model.getActiveModel().getAnimationHandler());

        for (String id : blueprint.getAnimationDescendingPriority()) {
            animUpdates.put(id, anims.contains(id));
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

    /**
     * The playing animations ModelEngine draws. An animation in override (from the blueprint or forced at runtime)
     * in its play phase hides every weaker animation on the bones and channels it keys. ModelEngine does that per
     * bone and channel while Bedrock only toggles whole animations and stacks those it plays, so an animation is left
     * out once anything it keys is hidden, and kept when it shares nothing with the overriding ones.
     */
    private Set<String> shownAnimations(ModelBlueprint blueprint, AnimationHandler handler) {
        List<IAnimationProperty> playing = strongestFirst(blueprint, handler);
        Set<String> shown = new HashSet<>();
        if (playing.stream().noneMatch(IAnimationProperty::isOverride)) {
            playing.forEach(property -> shown.add(property.getName()));
            return shown;
        }

        Map<UUID, Set<KeyframeType<?, ?>>> hidden = new HashMap<>();
        for (IAnimationProperty property : playing) {
            Map<UUID, Set<KeyframeType<?, ?>>> keyed = keyedChannels(property);
            if (keyed.entrySet().stream().allMatch(entry -> Collections.disjoint(hidden.getOrDefault(entry.getKey(), Set.of()), entry.getValue()))) {
                shown.add(property.getName());
            }
            if (property.isOverride() && property.getPhase() == IAnimationProperty.Phase.PLAY) {
                keyed.forEach((bone, channels) -> hidden.computeIfAbsent(bone, uuid -> new HashSet<>()).addAll(channels));
            }
        }
        return shown;
    }

    /**
     * The playing animations, strongest first. With the state machine handler a higher layer is drawn over a lower
     * one, one animation per layer, and getAnimations() keys them "layer:name"; the priority handler follows the
     * blueprint's animation order.
     */
    private List<IAnimationProperty> strongestFirst(ModelBlueprint blueprint, AnimationHandler handler) {
        if (handler instanceof IStateMachineHandler) {
            TreeMap<Integer, IAnimationProperty> layers = new TreeMap<>(Comparator.reverseOrder());
            for (Map.Entry<String, IAnimationProperty> entry : handler.getAnimations().entrySet()) {
                String key = entry.getKey();
                int colon = key.indexOf(':');
                if (colon <= 0 || !key.substring(0, colon).chars().allMatch(Character::isDigit)) {
                    layers = null;
                    break;
                }
                layers.put(Integer.parseInt(key.substring(0, colon)), entry.getValue());
            }
            if (layers != null) return new ArrayList<>(layers.values());
        }

        List<IAnimationProperty> playing = new ArrayList<>();
        for (String id : blueprint.getAnimationDescendingPriority()) {
            IAnimationProperty property = handler.isPlayingAnimation(id) ? handler.getAnimation(id) : null;
            if (property != null) playing.add(property);
        }
        return playing;
    }

    private Map<UUID, Set<KeyframeType<?, ?>>> keyedChannels(IAnimationProperty property) {
        Map<UUID, Set<KeyframeType<?, ?>>> keyed = new HashMap<>();
        for (UUID bone : property.getBlueprintAnimation().getTimelines().keySet()) {
            for (KeyframeType<?, ?> channel : BONE_CHANNELS) {
                if (property.containsKeyframe(channel, bone)) keyed.computeIfAbsent(bone, uuid -> new HashSet<>()).add(channel);
            }
        }
        return keyed;
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
