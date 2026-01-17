package re.imc.geysermodelengineextension.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import re.imc.geysermodelengineextension.GeyserModelEngineExtension;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static List<File> getAllFiles(File folder, String fileType) {
        List<File> files = new ArrayList<>();
        if (folder == null || !folder.exists()) return files;

        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getAllFiles(file, fileType));
            } else if (file.getName().endsWith(fileType)) {
                files.add(file);
            }
        }

        return files;
    }

    public static void createFiles(GeyserModelEngineExtension extension, String fileName) {
        Path config = extension.dataFolder().resolve(fileName);
        if (Files.exists(config)) return;

        try {
            Path parentDirectory = config.getParent();
            if (parentDirectory != null && !Files.exists(parentDirectory)) Files.createDirectories(parentDirectory);

            try (InputStream resourceAsStream = extension.getClass().getClassLoader().getResourceAsStream("Extension/" + fileName)) {
                if (resourceAsStream == null) {
                    extension.logger().warning(fileName + " is invalid!");
                    return;
                }

                Files.copy(resourceAsStream, config);
            } catch (IOException err) {
                throw new RuntimeException(err);
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    public static JsonObject getJsonObject(File file) {
        try (FileReader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
