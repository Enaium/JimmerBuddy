# What is JimmerBuddy?

JimmerBuddy is an IntelliJ plugin for the Jimmer project. Previously, you needed to manually clean and build the project to generate code. Now, you can generate code automatically whenever you change the source code.

::: tip
Just want to try it out? Skip to the [Quickstart](./quickstart.md)
:::

## First-class Java/Kotlin Development

- Automatically generate code from your source code, such as `draft`, `fetcher`, `props`, etc.
- List all `Immutables`, `DTOs`, and `ErrorFamilies` in the project.
- Generate entities from a database or DDL file.
- Jimmer project wizard.
- Inspections for Jimmer annotations.
- Navigate to `Immutable` or `Prop`.
- Completion for Jimmer annotations.
- Generate all sets of `Draft`.
- Navigate from DTO class members to DTO files.
- Execute SQL to clipboard.
- DTO type count and navigation.
- Immutable inheritor count and navigation.

## First-class DTO Development

- Automatically generate `view`, `input`, `specification`, etc. classes for DTOs.
- Syntax highlighting.
- Syntax checking.
- Native compiler checking.
- Navigation to `type`, `prop`, `import`, `annotation`, etc.
- Completion for `export`, `package`, `import`, `prop`, `macro`, `config`, etc.
- Automatic import for `export`, `implements`, `userProp`, etc.
- Format source code.
- Structure view.
- Visualize and create DTO files.

## Supported Annotations

`@Entity`, `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula`