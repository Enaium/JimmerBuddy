# 快速开始

## 安装

###  前置条件

- Intellij `2023.1`或最新。

### 在Intellij中安装

1. 打开IntelliJ IDEA.
2. 转到`File` > `Settings` > `Plugins`。
3. 点击`Marketplace`。
4. 搜索`JimmerBuddy`并安装。

### 在线安装

1. 打开[这个链接](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy)
2. 点击`Get`按钮。

### 离线安装

1. 打开[这个链接](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy)
2. 点击`versions`。
3. 下载最新版本。
4. 转到`File` > `Settings` > `Plugins` > `Install plugin from disk...`。

## FAQ

### 为什么插件没有用？

- 请等待项目索引完成，然后重新打开项目

### 什么时候会生成draft，props等？

- 当你改变Java或Kotlin源码时，等待2秒

### 什么时候会生成DTO的view，input等？

- 当你改变DTO源码时，等待2秒

### 为什么插件没有生成Immutable和DTO的源码？

- 请检查插件日志工具窗口在底部或左下角，然后创建issue

### 为什么生成的源码和Jimmer的apt/ksp不一样？

- 不要担心，Jimmer的apt/ksp会在你的项目构建时生成完整的源码。