package re.imc.geysermodelengineextension.managers.resourcepack.generator;

public class Material {

    public static final String TEMPLATE = """
                    {
                        "materials":{
                            "version":"1.0.0",
                            "entity_alphatest_anim_change_color:entity_alphatest_change_color":{
                                "+defines":[
                                    "USE_UV_ANIM"
                                ]
                            },
                            "entity_change_color_one_sided:entity": {
                                "+defines": [
                                    "USE_OVERLAY",
                                    "USE_COLOR_MASK"
                                ]
                            },
                            "entity_alphatest_change_color_one_sided:entity_change_color_one_sided": {
                                "+defines": [ "ALPHA_TEST" ],
                                "+samplerStates": [
                                    {
                                      "samplerIndex": 1,
                                      "textureWrap": "Repeat"
                                    }
                                ],
                                "msaaSupport": "Both"
                            },
                            "entity_alphatest_anim_change_color_one_sided:entity_alphatest_change_color_one_sided":{
                                "+defines":[
                                    "USE_UV_ANIM"
                                ]
                            }
                        }
                    }
                   """;
}
