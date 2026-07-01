# APKurl 维护文档

APKurl 是一个轻量 Android 链接拦截 App。它可以注册为系统浏览器候选应用，被其他 App 通过 `http://` 或 `https://` 链接唤起后，不主动访问链接，而是把链接显示在界面内容框中，方便用户复制。

## 当前功能

- 可作为系统默认浏览器候选。
- 首页提供“一键设置默认浏览器”按钮。
- 被外部 App 通过网页链接唤起时，拦截传入链接。
- 不加载、不访问、不联网打开被拦截链接。
- 在只读内容框显示最近一次拦截到的链接。
- 新链接到达时，先清空旧链接，再显示新的拦截结果，避免内容叠加。
- 提供“复制链接”按钮，把链接复制到系统剪贴板。
- 长链接内容框限制高度，避免挤掉按钮。

## Android 版本兼容

项目当前配置：

```kotlin
minSdk = 24
targetSdk = 36
```

兼容范围说明：

- `minSdk = 24` 表示支持 Android 7.0 及以上系统。
- Android 7 到 Android 9 使用标准 `ACTION_VIEW` intent 和默认应用设置页作为兼容路径。
- Android 10 及以上优先使用 `RoleManager.ROLE_BROWSER` 请求系统浏览器角色。
- Android 11 及以上包含 `queries` 声明，用于适配包可见性规则下的浏览器解析。
- 面向 Android 17 或更高版本升级时，优先检查 `compileSdk`、`targetSdk`、Android Gradle Plugin、Compose BOM 和浏览器角色 API 是否有行为变化。

注意：Android 不允许应用静默修改默认浏览器。按钮只能拉起系统授权界面或默认应用设置页，最终必须由用户确认。

## 关键文件

### `app/src/main/AndroidManifest.xml`

负责声明 App 入口、浏览器候选能力和包查询能力。

关键点：

- `android:launchMode="singleTask"`：避免每次外部链接唤起都创建多个 Activity 实例。
- `ACTION_VIEW` + `CATEGORY_DEFAULT` + `CATEGORY_BROWSABLE`：让系统把 App 识别为网页链接处理器。
- `<data android:scheme="http" />` 和 `<data android:scheme="https" />`：接收网页链接。
- `<queries>`：Android 11+ 默认应用/浏览器解析兼容声明。

当前没有声明 `INTERNET` 权限，因为 App 的设计目标是拦截并展示链接，不访问链接。

### `app/src/main/java/com/K2TEAM/apkurl/MainActivity.kt`

负责主业务逻辑和 Compose UI。

主要方法：

- `extractUrl(intent)`：从外部 `ACTION_VIEW` intent 中提取链接。
- `updateInterceptedUrl(intent)`：先清空旧内容，再写入新的拦截链接。
- `onCreate()`：处理首次启动或首次外部唤起。
- `onNewIntent()`：处理 App 已在前台/后台时再次被链接唤起。
- `openDefaultBrowserSettings()`：打开系统默认浏览器授权或默认应用设置页。
- `checkIsDefaultBrowser()`：判断当前 App 是否已经是默认浏览器。
- `copyInterceptedUrl(url)`：复制拦截到的链接。
- `BrowserEntryScreen()`：主界面，包含状态文案、链接内容框和按钮。

### `app/src/main/java/com/K2TEAM/apkurl/ui/theme/`

负责 Compose 主题。

当前主题使用灰黑按钮和灰黑主色，并关闭动态取色，避免 Android 12+ 根据系统壁纸把按钮染成紫色或其他颜色。

## 默认浏览器设置流程

用户点击“一键设置默认浏览器”后：

1. 如果当前已经是默认浏览器，显示提示。
2. Android 10+ 检查 `RoleManager.ROLE_BROWSER`。
3. 如果浏览器角色可用且当前 App 还未持有该角色，调用：

```kotlin
roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
```

4. 如果角色 API 不可用，退回到：

```kotlin
Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
```

5. 用户返回 App 时，`onResume()` 会刷新默认浏览器状态。

## 链接拦截流程

外部 App 打开链接时，系统会发出类似 intent：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "https://example.com"
```

APKurl 收到后：

1. `MainActivity` 读取 `intent.dataString`。
2. 先清空旧的 `interceptedUrl`。
3. 再保存新的链接到 `interceptedUrl` 状态。
4. UI 内容框显示该链接。
5. App 不使用 WebView，不调用 `loadUrl`，也不声明 `INTERNET` 权限。
6. 用户点击“复制链接”后写入系统剪贴板。

## UI 维护说明

当前界面布局使用 Compose：

- 根布局为 `Column`。
- 内容框使用 `OutlinedTextField`，只读。
- 内容框设置 `minLines = 4`、`maxLines = 8`，防止长链接挤掉按钮。
- 内容框使用 `.weight(1f, fill = false)`，只在有剩余空间时扩展。
- 两个按钮都使用 `Modifier.fillMaxWidth()`，适配窄屏。
- 按钮颜色通过 `ButtonDefaults.buttonColors(...)` 显式设置为灰黑。

如果后续增加更多按钮，建议继续使用整行按钮或底部固定操作区，避免小屏设备上操作入口被长内容挤出屏幕。

## 构建与验证

本机当前用 Android Studio 自带 JBR 构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug --no-daemon
```

安装 Debug APK：

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

测试链接拦截：

```bash
adb shell am start \
  -a android.intent.action.VIEW \
  -c android.intent.category.BROWSABLE \
  -d "https://example.com/path?a=1&b=2"
```

预期结果：

- APKurl 被唤起。
- 内容框显示完整链接。
- 不打开网页。
- “复制链接”按钮可点击。

## 依赖源与镜像

项目已把主要 Gradle/Maven 下载源替换为国内镜像：

- Maven 仓库：阿里云 Maven 镜像。
- Gradle Wrapper：腾讯云 Gradle 镜像。
- Gradle Toolchain JDK：阿里云 Eclipse Temurin 镜像。

相关文件：

- `settings.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/gradle-daemon-jvm.properties`

升级依赖时，优先确认国内镜像中已有对应版本。

## 后续升级建议

1. 升级 Android 版本支持时，先升级 Android Studio、Android Gradle Plugin、Gradle Wrapper 和 Compose BOM。
2. 升级 `targetSdk` 后，重点验证默认浏览器角色授权、包可见性、剪贴板行为和外部链接 intent。
3. 如需保存历史拦截记录，可引入本地数据库或 DataStore，但要避免自动访问链接。
4. 如需分享链接，可添加系统分享 intent。
5. 如需规则过滤，可在 `extractUrl()` 后增加解析和校验逻辑。
6. 如需真正打开链接，建议明确增加一个用户触发按钮，不要在外部唤起时自动访问。

## 常见问题

### 为什么不能真正“一键”静默设为默认浏览器？

这是 Android 系统安全限制。默认浏览器属于系统角色或默认应用设置，必须由用户在系统界面确认。

### 为什么不声明 `INTERNET` 权限？

当前需求是拦截并复制链接，不访问链接。移除 `INTERNET` 权限可以降低误联网风险，也更符合产品行为。

### 为什么 Android 10+ 用 `RoleManager`？

Android 10 引入角色管理机制，浏览器、短信、电话等默认应用角色通过 `RoleManager` 统一请求。它比直接跳设置页更接近系统推荐流程。

### 为什么还保留默认应用设置页兜底？

部分系统或定制 ROM 可能不完整支持浏览器角色请求。兜底跳转能保证用户仍有路径手动设置默认浏览器。
