package eu.ttbox.velib.ui.preference;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import eu.ttbox.velib.AndroLibApplication;
import eu.ttbox.velib.R;
import eu.ttbox.velib.model.VelibProvider;

/**
 * @see http://developer.android.com/reference/android/preference/PreferenceActivity.html cool preference On/Off @see
 *      http://stackoverflow.com/questions/9738658/how-do-i-create-one-preference-with-an-edittextpreference-and-a-togglebutton
 */
public class VelibPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		// Init Summary
		initSummaries(this.getPreferenceScreen());
		// Register change listener
		this.getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	     // Tracker
        GoogleAnalyticsTracker tracker = ((AndroLibApplication)getApplication()).getTracker();
        tracker.trackPageView("/Preference");

	}

	/**
	 * Set the summaries of all preferences
	 */
	private void initSummaries(PreferenceGroup pg) {
		for (int i = 0; i < pg.getPreferenceCount(); ++i) {
			Preference p = pg.getPreference(i);
			if ("providerSelect".equals(p.getKey())) {
				ListPreference provilderList = (ListPreference) p;
				VelibProvider[] velibs = VelibProvider.values();
				String[] entries = new String[velibs.length];
				String[] entryValues = new String[velibs.length];
				for (int e = velibs.length - 1; e >= 0; e--) {
					VelibProvider velib = velibs[e];
					entries[e] = velib.getName();
					entryValues[e] = velib.getProviderName();
				}
				provilderList.setEntries(entries);
				provilderList.setEntryValues(entryValues);
			}
			// Init
			if (p instanceof PreferenceGroup) {
				this.initSummaries((PreferenceGroup) p); // recursion
			} else {
				this.setSummary(p);
			}
		}
	}

	/**
	 * Set the summaries of the given preference
	 */
	private void setSummary(Preference pref) {
		// react on type or key
		if (pref instanceof EditTextPreference) {
			EditTextPreference editPref = (EditTextPreference) pref;
			String prefText = editPref.getText();
			if (prefText != null && prefText.length() > 0)
				pref.setSummary(prefText);
		} else if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = findPreference(key);
		setSummary(pref);
	}
	//
	// @Override
	// public boolean onPreferenceChange(Preference preference, Object newValue) {
	// setSummary(preference);
	// return true;
	// }
}
