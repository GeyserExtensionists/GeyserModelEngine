# GeyserModelEngine

> GitHub仓库：https://github.com/zimzaza4/GeyserModelEngine

[English (outmode)](README_EN.md) | [简体中文](README.md)

# 这是个什么玩意

能让你Geyser服务器支持MEG4

此教程需要对模型有一定基础

# 如何安装

根据服务端版本下载以下插件

[GeyserUtils](https://github.com/zimzaza4/GeyserUtils)

[GeyserModelEngine](https://github.com/zimzaza4/GeyserModelEngine)

[Geyser自定义实体分支](https://github.com/zimzaza4/Geyser)

[GeyserModelEnginePackGenerator](https://github.com/zimzaza4/GeyserModelEnginePackGenerator)

下载完后，将`GeyserModelEngine` `Geyser自定义实体的分支`放入插件文件夹

根据服务端版本把`geyserutils-spigot`/`velocity`/`bungeecord`放入插件文件夹

将`GeyserModelEnginePackGenerator` `geyserutils-geyser`放入geyser的扩展文件夹

先启动服务器生成相关配置文件，之后关闭服务器就安装好了

当然，先别急着用，现在你还得接着读下去

# 转换模型

打开你的bbmodel模型工程文件，将模型转换为基岩版模型

打开刚转换完的模型，做适当调整

记得把这个多余的hitbox删了（如果没有就不用管）

<img src="docimg/hitbox.png" width="500">

不然BE玩家看会看到这个hixbox碰撞箱

<img src="docimg/hitbox1.jpg" width="500">

然后记得导出模型、动画和纹理

# 安装模型

我们打开以下路径 `plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator/input/`

在此目录创建一个文件夹名为模型的ID，比如我有个模型id为`parry_knight`，那就命名为`parry_knight`

<img src="docimg/example.jpg" width="500">

> tips: 每个模型都要有独立的模型文件夹

我们将模型、动画和纹理文件全部丢进这个文件夹

<img src="docimg/example1.jpg" width="500">

重启服务器，来到`plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator`目录

<img src="docimg/example2.jpg" width="500">

将geysermodelenginepackgenerator生成的`generated_pack.zip`丢进Geyser-Spigot/packs目录就安装好了

最后一步，重载Geyser或者重启服务器

# 完结

恭喜你现在学会如何使用了，如果还不会V我五毛帮你解决

# 当前限制

* 不支持多贴图

# 常见问题

### 为什么生成模型后会变成史蒂夫?

你没好好读怎么安装模型

