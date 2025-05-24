# 检查

JimmerBuddy 提供了一些检查功能来帮助您发现代码中的问题。

## 关联

检查关联的一些注释。

- `@OneToOne` 和 `@ManyToOne` 不能用于集合属性。
- `@OneToMany` 和 `@ManyToMany` 不能用于非集合属性。

## MappedBy

检查关联的 `mappedBy` 属性。

- `mappedBy` 属性必须存在。
- `mappedBy` 属性类型必须与反向属性类型匹配。

## IdView

检查 `@IdView` 注释。

- `value` 必须存在，如果属性是集合。
- 如果类型是泛型类型，则属性类型必须是集合。
- 基础属性必须存在。

## Formula

检查 `@Formula` 注释。

- `dependencies` 必须存在。

## FetchBy

检查 `@FetchBy` 注释。

- 属性必须是`Fetcher`。
- 属性必须存在。
- `Fetcher`类型必须与获抓取类型匹配。

## Immutable

检查一些关于 Immutable 的注释，例如 `@Immutable`、`@Entity`、`@MappedSuperclass` 和 `@Embeddable`。

- 必须在接口上。

## OrderedProp

检查 `@OrderedProp` 注释。

- `value` 必须存在。
