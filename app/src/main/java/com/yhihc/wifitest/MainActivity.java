package com.yhihc.wifitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public InetAddress servIpAddr = null;
    public String ssid = "";
    public String password = "";
    public String staSec = "";
    public String netProto = "TCP";
    public String port = "5000";
    public String remoteServ = "";

    //
    private ListView wifiListView;
    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;
    WifiReceiver receiverWifi;
    private Button search;
    private Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //wifi's search result ListView init
        wifiListView = findViewById(R.id.wifiList);

        //wifiManager init
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {  //open wifi device
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        //wifi search button
        search = findViewById(R.id.button_scan);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //need ACCESS LOCAL
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
                } else {
                    wifiManager.startScan();  //search wifi devices
                }
            }
        });

        connect = findViewById(R.id.btnConnect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager, wifiListView);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiverWifi, intentFilter);
        getWifi();
    }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(MainActivity.this, "version> = marshmallow", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else {
                Toast.makeText(MainActivity.this, "location turned on", Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(MainActivity.this, "scanning", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //取消廣播註冊
        unregisterReceiver(receiverWifi);
    }

    @Override  //權限回調
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                wifiManager.startScan();
            } else {
                Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                return;
            }
            break;
        }
    }

    /******************** 廠商給的sample code ********************************************/
    public static ArrayList<String> getWiFiModuleIp() {
        ArrayList<String> ipList = new ArrayList<>();
        byte[] broadcast = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        byte[] getDevInfoReq = {0x01, 0x21, 0x01};
        byte[] rxData = new byte[1024];
        DatagramSocket clientSocket = null;
        Log.d("CJNWiFi","getWiFiModuleIp");
        DatagramPacket dp = new DatagramPacket(rxData, rxData.length);
        try {
            clientSocket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(getDevInfoReq, getDevInfoReq.length, InetAddress.getByAddress(broadcast), 60002);
            clientSocket.setBroadcast(true);
            clientSocket.send(packet);
            while(true){
                try{
                    clientSocket.setSoTimeout(2000);
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
//                            devCount += 1;
                        }
                    }
                }catch (SocketTimeoutException e) {
                    Log.d("CJNWiFi", "getDevInfo Rx SocketTimeoutException");
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
        return ipList;
    }


    private byte[] wmcpDataSetReq(int cmdcode, String params) {
        byte[] wmcpDataBuffer = new byte[params.length() + 3];
        wmcpDataBuffer[0] = 0x01;
        wmcpDataBuffer[1] = (byte)cmdcode;
        wmcpDataBuffer[2] = 0x02;

        if (!params.equals("")) {
            int i, j;
            byte[] byteParams = params.getBytes();

            j = 3;
            for (i = 0; i < byteParams.length; i++){
                wmcpDataBuffer[j++] = byteParams[i];
            }
        }

        return wmcpDataBuffer;
    }

    public String setWiFiModuleConfig() {
        DatagramSocket clientSocket = null;
        byte[] rxData = new byte[512];
        DatagramPacket dp = new DatagramPacket(rxData, rxData.length);
        boolean setOK = false;
        ArrayList<byte[]> wmcpDataArray = new ArrayList<>();

        if (servIpAddr == null)
            return "IP ERROR";

        wmcpDataArray.add(wmcpDataSetReq(0x11, "0"));	//OPMODE 0:STA 1:AP
        wmcpDataArray.add(wmcpDataSetReq(0x14, ssid));  //WSTA
        if (password == "") {
            wmcpDataArray.add(wmcpDataSetReq(0x15, "NONE"));  //WSTASEC
        }
        else {
            wmcpDataArray.add(wmcpDataSetReq(0x15, "WPA2"));  //WSTASEC
            String cypher = "AES";
            String psk = cypher + "," + password;
            wmcpDataArray.add(wmcpDataSetReq(0x16, psk));  //WSTAPSK
        }
        wmcpDataArray.add(wmcpDataSetReq(0x23, netProto)); //NSOCK
        String client = remoteServ + "," + port;
        wmcpDataArray.add(wmcpDataSetReq(0x24, client)); //NCLIENT
        wmcpDataArray.add(wmcpDataSetReq(0x32, "")); // APPLY

        try {
            clientSocket = new DatagramSocket();
            for (int i = 0; i < wmcpDataArray.size(); i++) {
                byte[] wmcpDataPayload = wmcpDataArray.get(i);
                byte cmdcode = wmcpDataPayload[1];
                DatagramPacket packet = new DatagramPacket(wmcpDataPayload, wmcpDataPayload.length, servIpAddr, 60002);
                clientSocket.send(packet);
                int retry = 0;
                while (retry < 3) {
                    try {
                        clientSocket.setSoTimeout(2000);
                        clientSocket.receive(dp);

                        byte[] data = dp.getData();
                        if (data[0] == 0x02 &&
                                data[1] == cmdcode &&
                                data[2] == 0x02) {
                            // handle rx data
                            byte[] resp = Arrays.copyOfRange(data, 3, dp.getLength());
                            String strResp = new String(resp);
                            String msg = String.format("Set cmdcode %x response=", cmdcode);
                            Log.i("CJN", msg+strResp);
                            if (strResp.equals("ok")) {
                                setOK = true;
                                break;
                            }
                            else {
                                setOK = false;
                                break;
                            }


                        }
                    } catch (SocketTimeoutException e) {
                        retry += 1;
                        clientSocket.send(packet);
                        String str = String.format("Retry device configuration request : %d", retry);
                        Log.i("CJN", str);
                    }
                }
                if (!setOK)
                    break;
            }

        } catch (SocketException e) {
            if (clientSocket != null) {
                clientSocket.close();
            }
            return "Socket Exception";
        } catch (IOException e) {
            if (clientSocket != null) {
                clientSocket.close();
            }
            return "IO Exception";
        }

        if (clientSocket != null) {
            clientSocket.close();
        }
        return "OK";
    }
}