# JimmerBuddy

A plugin that adds first-class support for Project Jimmer

![](https://s2.loli.net/2025/04/05/2RNcPghd1AeB8Fs.png)

## Features

### First-class Java or Kotlin development

- Automatically generate `draft`, `fetcher`, `props` etc. class for Immutable.
- List all Immutables and DTOs in the project.
- Generate entity from database or ddl and choose which table or columns to generate.
- Jimmer Project Wizard.
- Inspection for immutable and prop such as `@Entity`, `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula`
  etc.
- Navigate to immutable or prop such as `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula` etc.
- Completion for `@OneToOne`, `@OneToMany`, `@ManyToMany`, `@IdView`, `@Formula` etc.
- Generate all set of Draft
- DTO class member go to DTO file.

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

## Screenshots

![automatically generate class](https://s2.loli.net/2025/03/05/WAxQ34sUnS9i7q5.gif)

![immutables](https://s2.loli.net/2025/03/21/dcZQLJeAhqNSTvH.gif)

![project wizard](https://s2.loli.net/2025/03/05/USP5VdZvlA6iNzO.png)

![generate entity](https://s2.loli.net/2025/03/26/uLvkng5bNHhqeaw.png)

![new dto file](https://s2.loli.net/2025/03/11/gjAWhY8DiOKT5vz.gif)

![inspection](https://s2.loli.net/2025/03/19/GYUof7MaizypW9B.png)

![inspection](https://s2.loli.net/2025/03/19/WSbH2kPVGIwZ4Lr.png)

![nav](https://s2.loli.net/2025/03/20/Kp6ErJH1aNvk8Sl.png)

![generate all set](https://s2.loli.net/2025/03/26/oK5duRqIs2Hb8mj.gif)

![id view completion](https://s2.loli.net/2025/04/03/PlrFSvd42CTw8XZ.gif)

![formula completion](https://s2.loli.net/2025/04/03/j2tM4JePk1hfSBT.gif)

![mapped by](https://s2.loli.net/2025/04/03/fpkjVF7tnSwIKlW.gif)

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