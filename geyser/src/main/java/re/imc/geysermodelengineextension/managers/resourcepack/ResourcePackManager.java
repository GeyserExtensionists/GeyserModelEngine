package re.imc.geysermodelengineextension.managers.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.*;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.TextureData;
import re.imc.geysermodelengineextension.util.ZipUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {

    private final GeyserModelEngineExtension extension;

    private final File inputFolder;
    private final File generatedPack;

    private Path generatedPackZipPath;

    private final HashMap<String, Entity> entityCache = new HashMap<>();
    private final HashMap<String, Animation> animationCache = new HashMap<>();
    private final HashMap<String, Geometry> geometryCache = new HashMap<>();
    private final HashMap<String, Map<String, TextureData>> textureCache = new HashMap<>();

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ResourcePackManager(GeyserModelEngineExtension extension) {
        this.extension = extension;

        this.inputFolder = extension.dataFolder().resolve("input").toFile();
        this.inputFolder.mkdirs();

        this.generatedPack = extension.dataFolder().resolve("generated_pack").toFile();
    }

    public void loadPack() {
        generateResourcePack(inputFolder, generatedPack);

        generatedPackZipPath = extension.dataFolder().resolve("generated_pack.zip");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(generatedPackZipPath))) {
            ZipUtil.compressFolder(generatedPack, null, zipOutputStream);
        } catch (IOException err) {
            throw new RuntimeException(err);
        }

        for (Entity entity : entityCache.values()) {
            entity.register(extension.getConfigManager().getConfig().getString("models.namespace"));
        }
    }

    private void generateResourcePack(File inputFolder, File output) {
        generateFromFolder("", inputFolder, true);

        File animationsFolder = new File(output, "animations");
        File entityFolder = new File(output, "entity");
        File modelsFolder = new File(output, "models/entity");
        File texturesFolder = new File(output, "textures/entity");
        File animationControllersFolder = new File(output, "animation_controllers");
        File renderControllersFolder = new File(output, "render_controllers");
        File materialsFolder = new File(output, "materials");

        File manifestFile = new File(output, "manifest.json");

        output.mkdirs();
        if (!manifestFile.exists()) {
            try {
                Files.writeString(manifestFile.toPath(), PackManifest.generate(), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        animationsFolder.mkdirs();
        entityFolder.mkdirs();
        modelsFolder.mkdirs();
        texturesFolder.mkdirs();
        animationControllersFolder.mkdirs();
        renderControllersFolder.mkdirs();
        materialsFolder.mkdirs();

        File materialFile = new File(materialsFolder, "entity.material");

        if (!materialFile.exists()) {
            try {
                Files.writeString(materialFile.toPath(), Material.TEMPLATE, StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        for (Map.Entry<String, Animation> entry : animationCache.entrySet()) {
            Entity entity = entityCache.get(entry.getKey());
            Geometry geo = geometryCache.get(entry.getKey());

            if (geo != null) entry.getValue().addHeadBind(geo);

            Path path = animationsFolder.toPath().resolve(entry.getValue().getPath() + entry.getKey() + ".animation.json");
            Path pathController = animationControllersFolder.toPath().resolve(entry.getValue().getPath() + entry.getKey() + ".animation_controllers.json");

            pathController.toFile().getParentFile().mkdirs();
            path.toFile().getParentFile().mkdirs();

            if (path.toFile().exists()) continue;

            AnimationController controller = new AnimationController();
            controller.load(extension, entry.getValue(), entity);

            try {
                Files.writeString(path, GSON.toJson(entry.getValue().getJson()), StandardCharsets.UTF_8);
                Files.writeString(pathController, controller.getJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        for (Map.Entry<String, Geometry> entry : geometryCache.entrySet()) {
            entry.getValue().modify();
            Path path = modelsFolder.toPath().resolve(entry.getValue().getPath() + entry.getKey() + ".geo.json");
            path.toFile().getParentFile().mkdirs();
            String id = entry.getValue().getGeometryId();

            Entity entity = entityCache.get(entry.getKey());
            if (entity != null) {
                ModelConfig modelConfig = entity.getModelConfig();
                if (!modelConfig.getPerTextureUvSize().isEmpty()) {
                    for (Map.Entry<String, TextureData> textureEntry : entity.getTextureMap().entrySet()) {
                        String name = textureEntry.getKey();

                        Integer[] size = modelConfig.getPerTextureUvSize().getOrDefault(name, new Integer[]{16, 16});
                        String suffix = size[0] + "_" + size[1];
                        entry.getValue().setTextureWidth(size[0]);
                        entry.getValue().setTextureHeight(size[1]);
                        path = modelsFolder.toPath().resolve(entry.getValue().getPath() + entry.getKey() + "_" + suffix + ".geo.json");

                        entry.getValue().setId(id + "_" + suffix);

                        if (path.toFile().exists()) continue;

                        try {
                            Files.writeString(path, GSON.toJson(entry.getValue().getJson()), StandardCharsets.UTF_8);
                        } catch (IOException err) {
                            throw new RuntimeException(err);
                        }
                    }
                }
            }

            if (path.toFile().exists()) continue;

            try {
                Files.writeString(path, GSON.toJson(entry.getValue().getJson()), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        for (Map.Entry<String, Map<String, TextureData>> textures : textureCache.entrySet()) {
            for (Map.Entry<String, TextureData> entry : textures.getValue().entrySet()) {
                Path path = texturesFolder.toPath().resolve(entry.getValue().getPath() + textures.getKey() + "/" + entry.getKey() + ".png");
                path.toFile().getParentFile().mkdirs();

                if (path.toFile().exists()) continue;

                try {
                    if (entry.getValue().getImage() != null) Files.write(path, entry.getValue().getImage());
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }
            }
        }

        for (Map.Entry<String, Entity> entry : entityCache.entrySet()) {
            Entity entity = entry.getValue();
            entity.modify(extension.getConfigManager().getConfig().getString("models.namespace"));

            Path entityPath = entityFolder.toPath().resolve(entity.getPath() + entry.getKey() + ".entity.json");
            entityPath.toFile().getParentFile().mkdirs();

            if (entityPath.toFile().exists()) continue;

            try {
                Files.writeString(entityPath, entity.getJson().toString(), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }

            // render controller part

            String id = entity.getModelId();
            if (!geometryCache.containsKey(id)) continue;
            RenderController controller = new RenderController(id, geometryCache.get(id).getBones(), entity);
            entity.setRenderController(controller);
            Path renderPath = new File(renderControllersFolder, "controller.render." + id + ".json").toPath();
            if (renderPath.toFile().exists()) continue;

            try {
                Files.writeString(renderPath, controller.generate(extension.getConfigManager().getConfig().getString("models.namespace")), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }
    }

    public void generateFromFolder(String currentPath, File folder, boolean root) {
        if (folder.listFiles() == null) return;

        String modelId = root ? "" : folder.getName().toLowerCase();

        Entity entity = new Entity(modelId);
        ModelConfig modelConfig = new ModelConfig();
        boolean shouldOverrideConfig = false;
        File textureConfigFile = new File(folder, "config.json");

        if (textureConfigFile.exists()) {
            try {
                modelConfig = GSON.fromJson(Files.readString(textureConfigFile.toPath()), ModelConfig.class);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        boolean canAdd = false;
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) generateFromFolder(currentPath + (root ? "" : folder.getName() + "/"), file, false);

            if (file.getName().endsWith(".zip")) {
                try {
                    generateFromZip(currentPath, file.getName().replace(".zip", "").toLowerCase(Locale.ROOT), new ZipFile(file));
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }
            }

            if (entityCache.containsKey(modelId)) continue;

            if (file.getName().endsWith(".png")) {
                String textureName = file.getName().replace(".png", "");
                Set<String> bindingBones = new HashSet<>();
                bindingBones.add("*");
                if (modelConfig.getBingingBones().containsKey(textureName)) bindingBones = modelConfig.getBingingBones().get(textureName);

                Map<String, TextureData> map = textureCache.computeIfAbsent(modelId, s -> new HashMap<>());
                try {
                    map.put(textureName, new TextureData(modelId, currentPath, bindingBones, Files.readAllBytes(file.toPath())));
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }

                entity.setTextureMap(map);
                if (modelConfig.getBingingBones().isEmpty()) {
                    modelConfig.getBingingBones().put(textureName, Set.of("*"));
                    shouldOverrideConfig = true;
                }
            }

            if (file.getName().endsWith(".json")) {
                try {
                    String json = Files.readString(file.toPath());
                    if (isAnimationFile(json)) {
                        Animation animation = new Animation();
                        animation.setPath(currentPath);
                        animation.setModelId(modelId);

                        animation.load(json);
                        animationCache.put(modelId, animation);
                        entity.setAnimation(animation);
                    }

                    if (isGeometryFile(json)) {
                        Geometry geometry = new Geometry();
                        geometry.load(json);
                        geometry.setPath(currentPath);
                        geometry.setModelId(modelId);
                        geometryCache.put(modelId, geometry);
                        entity.setGeometry(geometry);
                        canAdd = true;
                    }
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }
            }
        }

        if (canAdd) {
            // old config
            File oldConfig = new File(folder, "config.properties");
            Properties old = new Properties();
            try {
                if (oldConfig.exists()) {
                    old.load(new FileReader(oldConfig));
                    modelConfig.setMaterial(old.getProperty("material", "entity_alphatest_change_color"));
                    modelConfig.setEnableBlendTransition(Boolean.parseBoolean(old.getProperty("blend-transition", "true")));
                    modelConfig.setEnableHeadRotation(Boolean.parseBoolean(old.getProperty("head-rotation", "true")));
                    shouldOverrideConfig = true;
                    oldConfig.delete();
                }
            } catch (IOException err) {
                throw new RuntimeException(err);
            }

            if (shouldOverrideConfig) {
                try {
                    Files.writeString(textureConfigFile.toPath(), GSON.toJson(modelConfig));
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }
            }

            entity.setModelConfig(modelConfig);
            entity.setPath(currentPath);
            entityCache.put(modelId, entity);
        }
    }

    public void generateFromZip(String currentPath, String modelId, ZipFile zip) {
        Entity entity = new Entity(modelId);
        if (entityCache.containsKey(modelId)) return;

        ModelConfig modelConfig = new ModelConfig();
        ZipEntry textureConfigFile = null;

        for (Iterator<? extends ZipEntry> it = zip.entries().asIterator(); it.hasNext(); ) {
            ZipEntry entry = it.next();
            if (entry.getName().endsWith("config.json")) {
                textureConfigFile = entry;
            }
        }

        if (textureConfigFile != null) {
            try {
                modelConfig = GSON.fromJson(new InputStreamReader(zip.getInputStream(textureConfigFile)), ModelConfig.class);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        }

        boolean canAdd = false;
        for (Iterator<? extends ZipEntry> it = zip.entries().asIterator(); it.hasNext(); ) {
            ZipEntry e = it.next();
            if (e.getName().endsWith(".png")) {
                String[] path = e.getName().split("/");
                String textureName = path[path.length - 1].replace(".png", "");
                Set<String> bindingBones = new HashSet<>();
                bindingBones.add("*");

                if (modelConfig.getBingingBones().containsKey(textureName)) {
                    bindingBones = modelConfig.getBingingBones().get(textureName);
                }

                Map<String, TextureData> map = textureCache.computeIfAbsent(modelId, s -> new HashMap<>());
                try {
                    map.put(textureName, new TextureData(modelId, currentPath, bindingBones, zip.getInputStream(e).readAllBytes()));
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }

                entity.setTextureMap(map);
                if (modelConfig.getBingingBones().isEmpty()) modelConfig.getBingingBones().put(textureName, Set.of("*"));
            }

            if (e.getName().endsWith(".json")) {
                try {
                    InputStream stream = zip.getInputStream(e);
                    String json = new String(stream.readAllBytes());
                    if (isAnimationFile(json)) {
                        Animation animation = new Animation();
                        animation.setPath(currentPath);
                        animation.setModelId(modelId);

                        animation.load(json);
                        animationCache.put(modelId, animation);
                        entity.setAnimation(animation);
                    }

                    if (isGeometryFile(json)) {
                        Geometry geometry = new Geometry();
                        geometry.load(json);
                        geometry.setPath(currentPath);
                        geometry.setModelId(modelId);
                        geometryCache.put(modelId, geometry);
                        entity.setGeometry(geometry);
                        canAdd = true;
                    }
                } catch (IOException err) {
                    throw new RuntimeException(err);
                }
            }
        }

        if (canAdd) {
            entity.setModelConfig(modelConfig);
            entity.setPath(currentPath);
            entityCache.put(modelId, entity);
        }
    }

    private boolean isGeometryFile(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject().has("minecraft:geometry");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isAnimationFile(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject().has("animations");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public File getInputFolder() {
        return inputFolder;
    }

    public Path getGeneratedPackZipPath() {
        return generatedPackZipPath;
    }

    public HashMap<String, Entity> getEntityCache() {
        return entityCache;
    }

    public HashMap<String, Animation> getAnimationCache() {
        return animationCache;
    }

    public HashMap<String, Geometry> getGeometryCache() {
        return geometryCache;
    }

    public HashMap<String, Map<String, TextureData>> getTextureCache() {
        return textureCache;
    }
}
