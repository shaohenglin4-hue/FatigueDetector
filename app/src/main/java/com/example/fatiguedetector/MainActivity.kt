package com.example.fatiguedetector

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // 1. 声明 UI 控件
    private lateinit var viewFinder: PreviewView
    private lateinit var tvStatus: TextView
    private lateinit var tvEAR: TextView
    private lateinit var btnStart: MaterialButton
    private lateinit var earProgressBar: ProgressBar

    // 2. 声明工具类与变量
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetectorHelper: FaceDetectorHelper
    private lateinit var soundPool: SoundPool

    private var alarmSoundId: Int = 0
    private var lastAlarmTime: Long = 0L // 用于控制声音播放频率
    private var isDetecting = false

    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要摄像头权限才能监控驾驶状态", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 夺取休眠控制权：只要本界面开启，屏幕永不自动锁屏
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        // 绑定控件
        viewFinder = findViewById(R.id.viewFinder)
        tvStatus = findViewById(R.id.tvStatus)
        tvEAR = findViewById(R.id.tvEAR)
        btnStart = findViewById(R.id.btnStart)
        earProgressBar = findViewById(R.id.earProgressBar)

        // 初始化 AI 辅助类
        faceDetectorHelper = FaceDetectorHelper(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 初始化声音引擎 (SoundPool)
        initSoundPool()

        // 按钮点击逻辑：开启或彻底关闭
        btnStart.setOnClickListener {
            if (!isDetecting) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                    isDetecting = true
                    btnStart.text = "停止监控"
                    btnStart.setBackgroundColor(Color.parseColor("#C62828")) // 停止时变红
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            } else {
                stopCamera() // 关闭摄像头
                isDetecting = false
                btnStart.text = "开始监控"
                btnStart.setBackgroundColor(Color.parseColor("#2E7D32")) // 启动前变绿
                tvStatus.text = "已停止"
                tvStatus.setTextColor(Color.WHITE)
                tvEAR.text = "EAR: --"
                earProgressBar.progress = 0
            }
        }

        // 1. 绑定控件
        val btnSleep = findViewById<MaterialButton>(R.id.btnSleep)
        val blackScreenOverlay = findViewById<android.widget.FrameLayout>(R.id.blackScreenOverlay)

        // 2. 点击息屏监控按钮：进入假息屏
        btnSleep.setOnClickListener {
            if (!isDetecting) {
                Toast.makeText(this, "请先开始监控，再进入息屏模式", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 显示纯黑遮罩
            blackScreenOverlay.visibility = android.view.View.VISIBLE

            // 将屏幕亮度强制调到最低
            val attributes = window.attributes
            attributes.screenBrightness = 0.01f
            window.attributes = attributes
        }

        // 3. 点击纯黑屏幕的任意区域
        blackScreenOverlay.setOnClickListener {
            // 隐藏遮罩
            blackScreenOverlay.visibility = android.view.View.GONE

            // 恢复系统默认亮度
            val attributes = window.attributes
            attributes.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = attributes
        }
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        // 确保你的 res/raw 文件夹下有 chime.mp3 文件
        alarmSoundId = soundPool.load(this, R.raw.chime, 1)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        // 核心：调用 AI 提取与疲劳算法 [cite: 2, 3]
                        val result = faceDetectorHelper.processFrame(imageProxy)

                        runOnUiThread {
                            updateUI(result)
                        }
                        imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("FatigueApp", "绑定失败", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateUI(result: FaceDetectorHelper.FaceAnalysisResult) {
        if (!result.isFaceDetected) {
            tvStatus.text = "寻找面部..."
            tvStatus.setTextColor(Color.YELLOW)
            earProgressBar.progress = 0
            return
        }

        tvEAR.text = String.format("EAR: %.3f", result.ear)
        earProgressBar.progress = (result.ear * 100).toInt() // 进度条同步

        if (result.isAlarmOn) {
            tvStatus.text = "⚠️ 疲劳警告"
            tvStatus.setTextColor(Color.RED)
            earProgressBar.progressTintList = ColorStateList.valueOf(Color.RED)

            // 播放声音 (每 2 秒响一次)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAlarmTime > 2000) {
                soundPool.play(alarmSoundId, 1f, 1f, 1, 0, 1f)
                lastAlarmTime = currentTime
            }
        } else {
            tvStatus.text = "安全驾驶中"
            tvStatus.setTextColor(Color.GREEN)
            earProgressBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        soundPool.release()
    }
}