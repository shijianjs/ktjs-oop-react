# AGENTS.md

本文件记录跨会话需要保留的项目约定：构建方式、模块结构、注解机制、容易踩的坑。只与某个目录强相关的
说明放在该目录下的 `AGENTS.md`，这里保留指向它的简短索引。

## 本地开发环境

- 仓库按 `<workspace>/<owner>/<repo>` 存放；目录约定、自有项目清单与可参考的第三方克隆见上级目录的
  `AGENTS.md`，查资料前先读它。
- 上游资料一律读本地克隆，**不要访问 `raw.githubusercontent.com`，也不要用网页抓取 GitHub**：本机所在的
  国内网络访问这些域名经常超时。
- 查上游 API 的语义信息（某个类型的父类型链、谁实现了某个接口、某个方法覆盖了谁）优先走 IDE 的 MCP
  server，而不是只 grep 源码目录：两个 server 的分工、`scope: project_and_libraries` 之类的坑与现成脚本见
  [`.agents/skills/intellij-mcp-plugins/`](.agents/skills/intellij-mcp-plugins/SKILL.md)。

## 项目概况与模块结构

`ktjs-oop-react` 把 TypeScript 库 [`oop-react`](https://github.com/shijianjs/oop-react) 的模型移植到
Kotlin/JS：状态和行为待在一个**普通类**里，只有标了 `@OopReactRender` 的方法参与重渲染；可观察性就是
MobX 本身，不做抽象层。**它不是 npm 包的 wrapper**，所以版本是手写的语义版本，模块列在根构建脚本的
`semanticModules` 里。

- `ktjs-oop-react`：发布的库。注解、运行时（patch prototype、渲染岛、错误边界、配置、异步助手、
  选择集、生命周期）与 MobX 绑定。
- `ktjs-oop-react-ksp`：KSP 处理器，把注解变成运行时用的元数据表。消费者在自己的模块里应用 KSP。**不发布
  Dokka**，它是构建期工具。
- `demo`：演示页，发布在 Pages 的 `/demo/`。**它是参考消费者**：应用 KSP、用 MUI、用一张对象图渲染整页。
- `docs`：文档站（模板遗留，尚未改写，`settings.gradle.kts` 里暂时不 include）。logo 仍放在
  `docs/src/jsMain/resources/static/img/`，demo 通过 `brandAssets` 任务拷过去。

Kotlin 包名统一以账号的 GitHub Pages 域名倒写为前缀：`io.github.shijianjs.ktjs.oopreact`。库里按能力分
包：`render/`（注解、岛、observer、回调、observer 原语）、`reactivity/`（注解、可观察性、元数据、列表）、
`component/`、`error/`、`config/`、`async/`、`lifecycle/`、`selection/`、`internal/`（JS 互操作）。

## 注解机制（改库之前先读）

TypeScript 版靠装饰器在**类定义时**改 prototype、包构造器。Kotlin 两样都没有：既没有类定义钩子，也没有
运行时注解反射。这里的替代方案是：

1. **注解 + KSP 生成元数据表。** `ktjs-oop-react-ksp` 在构建期收集 `@OopReactState` / `@OopReactRender` /
   `@OopReactIgnore` / `@OopReactAction`，生成
   `io.github.shijianjs.ktjs.oopreact.generated.initializeOopReact()`：一张 `oopReactMeta(...)` 的表。
2. **应用启动时调一次那个函数。** 它把元数据挂到构造器上（symbol key，不进枚举），并给每个渲染方法在
   prototype 上装一个包装函数。没有这一行，注解类就是普通类。这是全项目**唯一**需要手写的仪式。
3. **成员必须写 `@JsName`。** Kotlin/JS 把类成员编译成 `name_<hash>_k$`，哈希无法从源码推出，运行时只能靠
   `@JsName` 找到它们。处理器对缺少 `@JsName` 的注解成员直接报错，并给出该写什么。
4. **可观察性是惰性的，而且会沿对象图扩散。** `@OopReactState` 的实例在**第一次被渲染**时（或者显式调用
   `oopReactObservable(this)` 时）才 `makeObservable`；随后它自己的字段被标注，字段里那些属于已注册状态类的
   对象也被递归标注。这样继承不需要包装构造器：那一刻所有类都构造完了。库自带的类
   （`OopReactLoading` / `OopReactResource` / `OopReactSelection`）在 `init` 里自己调用，不依赖 KSP。
5. **渲染方法是岛。** patch 后的方法返回 `<TrackedRender>` + 错误边界 + memo 三件套；一个方法一个订阅、一个
   边界。岛按 `(实例, 原方法, 参数, 名字)` 记忆化，所以父岛重渲染不会带着子岛一起跑。
6. **`@OopReactRender` 的 key 从属性读。** `key = "id"` 先看第一个参数再看实例；读到方法就调用它
   （`key = "rowKey"` + `@JsName("rowKey") fun rowKey(item)`）。没有 key 时用 `<实例 id>.<方法>` 兜底，
   这样 React 不会把同一个 fiber 复用给另一个岛。

改这里时要守住的契约（与 TypeScript 版一致）：渲染方法只读状态；一个渲染方法 = 一个岛；错误边界在岛的外面；
库不依赖 UI 组件库（默认 fallback 是内联样式的朴素 `div`）；不引入 React 的状态驱动重渲染模型。

## 容易踩的坑

- **`js(...)` 只替换局部变量，不替换属性。** `js("items[index]")` 里如果 `items` 是属性，会编译成一个裸标识
  符，运行时 `ReferenceError`。先把属性取到 `val` 再拼进片段。
- **DSL 里的名字会被元素属性遮蔽。** `div { +"hidden=$hidden" }` 里的 `hidden` 是 `<div>` 的 `hidden` 属性，
  不是类字段；`Button { value = ... }` 同理（`value` 是 button 属性）。在方法开头把值取到局部变量，或写
  `this@ClassName.field`。这是本项目最容易出血的地方。
- **`jsx { }` 是渲染方法的主体。** 元素 DSL（`div {}`、`Button {}`）只在 `ChildrenBuilder` 里可用，而渲染方
  法返回 `ReactNode`；`jsx { }` 就是那个 block（一个 Fragment，DOM 上不额外加节点）。
- **suspend 函数不要被标注成 action。** MobX 的 action 包装夹在 `suspend` 调用和续体机制之间，运行时会抛
  `TypeError: yield* ... is not iterable`。`OopReactLoading` 因此把 `loading` 放进一个独立的小状态对象，
  `run` 本身保持不可观察。
- **可观察字段必须是可写属性。** `var x private set` 会让 `makeObservable` 失败；状态字段用普通 `var`。
- **MUI wrapper 的 `sx` 不是所有组件都有，`TextField` 也没有 `onChange`。** 前者只在 `PropsWithSx` 上；后者
  只声明在三个 variant 类型上（`TextFieldProps` 是 `BaseTextFieldProps` 的别名，没有 `onChange`）。demo 的做
  法是：布局用 `Container`/`Stack` 自带属性和 `demo.css` 里的几个类；输入用
  `FormControl` + `InputLabel` + `OutlinedInput`。
- **MUI 的 `ChangeEventHandler<C, T>` 目标是 `Element`，不是 `HTMLInputElement`。** `event.target.value` 编不过，
  统一走 demo 里的 `inputValue(event)`（一处 `js()` 说明为什么）。
- **生产构建会把类名压成 `iN`。** 岛名用的是 KSP 记下的**源码类名**（`oopReactMeta(..., name = "Foo")`），所以
  错误报告里看到的是 `FailurePage.broken` 而不是压缩名。

## 浏览器 DOM 类型体系

统一使用 JetBrains `kotlin-browser`（`org.jetbrains.kotlin-wrappers:kotlin-browser`）的 `web.*` 类型，不要在
公开 API 中混用 `org.w3c.dom.*` 或 `kotlinx.browser.*`。`build`/`check` 会执行 Detekt 的 `ForbiddenImport`
规则（根 `detekt.yml`）。Detekt 版本固定为 `2.0.0-alpha.6`。

## Kotlin/JS 模块格式与测试运行时

- 所有 `js` 目标统一 `useEsModules()`；库在 JS target 上加 `-Xir-dce=false`（库没有 `@JsExport` 根，否则产物
  会被削空）。
- 浏览器测试用新版测试 DSL：`@OptIn(ExperimentalJsTestDsl::class) browser { test { chromium() } }`，由
  Playwright 驱动。Playwright 浏览器从国内镜像装：设 `PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright/`
  后先 `./gradlew --stop` 再跑安装任务（只在本机生效，不要提交，CI 走官方源）。
- 异步用例直接返回 `Promise<Unit>`（`oopReactTest { … }` 助手），不要 sleep，也不要用协程测试库：被测的是
  React 自己的调度。
- 大文件或网络盘上的 Gradle 文件监听会漏改动，本机加 `-Dorg.gradle.vfs.watch=false`。
- 根构建脚本用 `NpmExtension.override("webpack", ...)` 固定一份 webpack。

## 测试

- 库的浏览器测试在 `ktjs-oop-react/src/jsTest`，用 Testing Library 驱动真实 DOM。**测试里的注解类手写
  `oopReactMeta(...)`**（KSP 只看消费者的 `jsMain`），这同时是生成代码形状的文档。
- demo 的端到端套件是 `demo/src/jvmTest` 的 Playwright JUnit 5 套件，覆盖「整页能不能跑起来」：岛隔离、列表、
  异步表格、错误边界、语言/主题切换、控制台无错。`./gradlew :demo:e2eTest`；不在 `check` 里。
- 断言一律用 Playwright 可重试的 `assertThat`（状态变化发生在点击之后，`isVisible()` 不重试）。
- 已知未解决、暂时 `@Ignore` 的用例在文件里注明症状，清单见 `docs/development/parity.md`。

## 版本约定

两个模块都是语义版本（`0.1.0`），写在各自 `build.gradle.kts` 里；工作树里带 `-SNAPSHOT`，`-Pktjs.release=true`
去掉它。发布走 `.github/workflows/release.yml` 两段式（本仓库 → hub → Central），流程与模板项目一致。

## CI 与依赖更新

- `ci.yml` 只在 PR / 手动触发时跑 `./gradlew build`（JDK 21，Ubuntu），不被 push 触发。
- **锁文件**：新增 npm 依赖（`npm(...)` 声明、根 `package.json`）后，必须在**官方源**下重刷：
  `npm_config_registry=https://registry.npmjs.org/ ./gradlew --stop && ./gradlew kotlinUpgradePackageLock --rerun-tasks`，
  否则锁里会写进镜像地址，CI 在 `:kotlinStorePackageLock` 报 `Lock file was changed`。
- Gradle wrapper 固定 `9.4.1`，Dependabot 忽略 `gradle-wrapper`（IDEA 2026.2.3 同步 9.8.x 会失败）。

## 演示页面与 GitHub Pages

- demo 是哈希路由（`#/todos`），静态目录里不需要服务器配置。
- `./gradlew pagesSite` 组装 `build/pages`：demo 在 `demo/`，API 在 `api/`。文档站接入后放根目录。
- 端到端套件与 demo 共用一份 Chromium，`com.microsoft.playwright:playwright` 的版本必须与 KGP 安装的 npm
  `playwright` 一致（当前 1.61.0）。

## 文档约定

- 需要跨会话的信息（环境、机制、坑）写在本文件；只与某个目录强相关的放该目录 `AGENTS.md`。
- 公开声明都要有 KDoc，解释「为什么」，不解释「做了什么」。面向使用者的文案中文，代码标识符英文。
- `TODO.md` 是长期任务的进度总览，明细在 `docs/development/`。
