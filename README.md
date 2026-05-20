# 🚗 Driver-Fatigue-Detection-Android (车载疲劳驾驶监控系统)

## 📖 项目简介
本项目是一个基于 Android 端侧计算的实时疲劳驾驶检测系统。系统通过设备前置摄像头，结合 Google MediaPipe 视觉模型提取面部特征点，实时计算眼睛纵横比 (EAR, Eye Aspect Ratio)，从而判定驾驶员的疲劳状态并触发音频警报。

为了适应车载环境，系统实现了完全离线运行，并加入了 OLED 息屏节能模式，在保证持续监控的同时有效降低设备功耗。

## ✨ 核心功能
* **端侧实时推理**：集成 `MediaPipe Tasks Vision API`，实现面部特征点捕捉。图像数据处理均在设备本地完成，无需依赖网络连接，保护用户隐私。
* **OLED 息屏节能模式**：针对 Android 系统物理锁屏会断开摄像头数据流的底层限制，通过代码实现纯黑界面与最低亮度控制。结合 OLED 屏幕物理特性，在“假息屏”状态下实现低功耗的持续后台监控。
* **算法移植与状态机**：基于前期的 Python 算法原型，在 Kotlin 环境下重构了欧氏距离计算与 EAR 状态机逻辑。当眼部闭合帧数达到设定阈值时，精确触发判定。
* **低延迟音频反馈**：使用 `SoundPool` 构建带有防抖冷却机制的音频播放逻辑，提供及时的疲劳提示音。

## 📐 算法原理
系统核心基于眼睛纵横比 (EAR) 公式。通过定位眼睛轮廓的 6 个关键特征点，计算垂直距离与水平距离的比值：

$$EAR = \frac{||p_2 - p_6|| + ||p_3 - p_5||}{2 ||p_1 - p_4||}$$

* **判定逻辑**：当实时 $EAR$ 值持续低于设定的疲劳阈值，且持续时间超过设定的帧数窗口时，系统判定用户处于闭眼疲劳状态并触发警报。

## 🛠️ 技术栈
* **开发语言**：Kotlin
* **相机架构**：AndroidX CameraX (`ImageAnalysis`)
* **AI 视觉引擎**：MediaPipe Face Landmarker (`com.google.mediapipe:tasks-vision`)
* **UI 与媒体**：自定义 `View` (Canvas绘制) / `MaterialButton` / `SoundPool`

## 📂 核心代码结构
* `MainActivity.kt`：中枢控制，管理 CameraX 生命周期、休眠控制权与 UI 状态同步。
* `FaceDetectorHelper.kt`：MediaPipe 包装类，负责图像格式转换与特征点坐标映射。
* `FatigueAnalyzer.kt`：核心算法逻辑，负责 EAR 计算与疲劳状态机维护。
* `FaceOverlayView.kt`：自定义 UI 遮罩层，提供取景框对焦引导。

## 🚀 运行说明
1. 克隆本项目：`git clone <repository-url>`
2. 使用 Android Studio 打开项目并同步 Gradle 依赖。
3. 建议使用真实 Android 设备（开启 USB 调试）进行编译运行，模拟器环境可能无法稳定提供所需的相机视频流。
4. 授予应用相机权限后即可开始检测。测试时可点击“息屏监控”按钮体验低功耗运行模式。
