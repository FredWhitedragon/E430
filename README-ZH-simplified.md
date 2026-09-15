# E430

[English](README.md) | **简体中文**

E430 是使用 Kotlin 和 Jetpack Compose 开发的轻量化 e621/e926 原生 Android 客户端。项目当前处于 `0.1.0-beta` 阶段，已经具备浏览、搜索、账户互动、Pool 阅读和媒体查看的主要业务闭环。

> E430 是第三方项目，与 e621 或 e926 站点没有隶属、授权或合作关系。

## 已实现功能

### 浏览与搜索

- Home、Latest、Popular、Favorites 和 Pools 五个一级入口。
- Home、Latest 与 Popular 使用双列瀑布流，显示分数、收藏数、评论数、评级和非静态媒体格式角标。
- Popular 使用站点月榜接口，仅展示当前日历月的热门内容；Latest 始终按新到旧排列。
- 顶部搜索栏支持直接输入 e621 metatag。从任意一级页面提交搜索都会进入 Home 并滚动到顶部，不会改变 Latest 或 Popular 的查询条件。
- 搜索下拉栏提供互斥 Rating、Rating 反选、日期/收藏/分数/评论排序、独立升序开关和系统日期范围选择器。控件与手动输入的 `rating:`、`order:`、`date:` 字段双向同步。
- 所有媒体列表支持下拉刷新，并区分首次加载、空结果、刷新失败和加载更多失败。
- 登录后自动应用账户黑名单，并可在账户页面查看和修改黑名单。

### Presets

- Presets 按登录账户分别保存，匿名用户使用独立配置。
- 预设文件保存到共享目录 `Documents/E430/<username>.json`，便于备份和导入；首次使用需要通过 Android 系统文件选择器授权 Documents 目录。
- 搜索栏可以导入预设，Home 也可以直接使用选定预设。
- Home 没有可用预设或预设内容为空时，使用 `date:1_month_ago.. order:score` 作为回退查询。

### 账户与互动

- 使用 e621/e926 用户名和 API key 登录，并在启动时自动恢复会话。
- 凭据由 Android Keystore 管理的密钥加密后保存在应用私有空间；登出时会清除凭据、会话状态和账户相关缓存。
- 登录失败和自动恢复失败会向用户显示提示。
- 登录用户可以浏览 Favorites、评分、添加或移除收藏、发表评论、编辑自己的评论以及隐藏自己的评论。
- 匿名状态下触发账户功能时不会发送写请求，并会提示需要登录。

### Post 详情与媒体

- 详情页展示媒体、评分、收藏数、完整 Rating、文件信息、来源链接、描述、Tags 和评论。
- 图片和 GIF 支持全屏拖动及缩放；视频支持播放、进度、倍速、循环、静音和全屏。
- HTTP/HTTPS 来源链接可交给系统默认浏览器打开。
- 下载使用 Android DownloadManager，默认目录为 `Download/E430`；可选择质量并查看预估大小。分享通过 Android 系统分享面板完成。
- Tags 默认可折叠，点击 Tag 会打开对应搜索结果；详情页的 Parent 和 Children 使用可点击预览卡展示。
- 左右滑动可按照来源列表切换 Post：从右向左进入下一项，从左向右进入上一项。返回列表时会将最后查看的预览图定位到屏幕中部。
- 详情数据使用最多 10 个 Post 的内存 LRU 缓存。Wi-Fi 下预取列表后续 10 张预览图，并预取详情前后各两个 Post 的详情、Tags、评论和图片；按流量计费网络的预取默认关闭。

### Pools 与导航

- Pools 按新到旧显示封面、标题和 Post 数量。
- Pool 详情按目录顺序单列展示媒体，只保留当前 Post 和下一 Post 的加载窗口。
- 图片和 GIF 可直接进入统一的 Post 详情页，视频提供进入详情页的按钮。
- Post 详情中的 Pool 区域提供 First、Previous、Next 和 Last 导航，并保持 Pool 内滑动顺序。
- Pool、Post、Tag、Parent 和 Children 共用有界导航状态；页面层级达到六层后，返回操作会回到最初的二级页面。
- 抽屉、二级页面和详情覆盖层支持系统返回及方向一致的转场动画。一级页面需要在两秒内连续返回两次才会退出应用。

### 设置与语言

- 可切换 e621/e926、深色主题、不同网络下的图片质量、视频自动播放、默认静音、循环播放、Tags 默认折叠、按流量计费网络预取和下载目录。
- 支持英语和简体中文，资源分别存放在 `Language/en` 与 `Language/zh-s`。
- 首次启动会从系统语言列表中选择首个受支持语言；没有匹配项时使用英语。手动选择后始终使用用户保存的设置。
- 抽屉底部显示应用版本、第三方无关联声明和项目仓库链接。

## 网络与隐私

- API 请求统一使用 `E430/0.1 (by FredWd on e621)` User-Agent，并将持续请求频率限制为每秒最多一次。
- 账户 API 使用 HTTP Basic Authentication；凭据只会发送到所选 e621/e926 API 域名。
- 媒体文件、缩略图和外部来源链接不会附加账户凭据。
- 项目没有分析追踪、广告 SDK 或后台全站同步。

接口行为以 [E621_API.md](E621_API.md) 和 [e621_openapi.yaml](e621_openapi.yaml) 为准。

## 技术与项目结构

- 单 Activity、Jetpack Compose 和 Material 3。
- AndroidX ViewModel、StateFlow、Coroutines 和单向数据流。
- Retrofit、OkHttp 与 kotlinx.serialization。
- Coil 负责图片及 GIF，Media3 ExoPlayer 负责视频。
- DataStore 保存普通设置，Android Keystore 保护账户凭据。
- 代码按照 `account`、`posts`、`pools`、`presets`、`search`、`settings` 等业务包组织，共用基础设施位于 `core`。

## 构建环境

| 项目 | 当前配置 |
| --- | --- |
| 应用版本 | `0.1.0-beta` |
| Gradle Wrapper | 9.3.1 |
| Android Gradle Plugin | 9.1.1 |
| Kotlin / Compose 编译器插件 | 2.2.10 |
| Gradle Daemon JDK | 21 |
| Java 编译目标 | 11 |
| compileSdk | 37.0 |
| targetSdk | 36 |
| minSdk | 28（Android 9） |

Android Studio 需要支持 AGP 9.1.1，并安装 Android SDK Platform 37.0。依赖版本集中记录在 `gradle/libs.versions.toml`，本机 SDK 路径由 `local.properties` 管理。

## 构建与验证

在 Android Studio 中打开仓库根目录并运行 `app` 配置，或在 Windows PowerShell 中执行：

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

生成的调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

单元测试覆盖查询字段替换、搜索排序、黑名单匹配、语言选择、预设序列化、网络 DTO 和凭据边界。设备 UI 自动化测试仍需在连接模拟器或真机后单独执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## 当前限制

- Popular 月榜接口不提供连续分页。
- 评论列表目前读取接口返回的前 100 条。
- 无法加载、缺少地址或不支持预览的媒体会从列表中隐藏。
- 项目仍处于 Beta 阶段，尚未完成发布签名、商店分发和完整真机 UI 自动化验证。

## 项目链接

[https://github.com/FredWhitedragon/E430](https://github.com/FredWhitedragon/E430)

## 碎碎念……

codex真好用……
这算是我用来熟悉codex和移动开发的练手小项目，我负责需求还有架构，codex负责具体实现。当然主要是自用，因为不管是直接访问网站还是用其它的第三方app都感觉……差点意思？所以目前这一版主要就是实现了基本功能和预设项，后面应该会追加更多功能……吧……？
