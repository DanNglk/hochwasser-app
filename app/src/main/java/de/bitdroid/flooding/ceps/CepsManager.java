package de.bitdroid.flooding.ceps;


import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import org.jvalue.ceps.api.RegistrationApi;
import org.jvalue.ceps.api.notifications.Client;
import org.jvalue.ceps.api.notifications.GcmClientDescription;
import org.roboguice.shaded.goole.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.bitdroid.flooding.gcm.GcmManager;
import de.bitdroid.flooding.gcm.GcmService;
import de.bitdroid.flooding.ods.OdsManager;
import de.bitdroid.flooding.ods.Station;
import de.bitdroid.flooding.ods.StationMeasurements;
import retrofit.client.Response;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;

@Singleton
public class CepsManager {

	private static final String
			PEGEL_ALARM_ABOVE_LEVEL_ADAPTER_ID = "pegelAlarmAboveLevel",
			PEGEL_ALARM_BELOW_LEVEL_ADAPTER_ID = "pegelAlarmBelowLevel",
			PEGEL_ALARM_ABOVE_OR_BELOW_LEVEL_ADAPTER_ID = "pegelAlarmAboveOrBelowLevel";
	private static final String
			ARGUMENT_GAUGE_ID = "GAUGE_ID",
			ARGUMENT_LEVEL = "LEVEL";

	private final Context context;
	private final RegistrationApi registrationApi;
	private final OdsManager odsManager;
	private final GcmManager gcmManager;

	private List<Alarm> alarmsCache = null;

	@Inject
	CepsManager(Context context, RegistrationApi registrationApi, OdsManager odsManager, GcmManager gcmManager) {
		this.context = context;
		this.registrationApi = registrationApi;
		this.odsManager = odsManager;
		this.gcmManager = gcmManager;
	}


	public Observable<List<Alarm>> getAlarms() {
		if (alarmsCache != null) {
			List<Alarm> alarms = new ArrayList<>(alarmsCache);
			return Observable.just(alarms);
			
		} else {
			return Observable
					.merge(
							registrationApi.getAllClients(PEGEL_ALARM_ABOVE_LEVEL_ADAPTER_ID)
									.flatMap(new Func1<List<Client>, Observable<Pair<String, Alarm.Builder>>>() {
										@Override
										public Observable<Pair<String, Alarm.Builder>> call(List<Client> clients) {
											return Observable.from(parseClients(clients, true, false));
										}
									}),
							registrationApi.getAllClients(PEGEL_ALARM_BELOW_LEVEL_ADAPTER_ID)
									.flatMap(new Func1<List<Client>, Observable<Pair<String, Alarm.Builder>>>() {
										@Override
										public Observable<Pair<String, Alarm.Builder>> call(List<Client> clients) {
											return Observable.from(parseClients(clients, false, true));
										}
									}),
							registrationApi.getAllClients(PEGEL_ALARM_ABOVE_OR_BELOW_LEVEL_ADAPTER_ID)
									.flatMap(new Func1<List<Client>, Observable<Pair<String, Alarm.Builder>>>() {
										@Override
										public Observable<Pair<String, Alarm.Builder>> call(List<Client> clients) {
											return Observable.from(parseClients(clients, true, true));
										}
									}))
					.flatMap(new Func1<Pair<String, Alarm.Builder>, Observable<Alarm>>() {
						@Override
						public Observable<Alarm> call(final Pair<String, Alarm.Builder> entry) {
							return odsManager.getStationByGaugeId(entry.first)
									.flatMap(new Func1<Optional<Station>, Observable<Alarm>>() {
										@Override
										public Observable<Alarm> call(Optional<Station> stationOptional) {
											if (!stationOptional.isPresent()) {
												Timber.e("failed to find station with uuid " + entry.first);
												return Observable.empty();
											}
											return Observable.just(entry.second.setStation(stationOptional.get()).build());
										}
									});
						}
					})
					.toSortedList(new Func2<Alarm, Alarm, Integer>() {
						@Override
						public Integer call(Alarm alarm1, Alarm alarm2) {
							return alarm1.getStation().getStationName().compareTo(alarm2.getStation().getStationName());
						}
					})
					.flatMap(new Func1<List<Alarm>, Observable<List<Alarm>>>() {
						@Override
						public Observable<List<Alarm>> call(List<Alarm> alarms) {
							CepsManager.this.alarmsCache = new ArrayList<>(alarms);
							return Observable.just(alarms);
						}
					});
		}
	}


	public Observable<Void> addAlarm(final Alarm alarm) {
		// collect alarm parameters
		final Map<String, Object> args = new HashMap<>();
		args.put(ARGUMENT_GAUGE_ID, alarm.getStation().getGaugeId());
		args.put(ARGUMENT_LEVEL, alarm.getLevel());
		final String adapterId = alarmToAdapterId(alarm);

		// register for GCM
		Observable<Void> gcmObservable;
		if (!gcmManager.isRegistered()) {
			Timber.d("registering for GCM");
			gcmObservable = gcmManager.register();
		} else {
			gcmObservable = Observable.just(null);
		}

		return gcmObservable
				.flatMap(new Func1<Void, Observable<Client>>() {
					@Override
					public Observable<Client> call(Void nothing) {
						return registrationApi.registerClient(adapterId, new GcmClientDescription(gcmManager.getRegId(), args));
					}
				})
				.flatMap(new Func1<Client, Observable<Void>>() {
					@Override
					public Observable<Void> call(Client client) {
						// cache new alarm
						alarmsCache.add(new Alarm(client.getId(), alarm));

						// check if alarm should be triggered immediately
						StationMeasurements measurements = odsManager.getMeasurements(alarm.getStation()).toBlocking().first();
						boolean triggerAlarm = false;
						if (alarm.isAlarmWhenAboveLevel() && alarm.isAlarmWhenBelowLevel() && alarm.getLevel() != measurements.getLevel().getValueInCm()) triggerAlarm = true;
						else if (alarm.isAlarmWhenAboveLevel() && measurements.getLevel().getValueInCm() > alarm.getLevel()) triggerAlarm = true;
						else if (alarm.isAlarmWhenBelowLevel() && measurements.getLevel().getValueInCm() < alarm.getLevel()) triggerAlarm = true;
						if (triggerAlarm) {
							Intent serviceIntent = new Intent(context, GcmService.class);
							serviceIntent.putExtra(GcmService.ARGUMENT_REGISTRATION_ID, client.getId());
							context.startService(serviceIntent);
						}

						return Observable.just(null);
					}
				});
	}


	public Observable<Void> removeAlarm(final Alarm alarm) {
		String adapterId = alarmToAdapterId(alarm);
		String clientId = alarm.getId();
		return registrationApi.unregisterClient(adapterId, clientId)
				.flatMap(new Func1<Response, Observable<Void>>() {
					@Override
					public Observable<Void> call(Response response) {
						alarmsCache.remove(alarm);
						return Observable.just(null);
					}
				});
	}


	private List<Pair<String, Alarm.Builder>> parseClients(List<Client> clients, boolean alarmWhenAboveLevel, boolean alarmWhenBelowLevel) {
		List<Pair<String, Alarm.Builder>> builders = new ArrayList<>(); // station uuid --> builder
		for (Client client : clients) {
			Map<String, Object> args = client.getEplArguments();
			Alarm.Builder builder = new Alarm.Builder()
					.setId(client.getId())
					.setLevel((double) args.get(ARGUMENT_LEVEL))
					.setAlarmWhenAboveLevel(alarmWhenAboveLevel)
					.setAlarmWhenBelowLevel(alarmWhenBelowLevel);
			builders.add(new Pair<>((String) args.get(ARGUMENT_GAUGE_ID), builder));
		}
		return builders;
	}


	private String alarmToAdapterId(Alarm alarm) {
		if (alarm.isAlarmWhenAboveLevel() && alarm.isAlarmWhenBelowLevel()) {
			return PEGEL_ALARM_ABOVE_OR_BELOW_LEVEL_ADAPTER_ID;
		} else if (alarm.isAlarmWhenAboveLevel()) {
			return PEGEL_ALARM_ABOVE_LEVEL_ADAPTER_ID;
		} else if (alarm.isAlarmWhenBelowLevel()) {
			return PEGEL_ALARM_BELOW_LEVEL_ADAPTER_ID;
		} else {
			throw new IllegalArgumentException("alarm is neither for above or below level");
		}
	}

}
