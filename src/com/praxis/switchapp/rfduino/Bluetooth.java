package com.praxis.switchapp.rfduino;

import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

public class Bluetooth {
	/**
	 * Bluetooth class to manage the connection
	 */
	
	
	/**
	 * The Bluetooth adapter.
	 */
	private BluetoothAdapter bluetoothAdapter;

	/**
	 * The GATT connection.
	 */
	private BluetoothGatt bluetoothGatt;

	/**
	 * The characteristic where to send data.
	 */
	private BluetoothGattCharacteristic sendCharacteristic;

	/**
	 * Service where to send data.
	 * Discovered empirically,
	 * it would be possible to find it programatically by parsing the GATT messages.
	 */
	private final String serviceUUID = "00002220-0000-1000-8000-00805f9b34fb";

	/**
	 * Characteristic where to send data.
	 * Discovered empirically,
	 * it would be possible to find it programatically by parsing the GATT messages.
	 */
	private final String sendCharacteristicUUID = "00002222-0000-1000-8000-00805f9b34fb";

	/**
	 * Tells if we are connected or not.
	 */
	private boolean connected;
	
	/**
	 * Switch state
	 */
	private boolean state;

	/**
	 * Used for logging.
	 */
	private static final String TAG = "BluetoothLE";
	
	private Activity parentActivity;
	
	public Bluetooth(Context context) {
		super();
		connected = false;
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
		
	public void setParentActivity(Activity parentActivity) {
		this.parentActivity = parentActivity;  
	}
	
	public void startScan() {
		Log.d(TAG, "Starting scan");
		bluetoothAdapter.startLeScan(handleScan);
	}
	
	public void stopScan() {
		bluetoothAdapter.stopLeScan(handleScan);
	}
	
	public void startConnection() {
		startScan();
	}
	
	private void sendData(byte[] data) {
		if (sendCharacteristic != null) {
			sendCharacteristic.setValue(data);
			sendCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			bluetoothGatt.writeCharacteristic(sendCharacteristic);
		}
	}

	public void setSwitch(boolean on) {
		byte[] data = new byte[1];
		state = false;
		if (on) {
			state = true;
		}
		data[0] = (byte) ((state) ? 1 : 0);
		sendData(data);
	}
	
	public boolean getSwitch() {
		return state;
	}
	
	LeScanCallback handleScan = new LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			Log.d(TAG, "Found BTLE device: " + device.getName());
			if (device.getName().equalsIgnoreCase("rfduino1")) {
				stopScan();
				
				Log.d(TAG, "Found RFDuino 'RFduino' trying to connect to " + device.getAddress());
				bluetoothGatt = device.connectGatt(parentActivity, true, new BluetoothGattCallback() {
					@Override
					public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
						if (newState == BluetoothProfile.STATE_CONNECTED) {
							Log.d(TAG, "Connected to RFduino, attempting to start service discovery: " +
									    bluetoothGatt.discoverServices());
						} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
							connected = false;
							state = false;
							stop();
							Log.d(TAG, "Disconnected from RFduino");
						}
					}
					
					@Override
					public void onServicesDiscovered(BluetoothGatt gatt, int status) {
						Log.d(TAG, "Services discovered");
						if (status == BluetoothGatt.GATT_SUCCESS) {
							BluetoothGattService serv = bluetoothGatt.getService(UUID.fromString(serviceUUID));
							sendCharacteristic = serv.getCharacteristic(UUID.fromString(sendCharacteristicUUID));
							connected = true;
							Log.d(TAG, "Services connected");
							setSwitch(true);
						}
					}
				});
			}
		}
	};

	/**
	 * Getter method for initial scan
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Getter method if adapter is enabled
	 * @return
	 */
	public boolean isEnabled() {
		return bluetoothAdapter.isEnabled();
	}
	
	public void stop() {
		if (bluetoothAdapter != null) bluetoothAdapter.stopLeScan(handleScan);
		if (bluetoothGatt != null) bluetoothGatt.disconnect();
	}
}
