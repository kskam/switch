package com.praxis.switchapp;

import com.praxis.switchapp.rfduino.Bluetooth;

import android.app.Application;

public class App extends Application {
	/**
	 * Bluetooth singleton object
	 */
	public Bluetooth bluetooth = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		initBluetooth();
	}
	
	protected void initBluetooth() {
		bluetooth = new Bluetooth(this);
	}
	
	public Bluetooth getBluetooth() {
		return bluetooth;
	}
	
}
