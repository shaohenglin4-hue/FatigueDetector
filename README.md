# 🚗 Smart Driving Warning System (FatigueDetector)

## 📖 项目简介 (Overview)
这是一个基于**完全离线边缘计算 (Edge Computing)** 的 Android 端侧实时疲劳驾驶检测系统。
项目利用设备前置摄像头，结合 Google MediaPipe 视觉引擎提取面部特征点，通过计算**眼睛纵横比 (EAR, Eye Aspect Ratio)** 状态机，精准判定驾驶员的闭眼与打瞌睡状态，并触发柔和的提示音报警。

本系统无需任何网络连接，所有 AI 推理与图像渲染均在设备本地极速完成，最大限度保护驾驶员隐私。

## ✨ 核心特性 (Key Features)
* **⚡ 毫秒级端侧推理**：集成最新 `MediaPipe Tasks Vision API`，实现流畅的 468 个面部 3D 特征点追踪。
* **🧠 核心算法移植**：完美将基于 Python 的欧氏距离与 EAR 算法翻译至 Kotlin 并在 Android 侧落地。
* **🛡️ 隐私保护优先**：基于 CameraX 的 `ImageAnalysis`，图像数据实时抽帧分析，真正做到离线防窥。
* **🎵 柔和警报系统**：使用 `SoundPool` 实现类似智能汽车的“无惊吓”防疲劳提示音，内置防抖冷却机制。
* **💅 现代车载 UI**：
    * 自定义人脸取景遮罩 (`FaceOverlayView`) 引导对焦。
    * 深色模式 (Dark Mode) 仪表盘配合动态 EAR 进度条。
    * Material Design 胶囊型交互按钮。

## 📐 算法原理 (Algorithm)
系统核心基于 EAR (Eye Aspect Ratio) 公式。通过定位眼睛轮廓的 6 个关键点，计算垂直距离与水平距离的比值：

$$EAR = \frac{||p_2 - p_6|| + ||p_3 - p_5||}{2 ||p_1 - p_4||}$$

* **阈值设定**：当 $EAR < 0.22$ 时触发闭眼计数。
* **状态机判定**：连续闭眼超过 15 帧（约 0.5~1 秒）即触发红色疲劳警告与提示音。

## 🛠️ 技术栈 (Tech Stack)
* **语言**：Kotlin
* **相机流**：AndroidX CameraX (`Preview` & `ImageAnalysis`)
* **AI 视觉引擎**：MediaPipe Face Landmarker (`com.google.mediapipe:tasks-vision`)
* **UI / 绘制**：自定义 `View` (Canvas/Paint), `MaterialButton`, `CardView`
* **音频引擎**：`SoundPool`

## 📂 核心代码结构 (Project Structure)
* `MainActivity.kt`：中枢控制，管理 CameraX 生命周期与 UI 状态同步。
* `FaceDetectorHelper.kt`：MediaPipe 包装类，负责图像格式转换与特征点坐标映射。
* `FatigueAnalyzer.kt`：核心算法大脑，负责 EAR 计算与疲劳状态机。
* `FaceOverlayView.kt`：自定义 UI 遮罩层，提供科技感扫描取景框。

## 🚀 如何运行 (How to Run)
1.  克隆本项目到本地 `git clone <你的仓库地址>`。
2.  使用 Android Studio 打开项目。
3.  确保 Android 设备已开启“开发者选项”及“USB 调试”，并连接至电脑。
4.  点击 `Run 'app'` 编译并安装到真机（由于涉及物理摄像头，不支持在模拟器中完整运行）。
5.  授予应用相机权限后即可开始检测！
