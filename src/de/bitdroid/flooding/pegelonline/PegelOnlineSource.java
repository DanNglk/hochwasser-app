package de.bitdroid.flooding.pegelonline;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.util.Pair;

import de.bitdroid.flooding.ods.OdsSource;
import de.bitdroid.flooding.utils.SQLiteType;


public final class PegelOnlineSource extends OdsSource { 

	public static final String
		COLUMN_WATER_NAME = "waterName",
		COLUMN_STATION_NAME = "stationName",
		COLUMN_STATION_LAT = "stationLat",
		COLUMN_STATION_LONG = "stationLong",
		COLUMN_STATION_KM = "stationKm",
		COLUMN_LEVEL_TIMESTAMP = "levelTimestamp",
		COLUMN_LEVEL_VALUE = "levelValue",
		COLUMN_LEVEL_UNIT = "leveUnit",
		COLUMN_LEVEL_TYPE = "leveType",
		COLUMN_LEVEL_ZERO_VALUE = "levelZeroValue",
		COLUMN_LEVEL_ZERO_UNIT = "levelZeroUnit",
		COLUMN_CHARVALUES_MW_VALUE = "mvValue",
		COLUMN_CHARVALUES_MW_UNIT = "mvUnit",
		COLUMN_CHARVALUES_MHW_VALUE = "mhwValue",
		COLUMN_CHARVALUES_MHW_UNIT = "mhwUnit",
		COLUMN_CHARVALUES_MNW_VALUE = "mnwValue",
		COLUMN_CHARVALUES_MNW_UNIT = "mnwUnit",
		COLUMN_CHARVALUES_MTNW_VALUE = "mtnwValue",
		COLUMN_CHARVALUES_MTNW_UNIT = "mtnwUnit",
		COLUMN_CHARVALUES_MTHW_VALUE = "mthwValue",
		COLUMN_CHARVALUES_MTHW_UNIT = "mthwUnit",
		COLUMN_CHARVALUES_HTHW_VALUE = "hthwValue",
		COLUMN_CHARVALUES_HTHW_UNIT = "hthwUnit",
		COLUMN_CHARVALUES_NTNW_VALUE = "ntnwValue",
		COLUMN_CHARVALUES_NTNW_UNIT = "ntnwUnit";


	private static final String
//		SOURCE_URL = "ods/de/pegelonline/stations";
		SOURCE_URL = "stations.json?includeTimeseries=true&includeCurrentMeasurement=true&includeCharacteristicValues=true";


	private static final Map<String, SQLiteType> SCHEMA = new HashMap<String, SQLiteType>();

	static {
		SCHEMA.put(COLUMN_WATER_NAME, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_STATION_NAME, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_STATION_LAT, SQLiteType.REAL);
		SCHEMA.put(COLUMN_STATION_LONG, SQLiteType.REAL);
		SCHEMA.put(COLUMN_STATION_KM, SQLiteType.REAL);
		SCHEMA.put(COLUMN_LEVEL_TIMESTAMP, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_LEVEL_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_LEVEL_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_LEVEL_ZERO_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_LEVEL_ZERO_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_LEVEL_TYPE, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_MW_VALUE , SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_MW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_MHW_VALUE , SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_MHW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_MNW_VALUE , SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_MNW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_MTNW_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_MTNW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_MTHW_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_MTHW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_HTHW_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_HTHW_UNIT, SQLiteType.TEXT);
		SCHEMA.put(COLUMN_CHARVALUES_NTNW_VALUE, SQLiteType.REAL);
		SCHEMA.put(COLUMN_CHARVALUES_NTNW_UNIT, SQLiteType.TEXT);
	}


	@Override
	public String getSourceUrlPath() {
		return SOURCE_URL;
	}


	@Override
	public Map<String, SQLiteType> getSchema() {
		return SCHEMA;
	}


	@Override
	public ContentValues saveData(JSONObject json) {
		ContentValues values = new ContentValues();

		values.put(COLUMN_WATER_NAME, json.optJSONObject("water").optString("longname"));
		values.put(COLUMN_STATION_NAME, json.optString("longname"));
		values.put(COLUMN_STATION_LAT, json.optDouble("latitude"));
		values.put(COLUMN_STATION_LONG, json.optDouble("longitude"));
		values.put(COLUMN_STATION_KM, json.optDouble("km"));

		JSONObject timeseries = json.optJSONArray("timeseries").optJSONObject(0);
		values.put(COLUMN_LEVEL_UNIT, timeseries.optString("unit"));
		values.put(COLUMN_LEVEL_TYPE, timeseries.optString("shortname"));

		JSONObject measurement = timeseries.optJSONObject("currentMeasurement");
		values.put(COLUMN_LEVEL_TIMESTAMP, measurement.optString("timestamp"));
		values.put(COLUMN_LEVEL_VALUE, measurement.optDouble("value"));

		JSONObject gaugeZero = timeseries.optJSONObject("gaugeZero");
		if (gaugeZero != null) {
			values.put(COLUMN_LEVEL_ZERO_VALUE, gaugeZero.optDouble("value"));
			values.put(COLUMN_LEVEL_ZERO_UNIT, gaugeZero.optString("unit"));
		}

		// get charactersitic values
		Map<String, Pair<String, String>> charValues = new HashMap<String, Pair<String, String>>();
		charValues.put("MW", new Pair<String, String>(
					COLUMN_CHARVALUES_MW_VALUE, 
					COLUMN_CHARVALUES_MW_UNIT));
		charValues.put("MHW", new Pair<String, String>(
					COLUMN_CHARVALUES_MHW_VALUE, 
					COLUMN_CHARVALUES_MHW_UNIT));
		charValues.put("MNW", new Pair<String, String>(
					COLUMN_CHARVALUES_MNW_VALUE, 
					COLUMN_CHARVALUES_MNW_UNIT));
		charValues.put("MThw", new Pair<String, String>(
					COLUMN_CHARVALUES_MTHW_VALUE,
					COLUMN_CHARVALUES_MTHW_UNIT));
		charValues.put("MTnw", new Pair<String, String>(
					COLUMN_CHARVALUES_MTNW_VALUE,
					COLUMN_CHARVALUES_MTNW_UNIT));
		charValues.put("HThw", new Pair<String, String>(
					COLUMN_CHARVALUES_HTHW_VALUE,
					COLUMN_CHARVALUES_HTHW_UNIT));
		charValues.put("NTnw", new Pair<String, String>(
					COLUMN_CHARVALUES_NTNW_VALUE,
					COLUMN_CHARVALUES_NTNW_UNIT));

		JSONArray charValuesArray = timeseries.optJSONArray("characteristicValues");
		if (charValuesArray != null) {
			for (int i = 0; i < charValuesArray.length(); i++) {
				JSONObject o =  charValuesArray.optJSONObject(i);
				String charType = o.optString("shortname");
				if (charValues.containsKey(charType)) {
					values.put(charValues.get(charType).first, o.optString("value"));
					values.put(charValues.get(charType).second, o.optString("unit"));
				}
			}

		}


		return values;
	}

}