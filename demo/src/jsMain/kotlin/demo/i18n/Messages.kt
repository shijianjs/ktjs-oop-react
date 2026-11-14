package demo.i18n

/**
 * The demo's copy, one entry per phrase with both languages side by side.
 *
 * A map lookup keyed by an enum, not an i18n library: two languages and a few dozen strings do not
 * need one, and the enum makes a missing translation a compile error instead of a runtime fallback.
 */
enum class Msg(
    val zh: String,
    val en: String,
) {
    appTitle("ktjs-oop-react 演示", "ktjs-oop-react demo"),
    tagline(
        "状态和方法待在一个普通类里，只有 @OopReactRender 方法参与重渲染",
        "State and behaviour live in a plain class; only @OopReactRender methods re-render",
    ),
    navCounter("计数器", "Counter"),
    navTodos("待办", "Todos"),
    navTictactoe("井字棋", "Tic-tac-toe"),
    navOrders("订单", "Orders"),
    navFailure("失败", "Failure"),
    toggleTheme("切换主题", "Toggle theme"),
    toggleLocale("切换语言", "Switch language"),

    counterTitle("一个对象的三个岛", "Three islands of one object"),
    counterHint(
        "三个方法各自是一个岛：点其中一个，只有它自己的计数变。页面顶部那个数字是整棵子树的重渲染次数。",
        "Each method is its own island: clicking one updates only its own counter. The number at the top of " +
            "the page is how many times the whole subtree re-rendered.",
    ),
    increment("加一", "Increment"),
    decrement("减一", "Decrement"),
    reset("重置", "Reset"),
    renders("渲染次数", "renders"),

    todosTitle("列表就是对象字段", "The list is an object field"),
    todosHint(
        "OopReactList 的每次增删都只通知一次；条目是否完成读的是它自己的字段。",
        "Every add or remove on OopReactList notifies once; whether an item is done is that item's own field.",
    ),
    todoPlaceholder("要做什么？", "What needs doing?"),
    add("添加", "Add"),
    remove("删除", "Remove"),
    remaining("{count} 条未完成", "{count} left"),

    tictactoeTitle("官方教程 + 全部作业", "The official tutorial, all assignments"),
    tictactoeHint(
        "棋盘格子读的是 history 的当前项，判胜是一次普通方法调用；两个岛分别订阅这两件事。",
        "The board reads the current entry of `history`; the winner is an ordinary method call. Two islands " +
            "subscribe to the two.",
    ),
    nextPlayer("轮到 {player}", "{player} to move"),
    winner("赢家：{player}", "Winner: {player}"),
    draw("这一局平了", "It is a draw"),
    restart("重开", "Restart"),
    jumpTo("回到第 {step} 步", "Go to move #{step}"),
    jumpStart("回到开局", "Go to start"),

    ordersTitle("异步数据 + 受控表格", "Async data and a controlled table"),
    ordersHint(
        "行数据来自 OopReactResource（模拟一次网络请求），选中状态是 OopReactSelection，编辑走对话框。",
        "Rows come from OopReactResource (a simulated request), selection is OopReactSelection, editing goes " +
            "through a dialog.",
    ),
    refresh("刷新", "Refresh"),
    loadingMessage("正在加载…", "Loading…"),
    loadFailed("加载失败：{message}", "Load failed: {message}"),
    newOrder("新建订单", "New order"),
    editOrder("编辑订单", "Edit order"),
    customer("客户", "Customer"),
    amount("金额", "Amount"),
    status("状态", "Status"),
    actions("操作", "Actions"),
    edit("编辑", "Edit"),
    save("保存", "Save"),
    cancel("取消", "Cancel"),
    selectedCount("已选 {count} 行", "{count} row(s) selected"),
    clearSelection("清空选择", "Clear selection"),
    saved("已保存", "Saved"),
    deleted("已删除", "Deleted"),
    customerRequired("客户不能为空", "Customer is required"),
    amountInvalid("金额必须是正数", "Amount must be a positive number"),
    statusNew("待处理", "New"),
    statusPaid("已付款", "Paid"),
    statusShipped("已发货", "Shipped"),

    failureTitle("失败的只是那个岛", "Only the island fails"),
    failureHint(
        "下面两个方法属于同一个对象：一个抛错，一个正常。错误边界包住的是岛，不是页面。",
        "These two methods belong to the same object: one throws, one works. The boundary wraps the island, " +
            "not the page.",
    ),
    throwError("抛一个错误", "Throw an error"),
    healthy("这个岛一直在", "This island is still here"),
    brokenOnPurpose("这个岛每次都抛错", "This island throws on purpose"),

    renderCount("整棵子树渲染了 {count} 次", "The subtree rendered {count} time(s)"),
}
