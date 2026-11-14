# ktjs-oop-react

**简体中文** | English (TODO)

把 [`oop-react`](https://github.com/shijianjs/oop-react) 的模型移植到 Kotlin/JS：**状态和行为待在一个普通类里**，
只有标了 `@OopReactRender` 的方法参与重渲染。React 用官方 kotlin-wrappers 绑定，可观察性就是 MobX 本身。

```kotlin
@OopReactState
class Cart {
    val items: MutableList<Item> = OopReactList()

    fun add(item: Item) {
        items.add(item)
    }

    @OopReactRender
    @JsName("view")
    fun view(): ReactNode = jsx {
        ul {
            for (item in items) {
                li { +item.title }
            }
        }
    }
}

val cart = Cart()

// 唯一和 React 有关的东西
fun main() {
    initializeOopReact()   // KSP 生成，调一次
    createRoot(container).render(cart.view())
}
```

- **没有 `useState` / `useCallback` / 依赖数组**：字段就是状态，方法就是回调。
- **重渲染有边界**：一个类可以有几个 `@OopReactRender` 方法，每个都是独立的岛，只订阅自己读到的字段，各自被一个
  错误边界包住。
- **逻辑不搬家**：对象相互持有，孙子可以直接写祖父的字段，不经过 context、回调 prop 或 ref。
- **状态类本身就是普通对象**：可以在测试里 `new`，可以被别的类注入，可以脱离 React 单独使用。

## 两个必须知道的约定

1. **成员要写 `@JsName`。** Kotlin/JS 把类成员编译成 `name_<hash>_k$`，运行时只能靠 `@JsName` 找到它们；
   注解处理器对缺少它的成员直接报错。
2. **启动时调一次 `initializeOopReact()`。** Kotlin/JS 既没有类定义钩子也没有运行时注解反射，这一行取代了
   TypeScript 装饰器的时机。

## 仓库结构

| 目录 | 内容 |
| --- | --- |
| `ktjs-oop-react/` | 发布的库：注解、运行时、MobX 绑定 |
| `ktjs-oop-react-ksp/` | KSP 处理器：注解 → 元数据表 |
| `demo/` | 演示页：一张对象图 + 五条哈希路由，UI 用 MUI |
| `docs/` | 文档站（未改写，见 `TODO.md`） |

## 本地开发

```bash
./gradlew :ktjs-oop-react:jsBrowserTest   # 库的浏览器测试
./gradlew :demo:e2eTest                   # demo 的端到端套件
./gradlew pagesSite                       # 组装 Pages 产物到 build/pages
```

约定、机制与坑见 [`AGENTS.md`](AGENTS.md)，进度见 [`TODO.md`](TODO.md)。

## 许可

MIT
