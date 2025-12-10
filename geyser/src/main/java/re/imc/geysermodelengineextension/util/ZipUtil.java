package re.imc.geysermodelengineextension.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void compressFolder(File folder, String folderName, ZipOutputStream zipOutputStream) throws IOException {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (folderName == null) {
                        compressFolder(file, file.getName(), zipOutputStream);
                        continue;
                    }

                    compressFolder(file, folderName + "/" + file.getName(), zipOutputStream);
                } else {
                    if (folderName == null) {
                        addToZipFile(file.getName(), file, zipOutputStream);
                        continue;
                    }

                    addToZipFile(folderName + "/" + file.getName(), file, zipOutputStream);
                }
            }
        }
    }

    private static void addToZipFile(String fileName, File file, ZipOutputStream zipOutputStream) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zipOutputStream.putNextEntry(entry);

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }
        }

        zipOutputStream.closeEntry();
    }
}
