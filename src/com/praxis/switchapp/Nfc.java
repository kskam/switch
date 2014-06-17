package com.praxis.switchapp;
//Copyright (C) 2014 Kai Kam
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU Lesser General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Lesser General Public License for more details.
//
//You should have received a copy of the GNU Lesser General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.io.IOException;
import java.nio.charset.Charset;

import com.praxis.switchapp.rfduino.BluetoothNfcService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class Nfc extends Activity {
    private static String TAG = "NfcActivity"; 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// NFC
		Intent intent = getIntent();

		if(intent.getType() != null && intent.getType().equals("application/" + getPackageName())) {
			// Read the first record which contains the relay info
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefRecord connectRecord = ((NdefMessage)rawMsgs[0]).getRecords()[0];
			String nfcData = new String(connectRecord.getPayload());
			
			Toast.makeText(this,  "Toggling Switch",  Toast.LENGTH_SHORT).show();
			Log.d(TAG, "NFC Data: " + nfcData);

			Intent serviceIntent = new Intent(this, BluetoothNfcService.class);
			this.startService(serviceIntent);
			
			finish();
		}
	} 

	public static boolean writeTag(Context context, Tag tag, String data) {
		Log.d(TAG, "Writing tag");
		// Record to launch Play Store if app is not installed
		NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());
     
		// Record with actual data we care about
		NdefRecord connectRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                                             new String("application/" + context.getPackageName()).getBytes(Charset.forName("US-ASCII")),
                                             null, data.getBytes());
     
		// Complete NDEF message with both records
		NdefMessage message = new NdefMessage(new NdefRecord[] {connectRecord, appRecord});

		try {
			// If the tag is already formatted, just write the message to it
			Ndef ndef = Ndef.get(tag);
			if(ndef != null) {
				ndef.connect();

				// Make sure the tag is writable
				if(!ndef.isWritable()) {
					Log.d(TAG, "NFC not writable!");
					return false;
				}

				// Check if there's enough space on the tag for the message
				int size = message.toByteArray().length;
				if(ndef.getMaxSize() < size) {
					Log.d(TAG, "NFC not enough space!");
					return false;
				}

				try {
					// Write the data to the tag
					Log.d(TAG, "Writing to tag: " + message);
					ndef.writeNdefMessage(message);
					Log.d(TAG, "Done writing");
					return true;
				} catch (TagLostException tle) {
					Log.d(TAG, "NFC Tag lost!");
					return false;
				} catch (IOException ioe) {
					Log.d(TAG, "NFC IO error!");
					return false;
				} catch (FormatException fe) {
					Log.d(TAG, "NFC format error!");
					return false;
				}
         // If the tag is not formatted, format it with the message
			} else {
				Log.d(TAG, "Trying to format tag");
				NdefFormatable format = NdefFormatable.get(tag);
				if(format != null) {
					try {
						format.connect();
						format.format(message);
						
						Log.d(TAG, "NFC written");
						return true;
					} catch (TagLostException tle) {
						Log.d(TAG, "NFC Tag lost!");
						return false;
					} catch (IOException ioe) {
                	 	Log.d(TAG, "NFC format error!");
                     	return false;
                 	} catch (FormatException fe) {
                	 	Log.d(TAG, "NFC format error!");
                     	return false;
                 	}
				} else {
					Log.d(TAG, "No NFC");
                 	return false;
             	}
			}
		} catch(Exception e) {
			Log.d(TAG, "NFC unknown Error");
     	}

		return false;
	}

}