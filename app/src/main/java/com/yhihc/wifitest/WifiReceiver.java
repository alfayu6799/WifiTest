package com.yhihc.wifitest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class WifiReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiReceiver";

    WifiManager wifiManager;
    StringBuilder sb;
    ListView wifiDeviceList;

    public WifiReceiver(WifiManager wifiManager, ListView wifiDeviceList) {
        this.wifiManager = wifiManager;
        this.wifiDeviceList = wifiDeviceList;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            sb = new StringBuilder();
            List<ScanResult> wifiList = wifiManager.getScanResults();
            ArrayList<String> deviceList = new ArrayList<>();
            for (ScanResult scanResult : wifiList) {
                sb.append("\n").append(scanResult.SSID).append(" - ").append(scanResult.capabilities);
                deviceList.add(scanResult.SSID + " - " + scanResult.capabilities);
//                deviceList.add(scanResult.SSID);
            }
            Toast.makeText(context, sb, Toast.LENGTH_SHORT).show();
            ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, deviceList.toArray());
            wifiDeviceList.setAdapter(arrayAdapter);
            wifiDeviceList.setOnItemClickListener(onItemClickListener);
        }
    }

    public AdapterView.OnItemClickListener onItemClickListener= new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(TAG, "onItemClick: " + wifiDeviceList.getItemAtPosition(i).toString());
            String SSIDName = wifiDeviceList.getItemAtPosition(i).toString();
        }
    };

}
