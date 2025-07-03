# 检查

JimmerBuddy 提供了多种检查功能，帮助你发现代码中的问题。

## 关联注解

检查关联注解的正确用法：

- `@OneToOne` 和 `@ManyToOne` 不能用于集合属性。
- `@OneToMany` 和 `@ManyToMany` 不能用于非集合属性。

## mappedBy 属性

检查关联的 `mappedBy` 属性：

- 必须存在 `mappedBy` 属性。
- `mappedBy` 属性类型必须与反向属性类型匹配。

## IdView 注解

检查 `@IdView` 注解：

- 如果属性为集合，必须存在 `value`。
- 如果类型为泛型，属性类型必须为集合。
- 必须存在基础属性。

## Formula 注解

检查 `@Formula` 注解：

- 必须存在 `dependencies` 属性。

## FetchBy 注解

检查 `@FetchBy` 注解：

- 属性必须为 fetcher。
- 属性必须存在。
- fetcher 类型必须与 fetch 类型匹配。

## 不变类相关注解

检查 `@Immutable`、`@Entity`、`@MappedSuperclass` 和 `@Embeddable` 等注解的用法：

- 必须用于接口上。

## OrderedProp 注解

检查 `@OrderedProp` 注解：

- 必须存在 `value` 属性。
