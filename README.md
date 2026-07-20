# qlmd3 — 青龙面板 Android 客户端

![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)
![Material3](https://img.shields.io/badge/Material-3-006B5E?logo=materialdesign)
![AGP](https://img.shields.io/badge/AGP-9.3.0-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
[![Build](https://github.com/xl1124/android-qinglong/actions/workflows/build.yml/badge.svg)](https://github.com/xl1124/android-qinglong/actions/workflows/build.yml)

**qlmd3** 是一个使用 **Kotlin + Jetpack Compose (Material 3)** 构建的 [青龙面板 (QingLong)](https://github.com/whyour/qinglong) 非官方 Android 客户端。它通过青龙面板 REST API 在移动设备上提供完整的面板管理体验。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [使用指南](#使用指南)
- [功能模块详解](#功能模块详解)
- [构建配置](#构建配置)
- [常见问题](#常见问题)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 功能特性

### 📋 定时任务管理
- 查看、添加、编辑、删除定时任务（cron 任务）
- 手动运行 / 停止任务
- 启用 / 禁用单个或批量任务
- 置顶 / 取消置顶任务
- 按状态筛选（全部 / 已启用 / 已禁用）
- 长按进入多选模式，批量操作
- 查看任务运行日志（实时轮询）

### 🌐 环境变量管理
- 查看、添加、编辑、删除环境变量
- 单个启用 / 禁用
- 批量操作（多选模式）
- 批量导入 `export KEY="value"` 格式

### 📜 脚本管理
- 文件浏览器：目录树导航
- 在线查看脚本内容（等宽字体只读 / 编辑模式）
- 上传新脚本（文件选择器）
- 在线编辑并保存
- 删除脚本

### 📦 依赖管理
- 查看已安装的依赖（Node.js / Python3 / Linux 分类）
- 按类型筛选
- 安装新依赖
- 重新安装 / 删除依赖
- 查看安装日志（实时轮询）

### 📎 订阅管理
- 查看、添加、编辑、删除订阅
- 手动运行 / 停止订阅
- 启用 / 禁用订阅

### ⚙️ 配置文件管理
- 浏览青龙面板配置文件列表
- 在线查看配置文件内容
- 在线编辑并保存

### 🪵 日志查看
- 浏览日志文件树
- 查看日志文件详情

### 🔐 多账号支持
- 保存多个服务器连接
- 一键切换账号
- 登录日志查看

### 🎨 Material 3 主题
- **动态取色**（Android 12+）：自动跟随系统壁纸颜色
- **深色模式**：支持跟随系统、浅色、深色三种模式
- Material 3 完整配色系统

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0+ |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2026.04.01 |
| 导航 | Navigation Compose | 2.9.8 |
| 网络 | Retrofit 2 + OkHttp 5 | 2.12.0 / 5.4.0 |
| 数据序列化 | Gson | 2.14.0 |
| 本地存储 | DataStore Preferences | 1.2.1 |
| 协程 | Kotlinx Coroutines | 1.11.0 |
| 构建工具 | Android Gradle Plugin | 9.3.0 |
| Gradle | Gradle Wrapper | 9.6.1 |
| 最低 SDK | Android 8.0 (API 26) | |
| 目标 SDK | Android 17 (API 37) | |
| 应用包名 | `me.doujiang.app` | |

---

## 项目结构

```
android-qinglong/
├── app/
│   ├── build.gradle.kts          # 模块构建配置
│   ├── proguard-rules.pro        # 混淆规则
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/me/doujiang/app/
│   │       │   ├── QingLongApp.kt           # Application 入口
│   │       │   ├── MainActivity.kt          # 主 Activity
│   │       │   ├── data/
│   │       │   │   ├── api/
│   │       │   │   │   ├── QingLongApi.kt   # Retrofit API 接口
│   │       │   │   │   └── RetrofitClient.kt # Retrofit 客户端单例
│   │       │   │   ├── local/
│   │       │   │   │   └── LocalStorage.kt   # DataStore 本地存储
│   │       │   │   ├── model/
│   │       │   │   │   └── Models.kt         # 数据模型
│   │       │   │   └── repository/
│   │       │   │       └── QingLongRepository.kt  # 数据仓库层
│   │       │   └── ui/
│   │       │       ├── components/
│   │       │       │   └── SharedComponents.kt    # 共享 UI 组件
│   │       │       ├── navigation/
│   │       │       │   ├── NavGraph.kt       # 导航图
│   │       │       │   └── Routes.kt         # 路由常量
│   │       │       ├── screens/
│   │       │       │   ├── config/           # 配置文件管理
│   │       │       │   ├── dependencies/     # 依赖管理
│   │       │       │   ├── env/              # 环境变量管理
│   │       │       │   ├── home/             # 首页
│   │       │       │   ├── login/            # 登录
│   │       │       │   ├── logs/             # 日志浏览
│   │       │       │   ├── main/             # 主界面（底部导航）
│   │       │       │   ├── scripts/          # 脚本管理
│   │       │       │   ├── settings/         # 设置
│   │       │       │   ├── subscriptions/    # 订阅管理
│   │       │       │   └── tasks/            # 定时任务管理
│   │       │       └── theme/
│   │       │           ├── Color.kt          # 配色方案
│   │       │           ├── Theme.kt          # 主题配置
│   │       │           └── Type.kt           # 排版系统
│   │       └── res/
│   │           ├── drawable/                 # 图标资源
│   │           ├── mipmap-*/                 # 启动图标
│   │           ├── values/                   # 字符串、颜色、主题
│   │           └── xml/                      # 网络安全配置
├── build.gradle.kts              # 根构建配置
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性
├── gradle/wrapper/               # Gradle Wrapper
├── local.properties              # 本地 SDK 路径
├── .gitignore                    # Git 忽略规则
└── README.md                     # 项目文档
```

### 架构分层

项目采用简洁的分层架构：

- **UI 层**（`ui/screens/`）— Jetpack Compose 屏幕 + ViewModel，管理 UI 状态
- **数据层**（`data/`）— Repository 模式，封装 API 和本地存储
- **API 层**（`data/api/`）— Retrofit 接口定义 + 动态 Base URL 客户端
- **模型层**（`data/model/`）— API 响应数据类
- **本地存储**（`data/local/`）— DataStore 持久化用户偏好和账号信息

---

## 快速开始

### 前置要求

- **Android Studio** Ladybug 或更高版本
- **JDK 17** 或更高版本
- **Android SDK 37**（Android 17）
- Android 设备或模拟器（API 26+）
- 一台运行中的 **青龙面板 v2.x+** 服务

### 从源码构建

```bash
# 1. 克隆项目
git clone https://github.com/xl1124/android-qinglong.git
cd android-qinglong

# 2. 确保 local.properties 指向正确的 SDK 路径
# （Android Studio 会自动处理）

# 3. 使用 Gradle Wrapper 构建 Debug APK
./gradlew assembleDebug

# 4. 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **提示**：首次构建 Gradle Wrapper 会自动下载 Gradle 9.6.1，耗时约 1-2 分钟。

### 通过 GitHub Actions 自动构建

每次推送到 `main` 分支或提交 PR 时，GitHub Actions 会自动编译项目并生成 APK 构建产物，确保代码变更不会破坏编译。

### 发布新版本

1. 在 GitHub 仓库页面点击 **Releases** → **Create a new release**
2. 填写版本号（如 `v1.0.0`），标签名以 `v` 开头
3. GitHub Actions 会自动编译 Debug + Release APK，并发布到 Release 页面

> Release APK 是未签名的，安装前需要自行签名，或直接使用 Debug APK（已使用 debug 密钥签名）。

你也可以在本地构建 Release APK：

```bash
./gradlew assembleRelease
# 输出：
# app/build/outputs/apk/debug/app-debug.apk     （已签名，可直接安装）
# app/build/outputs/apk/release/app-release-unsigned.apk  （未签名，需自行签名）
```

> 本地 Release 构建需要配置签名密钥，详见 [Android 官方文档](https://developer.android.com/studio/publish/app-signing)。

---

## 使用指南

### 初次使用

1. 安装 APK 后打开应用
2. 输入青龙面板的**服务器地址**（如 `http://192.168.1.100:5700`）
3. 输入**用户名**和**密码**（青龙面板的管理员账号）
4. 点击登录

> **注意**：如果青龙面板开启了**两步验证（2FA）**，登录后会弹出双因素认证对话框，输入验证码即可。

### 主界面导航

底部导航栏包含 4 个主标签页：

- ⏰ **定时任务** — 任务管理和日志查看
- 🌐 **环境变量** — 环境变量 CRUD 和批量操作
- ⚙️ **配置文件** — 在线查看和编辑配置文件
- 👤 **设置** — 账号管理、主题设置、登录日志

其他功能（脚本、依赖、日志、订阅）通过右下角或各模块中的按钮以**对话框**形式打开，不离开当前导航。

### 多账号切换

在「设置」页面点击顶部账号区域，可以：

- 切换已保存的服务器连接
- 添加新服务器

---

## 功能模块详解

### 定时任务管理

任务列表支持下拉刷新，每个任务卡片显示：

- 任务名称和命令
- 调度规则（cron 表达式）
- 运行状态（运行中 / 空闲 / 已禁用 / 队列中）
- 上次运行时间和状态

**操作**：

| 操作 | 方式 |
|------|------|
| 运行 / 停止 | 直接点击卡片上的操作按钮 |
| 启用 / 禁用 | 单卡片操作或多选后批量操作 |
| 置顶 / 取消置顶 | 单卡片操作 |
| 编辑 / 删除 | 点击卡片上的菜单按钮 |
| 查看日志 | 点击日志按钮，实时轮询显示 |
| 批量操作 | 长按进入多选模式，选择多个后通过底部操作栏执行 |

### 环境变量管理

- 支持 `export KEY="value"` 格式的批量导入
- 每个变量可单独启用/禁用
- 支持编辑变量名称、值和备注
- 批量删除和批量启用/禁用

### 脚本管理

- 目录树浏览器，支持文件夹导航
- 文件上传（通过系统文件选择器选择 `.js` / `.py` / `.sh` 等）
- 在线编辑器：等宽字体显示，支持编辑和保存
- 文件删除

### 依赖管理

支持三种依赖类型：

| 类型 | 说明 |
|------|------|
| Node.js | npm 包（如 `axios`） |
| Python3 | pip 包（如 `requests`） |
| Linux | apt 软件包（如 `wget`） |

每个依赖卡片显示安装状态（安装中 / 已安装 / 安装失败 / 取消等），支持查看安装日志。

### 订阅管理

- 支持添加、编辑、删除订阅
- 手动运行 / 停止订阅
- 启用 / 禁用订阅

### 设置

- **账号信息**：查看当前登录的用户名和服务器地址
- **账号切换**：管理多个青龙面板连接
- **深色模式**：跟随系统 / 浅色 / 深色
- **动态取色**：开启后自动跟随系统壁纸颜色（Android 12+）
- **登录日志**：查看账号的登录历史记录
- **调试信息**：查看应用版本和设备信息

---

## 构建配置

### 构建类型

| 类型 | 混淆 | 资源压缩 | 用途 |
|------|------|---------|------|
| `debug` | 否 | 否 | 开发调试 |
| `release` | 是 | 是 | 发布 |

### 编译选项

- **Java 兼容性**：Java 17
- **Kotlin Compose 编译器插件**：已启用
- **View Binding**：未使用（纯 Compose）
- **R8 混淆**：Release 构建启用，已配置 Retrofit / Gson / OkHttp 规则

---

## 常见问题

### Q: 应用支持哪些青龙面板版本？

理论上支持青龙面板 v2.x+ 的 API。最低要求青龙面板已初始化（设置了管理员账号）。

### Q: 连接服务器失败怎么办？

1. 确保手机和青龙面板服务器在同一个网络
2. 确认服务器地址格式正确（如 `http://192.168.1.100:5700`）
3. 检查青龙面板是否正常运行
4. 如果使用 HTTPS，确保证书有效
5. 应用已配置 `usesCleartextTraffic=true`，支持 HTTP 明文连接

### Q: 如何查看崩溃日志？

应用在崩溃时会自动在内部存储的 `crash.log` 文件中记录崩溃信息。可以通过 ADB 拉取：

```bash
adb shell run-as me.doujiang.app cat /data/data/me.doujiang.app/files/crash.log
```

---

## 贡献指南

欢迎贡献代码、提交 Issue 或功能请求！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 代码规范

- 遵循 Kotlin 官方编码规范
- Commits 使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式
- 提交前确保项目能正常编译

---

## 许可证

```
MIT License

Copyright (c) 2025 xl1124

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 致谢

- [青龙面板 (QingLong)](https://github.com/whyour/qinglong) — 强大的定时任务管理面板
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代 Android UI 工具包
- [Material 3](https://m3.material.io/) — Material Design 3 设计系统
