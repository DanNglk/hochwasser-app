package de.bitdroid.flooding;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import de.bitdroid.flooding.monitor.SourceMonitor;
import de.bitdroid.flooding.ods.OdsSource;
import de.bitdroid.flooding.ods.OdsSourceManager;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;


public final class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);


		// ods servername
		Preference servername = findPreference(getString(R.string.prefs_ods_servername_key));
		servername.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				try {
					OdsSourceManager
						.getInstance(getActivity().getApplicationContext())
						.setOdsServerName(newValue.toString());
					return true;
				} catch (IllegalArgumentException iae) {
					Toast.makeText(
						getActivity().getApplicationContext(), 
						getString(R.string.error_invalid_server_url), 
						Toast.LENGTH_LONG)
						.show();
					return false;
				}
			}
		});


		// source monitoring
		Preference monitoring = findPreference(getString(R.string.prefs_ods_monitor_key));
		monitoring.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				SourceMonitor monitor = SourceMonitor
					.getInstance(getActivity().getApplicationContext());
				OdsSource source = PegelOnlineSource.INSTANCE;

				if (!monitor.isBeingMonitored(source)) monitor.startMonitoring(source);
				else monitor.stopMonitoring(source);

				return true;
			}
		});
	}

}