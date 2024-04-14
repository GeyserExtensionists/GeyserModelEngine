# GeyserModelEngine

> GitHub：https://github.com/zimzaza4/GeyserModelEngine

[English](README_EN.md) | [简体中文](README.md)

# About

it can let your Geyser server support  on MEG4

# How to install？

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

now restart your server to generated MEG4 model , your BE player should be able to see the model normally.

restart you server,summon model.We should be able to see the model normally

after that , it's about model's animation's part :

# model animation


let model's animation export to json format

named the animation file by 'animation.modelID.json'

then put it into your resource pack

<img src="docimg/example1.jpg" width="500">

now let us open the animation file to starting change the animaiton ID

originally , all the 'action ID' were basic named like  'idle' , 'walk' , but now you should add a prefix

such as :

'idle' to 'animation.modelID.idle'

example : 'animation.parry knight.idle'

after changed the resource pack 's version or uuid , pack the resource pack .

finally , restart the Geyser or server

# END

Congratulations ! now you exactly know how to use , if you still can't finish yet

well , maybe that's my problem = D

#restrictions

,,,,,a lot

# problems

### why 


例如：`idle`

改为：`animation.模型ID.idle`

示例：`animation.parry_knight.idle`

改完后修改资源包版本号或者uuid，打包资源包

最后一步，重载Geyser或者重启服务器

# 完结

恭喜你现在学会如何使用了，如果还不会V我五毛帮你解决

# 当前限制

用了就知道, 一堆

# 常见问题

### 为什么生成模型后会变成史蒂夫?

你没好好读怎么安装模型
