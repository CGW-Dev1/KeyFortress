# KeyFortress (安卓轻量级个人密码管家)

> **安全 · 离线 · 纯粹**
> 
> 一款专为个人打造的离线优先、高安全性、极简交互的 Android 本地密码管理工具。

---

## 🌟 核心原则与安全架构

1. **绝对离线 (Zero-Network Permission)**
   * `AndroidManifest.xml` 中 **绝不声明** `android.permission.INTERNET` 权限。
   * 无任何第三方分析上报 SDK，从底层彻底根除云端泄露与网络侧攻击风险。
2. **零明文落盘 (Zero Plaintext on Disk)**
   * **密钥派生**：用户设置的主密码配合高强度随机盐（Salt），经由 **PBKDF2WithHmacSHA256 (100,000 次哈希迭代)** 派生出 256 位加密密钥。
   * **字段与全库双重加密**：采用 **AES-256-GCM** 算法加密密码与备注字段，并结合 **SQLCipher** 实现 Room 数据库全库高强度加密。
   * **硬件级密钥托管**：利用 **Android Keystore** 系统生成硬件关联对称密钥，配合指纹/面部识别进行硬件级保护与快速解锁。
3. **系统级防窥屏保护 (Screen Security)**
   * 全局页面均注入 `WindowManager.LayoutParams.FLAG_SECURE` 安全标记：
     * 阻止系统及第三方工具截图、录屏；
     * 在 Android 多任务切换视图（最近任务列表）中自动将应用卡片变黑防窥。
4. **剪贴板隐私防护 (Clipboard Auto-Clear)**
   * 复制密码时，针对 Android 13 (API 33) 及以上系统自动添加 `ClipDescription.EXTRA_IS_SENSITIVE` 标记，屏蔽系统前台弹出明文浮窗。
   * 启动 30 秒后台计时器，若剪贴板依然保留该密码，将自动从剪贴板彻底清除。

---

## 📱 核心功能清单

| 模块 | 功能名称 | 优先级 | 实现状态 | 说明 |
|---|---|---|---|---|
| **安全认证** | 主密码设置与验证 | P0 | ✅ 已实现 | 首次启动设置主密码，派生 256 位密钥，不直接存盘 |
| **安全认证** | 生物识别解锁 | P0 | ✅ 已实现 | 调用 AndroidX BiometricPrompt，支持指纹/面部秒级解锁 |
| **安全认证** | 后台自动锁定 | P0 | ✅ 已实现 | 监听应用前后台生命周期，支持立即 / 30秒 / 1分钟 / 5分钟锁定 |
| **密码管理** | 条目增删改查 | P0 | ✅ 已实现 | 支持平台、账号、密码、绑定手机/邮箱、分类、备注、收藏星标 |
| **密码管理** | 密码显隐与一键复制 | P0 | ✅ 已实现 | 默认掩码隐藏，支持一键切换明文，账号密码一键复制 |
| **密码管理** | 随机密码生成器 | P1 | ✅ 已实现 | 8-32位自定义长度，大小写/数字/特殊字符/排除易混淆字符配置 |
| **检索与分类** | 实时搜索与首字母匹配 | P0 | ✅ 已实现 | 内置拼音索引，支持输入拼音首字母（如 `wx` 匹配“微信”） |
| **检索与分类** | 多维度分类与收藏置顶 | P1 | ✅ 已实现 | 全部、收藏、社交、工作、金融、娱乐、其它横向标签快速过滤 |
| **数据安全** | 本地加密备份与恢复 | P0 | ✅ 已实现 | 基于 Android SAF，导出/导入受独立密码保护的 `.vault` 加密文件 |
| **数据安全** | 历史密码记录 | P2 | ✅ 已实现 | 修改密码时自动保留该账号最近 3 次的历史旧密码与修改时间 |

---

## 🛠️ 技术栈与架构

* **编程语言**：Kotlin 2.0.21
* **UI 框架**：Jetpack Compose (Material Design 3, 支持深色/浅色模式)
* **架构模式**：MVVM + 单向数据流 (UDF) + Coroutines Flow
* **本地持久化**：Jetpack Room + SQLCipher (4.5.4)
* **加密与认证**：
  * `androidx.biometric:biometric`
  * `androidx.security:security-crypto`
  * `PBKDF2WithHmacSHA256` + `AES-256-GCM` + `AndroidKeyStore`
* **文件存储交互**：Android Storage Access Framework (SAF)

---

## 📂 项目结构

```
KeyFortress/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/keyfortress/app/
│       │   │   ├── KeyFortressApplication.kt     // 生命周期监控与自动锁定
│       │   │   ├── MainActivity.kt               // FLAG_SECURE 防截屏与 Compose 入口
│       │   │   ├── data/
│       │   │   │   ├── model/                    // 数据模型 (分类、密码强度、备份格式)
│       │   │   │   ├── entity/                   // Room 数据表 (AccountEntity, HistoryEntity)
│       │   │   │   ├── dao/                      // 数据库访问对象 (AccountDao, HistoryDao)
│       │   │   │   ├── database/                 // SQLCipher 加密数据库 (AppDatabase)
│       │   │   │   └── repository/               // 统一数据仓库 (AccountRepository)
│       │   │   ├── security/
│       │   │   │   ├── CryptoManager.kt          // PBKDF2 派生与 AES-256-GCM 加解密
│       │   │   │   ├── KeyStoreManager.kt        // Android Keystore 硬件级生物密钥管理
│       │   │   │   ├── BiometricAuthHelper.kt    // 系统生物识别弹窗封装
│       │   │   │   ├── ClipboardHelper.kt        // 敏感标记与 30 秒自动清理剪贴板
│       │   │   │   ├── AppLockManager.kt         // 保险库会话锁定、超时判定与密钥管理
│       │   │   │   └── PasswordGenerator.kt      // 高强度随机密码生成器
│       │   │   ├── util/
│       │   │   │   ├── PinyinHelper.kt           // 中文汉字拼音与首字母提取 (wx -> 微信)
│       │   │   │   ├── AccountMasker.kt          // 账号脱敏遮罩 (138****1234)
│       │   │   │   └── TimeFormatter.kt          // 友好的时间格式化
│       │   │   └── ui/
│       │   │       ├── theme/                    // Fortress 风格主题与配色
│       │   │       ├── components/               // 密码卡片、搜索栏、密码生成器弹窗等
│       │   │       ├── navigation/               // Compose 页面导航路由
│       │   │       ├── lock/                     // 首次设置与解锁页面
│       │   │       ├── home/                     // 首页列表、搜索、过滤
│       │   │       ├── detail/                   // 详情页与历史旧密码
│       │   │       ├── edit/                     // 新增与编辑账号
│       │   │       └── settings/                 // 安全设置与数据导入导出
│       │   └── res/                              // 图标、矢量图、样式资源
│       └── test/                                 // 单元测试 (加密、密码生成、拼音匹配)
├── build.gradle.kts
├── settings.gradle.kts
└── KeyFortress-v1.0.0.apk                        // 已编译完成的安装包 APK
```

---

## 🚀 编译与构建

本工程已内置完整 Gradle Wrapper 与构建环境。在终端执行：

```powershell
# 运行单元测试
.\gradlew.bat testDebugUnitTest

# 编译打包 Debug APK
.\gradlew.bat assembleDebug
```

编译生成文件位于：
`app/build/outputs/apk/debug/app-debug.apk` （根目录已同步输出 `KeyFortress-v1.0.0.apk`）。
