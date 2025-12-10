package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ModelConfig {

    @SerializedName("head_rotation")
    boolean enableHeadRotation = true;
    @SerializedName("material")
    String material = "entity_alphatest_change_color_one_sided";
    @SerializedName("blend_transition")
    boolean enableBlendTransition = true;
    @SerializedName("binding_bones")
    Map<String, Set<String>> bingingBones = new HashMap<>();
    @SerializedName("anim_textures")
    Map<String, AnimTextureOptions> animTextures = new HashMap<>();
    @SerializedName("texture_materials")
    Map<String, String> textureMaterials = new HashMap<>();
    @SerializedName("per_texture_uv_size")
    Map<String, Integer[]> perTextureUvSize;
    @SerializedName("disable_part_visibility")
    boolean disablePartVisibility = true;

    public void setEnableHeadRotation(boolean enableHeadRotation) {
        this.enableHeadRotation = enableHeadRotation;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setEnableBlendTransition(boolean enableBlendTransition) {
        this.enableBlendTransition = enableBlendTransition;
    }

    public void setBingingBones(Map<String, Set<String>> bingingBones) {
        this.bingingBones = bingingBones;
    }

    public void setAnimTextures(Map<String, AnimTextureOptions> animTextures) {
        this.animTextures = animTextures;
    }

    public void setTextureMaterials(Map<String, String> textureMaterials) {
        this.textureMaterials = textureMaterials;
    }

    public void setPerTextureUvSize(Map<String, Integer[]> perTextureUvSize) {
        this.perTextureUvSize = perTextureUvSize;
    }

    public void setDisablePartVisibility(boolean disablePartVisibility) {
        this.disablePartVisibility = disablePartVisibility;
    }

    public Map<String, String> getTextureMaterials() {
        return textureMaterials != null ? textureMaterials : Map.of();
    }

    public Map<String, Integer[]> getPerTextureUvSize() {
        return perTextureUvSize != null ? perTextureUvSize : Map.of();
    }

    public boolean isEnableHeadRotation() {
        return enableHeadRotation;
    }

    public String getMaterial() {
        return material;
    }

    public boolean isEnableBlendTransition() {
        return enableBlendTransition;
    }

    public Map<String, Set<String>> getBingingBones() {
        return bingingBones;
    }

    public Map<String, AnimTextureOptions> getAnimTextures() {
        return animTextures;
    }

    public boolean isDisablePartVisibility() {
        return disablePartVisibility;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class AnimTextureOptions {
        float fps;
        int frames;
    }
}
