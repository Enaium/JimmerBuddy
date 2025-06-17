# JimmerBuddy

A plugin that adds first-class support for Project Jimmer

## Features

### First-class Java or Kotlin development

- Automatically generate `draft`, `fetcher`, `props` etc. class for Immutable.
- List all Immutables, DTOs and ErrorFamilies in the project.
- Generate entity from database or ddl and choose which table or columns to generate.
- Jimmer Project Wizard.
- Inspection for immutable and prop such as `@Entity`, `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula`
  etc.
- Navigate to immutable or prop such as `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula` etc.
- Completion for `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula` etc.
- Generate all set of Draft
- DTO class member go to DTO file.
- Execute SQL to clipboard.
- DTO type count and navigation.
- Immutable inheritor count and navigation.
- Provide some postfix template.
- Generate DDL from Entity.

### First-class DTO development

- Automatically generate `view`, `input`, `specification` etc. class for DTO.
- Syntax highlight.
- Syntax check.
- Native compiler check.
- Navigation to `type`, `prop`, `import`, `annotation`, etc.
- Completion `export`, `package`, `import`, `prop`, `macro`, `config` etc.
- Automatically import for `export`, `implements`, `userProp` etc.
- Format source.
- Structure view.
- Visualization to create DTO file.

## FAQ

### Why is the plugin not available

- Please wait for the project to be indexed, then reopen the project

### What time will the plugin generate draft, props, etc.?

- When you change the Java or Kotlin source, then wait 2 seconds

### What time will the plugin generate the DTO view, input, etc.?

- When you change the DTO source, then wait 2 seconds

### Why does the plugin not generate source for Immutable and DTO

- Please check the plugin log tool window on the bottom or the left bottom, then create an issue

### Why generated source is not same as the Jimmer's apt/ksp

Do not worry, the Jimmer's apt/ksp will generate full source when your project builds.