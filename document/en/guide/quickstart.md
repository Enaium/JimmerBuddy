# Quickstart

## Install

###  Prerequisites

- Intellij `2023.1` or later.

### Install in Intellij

1. Open IntelliJ IDEA.
2. Go to `File` > `Settings` > `Plugins`.
3. Click `Marketplace`.
4. Search for `JimmerBuddy` and install it.

### Install online

1. Open [the link](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy)
2. Click `Get` button.

### Install offline

1. Open [the link](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy)
2. Click `versions` button.
3. Download the latest version.
4. Install the plugin from `File` > `Settings` > `Plugins` > `Install plugin from disk...`.

## FAQ

### Why is the plugin not available?

- Please wait for the project to be indexed, then reopen the project

### What time will the plugin generate draft, props, etc.?

- When you change the Java or Kotlin source, then wait 2 seconds

### What time will the plugin generate the DTO view, input, etc.?

- When you change the DTO source, then wait 2 seconds

### Why does the plugin not generate source for Immutable and DTO

- Please check the plugin log tool window on the bottom or the left bottom, then create an issue

### Why generated source is not same as the Jimmer's apt/ksp

Do not worry, the Jimmer's apt/ksp will generate full source when your project builds.