package com.yhihc.wifitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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

/***
 * yhy 貼片wifi設定專用
 * 需要手動開啟 LOCATION權限(暫時)
 * */
public class MainActivity2 extends AppCompatActivity {

    private static final String TAG = "MainActivity2";

    private Button save;
    private ImageView yhySearch;
    private ImageView WifiSearch;
    private EditText  yhyPassword;
    private EditText  wifiPassword;
    private TextView  yhyWifiSSID;
    private TextView  wifiSSID;
    private TextView  serverIp;
    private EditText  serverPort;
    private ImageView pingYHYssid;
    private ImageView pingWifissid;

    private TextView  cjm410Info;
    private Button    showInfo;

    public InetAddress servIpAddr = null;
    public String ssid = "HuiKang";
    public String password = "Rc54195018";
    public String staSec = "";
    public String netProto = "TCP";
    public String port = "5000";
    public String remoteServ = "";
    public String apChannel = "6";
    public int opmode = 1;
    public String devName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        //自動開啟wifi
        WifiUtils.withContext(getApplicationContext()).enableWifi();

        List<String> wifiList = new ArrayList<>();

        cjm410Info = findViewById(R.id.tv_cjm410_info);
        showInfo = findViewById(R.id.btnInfo);
        showInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                setWiFiModuleConfig();
            }
        });

        yhyPassword = findViewById(R.id.edt_input_password); //自家wifi密碼
        wifiPassword = findViewById(R.id.edt_wifi_password); //客戶端wifi密碼

        yhyWifiSSID = findViewById(R.id.tv_yhy_SSID);    //搜尋自家的wifi ssid
        yhyWifiSSID.setText("CJN-WiFi");
        wifiSSID = findViewById(R.id.tv_internet_SSID);  //搜尋到客戶端的wifi ssid

        serverIp = findViewById(R.id.edt_server_ip);     //自家主機IP

        serverPort = findViewById(R.id.edt_server_port); //自家主機PORT

        pingYHYssid = findViewById(R.id.btn_ping_yhy);    //自家wifi ping
        pingWifissid = findViewById(R.id.btn_ping_wifi);  //客戶端wifi ping

        //搜尋自家的wifi
        yhySearch = findViewById(R.id.btn_yhy_search);
        yhySearch.setVisibility(View.GONE);
        yhySearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiUtils.withContext(getApplicationContext()).scanWifi(this::getScanResults).start();
            }

            private void getScanResults(List<ScanResult> scanResults) {
                if (scanResults.isEmpty()){
                    Log.d(TAG, "getScanResults1: SCAN RESULT IS EMPTY" );
                    return;
                }
                Log.d(TAG, "getScanResults1: " + scanResults); //ListView dialog
            }

        });

        //搜尋客戶端的wifi
        WifiSearch = findViewById(R.id.btn_wifi_search);
        WifiSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiUtils.withContext(getApplicationContext()).scanWifi(this::getScanResults).start();
            }

            private void getScanResults(List<ScanResult> scanResults) {
                if (scanResults.isEmpty()){
                    Log.d(TAG, "getScanResults2: SCAN RESULT IS EMPTY" );
                    return;
                }

                for (int i = 0; i < scanResults.size(); i++){
                    ScanResult result = scanResults.get(i);

                    Log.d(TAG, "getScanResults2: " + result);
                }

                //Log.d(TAG, "getScanResults2: " + result.SSID); //ListView dialog
            }
        });
        
        save = findViewById(R.id.btn_save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //checkBeforeToExecute();
                WifiUtils.withContext(getApplicationContext())
                        .connectWith(yhyWifiSSID.getText().toString(),yhyPassword.getText().toString())
                        .setTimeout(40000)
                        .onConnectionResult(new ConnectionSuccessListener() {
                            @Override
                            public void success() {
                                Log.d(TAG, "success: ");
                            }

                            @Override
                            public void failed(@NonNull ConnectionErrorCode errorCode) {
                                Log.d(TAG, "failed: " + errorCode.toString());
                            }
                        }).start();
            }
        });
    }

    //檢查所有的資訊是否有沒填寫的
    private void checkBeforeToExecute() {

        if (TextUtils.isEmpty(yhyWifiSSID.getText())){
            return;
        }else if (TextUtils.isEmpty(yhyPassword.getText().toString())){
            return;
        }else if (TextUtils.isEmpty(serverIp.getText().toString())){
            return;
        }else if (TextUtils.isEmpty(serverPort.getText().toString())){
            return;
        }else if (TextUtils.isEmpty(wifiSSID.getText())){
            return;
        }else if (TextUtils.isEmpty(wifiPassword.getText().toString())){
            Log.d(TAG, "checkBeforeToExecute: 聒聒" );
            return;
        }else {
            //以上資料都齊全後才會進行廠商給的sample code
            Log.d(TAG, "匯康wifi: " + yhyWifiSSID.getText() + ",客戶端wifi:" + wifiSSID.getText());
            Log.d(TAG, "匯康wifi password: " + yhyPassword.getText().toString() + ",客戶端wifi password:" + wifiPassword.getText().toString());
            Log.d(TAG, "匯康Server ip: " + serverIp.getText().toString() + ", 匯康Server port:" + serverPort.getText().toString());
            //連接wifi

        }
    }

    /******************** 廠商給的sample code ********************************************/

    public static ArrayList<String> getWiFiModuleIp() {
        ArrayList<String> ipList = new ArrayList<>();
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
        Log.d(TAG, "setWiFiModuleConfig: SSID:" + ssid + ",password:" + password);
        try {
            InetAddress IPAddress = InetAddress.getByName("192.168.1.10");
            servIpAddr = IPAddress;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        DatagramSocket clientSocket = null;
        byte[] rxData = new byte[512];
        DatagramPacket dp = new DatagramPacket(rxData, rxData.length);
        boolean setOK = false;
        ArrayList<byte[]> wmcpDataArray = new ArrayList<>();

        if (servIpAddr == null) {
            return "IP ERROR";
        }

        wmcpDataArray.add(wmcpDataSetReq(0x11, "0"));	//OPMODE 0:STA 1:AP
        wmcpDataArray.add(wmcpDataSetReq(0x14, ssid));  //WSTA
        if (password == "") {
            Log.d(TAG, "setWiFiModuleConfig: " + password);
            wmcpDataArray.add(wmcpDataSetReq(0x15, "NONE"));  //WSTASEC
        }
        else {
            Log.d(TAG, "setWiFiModuleConfig: " + password);
            wmcpDataArray.add(wmcpDataSetReq(0x15, "WPA2"));  //WSTASEC
            String cypher = "AES";
            String psk = cypher + "," + password;
            Log.d(TAG, "setWiFiModuleConfig: " + psk);
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

    private byte[] wmcpDataGetReq(byte cmdcode) {
        byte[] wmcpDataBuffer = new byte[3];
        wmcpDataBuffer[0] = 0x01;
        wmcpDataBuffer[1] = cmdcode;
        wmcpDataBuffer[2] = 0x02;
        return wmcpDataBuffer;
    }

    public void getWiFiModuleConfig() {
        DatagramSocket clientSocket = null;
        byte[] rxData = new byte[512];
        DatagramPacket dp = new DatagramPacket(rxData, rxData.length);
        ArrayList<byte[]> wmcpDataArray = new ArrayList<>();
        if (servIpAddr == null)
            return;

        wmcpDataArray.add(wmcpDataGetReq((byte) 0x11));	//OPMODE
        wmcpDataArray.add(wmcpDataGetReq((byte) 0x23)); //NSOCK
        wmcpDataArray.add(wmcpDataGetReq((byte) 0x36)); //DEVNAME
        try {
            clientSocket = new DatagramSocket();
            for (int i = 0; i < wmcpDataArray.size(); i++) {
                byte[] wmcpDataPayload = wmcpDataArray.get(i);
                DatagramPacket packet = new DatagramPacket(wmcpDataPayload, wmcpDataPayload.length, servIpAddr, 60002);
                clientSocket.send(packet);
                int retry = 0;
                while (retry < 3) {
                    try {
                        clientSocket.setSoTimeout(2000);
                        clientSocket.receive(dp);

                        byte[] data = dp.getData();
                        if (data[0] != 0x2)
                            continue;
                        byte cmdcode = data[1];
                        byte[] resp = Arrays.copyOfRange(data, 3, dp.getLength());
                        String strResp = new String(resp);
                        switch (cmdcode) {
                            case 0x11:
                                opmode = Integer.parseInt(strResp);
                                break;
                            case 0x23:
                                netProto = strResp;
                                break;
                            case 0x36:
                                devName = strResp;
                                break;
                            default:
                                break;
                        }
                        break;
                    } catch (SocketTimeoutException e) {
                        retry += 1;
                        clientSocket.send(packet);
                    }
                }
                if (retry == 3) {
                    Log.d("CJNWiFi","We can't get opmode/nsock reply from WiFi module");
                    return;
                }
            }

            wmcpDataArray.clear();
            if (opmode == 0) { //STA
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x14));
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x15));
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x16));
            }
            else { //AP
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x12));
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x13));
            }
            if (netProto.equals("MQTT")) {
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x51));
            }
            else {
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x24));
                wmcpDataArray.add(wmcpDataGetReq((byte) 0x25));
            }

            for (int i = 0; i < wmcpDataArray.size(); i++) {
                byte[] wmcpDataPayload = wmcpDataArray.get(i);
                DatagramPacket packet = new DatagramPacket(wmcpDataPayload, wmcpDataPayload.length, servIpAddr, 60002);
                clientSocket.send(packet);
                int retry = 0;
                while (retry < 3) {
                    try {
                        clientSocket.setSoTimeout(2000);
                        clientSocket.receive(dp);

                        byte[] data = dp.getData();
                        if (data[0] != 0x2)
                            continue;
                        byte cmdcode = data[1];
                        byte[] resp = Arrays.copyOfRange(data, 3, dp.getLength());
                        String strResp = new String(resp);
                        String[] params;
                        switch (cmdcode) {
                            case 0x12:
                                params = strResp.split(",");
                                ssid = params[0];
                                apChannel = params[1];
                                break;
                            case 0x13:
                                password = strResp;
                                break;
                            case 0x14:
                                ssid = strResp;
                                break;
                            case 0x15:
                                staSec = strResp;
                                break;
                            case 0x16:
                                if (staSec.equals("NONE"))
                                    break;
                                params = strResp.split(",");
                                password = params[1];
                                break;
                            case 0x24:
                                params = strResp.split(",");
                                remoteServ = params[0];
                                port = params[1];
                                break;
                            case 0x25:
                                if (remoteServ.equals("0.0.0.0"))
                                    port = strResp;
                                break;
                            case 0x51:
                                //                               params = strResp.split(",");
                                break;
                            default:
                                break;
                        }
                        break;
                    } catch (SocketTimeoutException e) {
                        retry += 1;
                        clientSocket.send(packet);
                    }
                }
                if (retry == 3) {
                    Log.d("CJNWiFi","We can't get reply from WiFi module");
                    return;
                }
            }
        } catch (SocketException e) {
            if(clientSocket != null){
                clientSocket.close();
            }
        }catch (IOException e) {
            if(clientSocket != null){
                clientSocket.close();
            }
        }
        if(clientSocket != null){
            clientSocket.close();
        }
    }
}