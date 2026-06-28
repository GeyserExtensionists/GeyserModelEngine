package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import re.imc.geysermodelengineextension.managers.resourcepack.generator.data.TextureData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TranslucencySplitter {

    public static final int EMPTY = 0;
    public static final int GLASS = 1;
    public static final int OPAQUE = 2;

    public static BufferedImage decode(byte[] image) {
        if (image == null) return null;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(image));
            if (img == null || !img.getColorModel().hasAlpha()) return null;
            return img;
        } catch (IOException e) {
            return null;
        }
    }

    public static double[] scale(JsonObject description, Integer[] uvSize, BufferedImage img) {
        double texW = img.getWidth();
        double texH = img.getHeight();
        if (uvSize != null && uvSize.length == 2 && uvSize[0] > 0 && uvSize[1] > 0) {
            texW = uvSize[0];
            texH = uvSize[1];
        } else if (description != null) {
            if (description.has("texture_width") && description.get("texture_width").getAsDouble() > 0) {
                texW = description.get("texture_width").getAsDouble();
            }
            if (description.has("texture_height") && description.get("texture_height").getAsDouble() > 0) {
                texH = description.get("texture_height").getAsDouble();
            }
        }
        return new double[]{img.getWidth() / texW, img.getHeight() / texH};
    }

    public static int cubeClass(JsonObject cube, BufferedImage img, double sx, double sy, double threshold) {
        long[] counts = new long[2];
        countCube(cube, img, sx, sy, counts);
        long visible = counts[0] + counts[1];
        if (visible == 0) return EMPTY;
        return (double) counts[0] / visible >= threshold ? GLASS : OPAQUE;
    }

    private static void countCube(JsonObject cube, BufferedImage img, double sx, double sy, long[] counts) {
        JsonElement uv = cube.get("uv");
        if (uv == null) return;
        if (uv.isJsonObject()) {
            for (Map.Entry<String, JsonElement> face : uv.getAsJsonObject().entrySet()) {
                if (!face.getValue().isJsonObject()) continue;
                JsonObject fd = face.getValue().getAsJsonObject();
                if (!fd.has("uv") || !fd.has("uv_size")) continue;
                JsonArray o = fd.get("uv").getAsJsonArray();
                JsonArray s = fd.get("uv_size").getAsJsonArray();
                countFace(img, o.get(0).getAsDouble(), o.get(1).getAsDouble(),
                        s.get(0).getAsDouble(), s.get(1).getAsDouble(), sx, sy, counts);
            }
        } else if (uv.isJsonArray() && cube.has("size")) {
            JsonArray off = uv.getAsJsonArray();
            JsonArray size = cube.get("size").getAsJsonArray();
            double u = off.get(0).getAsDouble(), v = off.get(1).getAsDouble();
            double dx = Math.abs(size.get(0).getAsDouble());
            double dy = Math.abs(size.get(1).getAsDouble());
            double dz = Math.abs(size.get(2).getAsDouble());
            double[][] faces = {
                    {u + dz, v, dx, dz},
                    {u + dz + dx, v, dx, dz},
                    {u, v + dz, dz, dy},
                    {u + dz, v + dz, dx, dy},
                    {u + dz + dx, v + dz, dz, dy},
                    {u + dz + dx + dz, v + dz, dx, dy},
            };
            for (double[] f : faces) {
                countFace(img, f[0], f[1], f[2], f[3], sx, sy, counts);
            }
        }
    }

    private static void countFace(BufferedImage img, double u, double v, double w, double h,
                                  double sx, double sy, long[] counts) {
        int x0 = Math.max(0, (int) Math.floor(Math.min(u, u + w) * sx));
        int x1 = Math.min(img.getWidth(), (int) Math.ceil(Math.max(u, u + w) * sx));
        int y0 = Math.max(0, (int) Math.floor(Math.min(v, v + h) * sy));
        int y1 = Math.min(img.getHeight(), (int) Math.ceil(Math.max(v, v + h) * sy));
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a == 0) continue;
                if (a < 255) counts[0]++;
                else counts[1]++;
            }
        }
    }

    public static void split(Geometry geometry, Entity entity, double threshold) {
        JsonObject description = geometry.getInternal().get("description").getAsJsonObject();
        JsonArray bones = geometry.getInternal().get("bones").getAsJsonArray();

        Map<String, JsonObject> byName = new HashMap<>();
        for (JsonElement element : bones) {
            byName.put(element.getAsJsonObject().get("name").getAsString().toLowerCase(), element.getAsJsonObject());
        }

        for (Map.Entry<String, TextureData> texture : entity.getTextureMap().entrySet()) {
            String key = texture.getKey();
            if (key.endsWith("_e")) continue;
            BufferedImage img = decode(texture.getValue().getImage());
            if (img == null) continue;

            Integer[] uvSize = entity.getModelConfig().getPerTextureUvSize().get(key);
            double[] sc = scale(description, uvSize, img);
            double sx = sc[0], sy = sc[1];

            Set<String> uvBonesId = entity.getModelConfig().getBingingBones().get(key);
            boolean allBones = uvBonesId == null || uvBonesId.contains("*");
            Set<String> bound = new HashSet<>();
            if (uvBonesId != null) {
                for (String b : uvBonesId) bound.add(b.toLowerCase());
            }

            List<JsonObject> added = new ArrayList<>();
            for (JsonObject bone : new ArrayList<>(byName.values())) {
                String name = bone.get("name").getAsString();
                if (name.endsWith("_t")) continue;
                if (!allBones && !bound.contains(name.toLowerCase())) continue;
                if (!bone.has("cubes")) continue;

                JsonArray glass = new JsonArray();
                JsonArray rest = new JsonArray();
                int opaque = 0;
                for (JsonElement cube : bone.get("cubes").getAsJsonArray()) {
                    int type = cubeClass(cube.getAsJsonObject(), img, sx, sy, threshold);
                    if (type == GLASS) {
                        glass.add(cube);
                    } else {
                        rest.add(cube);
                        if (type == OPAQUE) opaque++;
                    }
                }
                if (glass.isEmpty() || opaque == 0) continue;

                String tName = name + "_t";
                if (byName.containsKey(tName.toLowerCase())) continue;

                bone.add("cubes", rest);
                JsonObject tBone = new JsonObject();
                tBone.addProperty("name", tName);
                tBone.addProperty("parent", name);
                if (bone.has("pivot")) tBone.add("pivot", bone.get("pivot"));
                tBone.add("cubes", glass);
                added.add(tBone);
                byName.put(tName.toLowerCase(), tBone);
                entity.addTranslucentSplit(key, tName, name);
            }
            for (JsonObject t : added) {
                bones.add(t);
            }
        }
    }
}
