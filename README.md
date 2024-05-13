# GeyserModelEngine 自定义实体分支

> GitHub仓库：[https://github.com/zimzaza4/GeyserModelEngine/tree/custom-entity](https://github.com/zimzaza4/GeyserModelEngine/tree/custom-entity)

Language [English](README_EN.md) | [简体中文](README.md)

# 这个跟主分支有什么区别

这是个为[zimzaza4的Geyser自定义实体分支](https://github.com/zimzaza4/Geyser)做的版本，不支持原版的Geyser。

跟主分支不同的是，这个是真正的自定义实体

主分支是生成个steve发送4d皮肤给be玩家，支持原版Geyser。

只需要往资源包里赛动画文件即可，可以有效防止你的模型被白嫖，但似乎限制也挺多

总之两者各有各优势，根据各服务器情况选择

# 如何安装

根据服务端版本下载以下插件

[GeyserUtils](https://github.com/zimzaza4/GeyserUtils)

[GeyserModelEngine](https://github.com/zimzaza4/GeyserModelEngine)

[zimzaza4的Geyser自定义实体分支](https://github.com/zimzaza4/Geyser)

[GeyserModelEnginePackGenerator](https://github.com/zimzaza4/GeyserModelEnginePackGenerator)

下载完后，将`GeyserModelEngine` `Geyser自定义实体分支`放入插件文件夹

根据服务端版本把`geyserutils-spigot`/`velocity`/`bungeecord`放入插件文件夹

`GeyserModelEnginePackGenerator` `geyserutils-geyser`放入geyser的扩展文件夹

先启动服务器生成相关配置文件，之后关闭服务器就安装好了

当然，先别急着用，现在你还得接着读下去

# 转换模型

现在`GeyserModelEnginePackGenerator`长大了已经学会自己转换模型打包资源包了

我们打开以下路径 `plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator/input/`

在此目录创建一个文件夹名为模型的ID，比如我有个模型id为`parry_knight`，那就命名为`parry_knight`

<img src="docsimg/example.jpg" width="500">

> 每个模型都要有独立的模型文件夹

我们将模型、动画和纹理全部原封不动丢进这个文件夹

<img src="docsimg/example1.jpg" width="500">

重启服务器或者重载geyser让他开始生成资源包

来到`plugins/Geyser-Spigot/extensions/geysermodelenginepackgenerator`目录

<img src="docsimg/example2.jpg" width="500">

将`geysermodelenginepackgenerator`生成的`generated_pack.zip`丢进`Geyser-Spigot/packs`目录就安装好了

最后一步，重载Geyser或者重启服务器加载资源包

注意! 他是检测模型的数量来打包的，如果数量没有变更不会执行。

想重新打包建议先删掉`generated_pack.zip`然后改uuid或者版本号

# 完结

恭喜你现在学会如何使用了，有BUG请发Issues

# 当前限制

* 不支持多贴图
* 待挖掘

# 常见问题

### 为什么召唤模型后会变成史蒂夫?

如果你确定你根据上面的教程一步一步做了，可能是这个模型的问题?

