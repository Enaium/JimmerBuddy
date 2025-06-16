# Postfix Template

## produce

```java
Author.produce
```

```java
AuthorDraft.$.produce(draft -> {

});
```

## loaded

```java
draft.loaded
```

```java
ImmutableObjects.isLoaded(draft, )
```

## unload

```java
draft.unload
```

```java
ImmutableObjects.unload(draft, )
```

## show

```java
draft.show
```

```java
ImmutableObjects.show(draft, )
```

## hide

```java
draft.hide
```

```java
ImmutableObjects.hide(draft, )
```

## set

```java
draft.set
```

```java
ImmutableObjects.set(draft, )
```

## query

```kotlin
Author.query
```

```kotlin
sql.createQuery(Author::class) {

}
```

## update

```kotlin
Author.update
```

```kotlin
sql.createUpdate(Author::class) {

}
```

## delete

```kotlin
Author.delete
```

```kotlin
sql.createDelete(Author::class) {

}
```

## fbi

```java
Author.fbi
```

```java
sql.findById(Author.class, )
```

## fbis

```java
Author.fbis
```

```java
sql.findByIds(Author.class, )
```

## fobi

```java
Author.fobi
```

```java
sql.findOneById(Author.class, )
```

## fmbis

```java
Author.fmbis
```

```java
sql.findMapByIds(Author.class, )
```

## dbi

```java
Author.dbi
```

```java
sql.deleteById(Author.class, )
```

## dbis

```java
Author.dbis
```

```java
sql.deleteByIds(Author.class, )
```
