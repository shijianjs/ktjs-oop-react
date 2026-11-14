# TODO

## ktjs-oop-react：把 `oop-react` 的模型移植成 Kotlin/JS 库

TypeScript 的装饰器换成「注解 + KSP 生成的元数据表 + 运行时 patch prototype」；React 用官方
kotlin-wrappers 绑定，响应式就是 MobX。

- 总计：41
- 已完成：26
- 明确不做：0
- 待完成：15

详细清单与每项的验证依据：[docs/development/parity.md](docs/development/parity.md)

### 当前待办

- [ ] 修两个已 `@Ignore` 的行为缺口：继承字段/父岛重渲染后子岛不更新（见 parity.md「已知缺口」）
- [ ] 给 `oopReactConfigure`、`oopReactComponent`、`oopReactBridge`、`OopReactCleanup`、`OopReactSelection` 补测试
- [ ] `ktjs-oop-react-ksp` 的单元测试（生成结果 + 缺 `@JsName` 的报错）
- [ ] 文档站：`docs/` 还是模板内容，`settings.gradle.kts` 里暂未 include，需要整体改写
- [ ] 根 README 与 `ktjs-oop-react/README.md` 换成这个项目的说明
- [ ] `.github/workflows` 里残留模板的模块名与站点路径，需要按新结构核对
- [ ] `OopReactResource` / `OopReactAsyncView` 的两个 `@Ignore` 用例（测试写法本身可疑）

### 注意事项

- 成员必须写 `@JsName`：Kotlin/JS 把成员编译成 `name_<hash>_k$`，运行时只能靠它找到。处理器会直接报错。
- `initializeOopReact()` 必须在应用启动时调一次 —— 它取代了装饰器「类定义时执行」的时机。
- 库自带的类在自己的 `init` 里调 `oopReactObservable(this)`，不经 KSP。
- DSL 里的裸名字会被元素属性遮蔽（`hidden`、`value`…）：先取局部变量，或写 `this@Class.field`。
- 状态字段要可写（不能 `private set`），suspend 函数不能被标注成 MobX action。
- 完成状态以浏览器测试 / e2e 为准，代码写完不算。
