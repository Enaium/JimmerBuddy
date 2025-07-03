# Inspection

JimmerBuddy provides several inspection features to help you identify problems in your code.

## Association Annotations

Checks for correct usage of association annotations:

- `@OneToOne` and `@ManyToOne` cannot be used on collection properties.
- `@OneToMany` and `@ManyToMany` cannot be used on non-collection properties.

## MappedBy Property

Checks the `mappedBy` property of associations:

- The `mappedBy` property must exist.
- The `mappedBy` property type must match the inverse property type.

## IdView Annotation

Checks the `@IdView` annotation:

- The `value` must exist if the property is a collection.
- The property type must be a collection if the type is generic.
- The base property must exist.

## Formula Annotation

Checks the `@Formula` annotation:

- The `dependencies` property must exist.

## FetchBy Annotation

Checks the `@FetchBy` annotation:

- The property must be a fetcher.
- The property must exist.
- The fetcher type must match the fetch type.

## Immutable Annotations

Checks for correct usage of immutable-related annotations such as `@Immutable`, `@Entity`, `@MappedSuperclass`, and `@Embeddable`:

- Must be used on interfaces.

## OrderedProp Annotation

Checks the `@OrderedProp` annotation:

- The `value` property must exist.
