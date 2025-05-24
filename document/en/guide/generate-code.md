# Generate Code

Jimmer uses `apt`/`ksp` to generate code, But it's need in compile time. So, you need to run `clean` and `build`(Gradle) or `compile`(Maven) to generate code. If you project very large, it will take a long time to generate code.

JimmerBuddy solves this problem that automatically generating code when you change the source code.

## Configuration

You need to configure the `apt` or `ksp` in your project. For the example if you use `ksp`, you need to add the following configuration in your `build.gradle.kts`:

```kts
ksp {
    arg("jimmer.dto.mutable", "true")
}
```

If you use `apt`, you need to add the following configuration in your `build.gradle.kts`:

```kts
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Ajimmer.dto.mutable=true")
}
```

## What time to generate code?

When you change the source code, then wait for 2 seconds to generate code.