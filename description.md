JimmerBuddy — First-class Jimmer support for JetBrains IDEs

Supercharge your Java/Kotlin workflow with instant model/DTO generation, deep navigation, smart inspections, and a visual toolset built specifically for Project Jimmer. JimmerBuddy turns your IDE into a real-time companion for designing entities, DTOs, and relational mappings — with zero ceremony.

Why JimmerBuddy
- Instant feedback: Generate draft, props, fetcher, DTO view/input/specification and more as you type (debounced to stay fast and quiet).
- Navigate everything: Jump between entities, DTOs, properties, and relations in one keystroke.
- Design with confidence: Syntax highlighting, formatter, and compiler-grade checks for DTO files.
- Built for productivity: Postfix templates, auto-imports, and ready-to-use wizards.

Core Capabilities
- Entities & DDL
  - Generate entities from database or DDL (select tables/columns with precision).
  - Generate DDL from entities for round‑trip modeling.
  - Project wizard to bootstrap Jimmer projects quickly.
- Immutable Model Tooling
  - Auto-generate draft, props, fetcher, and related helpers for Immutables.
  - List and explore all Immutables and their inheritors.
- DTO Toolkit & Designer
  - Generate view, input, specification, and more from DTOs.
  - DTO structure view, visualization, and quick navigation.
  - Automatic import management (export, implements, userProp, etc.).
- Navigation & Discovery
  - Go to related types, properties, and annotations (@OneToOne, @ManyToMany, @IdView, @Formula…).
  - Count and navigate DTO types across the project.
- Code Assistance & Automation
  - Completion/inspection for Jimmer annotations and properties (Java & Kotlin).
  - Auto-generate sources on file changes with safe debounce.
  - Built-in formatter and organizer for DTO source.
- Extras
  - Handy postfix templates for common Jimmer patterns.
  - Execute SQL to clipboard.

How It Fits Your Build
- During IDE work, JimmerBuddy generates lightweight sources for instant feedback and navigation.
- During project builds, Jimmer apt/ksp generates the full sources; both workflows complement each other.

Getting Started
1) Install JimmerBuddy from JetBrains Marketplace.
2) Open a project using Jimmer.
3) Edit your entities or DTOs — generation and navigation work automatically.
4) Use context actions and the project wizard for advanced generation flows.

Notes
- If generation seems missing, check the plugin log tool window for details.
- Works with Java and Kotlin projects; no special configuration required.

Support
- Found an issue or have a feature request? Open an issue on GitHub. Contributions are welcome!

License
- Apache 2.0