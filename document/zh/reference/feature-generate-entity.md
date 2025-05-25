# 生成实体类

JimmerBuddy 可以从数据库或 ddl 文件生成实体类。

## 添加数据库或 ddl 文件

1. 打开 JimmerBuddy 工具窗口。
2. 切换到 `Database` 选项卡。

![20250525161526](https://s2.loli.net/2025/05/25/6LqW4NlZKJhR9uM.png)

3. 点击 `+` 按钮添加数据库或 ddl 文件。
4. 输入一些关于数据库或 ddl 文件的信息。

![20250525162253](https://s2.loli.net/2025/05/25/C1REKkceOMHq2AP.png)

5. 点击 `OK` 按钮保存配置。

![20250525162311](https://s2.loli.net/2025/05/25/N64wXcrDvsyY3qP.png)

6. 选择数据库或 ddl 文件，然后右键单击它。
7. 从上下文菜单中选择 `Generate`。
8. 在弹出的对话框中，输入相对路径和包名。

![20250525162405](https://s2.loli.net/2025/05/25/Bl3cTdeaEtIC49s.png)

9. 点击 `OK` 按钮生成实体类。

![20250525162454](https://s2.loli.net/2025/05/25/v85Khz2HL7Y6FQE.png)

## 配置实体注释

进入到 `File and Code Templates` 设置页面，选择 `Jimmer`。

![20250525162739](https://s2.loli.net/2025/05/25/c6bPYIXEo9wvutA.png)

## 注意事项

- 生成的实体类会覆盖同名的实体类。
- 没有主键的表不会生成。
- `BaseEntity`实体只有当公共字段在所有表中都有时才会生成。
- 中间表只有没有主键并只有两个字段时才会被当成中间表处理。