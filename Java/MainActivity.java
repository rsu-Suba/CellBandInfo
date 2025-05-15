package com.example.nrarfcn;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoNr;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CellInfoApp";
    private TextView cellInfoTextView;
    private Button getCellInfoButton;
    private TelephonyManager telephonyManager;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cellInfoTextView = findViewById(R.id.text_cell_info);
        getCellInfoButton = findViewById(R.id.button_get_cell_info);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean fineLocationGranted = permissions.getOrDefault(
                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                    boolean readPhoneStateGranted = permissions.getOrDefault(
                            Manifest.permission.READ_PHONE_STATE, false);

                    if (fineLocationGranted && readPhoneStateGranted) {
                        getCellInfo();
                    } else {
                        Toast.makeText(this, "Need permission.", Toast.LENGTH_LONG).show();
                        cellInfoTextView.setText("Permission denied\nPlease allow location permission.");
                    }
                });

        getCellInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndGetCellInfo();
            }
        });
    }

    private void checkPermissionsAndGetCellInfo() {
        boolean fineLocationGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean readPhoneStateGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;


        if (fineLocationGranted && readPhoneStateGranted) {
            getCellInfo();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            });
        }
    }

    private void getCellInfo() {
        if (telephonyManager == null) {
            cellInfoTextView.setText("Couldn't get TelephonyManager.");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
            cellInfoTextView.setText("No permission to get cell info.");
            return;
        }

        StringBuilder cellInfoText = new StringBuilder("Cell info:\n");

        try {
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();

            if (cellInfoList != null && !cellInfoList.isEmpty()) {
                boolean nrInfoFound = false;
                for (CellInfo cellInfo : cellInfoList) {
                    cellInfoText.append("  - ").append(cellInfo.getClass().getSimpleName()).append(" (Registered: ").append(cellInfo.isRegistered()).append(") ");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                        CellInfoNr cellInfoNr = (CellInfoNr) cellInfo;
                        CellIdentityNr cellIdentityNr = (CellIdentityNr) cellInfoNr.getCellIdentity();

                        if (cellIdentityNr != null) {
                            nrInfoFound = true;
                            cellInfoText.append("    -- NR (5G) Cell --\n");
                            cellInfoText.append("    PCI: ").append(cellIdentityNr.getPci()).append("\n");
                            cellInfoText.append("    TAC: ").append(cellIdentityNr.getTac()).append("\n");
                            cellInfoText.append("    NCI: ").append(cellIdentityNr.getNci()).append("\n");
                            cellInfoText.append("    NRARFCN: ").append(cellIdentityNr.getNrarfcn()).append("\n");

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                cellInfoText.append("    MCC: ").append(cellIdentityNr.getMccString()).append("\n");
                                cellInfoText.append("    MNC: ").append(cellIdentityNr.getMncString()).append("\n");
                            }

                            int[] bands = cellIdentityNr.getBands();
                            cellInfoText.append("    Bands: ");
                            if (bands != null && bands.length > 0) {
                                for (int i = 0; i < bands.length; i++) {
                                    cellInfoText.append(bands[i]);
                                    if (i < bands.length - 1) {
                                        cellInfoText.append(", ");
                                    }
                                }
                                cellInfoText.append("\n");
                            } else {
                                cellInfoText.append("No info\n");
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                cellInfoText.append("    Additional Bands: ").append(cellIdentityNr.getAdditionalPlmns()).append("\n");
                            }
                        }
                    }
                }

                if (!nrInfoFound) {
                    cellInfoText.append("\nNo 5G NR.\n");
                }

            } else {
                cellInfoText.append("Couldn't get info\n");
            }

        } catch (SecurityException e) {
            cellInfoText.append("Couldn't get info\nPermission issue").append(e.getMessage());
        } catch (Exception e) {
            cellInfoText.append("Something went wrong").append(e.getMessage());
        }

        cellInfoTextView.setText(cellInfoText.toString());
    }
}