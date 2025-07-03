# Quickstart

## Install

### Prerequisites

- IntelliJ IDEA 2023.1 or later

### Install via IntelliJ

1. Open IntelliJ IDEA.
2. Go to `File` > `Settings` > `Plugins`.
3. Click `Marketplace`.
4. Search for `JimmerBuddy` and install it.

### Install Online

1. Open [this link](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy).
2. Click the `Get` button.

### Install Offline

1. Open [this link](https://plugins.jetbrains.com/plugin/26622-jimmer-buddy).
2. Click the `Versions` button.
3. Download the latest version.
4. In IntelliJ, go to `File` > `Settings` > `Plugins` > `Install plugin from disk...` and select the downloaded file.

## FAQ

### Why is the plugin not available?

- Please wait for the project to finish indexing, then reopen the project.

### When does the plugin generate draft, props, etc.?

- When you change Java or Kotlin source files, wait about 2 seconds.

### When does the plugin generate DTO view, input, etc.?

- When you change DTO source files, wait about 2 seconds.

### Why doesn't the plugin generate source for Immutable and DTO?

- Please check the plugin log tool window at the bottom or lower left, then create an issue if needed.

### Why is the generated source different from Jimmer's apt/ksp?

- Don't worry, Jimmer's apt/ksp will generate the full source when your project builds.