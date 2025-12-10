package re.imc.geysermodelengineextension.managers.resourcepack.generator.data;

import java.util.Set;

public class BoneData {

   private final String name;
   private final String parent;
   private final Set<BoneData> children;
   private final Set<BoneData> allChildren;

   public BoneData(String name, String parent, Set<BoneData> children, Set<BoneData> allChildren) {
       this.name = name;
       this.parent = parent;
       this.children = children;
       this.allChildren = allChildren;
   }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public Set<BoneData> getChildren() {
        return children;
    }

    public Set<BoneData> getAllChildren() {
        return allChildren;
    }
}
