package re.imc.geysermodelengine.model;

import com.google.common.collect.Sets;
import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import lombok.Getter;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import re.imc.geysermodelengine.GeyserModelEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

        entity.teleportAsync(location);
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
        map.put(model, modelEntity);

        return modelEntity;
    }

    public LivingEntity spawnEntity() {
        entity = (LivingEntity) modeledEntity.getBase().getLocation().getWorld().spawnEntity(modeledEntity.getBase().getLocation(), GeyserModelEngine.getInstance().getModelEntityType());
        applyFeatures(entity, "model." + activeModel.getBlueprint().getName());
        ModelEntity model = this;
        int id = entity.getEntityId();
        MODEL_ENTITIES.put(id, model);
        controllerEntity = new BukkitEntity(entity);
        return entity;
    }

    public void runEntityTask() {
        task = new EntityTask(this);
        task.run(GeyserModelEngine.getInstance(), 0);
    }


    private void applyFeatures(LivingEntity display, String name) {
        display.setGravity(false);
        display.setMaxHealth(2048);
        display.setHealth(2048);


        //display.setInvulnerable(true);

        display.setAI(false);
        display.setSilent(true);
        display.setPersistent(false);

        // armorStand.setVisible(false);
        String uuid = UUID.randomUUID().toString();
        PlayerDisguise disguise = new PlayerDisguise(name + "_" + uuid);

        DisguiseAPI.disguiseEntity(display, disguise.setNameVisible(false));


    }




}
