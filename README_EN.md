# GeyserModelEngine

> GitHub：https://github.com/zimzaza4/GeyserModelEngine

[English](README_EN.md) | [简体中文](README.md)

# what is this

it can let you Geyser server support MEG4

# how to install

download this plugin

[GeyserUtils](https://github.com/zimzaza4/GeyserUtils)

[GeyserModelEngine](https://github.com/zimzaza4/GeyserModelEngine)

[LibsDisguises](https://www.spigotmc.org/resources/libs-disguises-free.81/)

After downloading, place GeyserModelEngine in the plugins folder

The `geyserutils-spigot`/`velocity`/`bungeecord` into the plugins folder

Put `geyserutils-geyser` into geyser extensions folder, and the installation is complete

# convert model

Open your bbmodel project file and convert the model to bedrock model

Open the newly converted model and delete the extra hitbox (if not, leave it alone)

<img src="docimg/hitbox.png" width="500">

Otherwise BE players will see this hixbox ox

<img src="docimg/hitbox1.jpg" width="500">

Then save to export the texture of the model

# install model

Open Geyser's extensions folder and create a folder called `geyserutils`, and then create a folder inside called `skins`

Now let's create a folder called the id of your model.  For example, if the id of the test model I use is `parry_knight`, I create a `parry_knight` folder

Your file path should look like this: `plugins/Geyser-Spigot/extensions/geyserutils/skins/模型id/`

Finally put the model and texture map in

<img src="docimg/example.jpg" width="500">

这时候重启服务器生成MEG4模型，你的BE玩家应该能正常看到模型了

接下来就是有关模型动画的部分了
