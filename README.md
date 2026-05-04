# NX IDE

🤖 AI 驱动的安卓开发环境

## 功能特性

### 📝 代码编辑器
- Kotlin / XML / Gradle / Properties 语法高亮
- 行号显示（可配置）
- 多标签页支持，带修改指示器
- 自动换行（可配置）
- 括号高亮
- 查找与替换（支持大小写、正则表达式）
- 全局搜索（跨文件搜索）

### 📁 文件管理
- 文件树浏览器
- 新建文件 / 文件夹
- 重命名 / 删除文件
- 右键上下文菜单
- 文件类型图标区分

### 📋 项目模板
- 20+ 安卓项目模板
- 分类浏览（基础、Google、架构、UI组件、数据、系统）
- 语言/构建/UI框架筛选

### 🔨 构建系统
- 可视化构建流程
- 实时构建状态
- 构建步骤详情与耗时

### 📋 Logcat
- 实时日志查看
- 日志级别过滤（Info/Warn/Error/Debug）
- 日志计数

### 💻 终端
- 内置终端模拟器
- 丰富的命令支持：
  - 文件系统：`ls`, `cd`, `pwd`, `mkdir`, `touch`, `cat`, `head`, `tail`, `wc`, `find`, `grep`
  - 构建工具：`gradle`, `./gradlew`
  - Android：`adb` (devices, install, shell)
  - 版本控制：`git` (status, log, branch, diff, commit, push, pull)
  - 系统工具：`echo`, `date`, `whoami`, `hostname`, `uname`, `history`
- 命令历史记录

### 📐 Layout Inspector
- UI 层级树
- 手机预览

### 🤖 AI 助手
- 智能代码建议
- 代码生成
- 架构优化建议
- 对话式交互
- 快捷建议面板

### ⚙️ 设置
- 字体大小调节
- Tab 宽度配置
- 行号显示开关
- 自动换行开关
- 自动缩进开关

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构**: MVVM + StateFlow
- **最低 SDK**: Android 8.0 (API 26)
- **目标 SDK**: Android 14 (API 34)
- **构建工具**: Gradle 8.5 + AGP 8.2.2

## 项目结构

```
app/src/main/java/com/nxide/
├── MainActivity.kt              # 入口 Activity
├── data/
│   ├── Models.kt                # 数据模型
│   ├── DefaultProject.kt        # 默认项目数据
│   └── SampleCode.kt            # 示例代码
├── viewmodel/
│   └── NxIdeViewModel.kt        # 主 ViewModel
├── ui/theme/
│   ├── Color.kt                 # 颜色定义
│   ├── Type.kt                  # 字体定义
│   └── Theme.kt                 # 主题配置
├── screens/
│   ├── MainScreen.kt            # 主屏幕
│   └── TemplateScreen.kt        # 模板选择
└── components/
    ├── NxTopBar.kt              # 顶部导航栏
    ├── FileExplorer.kt          # 文件浏览器
    ├── CodeEditor.kt            # 代码编辑器
    ├── BottomToolbar.kt         # 底部工具栏
    ├── BottomPanelContainer.kt  # 底部面板容器
    ├── AiPanel.kt               # AI 助手面板
    └── panels/
        ├── LayoutPanel.kt       # 布局检查器
        ├── BuildPanel.kt        # 构建输出
        ├── LogcatPanel.kt       # 日志查看
        ├── TerminalPanel.kt     # 终端
        ├── FilesPanel.kt        # 文件管理
        └── PanelHeader.kt       # 面板头部
```

## 构建与运行

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

## 键盘快捷键

| 快捷键 | 功能 |
|--------|------|
| Ctrl+F | 查找 |
| Ctrl+H | 查找并替换 |
| Ctrl+Shift+F | 全局搜索 |
| Ctrl+N | 新建文件 |
| Ctrl+W | 关闭当前标签页 |

## 许可证

MIT License
