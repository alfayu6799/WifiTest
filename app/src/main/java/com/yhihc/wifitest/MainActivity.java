package com.yhihc.wifitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ArrayList<String> ipList = new ArrayList<>();

    private ImageView searchWifi;
    private EditText  customPassword;
    private TextView  customWifiSSID;
    private Button    saveConfig;
    private TextView  customWifiInit;
    private Button    connectCustomSSID;

    private TextView  cjmWifiSSID;
    private EditText  cjmPassword;
    private Button    cjmConnect;
    private Button    cjm410IPAddr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        //自動開啟wifi
        WifiUtils.withContext(getApplicationContext()).enableWifi();

        saveConfig = findViewById(R.id.btn_write_conf);                 //write config to cjm410 button
        saveConfig.setEnabled(false);                                   //write config to cjm410 button disable

        cjm410IPAddr = findViewById(R.id.btn_find_ip);
        cjm410IPAddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FindCJM410Ip findCJM410Ip = new FindCJM410Ip();
                findCJM410Ip.execute();
            }
        });

        cjmWifiSSID = findViewById(R.id.tv_cjm40);                      //cjm410's ssid
        cjmPassword = findViewById(R.id.edt_cjm_password);              //cjm410's password
        cjmConnect = findViewById(R.id.btn_connect);                    //cjm410 connect button
        cjmConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //連上cjm410 wifi
                WifiUtils.withContext(getApplicationContext())
                        .connectWith(cjmWifiSSID.getText().toString(), cjmPassword.getText().toString())
                        .setTimeout(4000)
                        .onConnectionResult(new ConnectionSuccessListener() {
                            @Override
                            public void success() {
                                Toast.makeText(MainActivity.this, getString(R.string.connect_succed), Toast.LENGTH_SHORT).show();
                                //連上wifi成功後才可以進行寫入config的動作
                                saveConfig.setEnabled(true);
                            }

                            @Override
                            public void failed(@NonNull ConnectionErrorCode errorCode) {
                                Toast.makeText(MainActivity.this, getString(R.string.connect_fail) + errorCode.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }).start();
            }
        });

        customWifiSSID = findViewById(R.id.tv_custom_wifi_ssid);                       //客戶端可上網wifi之SSID
        customPassword = findViewById(R.id.edt_custom_wifi_ps);                        //客戶端可上網wifi之password
        customPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);   //password顯示
        customWifiInit = findViewById(R.id.tv_custom_init);

        //搜尋客戶端可以上網的無線基地台
        searchWifi = findViewById(R.id.iv_wifi_search);
        searchWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiUtils.withContext(getApplicationContext()).scanWifi(this::getScanResult).start();
            }

            private void getScanResult(List<ScanResult> scanResults) {
                if (scanResults.isEmpty()){
                 Toast.makeText(MainActivity.this, getString(R.string.scan_is_empty), Toast.LENGTH_SHORT).show();
                 return;
                }

                //將結果顯示在ListView
                showSSIDDialog(scanResults);
            }
        });

        //將設定寫到cjm410(客戶端wifi ssid,客戶端wifi password)
        saveConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBeforeUpdate();
            }
        });

        //連接客戶端ssid(for cjm410初始化用)
        connectCustomSSID = findViewById(R.id.btn_connect_custom_ssid);  //custom wifi connect button
        connectCustomSSID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //1.先連上客戶端wifi(ssid&password)
                WifiUtils.withContext(getApplicationContext())
                        .connectWith(customWifiSSID.getText().toString(), customPassword.getText().toString())
                        .setTimeout(4000)
                        .onConnectionResult(new ConnectionSuccessListener() {
                            @Override
                            public void success() {
                                Toast.makeText(MainActivity.this, getString(R.string.connect_succed), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void failed(@NonNull ConnectionErrorCode errorCode) {
                                if (errorCode.toString().equals("DID_NOT_FIND_NETWORK_BY_SCANNING")) {
                                    Toast.makeText(MainActivity.this, getString(R.string.custom_ssid_is_empty), Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(MainActivity.this, getString(R.string.connect_fail) + errorCode.toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).start();
            }
        });


        //3.command "0x34" to Restore the system configuration to factory default value and reboot the system


    }

    //客戶端wifi彈跳視窗
    private void showSSIDDialog(List<ScanResult> scanResults) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select wifi to connect");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item);

        ScanResult result;
        for (int i = 0; i < scanResults.size(); i++){
            result = scanResults.get(i);
            String ssid = result.SSID;
            arrayAdapter.add(ssid);
        }

        builder.setNegativeButton(getString(R.string.cancal), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });

        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                String strName = arrayAdapter.getItem(which);
                customWifiSSID.setText(strName);
                //將客戶端無線基地台ssid的值給customWifiInit作為初始化用
                customWifiInit.setText(customWifiSSID.getText().toString());
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //檢查是否資料齊全
    private void checkBeforeUpdate() {
        //客戶端wifi ssid
        if (TextUtils.isEmpty(customWifiSSID.getText().toString())){
            Toast.makeText(MainActivity.this, getString(R.string.custom_ssid_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        //客戶端wifi password
        if (TextUtils.isEmpty(customPassword.getText().toString())){
            Toast.makeText(MainActivity.this, getString(R.string.password_is_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        //write info to cjm410 flash and reboot system


        Log.d(TAG, "checkBeforeUpdate: customWifiSSID:" + customWifiSSID.getText().toString() + ",password:" + customPassword.getText().toString());
    }

    //找出CJM410的ip(已變成Client端)
    public class FindCJM410Ip extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            byte[] getDevInfoReq = {0x01, 0x21, 0x01};
            byte[] rxData = new byte[1024];
            DatagramSocket clientSocket = null;
            Log.d("CJN-WiFi","getWiFiModuleIp");
            DatagramPacket dp = new DatagramPacket(rxData, rxData.length); //數據存在rxData中
            try {
                clientSocket = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(getDevInfoReq, getDevInfoReq.length, InetAddress.getByAddress(broadcast), 60002);
                clientSocket.setBroadcast(true);
                clientSocket.send(packet);
                while(true){
                    try{
                        clientSocket.setSoTimeout(60000); //60秒(原:2000)
                        clientSocket.receive(dp);

                        byte[] data = dp.getData();
                        if (data[0] == 0x02 &&
                                data[1] == 0x21 &&
                                data[2] == 0x01) {
                            // handle rx data
                            byte[] ip = Arrays.copyOfRange(data, 3, dp.getLength());
                            String strIp = new String(ip);
                            if(!ipList.contains(strIp)){
                                ipList.add(strIp);
                            }
                        }
                    }catch (SocketTimeoutException e) {
                        Log.d("CJNWiFi", "getDevInfo Rx SocketTimeoutException" + e.toString());
                        // No more device ACK
                        break;
                    }
                }

            } catch (SocketException e) {
                if(clientSocket != null) {
                    clientSocket.close();
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "doInBackground: "  + ipList.toString());
            return ipList;
        }


        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }
}