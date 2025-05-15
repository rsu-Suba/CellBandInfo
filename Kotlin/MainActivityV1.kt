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
import android.telephony.CellSignalStrengthNr
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
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan


class MainActivity : AppCompatActivity() {
    private var textViewStatus: TextView? = null
    private var textViewTimestamp: TextView? = null
    private var textViewEarfcn: TextView? = null
    private var textViewSubEarfcn: TextView? = null
    private var textViewNrarfcn: TextView? = null
    private var textViewSubNrarfcn: TextView? = null
    private var textViewFullInfo: TextView? = null
    private var startButton: Button? = null
    private var changeButton: Button? = null

    val fullInfoSb = StringBuilder()
    private var infoMode = false;
    private var fullInfoStr = "";

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
                textViewFullInfo?.text = "Full Info: Permission Denied to access location."
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
        changeButton = findViewById(R.id.activity_main_change_button)

        startButton?.setOnClickListener {
            Log.d("CellInfo", "Button clicked. Checking permissions.")
            textViewStatus?.text = "Checking permissions..."
            textViewTimestamp?.text = "Updating..."
            clearCellInfoTextViews() // FullInfoもクリアされる
            checkLocationPermissionAndGetCellInfo()
            checkLocationPermissionAndGetCellInfo()
        }

        changeButton?.setOnClickListener {
            if (infoMode) {
                textViewFullInfo?.text = fullInfoStr;
            } else {
                textViewFullInfo?.text = fullInfoSb;
            }
            infoMode = !infoMode;
        }

        Log.d("CellInfo", "App started. Checking permissions for initial data.")
        textViewStatus?.text = "Initializing..."
        textViewTimestamp?.text = "Updating..."
        clearCellInfoTextViews() // FullInfoもクリアされる
        checkLocationPermissionAndGetCellInfo()
    }


    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            // API 30以上では isNavigationBarContrastEnforced はデフォルトで false のため、明示的な設定は不要な場合も
            // window.isNavigationBarContrastEnforced = false // 必要に応じて
        } else {
            @Suppress("DEPRECATION") // FLAG_LAYOUT_NO_LIMITS はAPI 30で非推奨
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
                Log.i("CellInfo", "Showing rationale for location permission")
                textViewStatus?.text = "Location permission required for this feature."
                textViewTimestamp?.text = "Last Update: Permission Needed"
                textViewFullInfo?.text = "Full Info: Permission needed to show details."
                // ユーザーに権限が必要な理由を説明するUIを表示する (例: AlertDialog)
                // ここでは、再度ボタンを押してもらうか、自動でリクエストはしない
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                telephonyManager.requestCellInfoUpdate(
                    mainExecutor,
                    object : TelephonyManager.CellInfoCallback() {
                        override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                            Log.d("CellInfo", "onCellInfo callback received with ${cellInfo.size} items.")
                            processCellInfoList(cellInfo)
                        }
                        override fun onError(errorCode: Int, detail: Throwable?) {
                            super.onError(errorCode, detail)
                            Log.e("CellInfo", "Error getting cell info (requestCellInfoUpdate): $errorCode", detail)
                            val errorMessage = "Cell Info Error (Callback): $errorCode" +
                                    (detail?.let { " - Detail: ${it.localizedMessage ?: it.toString()}" } ?: "")
                            textViewStatus?.text = errorMessage
                            textViewTimestamp?.text = "Last Update: Error"
                            textViewFullInfo?.text = "Full Info: Error - $errorMessage"
                        }
                    }
                )
            } else {
                Log.e("CellInfo", "Permission lost before requestCellInfoUpdate could be called.")
                textViewStatus?.text = "Error: Permission required to get cell info."
                textViewTimestamp?.text = "Last Update: Permission Error"
                textViewFullInfo?.text = "Full Info: Permission Error"
            }
        } else {
            Log.d("CellInfo", "Using telephonyManager.allCellInfo for API < Q")
            // allCellInfo はメインスレッドで呼び出す必要がある場合がある
            // また、ACCESS_FINE_LOCATION 権限が必要
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                @Suppress("Deprecation")
                val cellInfoList = telephonyManager.allCellInfo
                Log.d("CellInfo", "telephonyManager.allCellInfo returned ${cellInfoList?.size ?: 0} items.")
                processCellInfoList(cellInfoList)
            } else {
                Log.e("CellInfo", "Permission denied for telephonyManager.allCellInfo.")
                textViewStatus?.text = "Error: Permission required (API < Q)."
                textViewTimestamp?.text = "Last Update: Permission Error"
                textViewFullInfo?.text = "Full Info: Permission Error (API < Q)"
            }
        }
    }

    private fun processCellInfoList(cellInfoList: List<CellInfo>?) {
        fullInfoStr = cellInfoList.toString();

        fullInfoSb.clear();
        Log.d("CellInfo", "Processing cell info list. Size: ${cellInfoList?.size ?: 0}")

        val processedEarfcnSet = mutableSetOf<Int>()
        val processedNrarfcnSet = mutableSetOf<Int>()

        val currentTime = Date()
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val formattedTime = formatter.format(currentTime)
        textViewTimestamp?.text = "Last Update: $formattedTime"

        // clearCellInfoTextViews() はここで呼ばず、ボタンクリック時や初期化時に呼ぶ
        // textViewStatus と textViewFullInfo はこの関数内で更新する

        if (cellInfoList.isNullOrEmpty()) {
            Log.d("CellInfo", "No cell info available or list is empty")
            textViewStatus?.text = "No cell info available"
            textViewEarfcn?.text = "4G Band" // Clear specific fields
            textViewSubEarfcn?.text = "4G Info"
            textViewNrarfcn?.text = "5G Band"
            textViewSubNrarfcn?.text = "5G Info"
            textViewFullInfo?.text = "Full Info: No cell info available or list is empty."
            return
        }

        textViewStatus?.text = "Cell info detected (${cellInfoList.size} items)"
        // 他のTextViewはループ内で登録済みセルが見つかった場合に更新する

        fullInfoSb.append("All Cell Info Details (${cellInfoList.size} items):\n")
        fullInfoSb.append("====================================\n\n")

        var registeredCellFoundForSpecificDisplay = false

        for ((index, cellInfo) in cellInfoList.withIndex()) {
            var shouldAppend = true

            when (cellInfo) {
                is CellInfoNr -> {
                    fullInfoSb.append("Type: NR (5G)\n")
                    // CellInfoNr and its components require API 29+
                        val identity = cellInfo.cellIdentity as CellIdentityNr
                        val strength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        fullInfoSb.append("  Identity: NRARFCN=${identity.nrarfcn}, PCI=${identity.pci}\n")
                        fullInfoSb.append("    MCC=${identity.mccString}, MNC=${identity.mncString}\n")

                        if (cellInfo.isRegistered && !registeredCellFoundForSpecificDisplay) {
                            textViewNrarfcn?.text = "NR (Reg) NRARFCN: ${identity.nrarfcn}"
                            textViewSubNrarfcn?.text = "PCI: ${identity.pci}, SS_RSRP: ${strength.ssRsrp}"
                            registeredCellFoundForSpecificDisplay = true
                        }
                }
                is CellInfoLte -> {
                    val identity = cellInfo.cellIdentity
                    val strength = cellInfo.cellSignalStrength

                    if (processedEarfcnSet.contains(identity.earfcn)) {
                        shouldAppend = false
                    } else {
                        processedEarfcnSet.add(identity.earfcn)
                    }

                    if (shouldAppend) {
                        fullInfoSb.append("Type: LTE\n")
                        fullInfoSb.append("  Identity: EARFCN=${identity.earfcn}, Band=${identity.bands[0]}\n")
                    }


                    if (cellInfo.isRegistered && !registeredCellFoundForSpecificDisplay) {
                        textViewEarfcn?.text = "LTE (Reg) EARFCN: ${identity.earfcn}"
                        textViewSubEarfcn?.text = "PCI: ${identity.pci}, RSRP: ${strength.rsrp}"
                        registeredCellFoundForSpecificDisplay = true
                    }
                }
            }
            if (shouldAppend) {
                fullInfoSb.append("------------------------------------\n\n")
            }
        }

        if (infoMode) {
            textViewFullInfo?.text = fullInfoSb;
        } else {
            textViewFullInfo?.text = fullInfoStr;
        }

        Log.d("CellInfo.FullInfo", fullInfoStr);

        // 登録済みセルがなかった場合に、主要表示TextViewをクリアする（または「N/A」と表示する）
        if (!registeredCellFoundForSpecificDisplay) {
            textViewStatus?.text = "No registered cell found in list. Displaying all available (${cellInfoList.size} items)."
            textViewEarfcn?.text = "4G Band (N/A)"
            textViewSubEarfcn?.text = "4G Info (N/A)"
            textViewNrarfcn?.text = "5G Band (N/A)"
            textViewSubNrarfcn?.text = "5G Info (N/A)"
        } else {
            textViewStatus?.text = "Registered cell info found & displayed. All available (${cellInfoList.size} items)."
        }

        Log.d("CellInfo", "Cell info processing finished. Timestamp and FullInfo updated.")
    }
}