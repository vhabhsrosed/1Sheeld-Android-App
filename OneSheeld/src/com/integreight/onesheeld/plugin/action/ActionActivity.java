/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.integreight.onesheeld.plugin.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.integreight.onesheeld.OneSheeldApplication;
import com.integreight.onesheeld.R;
import com.integreight.onesheeld.enums.ArduinoPin;
import com.integreight.onesheeld.plugin.AbstractPluginActivity;
import com.integreight.onesheeld.plugin.BundleScrubber;
import com.integreight.onesheeld.utils.customviews.PluginConnectingPinsView;

/**
 * This is the "Edit" activity for a Locale Plug-in.
 * <p>
 * This Activity can be started in one of two states:
 * <ul>
 * <li>New plug-in instance: The Activity's Intent will not contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE}.</li>
 * <li>Old plug-in instance: The Activity's Intent will contain
 * {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} from a previously saved
 * plug-in instance that the user is editing.</li>
 * </ul>
 * 
 * @see com.twofortyfouram.locale.Intent#ACTION_EDIT_SETTING
 * @see com.twofortyfouram.locale.Intent#EXTRA_BUNDLE
 */
public final class ActionActivity extends AbstractPluginActivity {
	// private String[] pins = { "0", "1", "2", "3", "4", "5", "6", "7", "8",
	// "9",
	// "10", "11", "12", "13", "A0", "A1", "A2", "A3", "A4", "A5" };
	private String[] output = { "High", "Low" };

	// Spinner pinsSpinner;
	Spinner outputSpinner;
	int selectedPin = -1;

	@Override
	protected void onResume() {
		((PluginConnectingPinsView) getSupportFragmentManager()
				.findFragmentByTag("Pins")).reset(
				new PluginConnectingPinsView.OnPinSelectionListener() {

					@Override
					public void onUnSelect(ArduinoPin pin) {
						selectedPin = -1;
					}

					@Override
					public void onSelect(ArduinoPin pin) {
						if (pin != null)
							selectedPin = pin.microHardwarePin;
						else
							selectedPin = -1;
					}
				}, selectedPin);
		super.onResume();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectedPin = ((OneSheeldApplication) getApplication())
				.getAppPreferences().getInt("PluginActionPin", -1);
		BundleScrubber.scrub(getIntent());

		final Bundle localeBundle = getIntent().getBundleExtra(
				com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
		BundleScrubber.scrub(localeBundle);

		setContentView(R.layout.plugin_action_activity);
		// pinsSpinner = (Spinner) findViewById(R.id.pins_spinner);
		outputSpinner = (Spinner) findViewById(R.id.output_spinner);

		// ArrayAdapter<String> pinsArrayAdapter = new
		// ArrayAdapter<String>(this,
		// android.R.layout.simple_spinner_item, pins);
		ArrayAdapter<String> outputArrayAdapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item, output);
		// pinsSpinner.setAdapter(pinsArrayAdapter);
		outputSpinner.setAdapter(outputArrayAdapter);

		if (null == savedInstanceState) {
			if (PluginBundleManager.isBundleValid(localeBundle)) {
				// final int pin = localeBundle
				// .getInt(PluginBundleManager.BUNDLE_EXTRA_PIN_NUMBER);
				final boolean output = localeBundle
						.getBoolean(PluginBundleManager.BUNDLE_EXTRA_OUTPUT);
				// ((EditText)
				// findViewById(android.R.id.text1)).setText(message);
				// pinsSpinner.setSelection(pin);
				if (output)
					outputSpinner.setSelection(0);
				else
					outputSpinner.setSelection(1);
			}
		}
		PluginConnectingPinsView pluginPinsView = PluginConnectingPinsView
				.getInstance();
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.pluginPinsFrame, pluginPinsView, "Pins").commit();
	}

	@Override
	public void finish() {
		if (!isCanceled()) {
			// final String message = ((EditText)
			// findViewById(android.R.id.text1)).getText().toString();
			final boolean output = (outputSpinner).getSelectedItem().toString()
					.toLowerCase().equals("high") ? true : false;
			// if (message.length() > 0)
			// {
			final Intent resultIntent = new Intent();

			/*
			 * This extra is the data to ourselves: either for the Activity or
			 * the BroadcastReceiver. Note that anything placed in this Bundle
			 * must be available to Locale's class loader. So storing String,
			 * int, and other standard objects will work just fine. Parcelable
			 * objects are not acceptable, unless they also implement
			 * Serializable. Serializable objects must be standard Android
			 * platform objects (A Serializable class private to this plug-in's
			 * APK cannot be stored in the Bundle, as Locale's classloader will
			 * not recognize it).
			 */
			final Bundle resultBundle = PluginBundleManager.generateBundle(
					getApplicationContext(), selectedPin, output);
			resultIntent
					.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE,
							resultBundle);

			/*
			 * The blurb is concise status text to be displayed in the host's
			 * UI.
			 */
			final String blurb = selectedPin >= 0 ? (generateBlurb(
					getApplicationContext(), "Pin " + selectedPin + " set to "
							+ (output ? "High" : "Low"))) : generateBlurb(
					getApplicationContext(), "No Pins Selected");
			((OneSheeldApplication) getApplication()).getAppPreferences()
					.edit().putInt("PluginActionPin", selectedPin).commit();
			resultIntent.putExtra(
					com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, blurb);

			setResult(RESULT_OK, resultIntent);
			// }
		}

		super.finish();
	}

	/**
	 * @param context
	 *            Application context.
	 * @param message
	 *            The toast message to be displayed by the plug-in. Cannot be
	 *            null.
	 * @return A blurb for the plug-in.
	 */
	/* package */static String generateBlurb(final Context context,
			final String message) {
		final int maxBlurbLength = context.getResources().getInteger(
				R.integer.twofortyfouram_locale_maximum_blurb_length);

		if (message.length() > maxBlurbLength) {
			return message.substring(0, maxBlurbLength);
		}

		return message;
	}
}