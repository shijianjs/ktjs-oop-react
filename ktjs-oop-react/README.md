# ktjs-oop-react

把 [`oop-react`](https://github.com/shijianjs/oop-react) 的模型移植到 Kotlin/JS：状态和行为待在一个普通类里，
只有标了 `@OopReactRender` 的方法参与重渲染。

用两样东西：这个库，加上构建期的 [`ktjs-oop-react-ksp`](../ktjs-oop-react-ksp)。

```kotlin
@OopReactState
class Cart {
    val items: MutableList<Item> = OopReactList()

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode = jsx {
        ul { for (item in items) li { +item.title } }
    }
}
```

- `@OopReactState`：类级注解，实例在第一次渲染时变成可观察对象（MobX），并沿字段递归。
- `@OopReactRender`：一个方法一个岛——只订阅它读到的字段，外面包一层错误边界。
- `@OopReactIgnore` / `@OopReactAction`：不参与观察的字段 / 一次事务。
- 其余入口：`OopReactObserver`、`OopReactCallback`、`oopReactComponent`、`OopReactBridge`、
  `OopReactLoading` / `OopReactResource` / `OopReactAsyncView`、`OopReactSelection`、`OopReactCleanup`、
  `oopReactConfigure`。

约定：注解成员必须写 `@JsName`；应用启动时调一次 KSP 生成的 `initializeOopReact()`。

完整说明见仓库根的 [`AGENTS.md`](../AGENTS.md) 与 [`README.md`](../README.md)。
