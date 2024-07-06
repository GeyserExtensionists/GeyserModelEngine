package re.imc.geysermodelengine.model;

import com.google.common.collect.Sets;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import me.zimzaza4.geyserutils.spigot.api.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ModelEntity {

    public static Map<Integer, Map<ActiveModel, ModelEntity>> ENTITIES = new ConcurrentHashMap<>();

    public static Map<Integer, ModelEntity> MODEL_ENTITIES = new ConcurrentHashMap<>();

    private LivingEntity entity;
    private BukkitEntity controllerEntity;

    private final Set<Player> viewers = Sets.newConcurrentHashSet();

    private final ModeledEntity modeledEntity;

    private final ActiveModel activeModel;

    private EntityTask task;

    private ModelEntity(ModeledEntity modeledEntity, ActiveModel model) {
        this.modeledEntity = modeledEntity;
        this.activeModel = model;
        this.entity = spawnEntity();
        runEntityTask();
    }

    public void teleportToModel() {
        Location location = modeledEntity.getBase().getLocation();
        /*
        location.setPitch(modeledEntity.getXHeadRot());
        location.setYaw(modeledEntity.getYHeadRot());
        for (Player viewer : viewers) {
            viewer.sendActionBar("X:" + modeledEntity.getXHeadRot() + ", Y:" + modeledEntity.getYHeadRot());
        }

         */
        Vector vector = modeledEntity.getBase().getMoveController().getVelocity();
        ModelEngineAPI.getEntityHandler().setPosition(entity, location.getX(), location.getY(), location.getZ());
        // ModelEngineAPI.getEntityHandler().movePassenger(entity, location.getX(), location.getY(), location.getZ());
        controllerEntity.getMoveController().setVelocity(vector.getX(), vector.getY(), vector.getZ());
        if (modeledEntity.getBase() instanceof BukkitEntity bukkitEntity && bukkitEntity.getOriginal() instanceof LivingEntity livingEntity) {
            controllerEntity.getLookController().setHeadYaw(livingEntity.getEyeLocation().getYaw());
            controllerEntity.getLookController().setPitch(livingEntity.getEyeLocation().getPitch());
            controllerEntity.getLookController().setBodyYaw(livingEntity.getBodyYaw());
        }

    }
    public static ModelEntity create(ModeledEntity entity, ActiveModel model) {
        ModelEntity modelEntity = new ModelEntity(entity, model);
        int id = entity.getBase().getEntityId();
        Map<ActiveModel, ModelEntity> map = ENTITIES.computeIfAbsent(id, k -> new HashMap<>());
        for (Map.Entry<ActiveModel, ModelEntity> entry : map.entrySet()) {
            if (entry.getKey() !=  model && entry.getKey().getBlueprint().getName().equals(model.getBlueprint().getName())) {
                return null;
            }
        }
        map.put(model, modelEntity);

        return modelEntity;
    }

    public LivingEntity spawnEntity() {
        ModelEntity model = this;
        // int lastEntityId = ReflectionManager.getNewEntityId();
        // System.out.println("RID:" + entityId);
        GeyserModelEngine.getInstance().setSpawningModelEntity(true);
        GeyserModelEngine.getInstance().setCurrentModel(model);
        entity = (LivingEntity) modeledEntity.getBase().getLocation().getWorld().spawnEntity(modeledEntity.getBase().getLocation(), GeyserModelEngine.getInstance().getModelEntityType());
        controllerEntity = new BukkitEntity(entity);
        return entity;
    }

    public void runEntityTask() {
        task = new EntityTask(this);
        task.run(GeyserModelEngine.getInstance(), 0);
    }


    public void applyFeatures(LivingEntity display, String name) {
        display.setGravity(false);
        display.setMaxHealth(2048);
        display.setHealth(2048);
        display.setMetadata("model_entity", new FixedMetadataValue(GeyserModelEngine.getInstance(), true));

        //display.setInvulnerable(true);

        display.setAI(false);
        display.setSilent(true);
        display.setPersistent(false);

        // armorStand.setVisible(false);

        /*
        String uuid = UUID.randomUUID().toString();
        MobDisguise disguise = new MobDisguise(DisguiseType.getType(entity.getType()));
        disguise.setDisguiseName(uuid);

        DisguiseAPI.disguiseEntity(display, disguise);

         */

    }


}
