# 启用数字健康新UI (Wellbeing M3 Expressive Enabler)

[![Android](https://img.shields.io/badge/Android-9.0%2B-green.svg)](https://developer.android.com)
[![Xposed](https://img.shields.io/badge/Xposed-API%20102%20%7C%2082-orange.svg)](https://github.com/libxposed/api)
[![Package](https://img.shields.io/badge/Target-com.google.android.apps.wellbeing-blue.svg)](https://play.google.com/store/apps/details?id=com.google.android.apps.wellbeing)

专为 Android 设备打造的 Xposed / LSPosed 模块，为低于 Android 16 QPR1 的设备提前开启并深度完善 **Google 数字健康 (Google Digital Wellbeing)** 的 **Material 3 Expressive (M3E)** 界面体系。
<img width="8192" height="3556" alt="IMG_20260908_233903" src="https://github.com/user-attachments/assets/f8204773-5178-43fc-a623-bad5aed60bdb" />

---

## ✨ 核心特性

### 1. 全面解锁原生 M3 Expressive
- **全方位 Flag 激活**：深度挂钩激活 `SettingsThemeHelper`、`PhenotypeFlags`、`SystemProperties` 及活动重定向，解除系统版本限制。
- **无缝融合最新版（1.47+）**：完美协同 Google 官方原生 M3E 界面体系，针对官方已原生适配的主页、图表、应用详情及下拉筛选菜单完整放行，拒绝多余侵入，确保最原汁原味的排版与动画质感。

### 2. 深度重构未适配子页面
对于官方目前尚未完成 Material 3 适配的页面，模块提供了细致入微的卡片化重绘与控件升级：
- **看路提醒 (Walking Detection)**：
  - 权限请求项重构为 Google 风格的连续卡片（Top / Middle / Bottom 适配）；
  - 主开关条（SwitchBar）两端与卡片 16dp 边距精准对齐；
  - 底部“发送反馈”链接重塑为精致的 Material 3 药丸按钮（100dp 圆角、36dp 高度、PrimaryContainer 配色与水波纹）。
- **专注模式 (Focus Mode)**：
  - 应用选择列表中的复选框（CheckBox）无缝升级为 Material 3 Switch 开关；
  - 应用列表项 DarQ 风格连续卡片化；
  - “+ 设置时间表”与日程项精致卡片化。
- **屏幕使用时间提醒 (Mindful Nudge)**：
  - 主开关条边距对齐；
  - 应用列表复选框转换为 Switch，并完成连续卡片化。
- **顶栏右上角三点菜单**：
  - 精准限定仅美化右上角“更多选项”弹窗，赋予 16dp 圆角与 1dp 精细分割线，同时消除原生滚动条与边缘溢出。

### 3. 双架构现代化支持
- **现代 LSPosed 规范**：基于现代化 `io.github.libxposed:api:102.0.0` 构建，高性能、低占用；
- **传统 Xposed 兼容**：内置传统 `de.robv.android.xposed:api:82` 兜底实现，具备极佳的设备与框架兼容性。

---

## 📱 兼容性要求

- **操作系统**：Android 9.0 (API 26) 及以上
- **框架支持**：LSPosed / LSPosed-mod / EdXposed 等主流 Xposed 框架
- **目标应用**：Google 数字健康（`com.google.android.apps.wellbeing`，推荐 1.46 / 1.47+ 最新版）

---

## 🚀 安装与使用方法

1. 前往 [Releases](../../releases) 页面下载最新版 APK；
2. 安装后打开 **LSPosed 管理器**，在模块列表中启用 **「启用数字健康新UI」**；
3. 作用域勾选 **「数字健康」**（`com.google.android.apps.wellbeing`）；
4. 强制停止「数字健康」应用（或直接重启手机）；
5. 重新打开 Google 数字健康，即可体验全新的 Material 3 Expressive 界面。

---

## 🛠️ 项目构建

本项目采用标准 Gradle 构建系统：

```bash
# 检出代码后编译 Release APK
./gradlew assembleRelease
```

编译生成的产物位于 `app/build/outputs/apk/release/app-release.apk`。

---

## 📄 开源许可

本项目遵循 [Apache License 2.0](LICENSE) 开源协议。
