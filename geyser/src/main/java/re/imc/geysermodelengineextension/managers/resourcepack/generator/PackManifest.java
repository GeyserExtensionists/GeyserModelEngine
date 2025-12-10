package re.imc.geysermodelengineextension.managers.resourcepack.generator;

import java.util.UUID;

public class PackManifest {

    public static final String TEMPLATE = """
            {
              "format_version": 2,
              "header": {
                "name": "GeyserModelEngine",
                "description": "GeyserModelEngine For Geyser",
                "uuid": "%uuid-1%",
                "version": [0, 0, 1],
                "min_engine_version": [1, 21, 100]
              },
              "modules": [
                {
                  "type": "resources",
                  "description": "GeyserModelEngine",
                  "uuid": "%uuid-2%",
                  "version": [0, 0, 1]
                }
              ]
            }
            """;

    public static String generate() {
        return TEMPLATE.replace("%uuid-1%", UUID.randomUUID().toString())
                .replace("%uuid-2%", UUID.randomUUID().toString());
    }
}
