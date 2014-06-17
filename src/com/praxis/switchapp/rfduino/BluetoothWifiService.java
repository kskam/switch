package com.praxis.switchapp.rfduino;

import android.util.Log;

public class BluetoothWifiService extends BluetoothService {
	private static String TAG = "BluetoothWifiService";

	public void doSwitch(boolean sw) {
		Log.d(TAG, "Not setting switch");
	}
}