package com.example.arfcntest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var infoMode = false
    private var fullInfoStr = ""

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                textViewStatus?.text = "Permission granted. Getting cell info..."
                getCellInfo()
            } else {
                textViewStatus?.text = "Location permission denied."
                textViewTimestamp?.text = "Last Update: Permission Denied"
                fullInfoStr = "Full Info: Permission Denied to access location."
                updateFullInfoDisplay()
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
            // checkLocationPermissionAndGetCellInfo() // 元のコードで2回呼ばれていましたが、通常は1回で十分です。今回は元のままにしておきます。
        }

        changeButton?.setOnClickListener {
            infoMode = !infoMode
            // infoModeが切り替わった後に表示を更新する
            updateFullInfoDisplay()
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
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
    }

    private fun clearCellInfoTextViews() {
        textViewEarfcn?.text = "4G Band"
        textViewSubEarfcn?.text = "4G Info"
        textViewNrarfcn?.text = "5G Band"
        textViewSubNrarfcn?.text = "5G Info"
        textViewFullInfo?.text = "Full Info"
        fullInfoSb.clear() // fullInfoSbもクリア
        fullInfoStr = "" // fullInfoStrもクリア
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
                fullInfoStr = "Full Info: Permission needed to show details." // fullInfoStrを更新
                updateFullInfoDisplay() // 権限が必要な場合も表示を更新
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
                            // エラー発生時もfullInfoStrにエラーメッセージを設定して表示更新
                            fullInfoStr = "Full Info: Error - $errorMessage"
                            updateFullInfoDisplay()
                        }
                    }
                )
            } else {
                Log.e("CellInfo", "Permission lost before requestCellInfoUpdate could be called.")
                textViewStatus?.text = "Error: Permission required to get cell info."
                textViewTimestamp?.text = "Last Update: Permission Error"
                fullInfoStr = "Full Info: Permission Error"
                 updateFullInfoDisplay() // 表示更新
            }
        } else {
            Log.d("CellInfo", "Using telephonyManager.allCellInfo for API < Q")
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
                fullInfoStr = "Full Info: Permission Error (API < Q)"
                 updateFullInfoDisplay() // 表示更新
            }
        }
    }

    private fun processCellInfoList(cellInfoList: List<CellInfo>?) {
        // cellInfoList.toString() の結果をfullInfoStrに格納
        fullInfoStr = cellInfoList.toString()

        // fullInfoSb は整形された情報を格納（今回は色付けしない）
        fullInfoSb.clear() // 新しい情報でクリア
        Log.d("CellInfo", "Processing cell info list. Size: ${cellInfoList?.size ?: 0}")

        val processedEarfcnSet = mutableSetOf<Int>()

        val currentTime = Date()
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        val formattedTime = formatter.format(currentTime)
        textViewTimestamp?.text = "Last Update: $formattedTime"

        if (cellInfoList.isNullOrEmpty()) {
            Log.d("CellInfo", "No cell info available or list is empty")
            textViewStatus?.text = "No cell info available"
            // Specific fields remain as default or cleared by clearCellInfoTextViews()
            // fullInfoSbもここで内容を設定
            fullInfoSb.append("Full Info: No cell info available or list is empty.")
             updateFullInfoDisplay() // 表示更新
            return
        }

        textViewStatus?.text = "Cell info detected (${cellInfoList.size} items)"

        fullInfoSb.append("All Cell Info Details (Formatted, ${cellInfoList.size} items):\n")
        fullInfoSb.append("====================================\n\n")

        var registeredCellFoundForSpecificDisplay = false

        for (cellInfo in cellInfoList) {
            var shouldAppend = true
            // 各CellInfoオブジェクトの詳細をfullInfoSbに追加（整形）
            when (cellInfo) {
                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val identity = cellInfo.cellIdentity as CellIdentityNr
                        val strength = cellInfo.cellSignalStrength as CellSignalStrengthNr
                         fullInfoSb.append("Type: NR (5G)\n")
                         fullInfoSb.append("  Identity: NRARFCN=${identity.nrarfcn}, PCI=${identity.pci}\n")
                         fullInfoSb.append("    MCC=${identity.mccString}, MNC=${identity.mncString}\n")
                         fullInfoSb.append("  Signal: SS_RSRP=${strength.ssRsrp}, SS_RSRQ=${strength.ssRsrq}, SS_SINR=${strength.ssSinr}\n")

                        if (cellInfo.isRegistered && !registeredCellFoundForSpecificDisplay) {
                            textViewNrarfcn?.text = "NR (Reg) NRARFCN: ${identity.nrarfcn}"
                            textViewSubNrarfcn?.text = "PCI: ${identity.pci}, SS_RSRP: ${strength.ssRsrp}"
                            registeredCellFoundForSpecificDisplay = true
                        }
                    } else {
                        fullInfoSb.append("Type: NR (5G) - Details not available below API Q\n")
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
                 else -> {
                      fullInfoSb.append("Type: Unknown or Unsupported Cell Type\n")
                      fullInfoSb.append(cellInfo.toString().trim()).append("\n")
                 }
            }
            if (shouldAppend) {
                fullInfoSb.append("------------------------------------\n\n")
            }
        }

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

        // fullInfoSb または fullInfoStr の表示を更新
        updateFullInfoDisplay()
    }

    // fullInfoStr の内容を解析し、"arfcn" を赤色にして表示する
    // fullInfoSb は整形された内容で、ここでは色付けしない
    private fun updateFullInfoDisplay() {
        if (infoMode) {
            // infoModeがtrueの場合はfullInfoSb（整形）をそのまま表示（色付けなし）
            textViewFullInfo?.text = fullInfoSb

        } else {
            // infoModeがfalseの場合はfullInfoStr（toString()結果）に色付けして表示
            val fullInfoText = fullInfoStr // fullInfoStrを対象にする
            val spannableSb = SpannableStringBuilder(fullInfoText)
            val searchTerm = "arfcn" // 検索対象の文字列
            var startIndex = 0

            // 大文字小文字を区別せずに "arfcn" を検索し、見つかった部分を赤色にする
            while (startIndex < fullInfoText.length) {
                val index = fullInfoText.indexOf(searchTerm, startIndex, ignoreCase = true)
                if (index == -1) {
                    break // 見つからなければ終了
                }
                val endIndex = index + searchTerm.length
                // ForegroundColorSpan を適用
                spannableSb.setSpan(
                    ForegroundColorSpan(Color.RED),
                    index,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                startIndex = endIndex // 次の検索開始位置を更新
            }
            textViewFullInfo?.text = spannableSb // 色付けされたSpannableStringBuilderをTextViewに設定
        }
         Log.d("CellInfo.FullInfo.Display", if(infoMode) "Showing formatted info (no color)" else "Showing raw toString (color applied)")
    }
}