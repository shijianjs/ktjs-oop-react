# Parity with `oop-react` (TypeScript)

## 来源与范围

- 上游：https://github.com/shijianjs/oop-react ，本地克隆 `S:/workspace/github/shijianjs/oop-react`
- 版本：`packages/oop-react/package.json` 的 `0.1.1-dev.0`，核对日期 2026-11-15
- 范围：`packages/oop-react/src/index.ts` 导出的全部公开 API，加上 Kotlin 侧必需的补充
- 不在范围：TypeScript 版的 demo 页面逐个照搬（Kotlin demo 用 MUI 重写，覆盖面接近但不逐页对应）

## 统计

| 状态 | 数量 |
|---|---:|
| 总计 | 41 |
| 已完成 | 26 |
| 明确不做 | 0 |
| 待完成 | 15 |

## 判定口径

- **已完成**：有实现，且至少有一处浏览器测试或 demo e2e（`:demo:e2eTest`）覆盖到它。
- **待完成**：没有实现，或实现了但没有通过验证。
- 「代码写完」不算完成。

## 库 API

已完成（括号里是验证它的用例所在的文件）：

| API | 验证 |
|---|---|
| `@OopReactRender` + prototype patch | `render/OopReactRenderTest.kt`、`:demo:e2eTest` |
| `@OopReactState` | `reactivity/OopReactStateTest.kt` |
| `@OopReactIgnore` | 同上（ignored 字段不被观察） |
| `@OopReactAction` | 同上（一次事务一次通知） |
| `oopReactObservable` | 同上（含 overrides、幂等） |
| `OopReactObserver` | `render/OopReactObserverTest.kt` |
| `OopReactCallback` | 同上（带 key 的逐条岛） |
| `OopReactTrackedRender` | 上述岛的底层，两处用例都在跑它 |
| `OopReactErrorBoundary` | `:demo:e2eTest`（failure 页）、demo 手动验证 |
| `DefaultErrorFallback` | 同上 |
| `jsx { }` | 全部渲染方法 |
| `OopReactList` | `:demo:e2eTest`（todos 页增删 + 勾选） |
| `OopReactLoading` / `OopReactTaskResult` / `OopReactTimeoutError` | `async/OopReactAsyncTest.kt`（成功/失败/超时/重叠） |
| `OopReactResource` | `:demo:e2eTest`（异步表格） |
| `OopReactSelection` | `:demo:e2eTest`（选中计数） |
| `@OopReactRender(key = ...)` | `:demo:e2eTest`（todos 行 key） |

待完成：

| API | 状态 |
|---|---|
| `oopReactConfigure` / `OopReactConfig` / `OopReactEnforceActions` | 已实现，**无测试** |
| `oopReactComponent` | 已实现，**无测试** |
| `OopReactBridge` | 2026-11-15 补齐实现，**无测试** |
| `OopReactCleanup` | 已实现，**无测试** |
| `OopReactAsyncView` | 已实现；三态用例 `@Ignore`（依赖下面的 resource 用例） |

## 工具链

| 事项 | 状态 |
|---|---|
| KSP 收集 state/render/ignore/action | 已完成（`:demo:kspKotlinJs` 产物见 `demo/build/generated/ksp/js/jsMain/.../OopReactMetas.kt`） |
| 生成 `initializeOopReact()` | 已完成（demo 启动时调用） |
| 缺 `@JsName` 时报错 | 已实现（未单测） |
| 处理器单元测试 | 待完成 |
| 消费者模块应用 KSP 的构建接线 | 已完成（`demo/build.gradle.kts` 的 `kspJs`） |

## demo

| 页面/部件 | 状态 |
|---|---|
| 应用外壳（Settings/Router/Feedback/主题/语言/导航） | 已完成（e2e：语言与主题切换） |
| 计数器（岛隔离 + 各自的渲染次数） | 已完成（e2e） |
| 待办（`OopReactList` + 行级岛 + key） | 已完成（e2e） |
| 井字棋（历史 + 判胜 + 两层循环） | 已完成（页面可交互，e2e 只验首屏） |
| 订单（`OopReactResource` + 表格 + 对话框 + 校验） | 已完成（e2e：异步行 + 选中） |
| 失败（一个岛抛错、另一个存活） | 已完成（e2e） |
| 端到端套件 | 已完成（7 个用例，`:demo:e2eTest`） |

## 仓库

| 事项 | 状态 |
|---|---|
| 模块改名、`semanticModules`、POM、`package.json`、npm 锁文件 | 已完成 |
| `AGENTS.md` | 已完成 |
| `TODO.md` + 本文件 | 已完成 |
| 根 `README.md` / 模块 `README.md` | 待完成 |
| `docs/` 文档站 | 待完成（仍是模板内容，`settings.gradle.kts` 里未 include） |
| `.github/workflows`（模块名、站点路径、ksp） | 待完成 |

## 已知缺口

1. **继承来的字段写不触发重渲染**（`OopReactStateTest.aSubclassInheritsTheParentsState`，`@Ignore`）。
   症状：子类实例的岛首次渲染正常，写 `base` / `derived` 后不更新。怀疑点：`Base` 是 `open class`、字段在父类原型上，`metasFor` 能拿到 meta，但 `makeObservable` 对继承字段的标注未生效。
2. **父岛重渲染后子岛不更新**（`OopReactObserverTest.anIslandIsSkippedWhenItsArgumentsDidNotChange`，`@Ignore`）。
   症状：`Host.view()` 读了 `label` 并调用 `counter.child("child")`；写 `label` 后文本不变。可能与岛的记忆化比较器无关，而是 Host 的字段没被标注（同类问题）。
3. **一个测试体内连续两次 `resource.load`** 会触发 `TypeError: yield* ... is not iterable`（`OopReactAsyncTest` 两条 `@Ignore`）。demo 里的单次 `load` 正常，判断是测试写法和协程/Testing-Library 的 `act` 交互，不是库的问题。
4. **`TextField` / `sx` 的 MUI wrapper 限制**导致 demo 改用 `FormControl` + `OutlinedInput` 与 `className`，不影响库 API。

## 与上游的已知语义差异（有意为之）

| 点 | TypeScript | 这里 | 原因 |
|---|---|---|---|
| 启动 | 装饰器在类定义时执行 | 启动时调一次 `initializeOopReact()` | Kotlin/JS 没有类定义钩子，也没有运行时注解反射 |
| 成员名 | 装饰器拿到真实属性名 | 必须写 `@JsName` | Kotlin/JS 把成员编译成 `name_<hash>_k$` |
| `@OopReactState` 时机 | 包构造器，构造即观察 | 首次渲染时观察，并沿字段递归 | 没有构造器钩子；惰性观察反而让继承更简单 |
| `init()` 钩子 | 有 | 没有 | 惰性观察之后不再需要；需要更早时在 `init` 里调 `oopReactObservable(this)` |
| computed | MobX 自动识别 getter | getter 被当作方法（读取仍被追踪，但没有缓存） | Kotlin 的 getter 编译成普通方法 |
| 超时 | 任务继续跑 | 取消协程 | Kotlin 的协程可以取消 |
| `OopReactList` | 无（MobX 深度观察 JS 数组） | 新增 | Kotlin 的 `ArrayList` 不是 JS 数组，MobX 无法深观察 |
