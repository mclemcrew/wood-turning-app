/* Created by Javier Montaner  (twitter: @tumaku_) during M-week (February 2014) of MakeSpaceMadrid
 * http://www.makespacemadrid.org
 * @ 2014 Javier Montaner
 * 
 * Licensed under the MIT Open Source License 
 * http://opensource.org/licenses/MIT
 * 
 * Many thanks to Yeelight (special mention to Daping Liu) and Double Encore (Dave Smith)
 * for their support and shared knowlegde
 * 
 * Based on the API released by Yeelight:
 * http://www.yeelight.com/en_US/info/download
 * 
 * Based on the code created by Dave Smith (Double Encore):
 * https://github.com/devunwired/accessory-samples/tree/master/BluetoothGatt
 * http://www.doubleencore.com/2013/12/bluetooth-smart-for-android/
 * 
 * 
 * Scan Bluetooth Low Energy devices and their services and characteristics.
 * If the Yeelight Service is found, an activity can be launched to control colour and intensity of Yeelight Blue bulb
 * 
 * Tested on a Nexus 7 (2013)
 * 
 */
package com.tumaku.msmble;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class HM10Activity extends Activity {
	
	/* SensorTag BLE procedure:
	 * The sensors in SensorTag require a special mechanism to use them.
	 * In order to save battery power, every sensor needs to be enabled (i.e. activated) prior 
	 * to reading it or subscribing to notifications on value changes.
	 * This is done by writing a byte into a characteristic present in every service (*_CONF)
	 * Once the sensor is enabled, standard BLE mechanisms apply:
	 * - you can read its value
	 * - you can (un)subscribe to notifications  writing the *-DATA characteristic descriptor 
	 * 
	 * A special case is the Key service that controls the two buttons on sensor tag. This service 
	 * does not require to be enabled. To interact with this service, only the notification mechanism 
	 * applies (read value is not supported - to be confirmed)
	 */


	private final static int WSTATE_CONNECT=0;
	private final static int WSTATE_SEARCH_SERVICES=1;
	private final static int WSTATE_NOTIFY_KEY=2;
	private final static int WSTATE_READ_KEY=3;
	private final static int WSTATE_DUMMY=4;
	private final static int WSTATE_WRITE_KEY=5;


	private TextView mTextLongReceived;
	private ProgressBar progressBar;
	private SeekBar seekBar;
	private TextView seekText;
	public TextView viewText;
	public Button calibrationButton1;
	public Button calibrationButton2;
	public Button onButton;
	public Button stopButton;
	public boolean calibrationBoolean1 = false;
	public boolean calibrationBoolean2 = false;
	public boolean startBoolean = false;
	public boolean stopBoolean = true;

	public TextView calibrationText1;
	public TextView calibrationText2;
	public TextView startText;
	public TextView stopText;
	public String sendString;

	private Context mContext;
	private HM10BroadcastReceiver mBroadcastReceiver;
	private String mDeviceAddress;
	private int mState=0;
	private float progress_value = 0;

	private TumakuBLE  mTumakuBLE=null;

	public void seekBarFunction() {
		Float seekFloat = (float) seekBar.getProgress()/8;
		Float seekMax = (float) seekBar.getMax()/8;
		seekText.setText("Covered : " + seekFloat + "/" + seekMax);

		seekBar.setOnSeekBarChangeListener(
				new SeekBar.OnSeekBarChangeListener() {

					//public Float progress_value;
					Float progess_max = (float) seekBar.getMax()/8;
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						progress_value = (float) progress/8;
						seekText.setText("Covered : " + progress_value + " / " + progess_max);
						//Toast.makeText(getApplicationContext(),"SeekBar in progress",Toast.LENGTH_LONG).show();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						//Toast.makeText(getApplicationContext(),"SeekBar in StartTracking",Toast.LENGTH_LONG).show();
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						seekText.setText("Covered : " + progress_value + " / " + progess_max);
						//Toast.makeText(getApplicationContext(),"SeekBar in StopTracking",Toast.LENGTH_LONG).show();
					}

				});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.hm10);
		Toast.makeText(getApplicationContext(),"Testing Feature",Toast.LENGTH_LONG).show();
		mContext=this;
		mBroadcastReceiver= new HM10BroadcastReceiver();
		mDeviceAddress=getIntent().getStringExtra(TumakuBLE.EXTRA_ADDRESS);
		if (mDeviceAddress==null) {
			if (Constant.DEBUG) Log.i("JMG","No device address received to start SensorTag Activity");
			finish();
		}
		//Define font
		Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/CutiveMono-Regular.ttf");

		// Initialize Buttons
		//sendButton = (Button) findViewById(R.id.sendButton);
		calibrationButton1 = (Button) findViewById(R.id.calibrationButton1);
		calibrationButton2 = (Button) findViewById(R.id.calibrationButton2);
		onButton = (Button) findViewById(R.id.onButton);
		stopButton = (Button) findViewById(R.id.stopButton);

		//Initialize Text
		mTextLongReceived = (TextView) findViewById(R.id.longReceivedText);
		calibrationText1 = (TextView) findViewById(R.id.calibrationText1);
		calibrationText2 = (TextView) findViewById(R.id.calibrationText2);
		startText = (TextView) findViewById(R.id.startText);
		stopText = (TextView) findViewById(R.id.stopText);
		viewText = (TextView) findViewById(R.id.viewText);
		seekText = (TextView) findViewById(R.id.seekBarText);

		//Change Font on all TextViews
		viewText.setTypeface(custom_font);
		stopText.setTypeface(custom_font);
		startText.setTypeface(custom_font);
		calibrationText2.setTypeface(custom_font);
		calibrationText1.setTypeface(custom_font);
		mTextLongReceived.setTypeface(custom_font);
		seekText.setTypeface(custom_font);



		mTumakuBLE=((TumakuBLEApplication)getApplication()).getTumakuBLEInstance(this);
		mTumakuBLE.setDeviceAddress(mDeviceAddress);



		seekText = (TextView) findViewById(R.id.seekBarText);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		progressBar.setScaleY(8f);
		seekBarFunction();
	}

	@Override
	public void onResume(){
		super.onResume();
		IntentFilter filter = new IntentFilter(TumakuBLE.WRITE_SUCCESS);
		filter.addAction(TumakuBLE.READ_SUCCESS);
		filter.addAction(TumakuBLE.DEVICE_CONNECTED);
		filter.addAction(TumakuBLE.DEVICE_DISCONNECTED);
		filter.addAction(TumakuBLE.SERVICES_DISCOVERED);
		filter.addAction(TumakuBLE.NOTIFICATION);
		filter.addAction(TumakuBLE.WRITE_DESCRIPTOR_SUCCESS);
		this.registerReceiver(mBroadcastReceiver, filter);
		if (mTumakuBLE.isConnected()){
			mState=WSTATE_NOTIFY_KEY;
			nextState();
			updateInfoText("Resume connection to device");
		} else {
			mState=WSTATE_CONNECT;
			nextState();
			updateInfoText("Start connection to device");
		}

	}

	@Override
	public void onStop(){
		super.onStop();
		this.unregisterReceiver(this.mBroadcastReceiver);
	}

	public void changeProgress(Float num) {
		Integer number = Math.round(num);
		progressBar.setProgress(number);
	}

	public void sendButtonClicked(View view) {
		if (mState==WSTATE_DUMMY) {
			mState=WSTATE_WRITE_KEY;
			nextState();
		} else
			Toast.makeText(mContext, "Cannot send data in current statet. Do a reset first.", Toast.LENGTH_SHORT).show();
	}


	protected void nextState(){
		switch(mState) {
			case (WSTATE_CONNECT):
				if (Constant.DEBUG) Log.i("JMG","State Connected");
				mTumakuBLE.connect();
				break;
			case(WSTATE_SEARCH_SERVICES):
				if (Constant.DEBUG) Log.i("JMG","State Search Services");
				mTumakuBLE.discoverServices();
				break;
			case(WSTATE_READ_KEY):
				if (Constant.DEBUG) Log.i("JMG","State Read Key");
				mTumakuBLE.read(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA);
				break;
			case(WSTATE_NOTIFY_KEY):
				if (Constant.DEBUG) Log.i("JMG","State Notify Key");
				mTumakuBLE.enableNotifications(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA,true);
				break;
			case(WSTATE_WRITE_KEY):
				if (Constant.DEBUG) Log.i("JMG","State Write State " + sendString);
				byte tmpArray []= new byte[sendString.length()];
				for (int i=0; i<sendString.length();i++) tmpArray[i]=(byte)sendString.charAt(i);
				mTumakuBLE.write(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA, tmpArray);
				break;

			default:

		}

	}



	protected void updateInfoText(String text) {
		//mTextInfo.setText(text);
	}

	protected void updateNotificationText(String text) {
		//mTextNotification.setText(text);
	}

	protected void displayText(String text) {
		//mTextReceived.setText(text);
	}

	protected void updateLongText(String text) {
		//String tmp=mTextLongReceived.getText().toString();
		String tmp;
		tmp=text;
		Float floatingNum = Float.parseFloat(tmp);
		//progressBar.setProgressDrawable(view.get.drawable.yellowprogress);
		//progressBar.getProgressDrawable().setColorFilter(Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
		if(floatingNum<=progress_value) {
			progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
		}
		if((floatingNum<=(progress_value+0.25))&&(floatingNum>progress_value)) {
			progressBar.setProgressTintList(ColorStateList.valueOf(Color.YELLOW));
		}
		if(floatingNum>(progress_value+0.25)) {
			progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
		}
		changeProgress(floatingNum*100);

		int tmpLength=tmp.length();
		if (tmpLength>=400) {
			tmp=tmp.substring(tmpLength-400);
		}
		mTextLongReceived.setText(tmp+" in");
	}


	public void confirmationDialog1(View view) {


		AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
		builder1.setTitle("Calibration #1");
		builder1.setMessage("Please place the calibration card to the correct orientation and press 'Calibrate'.");
		builder1.setCancelable(true);

		builder1.setPositiveButton(
				"Calibrate",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						calibrationBoolean1 = true;
						if (mState==WSTATE_DUMMY) {
							mState=WSTATE_WRITE_KEY;
							sendString = "1";
							nextState();
							calibrationButton1.setBackgroundResource(R.drawable.lightgreenbutton);
						} else
							Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
					}
				});

		builder1.setNegativeButton(
				"Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		AlertDialog alert11 = builder1.create();
		alert11.show();
	}

	public void confirmationDialog2(View view) {

		AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
		builder1.setTitle("Calibration #2");
		builder1.setMessage("Please place the calibration block to the correct orientation and press 'Calibrate'.");
		builder1.setCancelable(true);

		builder1.setPositiveButton(
				"Calibrate",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						calibrationButton2.setBackgroundResource(R.drawable.lightgreenbutton);
						calibrationBoolean2 = true;
						if (mState==WSTATE_DUMMY) {
							mState=WSTATE_WRITE_KEY;
							sendString = "1";
							nextState();
						} else
							Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
					}
				});

		builder1.setNegativeButton(
				"Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		AlertDialog alert11 = builder1.create();
		alert11.show();
	}

	public void onDialog(View view) {
		if(calibrationBoolean1==true&&calibrationBoolean2==true&&startBoolean==false&&stopBoolean==true)
		{
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setTitle("Start Process");
			builder1.setMessage("Are you ready to start reading in the thickness?");
			builder1.setCancelable(true);

			builder1.setPositiveButton(
					"Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							startBoolean = true;
							stopBoolean = false;
							onButton.setBackgroundResource(R.drawable.lightgreenbutton);
							stopButton.setBackgroundResource(R.drawable.darkredbutton);
							if (mState==WSTATE_DUMMY) {
								mState=WSTATE_WRITE_KEY;
								sendString = "0";
								nextState();
							} else
								Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
						}
					});

			builder1.setNegativeButton(
					"No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert11 = builder1.create();
			alert11.show();
		}
	}

	public void stopDialog(View view) {
		if(stopBoolean==false) {
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setTitle("Stop Process");
			builder1.setMessage("Are you sure you want to stop the program?");
			builder1.setCancelable(true);

			builder1.setPositiveButton(
					"Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							stopButton.setBackgroundResource(R.drawable.lightredbutton);
							//calibrationButton1.setBackgroundResource(R.drawable.darkgreenbutton);
							//calibrationButton2.setBackgroundResource(R.drawable.darkgreenbutton);
							onButton.setBackgroundResource(R.drawable.darkgreenbutton);
							stopBoolean = true;
							startBoolean = false;
							if (mState==WSTATE_DUMMY) {
								mState=WSTATE_WRITE_KEY;
								sendString = "9";
								nextState();
							} else
								Toast.makeText(mContext, "Cannot send data in current state. Do a reset first.", Toast.LENGTH_SHORT).show();
						}
					});

			builder1.setNegativeButton(
					"No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert11 = builder1.create();
			alert11.show();
		}
	}



	private class HM10BroadcastReceiver extends BroadcastReceiver {


		public String bytesToString(byte[] bytes){
			StringBuilder stringBuilder = new StringBuilder(
					bytes.length);
			for (byte byteChar : bytes)
				stringBuilder.append(String.format("%02X ", byteChar));
			return stringBuilder.toString();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TumakuBLE.DEVICE_CONNECTED)) {
				if (Constant.DEBUG) Log.i("JMG","DEVICE_CONNECTED message received");

				updateInfoText("Received connection event");
				mState=WSTATE_SEARCH_SERVICES;
				nextState();
				return;
			}
			if (intent.getAction().equals(TumakuBLE.DEVICE_DISCONNECTED)) {
				if (Constant.DEBUG) Log.i("JMG","DEVICE_DISCONNECTED message received");
				//This is an unexpected device disconnect situation generated by Android BLE stack
				//Usually happens on the service discovery step :-(
				//Try to reconnect
				String fullReset=intent.getStringExtra(TumakuBLE.EXTRA_FULL_RESET);
				if (fullReset!=null){
					if (Constant.DEBUG) Log.i("JMG","DEVICE_DISCONNECTED message received with full reset flag");
					Toast.makeText(mContext, "Unrecoverable BT error received. Launching full reset", Toast.LENGTH_SHORT).show();
					mState=WSTATE_CONNECT;
					mTumakuBLE.resetTumakuBLE();
					mTumakuBLE.setDeviceAddress(mDeviceAddress);
					mTumakuBLE.setup();
					nextState();
					return;
				} else {
					if (mState!=WSTATE_CONNECT){
						Toast.makeText(mContext, "Device disconnected unexpectedly. Reconnecting.", Toast.LENGTH_SHORT).show();
						mState=WSTATE_CONNECT;
						mTumakuBLE.resetTumakuBLE();
						mTumakuBLE.setDeviceAddress(mDeviceAddress);
						nextState();
						return;
					}
				}
			}
			if (intent.getAction().equals(TumakuBLE.SERVICES_DISCOVERED)) {
				if (Constant.DEBUG) Log.i("JMG","SERVICES_DISCOVERED message received");

				updateInfoText("Received services discovered event");
				mState=WSTATE_NOTIFY_KEY;
				nextState();
				return;
			}

			if (intent.getAction().equals(TumakuBLE.READ_SUCCESS)) {
				if (Constant.DEBUG) Log.i("JMG","READ_SUCCESS message received");
				String readValue= intent.getStringExtra(TumakuBLE.EXTRA_VALUE);
				byte [] readByteArrayValue= intent.getByteArrayExtra(TumakuBLE.EXTRA_VALUE_BYTE_ARRAY);

				if (readValue==null) updateInfoText("Received Read Success Event but no value in Intent"  );
				else {
					updateInfoText("Received Read Success Event: " + readValue);
				}
				if (readValue==null) readValue="null";

				if (mState==WSTATE_READ_KEY) {
					if (readByteArrayValue!=null) displayText(readValue);
					mState=WSTATE_DUMMY;
					nextState();
					return;
				}
				return;
			}

			if (intent.getAction().equals(TumakuBLE.WRITE_SUCCESS)) {
				if (Constant.DEBUG) Log.i("JMG","WRITE_SUCCESS message received");
				updateInfoText("Received Write Success Event");
				if (mState==WSTATE_WRITE_KEY) {
					mState=WSTATE_DUMMY;
					nextState();
					return;
				}
				return;
			}

			if (intent.getAction().equals(TumakuBLE.NOTIFICATION)) {
				String notificationValue= intent.getStringExtra(TumakuBLE.EXTRA_VALUE);
				String characteristicUUID= intent.getStringExtra(TumakuBLE.EXTRA_CHARACTERISTIC);
				byte [] notificationValueByteArray =  intent.getByteArrayExtra(TumakuBLE.EXTRA_VALUE_BYTE_ARRAY);
				if (notificationValue==null) notificationValue="NULL";
				if (characteristicUUID==null) characteristicUUID="MISSING";
				if (Constant.DEBUG) {
					Log.i("JMG","NOTIFICATION message received");
					Log.i("JMG", "Characteristic: "+ characteristicUUID);
					Log.i("JMG","Value: " + notificationValue);
				}
				updateNotificationText("Received Notification Event: Value: " + notificationValue +
						" -  Characteristic UUID: " + characteristicUUID);
				if (!notificationValue.equalsIgnoreCase("null")) {
					if (characteristicUUID.equalsIgnoreCase(TumakuBLE.SENSORTAG_KEY_DATA)) {
						if (Constant.DEBUG) Log.i("JMG","NOTIFICATION of Key Service");
						if (notificationValueByteArray==null) {
							if (Constant.DEBUG) Log.i("JMG","No notificationValueByteArray received. Discard notification");
							return;
						}
						String tmpString="";
						for (int i=0; i<notificationValueByteArray.length; i++) tmpString+=(char)notificationValueByteArray[i];
						displayText(tmpString);
						updateLongText(tmpString);
					}
				}
				return;
			}

			if (intent.getAction().equals(TumakuBLE.WRITE_DESCRIPTOR_SUCCESS)) {
				if (Constant.DEBUG) Log.i("JMG","WRITE_DESCRIPTOR_SUCCESS message received");
				updateInfoText("Received Write Descriptor Success Event");
				if (mState==WSTATE_NOTIFY_KEY) {
					mState=WSTATE_READ_KEY;
					nextState();
				}
				return;
			}


		}

	}
}
