package com.example.hydrated_state.presentation

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.hydrated_state.R
import kotlinx.coroutines.*
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

class MainActivity : ComponentActivity() {

    var tag = "mainapp"
    private lateinit var SelectFile: TextView
    private lateinit var btnSubmit: Button
    private var selectedFileUri: Uri? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setTheme(android.R.style.Theme_DeviceDefault)
        setUpHomeScreen()
    }

    private fun setUpHomeScreen(){
        setContentView(R.layout.activity_home)
        Log.d(tag, "home")

        // Initialize Python only once
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
            Log.d(tag, "python")
        }
        val py = Python.getInstance()
        Log.d(tag, "py3")
        val module = py.getModule("preprocessing")

        // Preprocess CSV file (only needed to be done once)
        val assetManager = assets
        val inputStream = assetManager.open("fasting1.csv")
        val tempFile = File(cacheDir, "fasting1.csv")
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val ppg_bm: PyObject = module.callAttr("extract_feature", tempFile.path)
        Log.d(tag, ppg_bm.toString())
        Log.d(tag, "py5")

        SelectFile = findViewById(R.id.tv_select_file)
        btnSubmit = findViewById(R.id.btn_submit)

        SelectFile.setOnClickListener {
            selectFileLauncher.launch("*/*")
        }

        btnSubmit.setOnClickListener {
            Log.d(tag, "button tapped")
            showLoadingScreen()
            runMLAlgorithm { result ->
                showResultScreen(result)
            }
        }
    }

    // Register for activity results in onCreate
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            SelectFile.text = it.lastPathSegment
            btnSubmit.isEnabled = true
        }
    }

    private fun showLoadingScreen() {
        setContentView(R.layout.activity_loading2)

        val imageView = findViewById<ImageView>(R.id.animated_background)
        val animationDrawable = AnimationDrawable()

        // Loop through the frames and add them to the animation drawable
        for (i in 0..97) {
            val frameName = String.format("frame_%02d", i)
            val frameId = resources.getIdentifier(frameName, "drawable", packageName)
            val frame = ResourcesCompat.getDrawable(resources, frameId, null)
            frame?.let {
                animationDrawable.addFrame(it, 40) // Adjust the duration as needed
            }
        }

        // Set the animation drawable as the background of the ImageView
        imageView.background = animationDrawable

        // Start the animation
        imageView.post { animationDrawable.start() }
    }

    private fun showResultScreen(result: String) {
        setContentView(R.layout.activity_result)
        findViewById<TextView>(R.id.tv_result).text = result
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            setUpHomeScreen()
        }
    }

    private fun runMLAlgorithm(onResult: (String) -> Unit) {
        scope.launch {
            delay(5000) // Simulating a long-running task
            val isHydrated = (0..1).random() == 0
            onResult(if (isHydrated) "Hydrated" else "Dehydrated")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel() // Cancel the coroutine scope when the activity is destroyed
    }
}
