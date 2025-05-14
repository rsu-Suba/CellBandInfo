package com.example.arfcntest
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.widget.Button
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.WindowManager


class MainActivity : AppCompatActivity() {
    private var textViewStatus: TextView? = null
    private var textViewTimestamp: TextView? = null
    private var textViewEarfcn: TextView? = null
    private var textViewSubEarfcn: TextView? = null
    private var textViewNrarfcn: TextView? = null
    private var textViewSubNrarfcn: TextView? = null
    private var textViewFullInfo: TextView? = null
    private var startButton: Button? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("CellInfo", "Location permission granted. Getting cell info...")
                textViewStatus?.text = "Permission granted. Getting cell info..."
                getCellInfo()
            } else {
                Log.w("CellInfo", "Location permission denied.")
                textViewStatus?.text = "Location permission denied."
                textViewTimestamp?.text = "Last Update: Permission Denied"
                // 必要に応じてユーザーに権限が必要であることを伝えるUIを表示
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemBars()
        setContentView(R.layout.activity_main)

        textViewStatus = findViewById(R.id.activity_main_text_Status)
        textViewTimestamp = findViewById(R.id.textViewTimestamp)
        textViewEarfcn = findViewById(R.id.activity_main_text_view4G)
        textViewSubEarfcn = findViewById(R.id.activity_sub_text_view4G)
        textViewNrarfcn = findViewById(R.id.activity_main_text_view5G)
        textViewSubNrarfcn = findViewById(R.id.activity_sub_text_view5G)
        textViewFullInfo = findViewById(R.id.activity_full_text)
        startButton = findViewById(R.id.activity_main_button)

        startButton?.setOnClickListener {
            Log.d("CellInfo", "Button clicked. Checking permissions.")
            textViewStatus?.text = "Checking permissions..."
            textViewTimestamp?.text = "Updating..."
            clearCellInfoTextViews()
            checkLocationPermissionAndGetCellInfo()
        }

        Log.d("CellInfo", "App started. Checking permissions for initial data.")
        textViewStatus?.text = "Initializing..."
        textViewTimestamp?.text = "Updating..."
        clearCellInfoTextViews()
        checkLocationPermissionAndGetCellInfo()
    }


    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.isNavigationBarContrastEnforced = false
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    private fun clearCellInfoTextViews() {
        textViewEarfcn?.text = "4G Band"
        textViewSubEarfcn?.text = "4G Info"
        textViewNrarfcn?.text = "5G Band"
        textViewSubNrarfcn?.text = "5G Info"
        textViewFullInfo?.text = "Full Info"
    }


    private fun checkLocationPermissionAndGetCellInfo() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("CellInfo", "Location permission already granted. Getting cell info...")
                textViewStatus?.text = "Permission granted. Getting cell info..."
                getCellInfo()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // ユーザーに権限が必要な理由を説明するUIを表示する検討
                Log.i("CellInfo", "Showing rationale for location permission")
                textViewStatus?.text = "Location permission required for this feature."
                textViewTimestamp?.text = "Last Update: Permission Needed"
                // 例: AlertDialogなどで説明を表示し、ユーザーがOKしたら
                // requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) を呼び出す
            }
            else -> {
                Log.i("CellInfo", "Requesting location permission.")
                textViewStatus?.text = "Requesting location permission..."
                textViewTimestamp?.text = "Updating..."
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getCellInfo() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        // Android Q (API 29) 以降では requestCellInfoUpdate を使用することが推奨されます
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // checkLocationPermissionAndGetCellInfoで権限確認済みだが、念のためここでもチェック
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                telephonyManager.requestCellInfoUpdate(
                    mainExecutor,
                    object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                            Log.d("CellInfo", "onCellInfo callback received.")
                            processCellInfoList(cellInfo)
                        }
                        override fun onError(errorCode: Int, detail: Throwable?) {
                            super.onError(errorCode, detail)
                            Log.e("CellInfo", "Error getting cell info: $errorCode", detail)

                            val errorMessage = "Cell Info Error Code: $errorCode" +
                                    (detail?.let { " - Detail: ${it.localizedMessage ?: it.toString()}" } ?: "")
                            textViewStatus?.text = errorMessage
                            textViewTimestamp?.text = "Last Update: Error"
                        }
                    }
                )
            } else {
                // requestCellInfoUpdateを呼ぶ前に権限が無くなった場合など
                Log.e("CellInfo", "Permission lost before requestCellInfoUpdate could be called.")
                textViewStatus?.text = "Error: Permission required to get cell info."
                textViewTimestamp?.text = "Last Update: Permission Error"
            }
        } else {
            // Android P (API 28) 以前の場合
            Log.d("CellInfo", "Using telephonyManager.allCellInfo for API < Q")
            @Suppress("Deprecation")
            val cellInfoList = telephonyManager.allCellInfo
            processCellInfoList(cellInfoList)
        }
    }

    private fun processCellInfoList(cellInfoList: List<CellInfo>?) {
        Log.d("CellInfo", "Processing cell info list. Size: ${cellInfoList?.size ?: 0}")

        if (cellInfoList.isNullOrEmpty()) {
            Log.d("CellInfo", "No cell info available or list is empty")
            textViewStatus?.text = "No cell info available"
            clearCellInfoTextViews()
            textViewTimestamp?.text = "Last Update: N/A"
            return
        }

        val currentTime = Date()
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val formattedTime = formatter.format(currentTime)
        textViewTimestamp?.text = "Last Update: $formattedTime"
        clearCellInfoTextViews()
        textViewStatus?.text = "Cell info detected"


        var registeredCellFound = false

        for (cellInfo in cellInfoList) {
            // 登録済みセル（接続中のセル）を優先して探す
            if (cellInfo.isRegistered) {
                textViewStatus?.text = "Registered cell info found"
                textViewFullInfo?.text = cellInfo.toString()

                when (cellInfo) {
                    is CellInfoNr -> {
                        // 5G NR
                        val cellIdentityNr = cellInfo.cellIdentity as CellIdentityNr
                        val nrarfcn = cellIdentityNr.nrarfcn
                        val message = "NR (Registered) - NRARFCN: $nrarfcn"
                        Log.i("CellInfo", message)
                        textViewNrarfcn?.text = message
                        textViewSubNrarfcn?.text = cellIdentityNr.toString()
                        registeredCellFound = true
                        break
                    }
                    is CellInfoLte -> {
                        // 4G LTE
                        val cellIdentityLte = cellInfo.cellIdentity
                        val earfcn = cellIdentityLte.earfcn
                        val message = "LTE (Registered) - EARFCN: $earfcn"
                        Log.i("CellInfo", message)
                        textViewEarfcn?.text = message
                        textViewSubEarfcn?.text = cellIdentityLte.toString()
                        registeredCellFound = true
                        break
                    }
                    else -> {
                        Log.d("CellInfo", "Detected other registered cell type: ${cellInfo.javaClass.simpleName}")
                        // textViewStatus?.append("\nOther Registered Cell: ${cellInfo.javaClass.simpleName}")
                        registeredCellFound = true // 登録済みセル見つかったフラグを立てる (ここでは break しない)
                    }
                }
            }
        }

        if (!registeredCellFound) {
            textViewStatus?.text = "No registered cell info found. Displaying first cell in list (if any)."
            cellInfoList.firstOrNull()?.let { firstCell ->
                textViewFullInfo?.text = firstCell.toString()
                when (firstCell) {
                    is CellInfoNr -> {
                        val cellIdentityNr = firstCell.cellIdentity as CellIdentityNr
                        val nrarfcn = cellIdentityNr.nrarfcn
                        val message = "NR (First Found) - NRARFCN: $nrarfcn"
                        Log.i("CellInfo", message)
                        textViewNrarfcn?.text = message
                        textViewSubNrarfcn?.text = cellIdentityNr.toString()
                    }
                    is CellInfoLte -> {
                        val cellIdentityLte = firstCell.cellIdentity
                        val earfcn = cellIdentityLte.earfcn
                        val message = "LTE (First Found) - EARFCN: $earfcn"
                        Log.i("CellInfo", message)
                        textViewEarfcn?.text = message
                        textViewSubEarfcn?.text = cellIdentityLte.toString()
                    }
                    else -> {
                        Log.d("CellInfo", "First found cell type: ${firstCell.javaClass.simpleName}")
                    }
                }
            }
        }

        Log.d("CellInfo", "Cell info processing finished. Timestamp updated.")
    }
}