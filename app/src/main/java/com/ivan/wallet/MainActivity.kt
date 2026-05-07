package com.ivan.wallet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivan.wallet.ui.WalletApp
import com.ivan.wallet.ui.WalletViewModel
import com.ivan.wallet.ui.theme.WalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalletTheme {
                val viewModel: WalletViewModel = viewModel()
                var didAutoImport by remember { mutableStateOf(false) }
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    val granted = results.values.all { it }
                    hasPermission = granted
                    viewModel.onSmsPermissionChanged(granted)
                    if (granted) {
                        viewModel.importSms(this)
                    }
                }

                LaunchedEffect(hasPermission) {
                    viewModel.onSmsPermissionChanged(hasPermission)
                    if (hasPermission && !didAutoImport) {
                        didAutoImport = true
                        viewModel.importSms(this@MainActivity)
                    }
                }

                WalletApp(
                    viewModel = viewModel,
                    onRequestSmsPermission = {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                            )
                        )
                    },
                    onImportSms = {
                        viewModel.importSms(this)
                    },
                    onExportCsv = { uri ->
                        viewModel.exportCsv(this, uri)
                    }
                )
            }
        }
    }
}
