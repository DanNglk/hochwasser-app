package de.bitdroid.flooding.ods.data;

import android.accounts.Account;
import android.content.ContentValues;
import android.net.Uri;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.bitdroid.flooding.utils.Assert;
import de.bitdroid.flooding.utils.SQLiteType;


/**
 * Adapter that determines what to save of data fetched from the ODS.
 */
public abstract class OdsSource {

	public static final String
			ACCOUNT_NAME = "OpenDataService",
			ACCOUNT_TYPE = "de.bitdroid.flooding";

	public static final Account ACCOUNT = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);

	public static final String AUTHORITY = "de.bitdroid.flooding.provider";

	/** Primary key. NOT included in the schema information! */
	public static final String
		COLUMN_ID = "_id";

	public static final String
		COLUMN_SERVER_ID = "serverId",
		COLUMN_TIMESTAMP = "timestamp";


	private static final String
		BASE_PATH = "ods",
		SYNC_PATH = "sync";


	private final Uri baseUri;

	private final Map<String, SQLiteType> schema;

	protected OdsSource() {
		// build base url
		Uri.Builder builder = new Uri.Builder()
				.scheme("content")
				.authority(AUTHORITY)
				.appendPath(BASE_PATH);

		String[] classPaths = getClass().getName().split("\\.");
		for (String path : classPaths) {
			builder.appendPath(path);
		}
		this.baseUri = builder.build();

		// build readonly schema info
		Map<String, SQLiteType> schema = new HashMap<String, SQLiteType>();
		schema.put(COLUMN_SERVER_ID, SQLiteType.TEXT);
		schema.put(COLUMN_TIMESTAMP, SQLiteType.INTEGER);
		getSchema(schema);
		this.schema = Collections.unmodifiableMap(schema);
	}



	/**
	 * Returns the source URL NOT including domain name and port.
	 */
	public abstract String getSourceUrlPath();


	/**
	 * Describes what parts of data should be saved.
	 */
	public final Map<String, SQLiteType> getSchema() {
		return schema;
	}


	protected abstract void getSchema(Map<String, SQLiteType> schema);


	/**
	 * Returns the name of this source on the ODS. See name field when viewing metadata.
	 */
	public abstract String getSourceId();


	/**
	 * Pick values that should be stored in the db from a JSON object
	 */
	public final ContentValues saveData(JSONObject jsonObject, long timestamp) {
		ContentValues values = new ContentValues();
		values.put(OdsSource.COLUMN_SERVER_ID, jsonObject.optString("_id"));
		values.put(OdsSource.COLUMN_TIMESTAMP, timestamp);
		saveData(jsonObject, values);
		return values;
	}

	protected abstract void saveData(JSONObject jsonObject, ContentValues values);


	public final Uri toUri() {
		return baseUri;
	}

	public final Uri toSyncUri() {
		return baseUri 
			.buildUpon()
			.appendPath(SYNC_PATH)
			.build();
	}


	public final String toSqlTableName() {
		return getClass().getName().replaceAll("\\.", "_");
	}


	public static boolean isSyncUri(Uri uri) {
		return uri.getLastPathSegment().equals(SYNC_PATH);
	}


	public static OdsSource fromUri(Uri uri) {
		String path = uri.getPath();
		
		// remove sync path
		if (isSyncUri(uri)) {
			path = path.substring(0, path.length() - SYNC_PATH.length() - 1);
		}

		// remove base path
		path = path.substring(BASE_PATH.length() + 2);

		return fromString(path.replaceAll("/", "."));
	}


	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof OdsSource)) return false;
		OdsSource source = (OdsSource) other;
		return equals(getSourceUrlPath(), source.getSourceUrlPath())
			&& equals(getSchema(), source.getSchema());
	}


	private boolean equals(Object o1, Object o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}



	@Override
	public int hashCode() {
		final int MULT = 13;
		int hash = 11;
		hash = hash * MULT + (getSourceUrlPath() == null ? 0 : getSourceUrlPath().hashCode());
		hash = hash * MULT + (getSchema() == null ? 0 : getSchema().hashCode());
		return hash;
	}


	@SuppressWarnings("unchecked")
	public static OdsSource fromString(String source) {
		Assert.assertNotNull(source);
		try {
			Class<? extends OdsSource> sourceClass 
					= (Class<? extends OdsSource>) Class.forName(source);
			return sourceClass.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException("param is not a valid source", e);
		}
	}


	@Override
	public String toString() {
		return getClass().getName();
	}

}
