# E430

使用 Kotlin 和 Jetpack Compose 开发的轻量化 e621/e926 Android 客户端。目前已完成匿名浏览的初步业务闭环。

## 当前功能

- 常驻顶部搜索栏可输入标签搜索并展开筛选区域；从任意一级页面提交搜索都会回到 Home 并滚动到顶部，Latest、Popular 与 Pools 不受 Home 搜索词影响。
- 左侧导航抽屉包含 Home、Latest、Popular、Favorites 和 Pools；未登录状态会明确提示。
- Home、Latest 与 Popular 使用紧凑的双列瀑布流预览，显示分数、收藏数、评论数和评级；Pools 显示封面、标题和帖子数量。点击 Pool 后会按目录顺序单列浏览其中的媒体，页面只保留当前 Post 和下一个 Post 的加载窗口，并按当前网络对应的默认图片质量加载。
- Popular 使用站点的月榜接口，只展示当前日历月的热门内容；该接口不提供连续分页。
- 媒体与 Pool 页面支持下拉刷新，具备首次加载、空结果、错误和保留内容的刷新失败状态。
- 抽屉内可切换 e621/e926 和深色主题；从抽屉进入的账户、Presets 与更多设置页面从右侧滑入，返回时向右滑出并重新打开抽屉。
- Home 启动时直接载入当前用户选择的搜索预设；没有预设或预设搜索词为空时，将 `date:1_month_ago.. order:score` 填入搜索栏作为月度热门回退检索。
- Tools 中的 Presets 按账户保存搜索预设；首次使用由系统授权共享 Documents 目录，此后应用会在启动时读取或创建 `Documents/E430/<username>.json`。搜索下拉栏可以导入预设，Home 的自定义模式也从已有预设中选择。
- 可使用用户名和 API key 登录；凭据由 Android Keystore 管理的密钥加密保存，启动时会尝试恢复账户。登录后通过站点的 `/favorites.json` 接口浏览 Favorites，并可从账户二级页面查看基本资料、编辑黑名单或登出。帖子检索结果会自动应用当前账户的黑名单。
- 默认语言为英语。英语资源位于根目录 `Language/en`，并由 app 模块直接编译。
- API 请求统一使用 `E430/0.1 (by FredWd on e621)` User-Agent，并限制为持续每秒最多一次；媒体请求不附加账户凭据。
- 缺少预览地址或实际加载失败的媒体会从网格中隐藏，不显示损坏图片占位符；GIF、视频及其他非静态图片格式会在预览右上角显示格式角标。
- 点击帖子预览可进入详情页，并可按当前预览或 Pool 的目录顺序左右切换；切换时页面从对应方向滑入，返回列表时会将最后查看的帖子置于屏幕中部。Pool 内的图片和 GIF 可直接进入同一详情实现，视频通过独立按钮进入。
- Post 详情的 Information 区域优先以紧凑预览卡展示 Parent 和 Children；点击关联 Post 后返回会回到上一个 Post。属于 Pool 的 Post 会在媒体上方列出 Pool 名称以及 First、Previous、Next、Last 导航。导航状态采用替换式页面和有界返回栈，达到第六层后返回最初的二级页面。
- 图片在按流量计费网络与 Wi‑Fi 下分别使用独立的默认质量，初始值为 Low 与 Medium，并支持手动切换和全屏缩放。Wi‑Fi 下会预取列表后续 10 张预览图以及当前详情前后各两个帖子的详情、评论和图片；按流量计费网络的预取默认关闭，可在设置中开启。详情数据使用最多 10 个帖子的内存 LRU 缓存，执行新的 Home 搜索前会清理列表、详情和图片内存缓存。
- GIF 使用 Coil 解码；视频使用 Media3，控制栏直接提供播放、进度、速度、循环、静音和最右侧全屏按钮。视频默认暂停、静音并循环播放，关闭静音会立即恢复正常音量。详情中的 HTTP/HTTPS 媒体来源可交由系统默认浏览器打开。
- 登录用户可以评分、添加或移除收藏、发布和编辑自己的评论，并可隐藏自己的评论。点击 Tag 会打开可替换的三级检索页面。
- 下载使用系统 DownloadManager，默认保存到 `Download/E430`，可在设置中更改 Download 下的子目录；分享会将当前展示的媒体流式写入单文件临时缓存后调用系统分享面板。

筛选面板的具体选项尚未实现。评论列表当前按接口上限读取前 100 条。

## 构建环境

- Android Studio：使用支持 AGP 9.1.1 的版本。
- Gradle Wrapper：9.3.1；AGP：9.1.1。
- Gradle Daemon：JDK 21（由 `gradle/gradle-daemon-jvm.properties` 声明）；Java 编译目标：11。
- Kotlin：使用 AGP 内置 Kotlin 支持；Compose 编译器插件：2.2.10。
- Android SDK Platform：37.0；Build Tools：36.0.0。
- `compileSdk`：37.0；`targetSdk`：36；`minSdk`：28（Android 9）。

依赖版本统一记录在 `gradle/libs.versions.toml`。本机 SDK 路径由 Android Studio 在 `local.properties` 中配置，不应写入共享构建脚本。

## 运行与验证

在 Android Studio 打开仓库根目录，完成 Gradle 同步，选择 `app` 和 Android 9 或更高版本的设备，然后运行或调试。

Windows PowerShell 构建、单元测试和静态检查：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。现有单元测试覆盖帖子排序标签和网络 DTO 的关键边界；界面交互尚未进行设备自动化测试。有设备时可运行 `:app:connectedDebugAndroidTest`。

## 已修复的构建问题

原配置使用 `compileSdk 36.1`，但 Core 1.19.0 和解析得到的 Lifecycle Compose 2.11.0 要求 API 37 或更高，导致 `:app:checkDebugAarMetadata` 失败。已将编译 SDK 调整为 37.0，保留现有依赖、`targetSdk` 和 `minSdk`。

[AGP 9.1.1 官方兼容性说明](https://developer.android.com/build/releases/agp-9-1-0-release-notes)确认支持 API 37.0 和 Gradle 9.3.1。其他开发环境需要安装 SDK Platform 37.0 后再构建。
