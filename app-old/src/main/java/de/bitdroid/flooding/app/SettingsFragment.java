package de.bitdroid.flooding.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import de.bitdroid.flooding.R;
import de.bitdroid.flooding.pegelonline.PegelOnlineSource;
import de.bitdroid.flooding.ceps.RuleManagerFactory;
import de.bitdroid.flooding.data.OdsSource;
import de.bitdroid.flooding.data.OdsSourceManager;
import it.gmariotti.changelibs.library.view.ChangeLogListView;
import timber.log.Timber;


public final class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// server names
		updateServerName(R.string.prefs_ods_servername_key, new ChangeServerName() {
			@Override
			public void changeServerName(String newServerName) {
				OdsSourceManager
					.getInstance(getActivity().getApplicationContext())
					.setOdsServerName(newServerName);
			}
		});
		updateServerName(R.string.prefs_ceps_servername_key, new ChangeServerName() {
			@Override
			public void changeServerName(String newServerName) {
				RuleManagerFactory.createRuleManager(getActivity().getApplicationContext()).setCepServerName(newServerName);
			}
		});

		// source monitoring
		Preference monitoring = findPreference(getString(R.string.prefs_ods_monitor_key));
		final ListPreference monitorDurationPref = (ListPreference) findPreference(getString(R.string.prefs_ods_monitor_days_key));
		final ListPreference monitorIntervalPref = (ListPreference) findPreference(getString(R.string.prefs_ods_monitor_interval_key));
		final CheckBoxPreference wifiPref = (CheckBoxPreference) findPreference(getString(R.string.prefs_ods_monitor_wifi_key));

		monitoring.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				toggleMonitoring(
						(Boolean) newValue,
						wifiPref.isChecked(),
						Double.valueOf(monitorIntervalPref.getValue()));
				return true;
			}
		});

		wifiPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean wifiOnlySync = (Boolean) newValue;
				double interval = Double.valueOf(monitorIntervalPref.getValue());
				toggleMonitoring(false, !wifiOnlySync, interval);
				toggleMonitoring(true, wifiOnlySync, interval);
				return true;
			}
		});

		monitorDurationPref.setSummary(monitorDurationPref.getEntry());
		monitorDurationPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				monitorDurationPref.setSummary(
						monitorDurationPref.getEntries()[
								monitorDurationPref.findIndexOfValue(newValue.toString())]);
				return true;
			}
		});

		monitorIntervalPref.setSummary(monitorIntervalPref.getEntry());
		monitorIntervalPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				monitorIntervalPref.setSummary(
						monitorIntervalPref.getEntries()[
								monitorIntervalPref.findIndexOfValue(newValue.toString())]);

				boolean wifiOnlySync = wifiPref.isChecked();
				double intervalInHours = Double.valueOf(newValue.toString());

				toggleMonitoring(false, wifiOnlySync, intervalInHours);
				toggleMonitoring(true, wifiOnlySync, intervalInHours);
				return true;
			}
		});

		// sync status
		OdsSourceManager manager 
			= OdsSourceManager.getInstance(getActivity().getApplicationContext());

		Preference lastSync = findPreference(getString(R.string.prefs_ods_sync_last_key));
		lastSync.setSummary(formatTime(manager.getLastSync(PegelOnlineSource.INSTANCE)));

		Preference successSync = findPreference(getString(R.string.prefs_ods_sync_last_success_key));
		successSync.setSummary(formatTime(manager.getLastSuccessfulSync(PegelOnlineSource.INSTANCE)));

		Preference failSync = findPreference(getString(R.string.prefs_ods_sync_last_fail_key));
		failSync.setSummary(formatTime(manager.getLastFailedSync(PegelOnlineSource.INSTANCE)));

		// about - version
		final Preference versionPref = findPreference(getString(R.string.prefs_about_version_key));
		try {
			String versionName = getActivity()
					.getPackageManager()
					.getPackageInfo(getActivity().getPackageName(), 0)
					.versionName;
			versionPref.setSummary(versionName);
		} catch (PackageManager.NameNotFoundException nnfe) {
			Timber.e(nnfe, "Failed to get package name");
		}
		versionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				ChangeLogListView listView = (ChangeLogListView) inflater.inflate(R.layout.changelog, null);
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.prefs_about_changelog)
						.setView(listView)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								}
						)
						.show();
				return true;
			}
		});

		// about - feedback
		Preference feebackPref = findPreference(getString(R.string.prefs_about_feedback_key));
		feebackPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				String address = getString(R.string.feedback_mail_address);
				String subject = getString(
						R.string.feedback_mail_subject,
						getString(R.string.app_name),
						versionPref.getSummary().toString());
				Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", address, null));
				intent.putExtra(Intent.EXTRA_SUBJECT, subject);
				startActivity(Intent.createChooser(intent, getString(R.string.feedback_mail_chooser)));
				return true;
			}
		});

		// about - contribute
		Preference contributePref = findPreference(getString(R.string.prefs_about_contribute_key));
		contributePref.setSummary(getString(R.string.prefs_about_contribute_summary));
		contributePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog dialog = new AlertDialog.Builder(getActivity())
						.setTitle(getString(R.string.contribute_title, getString(R.string.app_name)))
						.setMessage(Html.fromHtml(getString(R.string.contribute_msg)))
						.setPositiveButton(getString(R.string.btn_ok), null)
						.create();
				dialog.show();
				TextView msgView = (TextView) dialog.findViewById(android.R.id.message);
				msgView.setTextAppearance(getActivity(), R.style.FontRegular);
				msgView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
				return true;
			}
		});

		// about - legal
		Preference legalPref = findPreference(getString(R.string.prefs_about_legal_key));
		legalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog dialog = new AlertDialog.Builder(getActivity())
						.setTitle(getString(R.string.prefs_about_legal))
						.setMessage(Html.fromHtml(getString(R.string.legal)))
						.setPositiveButton(getString(R.string.btn_ok), null)
						.create();
				dialog.show();
				TextView msgView = (TextView) dialog.findViewById(android.R.id.message);
				msgView.setMovementMethod(LinkMovementMethod.getInstance());
				msgView.setTextAppearance(getActivity(), R.style.FontRegular);
				return true;
			}
		});

	}


	private String formatTime(Calendar time) {
		if (time == null) return getString(R.string.never);
		else return DateFormat.getDateFormat(getActivity()).format(time.getTime())
			+ " " + DateFormat.getTimeFormat(getActivity()).format(time.getTime());
	}


	private void updateServerName(int prefsViewId, final ChangeServerName serverNameChanger) {
		final EditTextPreference servername 
			= (EditTextPreference) findPreference(getString(prefsViewId));
		servername.setSummary(servername.getText());
		servername.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String newServerName = newValue.toString();
				try {
					serverNameChanger.changeServerName(newServerName);
					servername.setSummary(newServerName);
					return true;
				} catch (Exception e) {
					Toast.makeText(
						getActivity().getApplicationContext(), 
						getString(R.string.error_invalid_server_url), 
						Toast.LENGTH_LONG)
						.show();
					return false;
				}
			}
		});
	}


	private void toggleMonitoring(boolean start, boolean wifiOnlySync, double intervalInHours) {
		Context context = getActivity().getApplicationContext();
		OdsSourceManager sourceManager = OdsSourceManager.getInstance(context);
		OdsSource source = PegelOnlineSource.INSTANCE;

		// only toggle polling, not the actual monitoring since SourceMonitor does not
		// perform any network tasks by itself
		if (start) {
			long interval = (long) (intervalInHours * 60 * 60);

			if (!sourceManager.isRegisteredForPolling(source))
				sourceManager.startPolling(interval, wifiOnlySync, source);
		} else {
			if (sourceManager.isRegisteredForPolling(source)) sourceManager.stopPolling();
		}
	}


	private static interface ChangeServerName {

		public void changeServerName(String newServerName) throws Exception;

	}

}