package re.imc.geysermodelengineextension.managers.resourcepack.generator.data;

import java.util.Set;

public class TextureData {

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
}
