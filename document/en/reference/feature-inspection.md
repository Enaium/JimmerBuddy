# Inspection

JimmerBuddy provide some inspection features to help you find problems in your code.

## Association

Check some annotations of the association.

- `@OneToOne` and `@ManyToOne` can not at collection property.
- `@OneToMany` and `@ManyToMany` can not at non-collection property.

## MappedBy

Check the `mappedBy` property of the association.

- `mappedBy` prop must exist.
- `mappedBy` prop type must match the inverse property type.

## IdView

Check the `@IdView` annotation.

- `value` must exist if the property is a collection.
- Property type must be a collection if the type is a generic type.
- Base prop must exist.

## Formula

Check the `@Formula` annotation.

- `dependencies` must exist.

## FetchBy

Check the `@FetchBy` annotation.

- Property must be a fetcher.
- Property must exist.
- Fetcher type must match the fetch type.

## Immutable

Check some annotations about Immutable such as  `@Immutable`, `@Entity`, `@MappedSuperclass`, and `@Embeddable`.

- Must on the interface.

## OrderedProp

Check the `@OrderedProp` annotation.

- `value` must exist.
