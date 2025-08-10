# Postfix Templates

JimmerBuddy provides a set of postfix templates to help you quickly generate common code patterns for Jimmer entities and drafts. Below are the available templates and their effects.

## Produce: Create Draft Instance

```java
Author.produce
```

Expands to:

```java
AuthorDraft.$.produce(draft -> {

});
```

## Loaded: Check if Draft is Loaded

```java
draft.loaded
```

Expands to:

```java
ImmutableObjects.isLoaded(draft, )
```

## Unload: Unload a Draft

```java
draft.unload
```

Expands to:

```java
ImmutableObjects.unload(draft, )
```

## Show: Show a Draft

```java
draft.show
```

Expands to:

```java
ImmutableObjects.show(draft, )
```

## Hide: Hide a Draft

```java
draft.hide
```

Expands to:

```java
ImmutableObjects.hide(draft, )
```

## Set: Set a Value on a Draft

```java
draft.set
```

Expands to:

```java
ImmutableObjects.set(draft, )
```

## Query: Create a Query (Kotlin)

```kotlin
Author.cq
```

Expands to:

```kotlin
sql.createQuery(Author::class) {

}
```

## Update: Create an Update (Kotlin)

```kotlin
Author.cu
```

Expands to:

```kotlin
sql.createUpdate(Author::class) {

}
```

## Delete: Create a Delete (Kotlin)

```kotlin
Author.cd
```

Expands to:

```kotlin
sql.createDelete(Author::class) {

}
```

## fbi: Find by ID

```java
Author.fbi
```

Expands to:

```java
sql.findById(Author.class, )
```

## fbis: Find by IDs

```java
Author.fbis
```

Expands to:

```java
sql.findByIds(Author.class, )
```

## fobi: Find One by ID

```java
Author.fobi
```

Expands to:

```java
sql.findOneById(Author.class, )
```

## fmbis: Find Map by IDs

```java
Author.fmbis
```

Expands to:

```java
sql.findMapByIds(Author.class, )
```

## dbi: Delete by ID

```java
Author.dbi
```

Expands to:

```java
sql.deleteById(Author.class, )
```

## dbis: Delete by IDs

```java
Author.dbis
```

Expands to:

```java
sql.deleteByIds(Author.class, )
```
