package com.praxis.switchapp.rfduino;

import android.util.Log;

public class BluetoothNfcService extends BluetoothService {
	private static String TAG = "BluetoothNfcService";

	public void doSwitch(boolean sw) {
		Log.d(TAG, "Setting switch to " + String.valueOf(!sw));
		bluetooth.setSwitch(sw);
	}
}