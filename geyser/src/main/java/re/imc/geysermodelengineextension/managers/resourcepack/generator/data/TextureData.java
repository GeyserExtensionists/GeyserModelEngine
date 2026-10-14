package re.imc.geysermodelengineextension.managers.resourcepack.generator.data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

public class TextureData {

    public static final String TINT_SUFFIX = "_tintlum";

    private final String modelId;
    private final String path;
    private final Set<String> bindingBones;
    private final byte[] image;

    public TextureData(String modelId, String path, Set<String> bindingBones, byte[] image) {
        this.modelId = modelId;
        this.path = path;
        this.bindingBones = bindingBones;
        this.image = image;
    }

    public String getModelId() {
        return modelId;
    }

    public String getPath() {
        return path;
    }

    public Set<String> getBindingBones() {
        return bindingBones;
    }

    public byte[] getImage() {
        return image;
    }

    /**
     * Builds the texture the "_tintblend" render controller draws over a black copy of the tinted bones:
     * white, with the original pixel's brightness as alpha. Blending the tint colour with that alpha gives
     * colour * brightness, like a Java tint (black stays black, shading is kept).
     */
    public static byte[] tintLuminance(byte[] png) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
        if (source == null) return null;

        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                // Same cut-out as the alpha-tested material: pixels under half alpha aren't drawn at all
                int luminance = alpha < 128 ? 0 : Math.round(0.299f * r + 0.587f * g + 0.114f * b);
                result.setRGB(x, y, (luminance << 24) | 0xFFFFFF);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(result, "png", out);
        return out.toByteArray();
    }
}
