package com.integreight.onesheeld.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.integreight.firmatabluetooth.ArduinoFirmata;
import com.integreight.firmatabluetooth.ArduinoFirmataEventHandler;
import com.integreight.firmatabluetooth.BluetoothService;
import com.integreight.onesheeld.MainActivity;
import com.integreight.onesheeld.OneSheeldApplication;
import com.integreight.onesheeld.R;
import com.integreight.onesheeld.plugin.PluginBundleManager;
import com.integreight.onesheeld.utils.Log;
import com.integreight.onesheeld.utils.WakeLocker;

public class OneSheeldService extends Service {

	// private static final String TAG = "OneSheeldService";
	// private static final boolean D = true;
	public static boolean isBound = false;
	SharedPreferences sharedPrefs;
	// private final IBinder mBinder = new OneSheeldBinder();
	private BluetoothAdapter mBluetoothAdapter = null;
	private String deviceAddress;
	private ArduinoFirmataEventHandler arduinoEventHandler = new ArduinoFirmataEventHandler() {

		@Override
		public void onError(String errorMessage) {
			// TODO Auto-generated method stub
			// sheeldConnectedMessageToActivity(COMMUNICAITON_ERROR);
			stopSelf();
		}

		@Override
		public void onConnect() {
			// TODO Auto-generated method stub
			// sheeldConnectedMessageToActivity(SHEELD_BLUETOOTH_CONNECTED);
			// sharedPrefs.edit().putString(DEVICE_ADDRESS_KEY, deviceAddress)
			// .commit();

			showNotification();

		}

		@Override
		public void onClose(boolean closedManually) {
			// TODO Auto-generated method stub
			// sheeldConnectedMessageToActivity(SHEELD_CLOSE_CONNECTION);
			// if (!closedManually)
			// sharedPrefs.edit().remove(DEVICE_ADDRESS_KEY).commit();
			stopSelf();

		}
	};

	// public static final String SHEELD_BLUETOOTH_CONNECTED =
	// "com.integreight.SHEELD_BLUETOOTH_CONNECTED";
	// public static final String COMMUNICAITON_ERROR =
	// "com.integreight.COMMUNICAITON_ERROR";
	// public static final String SHEELD_CLOSE_CONNECTION =
	// "com.integreight.SHEELD_CLOES_CONNECTION";
	public static final String PLUGIN_MESSAGE = "com.integreight.PLUGIN_MESSAGE";

	// public static final String DEVICE_ADDRESS_KEY =
	// "com.integreight.DEVICE_ADDRESS_KEY";
	OneSheeldApplication app;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		app = (OneSheeldApplication) getApplication();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver,
				new IntentFilter(OneSheeldService.PLUGIN_MESSAGE));
		sharedPrefs = this.getSharedPreferences("com.integreight.onesheeld",
				Context.MODE_PRIVATE);
		isBound = false;
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if (intent.getExtras() != null) {
			deviceAddress = intent.getExtras().getString(
					BluetoothService.EXTRA_DEVICE_ADDRESS);
			BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(deviceAddress);
			// Attempt to connect to the device
			app.getAppFirmata().addEventHandler(arduinoEventHandler);
			app.getAppFirmata().connect(device);
		}
		WakeLocker.acquire(this);
		return START_REDELIVER_INTENT;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		isBound = true;
		// return mBinder;
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		isBound = false;
		// stopSelf();
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		while (!app.getAppFirmata().close())
			;
		hideNotifcation();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(
				mMessageReceiver);
		isBound = false;
		WakeLocker.release();
		super.onDestroy();
	}

	private void showNotification() {
		NotificationCompat.Builder build = new NotificationCompat.Builder(this);
		build.setSmallIcon(R.drawable.white_ee_icon);
		build.setContentText("The service is running!");
		build.setContentTitle("1Sheeld is connected");
		build.setTicker("1Sheeld is connected!");
		// build.setContentInfo("");
		build.setWhen(System.currentTimeMillis());
		// PendingIntent pendingIntent = PendingIntent.getService(this, 0, new
		// Intent(this, OneSheeldService.class), 0);

		// build.addAction(R.drawable.action_cancel,
		// "Close Service",pendingIntent );
		// build.addAction(R.drawable.action_cancel,
		// "Close Service",pendingIntent );
		// build.addAction(R.drawable.action_cancel,
		// "Close Service",pendingIntent );
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent intent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		build.setContentIntent(intent);
		Notification notification = build.build();
		startForeground(1, notification);
	}

	private void hideNotifcation() {
		stopForeground(true);
	}

	// private void sheeldConnectedMessageToActivity(String event) {
	// Intent intent = new Intent(event);
	// LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	// }

	public ArduinoFirmata getFirmata() {
		return app.getAppFirmata();
	}

	// public class OneSheeldBinder extends Binder {
	// public OneSheeldService getService() {
	// // Return this instance of LocalService so clients can call public
	// // methods
	// return OneSheeldService.this;
	// }
	//
	// }

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			Log.test("plugin", "Receive");

			Log.sysOut(app.getAppFirmata()
					+ "    "
					+ intent.getIntExtra(
							PluginBundleManager.BUNDLE_EXTRA_PIN_NUMBER, -1)
					+ "    "
					+ intent.getBooleanExtra(
							PluginBundleManager.BUNDLE_EXTRA_OUTPUT, true)
					+ "    " + app.getAppFirmata().isOpen());
			String action = intent.getAction();
			if (action.equals(OneSheeldService.PLUGIN_MESSAGE)) {
				if (app.getAppFirmata() != null && app.getAppFirmata().isOpen()) {
					int pin = intent.getIntExtra("pin", -1);
					boolean output = intent.getBooleanExtra("output", false);
					app.getAppFirmata().pinMode(pin, ArduinoFirmata.OUTPUT);
					app.getAppFirmata().digitalWrite(pin, output);
					Toast.makeText(
							context,
							"Pin " + pin + " set to "
									+ (output ? "High" : "Low"),
							Toast.LENGTH_LONG).show();
				}

			}

		}
	};

}