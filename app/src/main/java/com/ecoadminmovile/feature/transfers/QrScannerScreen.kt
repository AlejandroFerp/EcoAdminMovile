/**
 * Pantalla de escaneo QR usando CameraX + ML Kit para lectura de códigos de barras.
 *
 * Conceptos Kotlin demostrados:
 * - rememberLauncherForActivityResult: Activity Result API en Compose (reemplaza onActivityResult).
 * - LaunchedEffect(Unit): efecto secundario que se ejecuta UNA sola vez al entrar en composición.
 * - AndroidView { factory }: integra Views tradicionales de Android dentro de Compose.
 * - Executors.newSingleThreadExecutor(): crea hilo de fondo para procesamiento de imágenes.
 * - @OptIn(ExperimentalGetImage::class): acepta uso de API experimental de CameraX.
 * - var ... by remember { mutableStateOf(...) }: patrón estándar para estado local en Compose.
 * - Callback pattern: `onResult: (String) -> Unit` como parámetro de función.
 *
 * Patrones de diseño:
 * - Callback/Observer: onQrScanned notifica al padre cuando se detecta un QR.
 * - Strategy: el ImageAnalysis.Analyzer procesa cada frame de cámara.
 */
package com.ecoadminmovile.feature.transfers

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    // Callback pattern: función que se invoca con el resultado del escaneo
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    // var ... by remember { mutableStateOf(...) }: estado local mutable que persiste entre recomposiciones.
    // `by` delega get/set a MutableState (property delegation).
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // rememberLauncherForActivityResult: reemplaza startActivityForResult/onActivityResult.
    // Registra un callback que se ejecuta cuando el sistema devuelve un resultado.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // LaunchedEffect(Unit): se ejecuta UNA SOLA VEZ cuando el Composable entra en composición.
    // Unit como key = nunca se re-ejecuta (equivale a "on mount" en React).
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            @kotlin.OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Escanear QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                CameraPreviewWithAnalysis(onQrScanned = onQrScanned)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Se necesita permiso de cámara para escanear QR")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Conceder permiso")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewWithAnalysis(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannedOnce by remember { mutableStateOf(false) }

    // AndroidView: permite embeber Views tradicionales de Android dentro de Jetpack Compose.
    // factory lambda: se ejecuta una vez para crear la View nativa (PreviewView de CameraX).
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        // Executors.newSingleThreadExecutor(): hilo dedicado para procesamiento.
                        // El análisis de imagen se hace fuera del main thread.
                        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processImage(imageProxy) { barcode ->
                                if (!scannedOnce) {
                                    scannedOnce = true
                                    onQrScanned(barcode)
                                }
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrScanner", "Error al iniciar cámara", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// @OptIn(ExperimentalGetImage::class): acepta uso de API experimental de CameraX.
// Sin esta anotación, acceder a imageProxy.image daría error de compilación.
@OptIn(ExperimentalGetImage::class)
private fun processImage(imageProxy: ImageProxy, onResult: (String) -> Unit) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val scanner = BarcodeScanning.getClient()

    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            barcodes.firstOrNull { it.valueType == Barcode.TYPE_TEXT || it.valueType == Barcode.TYPE_URL }
                ?.rawValue?.let(onResult)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
