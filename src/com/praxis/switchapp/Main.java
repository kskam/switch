package com.praxis.switchapp;

import java.util.Locale;

import com.praxis.switchapp.R;
import com.praxis.switchapp.rfduino.Bluetooth;

import android.os.Bundle;
import android.os.Handler;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

public class Main extends FragmentActivity {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	/**
	 * Used to call functions asynchronously.
	 */
	private Handler handler;

	/**
	 * Timeout for searching for an RFduino.
	 */
	private static final long SCAN_PERIOD = 20000;

	/**
	 * Used for logging.
	 */
	private static final String TAG = "SwitchActivity";
	
	/**
	 * Bluetooth object
	 */
	private Bluetooth bluetooth;
	
	/**
	 * NFC objects to capture NFC events.
	 */
	private NfcAdapter nfcAdapter;

	/**
	 * NFC Tag
	 */
	private Tag tag;

	/**
	 * Pending Intent to call when hitting an NFC tag
	 */
	private PendingIntent nfcPendingIntent;
	
	/**
	 * NFC triggers
	 */
	private IntentFilter[] nfcFilters;
	
	public Main() {
		super();
		handler = new Handler();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_switch);

		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		App switchApp = ((App) this.getApplication());
		bluetooth = switchApp.getBluetooth();
		bluetooth.setParentActivity(this);
		setSwitchState();
		setConnectButton();
		
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		
		// NFC
		nfcAdapter = NfcAdapter.getDefaultAdapter(switchApp);
		Intent nfcIntent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		nfcIntent.putExtra("connected", bluetooth.isConnected());
		nfcPendingIntent = PendingIntent.getActivity(this, 0, nfcIntent, 0);
		
		IntentFilter nfcDiscovery = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
		nfcFilters = new IntentFilter[] { nfcDiscovery };
	}
	
	public void setSwitchState() {
		// Sets the state of the switch based on bluetooth connection
		// and light
		Switch sw = (Switch) findViewById(R.id.switch1);
		if (sw != null) {
			if (bluetooth.isConnected()) {
				sw.setChecked(bluetooth.getSwitch());
			} else {
				sw.setChecked(false);
			}
		}
	}
	
	public void setConnectButton() {
		// Sets the connection button based on the bluetooth connection
		Button connectButton = (Button) findViewById(R.id.connectButton);
		Log.d(TAG, "Setting Connect Button");
		
		if (connectButton == null) {
			Log.d(TAG, "Connect Button is null");
			return;
		}
		if (bluetooth.isConnected()) {
			connectButton.setText(R.string.disconnect);
		} else {
			connectButton.setText(R.string.connect);
		}
	}
	
	public void toggleConnection(View v) {
		// Stop or start the bluetooth connection
		if (bluetooth.isConnected()) {
			bluetooth.stop();
		} else {
			bluetooth.startConnection();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (!bluetooth.isConnected()) {
						Log.d(TAG, "Scanning timeout, stopping");
						bluetooth.stopScan();
					}
				}
			}, SCAN_PERIOD);
		}
	}
	
	public boolean isConnected() {
		return bluetooth.isConnected();
	}
	
	public void onSwitchClicked(View v) {
		// Sets the switch based on user input
		boolean on = ((Switch) v).isChecked();
		
		Switch manual = (Switch) findViewById(R.id.manual1);
		boolean manualOn = manual.isChecked();
		if (!manualOn) {
			bluetooth.setSwitch(on);
		}
	}
	
	public void onManualClicked(View v) {
		// Sets the switch to use a manual override
		boolean manualOn = ((Switch) v).isChecked();
		
		Switch sw = (Switch) findViewById(R.id.switch1);
		boolean on = sw.isChecked();
		if (!manualOn) {
			manualOn = on;
		}
		bluetooth.setSwitch(manualOn);
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.light, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// resume NFC writing capabilities
		if (nfcAdapter != null) {
			if (nfcAdapter.isEnabled()) {
				Log.d(TAG, "Enabled NFC foreground dispatch");
				nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcFilters, null);
			}
		}
		setSwitchState();
		setConnectButton();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (nfcAdapter != null) {
			nfcAdapter.disableForegroundDispatch(this);
		}
		if (bluetooth != null) {
			bluetooth.stop();
		}
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, intent.toString());
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			boolean connected = intent.getBooleanExtra("connected", false);
			String data = String.valueOf(connected);
			
			Log.d(TAG, "Tag discovered");
			Nfc.writeTag(this, tag, data);
		}
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = null;
			switch(position) {
			case 0:	fragment = new LightFragment();
					break;
			case 1: fragment = new SettingsFragment();
					break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}

	/**
	 * The settings fragment that can manually connect or disconnect the bluetooth connection
	 */
	public static class SettingsFragment extends Fragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
			return rootView;
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			Main switchActivity = (Main) getActivity();
			Log.d(TAG, "bluetooth connected? " + String.valueOf(switchActivity.isConnected()));
		}
	}

	/**
	 * Light Fragment which contains the switch and manual override for the light.
	 */
	public static class LightFragment extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_light,
					container, false);
			return rootView;
		}
	}	
}
