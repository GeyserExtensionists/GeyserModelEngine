# GeyserModelEngine 
# About

Thanks to [Willem](https://github.com/OmeWillem/GeyserUtils) for adding the following features:
- Part Visibility
- Color support
- Scaling support
- & more

# How To Install

Download the following plugins according to what server software you use.

| plugins                        | Link                                                                 | effect                          |
| :---                           | :----                                                                | :---                            |
| GeyserUtils                    | [Github](https://github.com/OmeWillem/GeyserUtils)                    | Get your Geyser to support calling some BE stuff  |
| GeyserModelEngine              | [Github](https://github.com/OmeWillem/GeyserModelEngine)              | Make your bedrock support MEG4                            |
| GeyserModelEnginePackGenerator | [Github](https://github.com/OmeWillem/GeyserModelEnginePackGenerator) | Help you automatically transform the model to generate resource packs        |

- Put `GeyserModelEngine` in the plugins folder (only Spigot or forks of Spigot supported)
- Put either `geyserutils-spigot` in your plugins folder aswell (`geyserutils-velocity` in your Velocity plugins folder if you use it)
- Put `GeyserModelEnginePackGenerator` and `geyserutils-geyser` into `plugins/[Geyser-Folder]/extensions`

Start the server to generate the relevant configuration files, and then shut down the server to convert any models.

# Convert Models

`GeyserModelEnginePackGenerator` is capable of generating models all by itself. After generating it will also apply this pack automatically.

- First go to `plugins/[Geyser-Folder]/extensions/geysermodelenginepackgenerator/input/`
- Create a folder in this directory with the ID of the model. (this is the same name as your model within ModelEngine 4.)

<img src="docsimg/example.jpg" width="500">

> Each model should have a separate model folder

- Now use BlockBench and convert your model to a Bedrock Entity, this will allow you to export the Bedrock Geometry and Animations.
- Put the geometry, animations and texture file in this folder you've made.

> The new version of BlockBench exports the bedrock model format_version as `1.21.0`.
> You need to change it to `1.12.0` manually.
> Otherwise your client will NOT see the model.

<img src="docsimg/example1.jpg" width="500">

- Restart the server or reload geyser to start generating the resource pack.
- Go to  `plugins/[Geyser-Folder]/extensions/geysermodelenginepackgenerator`, and you should see your pack generated!

<img src="docsimg/example2.jpg" width="500">

- Final step, reload Geyser or restart the server to load the resource pack.
- Congratulations, you've completed this tutorial!

# Tips

* Pay attention! The pack only regenerates when the number of models changes, you can technically speaking remove the generated_pack folder to force a reload aswell.
* You do not have to manually put the pack into the packs folder of Geyser, the extension is capable of loading the pack itself.

# Current issues

* Multi-textures are not supported
* Please report any bugs (discord: https://discord.gg/NNNaUdAbpP)

# FAQ

### Where can I contact you?
You can contact me on our Discord: https://discord.gg/NNNaUdAbpP
