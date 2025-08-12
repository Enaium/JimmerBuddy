# JimmerBuddy

A powerful IntelliJ IDEA plugin that brings first-class support for [Project Jimmer](https://github.com/babyfish-ct/jimmer), enhancing Java and Kotlin development with advanced code generation, navigation, and productivity features.

![JetBrains Plugin Downloads](https://img.shields.io/jetbrains/plugin/d/26622?style=flat-square&logo=jetbrains)
![JetBrains Plugin Rating](https://img.shields.io/jetbrains/plugin/r/stars/26622?style=flat-square&logo=jetbrains)
![GitHub License](https://img.shields.io/github/license/Enaium/JimmerBuddy?style=flat-square&logo=apache)
![GitHub top language](https://img.shields.io/github/languages/top/Enaium/JimmerBuddy?style=flat-square&logo=kotlin)

---

## Features

### Entity & DDL Support
- Generate entities from database or DDL, with fine-grained selection of tables and columns.
- Generate DDL from entities.
- Jimmer Project Wizard for quick project setup.

### Immutable & DTO Enhancements
- Auto-generate `draft`, `fetcher`, `props`, and related classes for Immutables.
- List all Immutables, DTOs, and ErrorFamilies in your project.
- Auto-generate `view`, `input`, `specification`, and more for DTOs.
- DTO designer and visualization tools.
- Structure view and navigation for DTOs and Immutables.
- Syntax highlighting, checking, and native compiler checks for DTO files.
- Format and organize DTO source code.

### Navigation & Productivity
- Navigate to Immutables, DTOs, and properties (e.g., `@OneToOne`, `@ManyToMany`, `@IdView`, `@Formula`).
- Completion and inspection for Jimmer annotations and properties.
- Postfix templates for common Jimmer patterns (Java & Kotlin).
- Count and navigate DTO types and immutable inheritors.
- Execute SQL to clipboard.

### Automation & Quality
- Automatic import management for DTOs (`export`, `implements`, `userProp`, etc.).
- Auto-generation of sources on file changes (with debounce).
- Error reporting and inspection for common issues.

---

## Installation

1. **From JetBrains Marketplace:**
   - Open IntelliJ IDEA.
   - Go to `Settings` > `Plugins` > `Marketplace`.
   - Search for `JimmerBuddy` and install.
   - Restart the IDE if prompted.

2. **From Source:**
   - Clone this repository.
   - Build the plugin using Gradle: `./gradlew build`
   - Install the generated plugin ZIP via `Settings` > `Plugins` > `Install Plugin from Disk...`

---

## Usage

- **Entity/DTO Generation:**
  - Edit your Java, Kotlin, or DTO files. JimmerBuddy will auto-generate supporting code after a short delay.
  - Use the context menu or project wizard for advanced generation options.
- **Navigation:**
  - Use `Go to` actions or structure views to quickly jump between Immutables, DTOs, and their properties.
- **Postfix Templates:**
  - Type supported postfixes (e.g., `.findById`, `.deleteById`) and press Tab to expand.
- **Error Inspection:**
  - Check the plugin log tool window for issues or errors.

---

## FAQ

**Q: Why is the plugin not available after installation?**
- Wait for project indexing to complete, then reopen the project.

**Q: When does the plugin generate draft, props, etc.?**
- When you change Java or Kotlin source files, generation occurs after a 2-second delay.

**Q: When does the plugin generate DTO view, input, etc.?**
- When you change DTO source files, generation occurs after a 2-second delay.

**Q: Why is generated source different from Jimmer's apt/ksp?**
- Jimmer's apt/ksp will generate the full source during your project build. JimmerBuddy provides instant feedback and navigation during development.

**Q: Why is source not generated for Immutable and DTO?**
- Check the plugin log tool window for errors. If issues persist, please create an issue on GitHub.

---

## Contributing

Contributions are welcome! Please open issues or pull requests for bug fixes, features, or documentation improvements.

---

## License

This project is licensed under the [Apache 2.0 License](LICENSE).