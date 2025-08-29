# GeyserModelEngine 
# About

Thanks to [Willem](https://github.com/OmeWillem) for adding the following features:
- Part Visibility
- Color support
- Scaling support
- & more

# How To Install

Download the following plugins according to what server software you use.

| plugins                        | Link                                                                 | effect                          |
| :---                           | :----                                                                | :---                            |
| GeyserUtils                    | [Github](https://github.com/GeyserExtensionists/GeyserUtils)                    | Get your Geyser to support calling some BE stuff  |
| GeyserModelEngine              | [Github](https://github.com/GeyserExtensionists/GeyserModelEngine)              | Make your bedrock support MEG4                            |
| GeyserModelEnginePackGenerator | [Github](https://github.com/GeyserExtensionists/GeyserModelEnginePackGenerator) | Help you automatically transform the model to generate resource packs        |

- Put `GeyserModelEngine` in the plugins folder (only Spigot or forks of Spigot supported)
- Put either `geyserutils-spigot` in your plugins folder aswell (`geyserutils-velocity`/`geyserutils-bungeecord` in your Velocity/Bungeecord plugins folder if you use it)
- Put `GeyserModelEnginePackGenerator` and `geyserutils-geyser` into `plugins/[Geyser-Folder]/extensions`
- Inside `floodgate` set `send-floodgate-data` to `true` in your Velocity/Bungeecord folder and copy over the key.pem into your backend `floodgate` folders

Start the server to generate the relevant configuration files, and then shut down the server to convert any models.

# Convert Models
`GeyserModelEnginePackGenerator` is capable of generating models all by itself. After generating it will also apply this pack automatically.

- Firstly, install [packer plugin](https://github.com/GeyserExtensionists/GeyserModelEngineBlockbenchPacker) for your blockbench.

- Then, open your bbmodel, go `File -> Export -> Export GeyserModelEngine Model`, you will get a zip, just unzip it to `input` folder.

<img src="docsimg/example.jpg" width="500">

> Each model should have a separate model folder
> Subfolders are supported if you want to categorize them

- Restart the server or reload geyser to start generating the resource pack.
- Go to  `plugins/[Geyser-Folder]/extensions/geysermodelenginepackgenerator`, and you should see your pack generated!

<img src="docsimg/example2.jpg" width="500">

- Final step, reload Geyser or restart the server to load the resource pack.
- Congratulations, you've completed this tutorial!

# Tips

* Pay attention! The pack only regenerates when the number of models changes, you can technically speaking remove the generated_pack folder to force a reload aswell.
* You do not have to manually put the pack into the packs folder of Geyser, the extension is capable of loading the pack itself.
* Choose a right texture when use the packer.

# Current issues

* Multi-textures and Animated textures need use [a blockbench plugin](https://github.com/GeyserExtensionists/GeyserModelEngineBlockbenchPacker) to export

* Please report any bugs

# FAQ

### Where can I contact you?
You can contact us on our Discord: https://discord.gg/NNNaUdAbpP
