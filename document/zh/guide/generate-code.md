# 生成代码

Jimmer使用`apt`/`ksp`生成代码，但是它需要在编译时。所以，你需要运行`clean`和`build`(Gradle)或`compile`(Maven)来生成代码。如果你的项目非常大，它将花费很长时间来生成代码。

JimmerBuddy解决了这个问题，当你改变源代码时，自动生成代码。

## 配置

您需要在您的项目中配置`apt`或`ksp`。例如，如果您使用`ksp`，您需要在`build.gradle.kts`中添加以下配置：

```kts
ksp {
    arg("jimmer.dto.mutable", "true")
}
```

如果您使用`apt`，您需要在`build.gradle.kts`中添加以下配置：

```kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Ajimmer.dto.mutable=true")
}
```

## 什么时候生成代码？

当你改变源代码时，然后等待2秒生成代码。