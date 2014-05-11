package com.praxis.switchapp.rfduino;

import com.praxis.switchapp.App;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class BluetoothService extends Service {
	private Handler handler;
	private Bluetooth bluetooth;
	private boolean stopped = false;
	private static String TAG = "BLEService";
	private static final long SCAN_PERIOD = 20000;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		handler = new Handler(); 
		
		bluetooth = ((App) getApplication()).getBluetooth();
		if (bluetooth != null && bluetooth.isEnabled()) {
			boolean sw = intent.getBooleanExtra("switch", false);
			if (bluetooth.isConnected()) {
				bluetooth.setSwitch(!sw);
			} else {
				bluetooth.startConnection();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (!bluetooth.isConnected()) {
							Log.d(TAG, "Scanning timeout, stopping");
							bluetooth.stopScan();
							stopped = true;
						}
					}
				}, SCAN_PERIOD);
		
				while (true) {
					if (stopped || bluetooth.isConnected()) break;
				}
				Log.d(TAG, "Done scanning");
			}
		}
		return Service.START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}