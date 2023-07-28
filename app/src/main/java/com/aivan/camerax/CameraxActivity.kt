package com.aivan.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.await
import com.aivan.camerax.base.BaseActivity
import com.aivan.camerax.databinding.ActivityCamaraxBinding
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 *
 * @author: Aivan
 * @date: 2023/7/28
 * @desc: https://github.com/android/camera-samples.git
 */
class CameraxActivity: BaseActivity<ActivityCamaraxBinding>(ActivityCamaraxBinding::inflate) {
    companion object {
        val TAG:String = CameraxActivity::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var inProgress = false
    private val totalTime = 6000L
    private var videoPath : String? = null
    private var videoArray: ByteArray? = null
    private var currentRecording: Recording? = null
    private var countDownTimer : CountDownTimer? =null
    private var recordingState: VideoRecordEvent? =null
    private var enumerationDeferred: Deferred<Unit>? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private val mainThreadExecutor by lazy { ContextCompat.getMainExecutor(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionCheck{
            initCamera()
        }
        binding.startButton.setOnClickListener {
            permissionCheck{
                initStartRecording()
            }
        }
        printWebVersion()
    }


    private fun printWebVersion(){
     packageManager.getInstalledPackages(0)?.forEach {
         if("com.android.webview".equals(it.packageName)){
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 LogUtils.i("packageName ${it.packageName}  versionCode ${it.versionCode} versionName ${it.versionName} longVersionCode ${it.longVersionCode}")
             }else{
                 LogUtils.i("packageName ${it.packageName}  versionCode ${it.versionCode} versionName ${it.versionName}")
             }
         }
         if("com.google.android.webview".equals(it.packageName)){
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 LogUtils.i("packageName ${it.packageName}  versionCode ${it.versionCode} versionName ${it.versionName} longVersionCode ${it.longVersionCode}")
             }else{
                 LogUtils.i("packageName ${it.packageName}  versionCode ${it.versionCode} versionName ${it.versionName}")
             }
         }
     }

    }

    /**
     * 兼容 android 13 权限
     */
    private fun permissionCheck(block:()-> Unit){
        val requestList = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestList.add(Manifest.permission.READ_MEDIA_IMAGES)
            requestList.add(Manifest.permission.READ_MEDIA_AUDIO)
            requestList.add(Manifest.permission.READ_MEDIA_VIDEO)
            requestList.add(Manifest.permission.RECORD_AUDIO)
            requestList.add(Manifest.permission.CAMERA)
        }else{
            requestList.add(Manifest.permission.CAMERA)
            requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestList.add(Manifest.permission.RECORD_AUDIO)
        }
        PermissionX.init(this).permissions(requestList)
            .request{ allGranted, deniedList, _ ->
                if(allGranted){
                    //若权限申请成功
                    block()
                }
                else{
                    ToastUtils.showLong("你拒绝对应权限")
                }
            }
    }

    private fun initCamera(){
        //启动预览
        setUpCamera(binding.previewView)
    }

    private fun setUpCamera(previewView: PreviewView) {
        lifecycleScope.launch {
            if (enumerationDeferred != null) {
                enumerationDeferred!!.await()
                enumerationDeferred = null
            }
            bindCaptureUseCase()
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun bindCaptureUseCase() {
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()

        // create the user required QualitySelector (video resolution): we know this is
        // supported, a valid qualitySelector will be created.
        val quality = Quality.SD
        val qualitySelector = QualitySelector.from(quality)

        val preview = Preview.Builder().setTargetAspectRatio(quality.getAspectRatio(quality)).build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // build a recorder, which can:
        //   - record video/audio to MediaStore(only shown here), File, ParcelFileDescriptor
        //   - be used create recording(s) (the recording performs recording)
        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            //DEFAULT_FRONT_CAMERA 前摄像头  DEFAULT_BACK_CAMERA 后摄像头
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, videoCapture, preview)
        } catch (exc: Exception) {
            // we are on main thread, let's reset the controls on the UI.
            Log.e(TAG, "Use case binding failed", exc)
            //resetUIandState("bindToLifecycle failed: $exc")
        }

    }

    private fun initStartRecording(){
        if(inProgress){
            ToastUtils.showLong("视频录制中")
            return
        }

        inProgress = true
        starTimer()
    }

    private fun updateUI(event: VideoRecordEvent) {
        val state = if (event is VideoRecordEvent.Status) recordingState?.getNameString()
        else event.getNameString()
        when (event) {
            is VideoRecordEvent.Status -> {
                // placeholder: we update the UI with new status after this when() block,
                // nothing needs to do here.
            }
            is VideoRecordEvent.Start -> {

            }
            is VideoRecordEvent.Finalize-> {

            }
            is VideoRecordEvent.Pause -> {

            }
            is VideoRecordEvent.Resume -> {

            }
        }

        /*val stats = event.recordingStats
        val size = stats.numBytesRecorded / 1000
        val time = TimeUnit.NANOSECONDS.toSeconds(stats.recordedDurationNanos)
        var text = "${state}: recorded ${size}KB, in ${time}second"
        if(event is VideoRecordEvent.Finalize)
            text = "${text}\nFile saved to: ${event.outputResults.outputUri}"

        //captureLiveStatus.value = text
        Log.i(TAG, "recording event: $text")*/
    }

    /**
     * CaptureEvent listener.
     */
    private val captureListener = Consumer<VideoRecordEvent> { event ->
        // cache the recording state
        if (event !is VideoRecordEvent.Status)
            recordingState = event

        updateUI(event)

        if (event is VideoRecordEvent.Finalize) {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val actualImageCursor = managedQuery(event.outputResults.outputUri, proj, null, null, null)
            val actualImageColumnIndex = actualImageCursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            actualImageCursor?.moveToFirst()
            videoPath = actualImageColumnIndex?.let { actualImageCursor.getString(it) }.toString();

            Log.i("TAG", "视频路径: $videoPath");
            Log.i("TAG", "视频保存成功: ${event.outputResults.outputUri}")
            if(videoPath!=null&& !videoPath.equals("null")){
                binding.previewView.isVisible = false
                binding.realNameVidePlayView.isVisible = true
                videoArray = File(videoPath).readBytes()
                permissionCheck{
                    startVideo()
                }
            }
        }
    }

    private fun startVideo(){
        binding.realNameVidePlayView.setVideoPath(videoPath)
        binding.realNameVidePlayView.start()
    }

    private fun starTimer(){
        binding.previewView.isVisible = true
        binding.realNameVidePlayView.isVisible = false
        countDownTimer=object : CountDownTimer(totalTime,1000){//1000ms运行一次onTick里面的方法
        @SuppressLint("RestrictedApi")
        override fun onFinish() {
            Log.d(TAG,"==倒计时结束")
            currentRecording?.stop() //这里为结束录制方法
            inProgress = false
            binding.startButton.text = "重新开始"
        }
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                val time = millisUntilFinished/1000
                binding.startButton.text = "请在${time}S内完成识别"
            }
        }.start()
        startRecording()
    }

    @SuppressLint("MissingPermission")
    private fun startRecording(){

        // create MediaStoreOutputOptions for our recorder: resulting our recording!
        val name = "CameraX-recording-" + SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build()

        // configure Recorder and Start recording to the mediaStoreOutput.
        currentRecording = videoCapture?.output?.prepareRecording(this, mediaStoreOutput)?.apply {  withAudioEnabled() }
            ?.start(mainThreadExecutor, captureListener)

        Log.i(TAG, "Recording started")

    }




}

fun Quality.getAspectRatio(quality: Quality): Int {
    return when {
        arrayOf(Quality.UHD, Quality.FHD, Quality.HD)
            .contains(quality)   -> AspectRatio.RATIO_16_9
        (quality ==  Quality.SD) -> AspectRatio.RATIO_4_3
        else -> throw UnsupportedOperationException()
    }
}

fun VideoRecordEvent.getNameString() : String {
    return when (this) {
        is VideoRecordEvent.Status -> "Status"
        is VideoRecordEvent.Start -> "Started"
        is VideoRecordEvent.Finalize-> "Finalized"
        is VideoRecordEvent.Pause -> "Paused"
        is VideoRecordEvent.Resume -> "Resumed"
        else -> throw IllegalArgumentException("Unknown VideoRecordEvent: $this")
    }
}

