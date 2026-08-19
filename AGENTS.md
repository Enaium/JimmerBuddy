# AGENTS.md — JimmerBuddy

## Project Description

JimmerBuddy is a JetBrains IntelliJ Platform plugin (written in Kotlin/Java) that provides first-class support for [Project Jimmer](https://github.com/babyfish-ct/jimmer) — an ORM framework for Java and Kotlin. The plugin delivers instant code generation, deep navigation, smart inspections, and a visual toolset for designing entities, DTOs, and relational mappings directly inside the IDE.

Key capabilities:
- **Entity & DDL generation** — from database tables, DDL, or IntelliJ Database Tools integration.
- **Immutable tooling** — auto-generate `draft`, `fetcher`, `props`, and related helpers.
- **DTO toolkit** — generate `view`, `input`, `specification` and more; visual designer, structure view, fragment completion.
- **Navigation & inspection** — jump between entities, DTOs, properties, and annotations; compiler-grade checks for DTO syntax.
- **Automation** — debounced auto-generation on file changes, automatic import management, postfix templates.

The plugin supports IntelliJ platform versions 231–261 (via the `since/` version-specific modules) and works with both Java and Kotlin projects.

## Project Structure

```
JimmerBuddy/
├── core/                          # Main plugin source (Kotlin/Java)
│   └── src/main/kotlin/cn/enaium/jimmer/buddy/
│       ├── JimmerBuddy.kt         # Plugin entry / project component
│       ├── extensions/
│       │   ├── dto/               # Custom DTO language (grammar, PSI, completion, inspection, formatter)
│       │   │   ├── lang/          # Highlighting, annotator, brace matcher, folding, formatter
│       │   │   ├── editor/        # Visual editor with panels (alias, modifier, tree, prop editor)
│       │   │   ├── completion/    # DTO keyword, type ref, branch completion providers
│       │   │   ├── inspection/    # DTO-specific inspections
│       │   │   ├── reference/     # Reference contributors and providers
│       │   │   ├── search/        # Search/contribute DTO types by name
│       │   │   ├── stub/          # Stub-based PSI for indexing
│       │   │   ├── psi/           # PSI element types, mixin classes
│       │   │   ├── pattern/       # PsiPattern matching for context awareness
│       │   │   ├── index/         # DtoTypeIndex for fast lookup
│       │   │   ├── insight/       # Inlay hints for prop types
│       │   │   └── spellcheck/    # Spellcheck strategy
│       │   ├── window/            # Tool windows (DTOList, ErrorFamilyTree, ImmutableTree, DatabaseList)
│       │   ├── completion/        # DB column/table completion
│       │   ├── debugger/          # Immutable debugger renderer
│       │   ├── editor/            # Immutable split editor and visual editor
│       │   ├── icon/              # Icon provider
│       │   ├── index/             # ClassKindIndex
│       │   ├── insight/           # Code vision, line markers, link markers
│       │   ├── inspection/        # Annotation inspections, super-type checks, unload inspection
│       │   └── reference/         # Jimmer reference contributor, transient reference
│       ├── database/              # Database metadata providers and entity generation
│       ├── dialog/                # AddDatabaseDialog, GenerateEntityDialog
│       ├── action/                # Actions: generate, DDL, database, go-to
│       ├── storage/               # Caches and settings (DatabaseCache, GenerateEntityCache, JimmerBuddySetting)
│       └── utility/               # Shared helpers (PSI, KSP, APT, database, DTO, immutable, UI, swing)
├── since/                         # IntelliJ version-specific modules
│   ├── shared/                    # Shared plugin.xml descriptor and resources
│   ├── 231/                       # IntelliJ 231 compat layer
│   ├── 242/                       # IntelliJ 242 compat layer
│   ├── 253/                       # IntelliJ 253 compat layer (search scope providers)
│   └── 261/                       # IntelliJ 261 compat layer (services, search providers)
├── gradle-tooling-extension/      # Gradle tooling for KSP project resolution
├── buildSrc/                      # Convention plugins (since.gradle.kts, intellij-platform.gradle.kts)
├── document/                      # VitePress documentation site (zh, en, public)
├── gradle/
│   └── libs.versions.toml         # Version catalog (Kotlin 2.4.10, Jimmer 0.11.4, IntelliJ 2.13.1 platform)
└── build.gradle.kts               # Root build — `buildPlugins` task assembles all `since/*` plugin ZIPs
```

**Build system:** Gradle with Kotlin DSL. The IntelliJ Platform Gradle Plugin (`intellij-platform`) handles plugin compilation and verification. GrammarKit generates the DTO lexer/parser from BNF/.flex grammars into `core/src/main/gen/`.

## Rules

### 1. Write Clear, Meaningful Comments

- All code MUST have clear, concise comments explaining *why* something is done, not just *what*.
- Complex logic, non-obvious workarounds, and IntelliJ platform integration points MUST be commented.
- Keep comments up-to-date with code changes — stale comments are worse than no comments.

### 2. AI-Generated Code Must Add the AI Collaborator

- Any PR or commit that includes AI-generated or AI-assisted code MUST add the appropriate AI collaborator to the commit message using the standard `Co-authored-by:` trailer.
- **Agent with its own built-in model:** the Agent decides who to list as the collaborator.
- **Agent with a custom/external model:** list the **model's** name and email, not the Agent's.
  - Example: Claude Code configured with DeepSeek → `Co-authored-by: DeepSeek <noreply@deepseek.com>`, NOT `Co-authored-by: Claude <noreply@anthropic.com>`.

### 3. Git Workflow

- **Sync via rebase:** MUST use `git rebase` (not `git merge`) when synchronizing with the upstream branch to keep a linear history.
- **Squash related commits:** Changes for the same feature or fix MUST be squashed into a single commit before merging.
- **PR branch from latest upstream:** When submitting a PR, MUST create the feature branch from the latest commit on the upstream repository's main branch (`git rebase upstream/main`), not from a stale local state.
- **Separate PRs for separate features:** Each independent feature or fix MUST be submitted as its own PR. When a previous PR has not yet been merged, the next feature branch MUST still be created from the latest commit on `upstream/main` — NOT continued from the previous PR's branch. This avoids hidden dependencies between PRs and keeps each review clean.

### 4. Run `verifyPlugins` After Completion

- After making any changes to the plugin (especially to `plugin.xml`, extensions, actions, PSI elements, or module structure), the `verifyPlugins` Gradle task MUST be run to ensure plugin descriptor validity.
- Run with: `./gradlew verifyPlugins`
- The task verifies plugin XML, dependency declarations, and descriptor consistency across all `since/*` modules.
- If `verifyPlugins` fails, fix descriptor issues before submitting.
