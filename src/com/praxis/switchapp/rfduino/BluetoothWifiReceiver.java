package com.praxis.switchapp.rfduino;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.util.Log;

public class BluetoothWifiReceiver extends BroadcastReceiver {
	private static String TAG = "WifiStateChangedReceiver";
	private static String WIFI_SSID = "";

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
		WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
		if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			if (wifiInfo.getSSID().contains(WIFI_SSID)) {
				Intent serviceIntent = new Intent(context, BluetoothService.class);
				context.startService(serviceIntent);
			}
		} else {
			Log.d(TAG, "Not connected to wifi");
		}
	}
	
}