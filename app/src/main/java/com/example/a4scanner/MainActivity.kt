package com.example.a4scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a4scanner.data.ScanPage
import com.example.a4scanner.ui.CameraScreen
import com.example.a4scanner.ui.CropScreen
import com.example.a4scanner.ui.PagesScreen
import com.example.a4scanner.ui.theme.A4ScannerTheme
import com.example.a4scanner.vm.ScanViewModel
import java.io.File

private sealed interface Screen {
    data object Camera : Screen
    data class Crop(val file: File, val page: ScanPage?, val isNew: Boolean) : Screen
    data object Pages : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            A4ScannerTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: ScanViewModel = viewModel()) {
    val context = LocalContext.current
    val pages by viewModel.pages.collectAsState()
    val busy by viewModel.busy.collectAsState()

    var screen by remember { mutableStateOf<Screen>(Screen.Camera) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = screen) {
                is Screen.Camera -> {
                    if (hasCameraPermission) {
                        CameraScreen(
                            pageCount = pages.size,
                            createCaptureFile = { viewModel.createCaptureFile() },
                            onCaptured = { file -> screen = Screen.Crop(file, null, true) },
                            onOpenPages = { screen = Screen.Pages }
                        )
                    } else {
                        PermissionRequest(onRequest = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        })
                    }
                }

                is Screen.Crop -> {
                    val page = current.page
                    CropScreen(
                        sourceFile = current.file,
                        initialCorners = page?.corners
                            ?: com.example.a4scanner.data.PageCorners.default(),
                        initialFilter = page?.filter
                            ?: com.example.a4scanner.data.PageFilter.ORIGINAL,
                        initialRotation = page?.rotationDegrees ?: 0,
                        isNewCapture = current.isNew,
                        onApply = { corners, filter, rotation ->
                            if (current.isNew) {
                                viewModel.addPage(current.file, corners, filter, rotation)
                                screen = Screen.Camera
                            } else if (page != null) {
                                viewModel.updatePage(page.id, corners, filter, rotation)
                                screen = Screen.Pages
                            }
                        },
                        onCancel = {
                            if (current.isNew) {
                                viewModel.discardCapture(current.file)
                                screen = Screen.Camera
                            } else {
                                screen = Screen.Pages
                            }
                        }
                    )
                }

                is Screen.Pages -> {
                    PagesScreen(
                        pages = pages,
                        onBack = { screen = Screen.Camera },
                        onEdit = { page -> screen = Screen.Crop(File(page.sourcePath), page, false) },
                        onDelete = { id -> viewModel.deletePage(id) },
                        onMove = { from, to -> viewModel.movePage(from, to) },
                        onExport = {
                            viewModel.exportPdf { file ->
                                Toast.makeText(
                                    context,
                                    "Saved: ${file.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                                sharePdf(context, file)
                            }
                        }
                    )
                }
            }

            if (busy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(8.dp))
                            Text("Processing\u2026", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Camera access is required to scan pages.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(16.dp))
        Button(onClick = onRequest, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant camera permission")
        }
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
}
