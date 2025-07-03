# 后缀模板

JimmerBuddy 提供了一系列后缀模板，帮助你快速生成 Jimmer 实体和草稿的常用代码模式。以下是可用模板及其作用。

## produce：创建草稿实例

```java
Author.produce
```

展开为：

```java
AuthorDraft.$.produce(draft -> {

});
```

## loaded：检查草稿是否已加载

```java
draft.loaded
```

展开为：

```java
ImmutableObjects.isLoaded(draft, )
```

## unload：卸载草稿

```java
draft.unload
```

展开为：

```java
ImmutableObjects.unload(draft, )
```

## show：显示草稿

```java
draft.show
```

展开为：

```java
ImmutableObjects.show(draft, )
```

## hide：隐藏草稿

```java
draft.hide
```

展开为：

```java
ImmutableObjects.hide(draft, )
```

## set：设置草稿属性

```java
draft.set
```

展开为：

```java
ImmutableObjects.set(draft, )
```

## query：创建查询（Kotlin）

```kotlin
Author.query
```

展开为：

```kotlin
sql.createQuery(Author::class) {

}
```

## update：创建更新（Kotlin）

```kotlin
Author.update
```

展开为：

```kotlin
sql.createUpdate(Author::class) {

}
```

## delete：创建删除（Kotlin）

```kotlin
Author.delete
```

展开为：

```kotlin
sql.createDelete(Author::class) {

}
```

## fbi：按 ID 查询

```java
Author.fbi
```

展开为：

```java
sql.findById(Author.class, )
```

## fbis：按多个 ID 查询

```java
Author.fbis
```

展开为：

```java
sql.findByIds(Author.class, )
```

## fobi：按 ID 查询单个

```java
Author.fobi
```

展开为：

```java
sql.findOneById(Author.class, )
```

## fmbis：按多个 ID 查询映射

```java
Author.fmbis
```

展开为：

```java
sql.findMapByIds(Author.class, )
```

## dbi：按 ID 删除

```java
Author.dbi
```

展开为：

```java
sql.deleteById(Author.class, )
```

## dbis：按多个 ID 删除

```java
Author.dbis
```

展开为：

```java
sql.deleteByIds(Author.class, )
```
