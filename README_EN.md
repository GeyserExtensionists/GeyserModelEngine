# GeyserModelEngine CustomEntity Fork

> GitHub：[https://github.com/zimzaza4/GeyserModelEngine/tree/custom-entity](https://github.com/zimzaza4/GeyserModelEngine/tree/custom-entity)

[English (working)](README_EN.md) | [简体中文](README.md)

# About

this is for [GeyserCustomEntityFork](https://github.com/zimzaza4/Geyser)'s version，no support vanilla Geyser

Unlike the master, this one is truly a custom entity

# how to install

Download the following plugins according to the server core

| plugins                        | Link                                                                 | effect                          |
| :---                           | :----                                                                | :---                            |
| GeyserUtils                    | [Github](https://github.com/zimzaza4/GeyserUtils)                    | Get your Geyser to support calling some BE stuff  |
| GeyserModelEngine              | [Github](https://github.com/zimzaza4/GeyserModelEngine)              | Make your bedrock support MEG4                            |
| GeyserCustomEntity Fork        | [Github](https://github.com/zimzaza4/Geyser)                         | Make Geyser support custom entities            |
| GeyserModelEnginePackGenerator | [Github](https://github.com/zimzaza4/GeyserModelEnginePackGenerator) | Help you automatically transform the model to generate resource packs        |

place `GeyserModelEngine` `GeyserCustomEntityFork` in the plugins folder

and `geyserutils-spigot`/`velocity`/`bungeecord` 

`GeyserModelEnginePackGenerator` `geyserutils-geyser` put into `plugins/geyser/extensions`

Start the server to generate the relevant configuration files, and then shut down the server to install

# convert model

`GeyserModelEnginePackGenerator` can generate resource packs himself

We came to `plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator/input/`

Create a folder in this directory called the ID of the model.

For example, if I have a model with the id `parry_knight`, name it `parry_knight`

<img src="docsimg/example.jpg" width="500">

> Each model should have a separate model folder

We drop the model, animations, and textures into this folder intact

<img src="docsimg/example1.jpg" width="500">

Restart the server or reload geyser to start generating resource packs

go to `plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator`

<img src="docsimg/example2.jpg" width="500">

final step, reload Geyser or restart the server to load the resource pack

# tips

* Pay attention! It is packaged by detecting the number of models and will not execute if the number does not change.
* To repackage it is recommended to delete `generated_pack.zip` and change the uuid or version
* instead of dropping the package into the `geyser/packs` directory, it will load pack itself

# The end

Congratulations you now learn how to use, any bugs please send Issues

# limit

* Multi-textures are not supported
* To be excavated

# FAQ

### Why does it turn into Steve after summoning a model?

If you're sure you did it step by step according to the tutorial above, there may be a problem with this model?

