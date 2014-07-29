package de.bitdroid.flooding.ods.data;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.bitdroid.flooding.utils.Log;


final class Processor {

	private final ContentProviderClient provider;
	private final OdsSource source;

	public Processor(ContentProviderClient provider, OdsSource source) {
		this.provider = provider;
		this.source = source;
	}


	public void processGetAll(String jsonString) 
			throws RemoteException, JSONException, OperationApplicationException {

		// insert new entries
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		JSONArray json = new JSONArray(jsonString);
		Log.debug("Fetched " + json.length() + " items from server");
		for (int i = 0; i < json.length(); i++) {
			ContentProviderOperation operation = insertIntoProvider(json.getJSONObject(i));
			if (operation != null) operations.add(operation);
			if (operations.size() >= 100) {
				provider.applyBatch(operations);
				operations.clear();
			}
		}

		if (operations.size() > 0) provider.applyBatch(operations);
	}


	private ContentProviderOperation insertIntoProvider(JSONObject object) 
			throws RemoteException, JSONException {

		// this is a hack that updates pegelonline data only, since ODS does currently
		// (as of 29/07/14) not support updating data
		String serverId = object.optString("uuid");
		ContentValues data = source.saveData(object);
		data.put(OdsSource.COLUMN_SERVER_ID, serverId);
		data.put(OdsSource.COLUMN_SYNC_STATUS, SyncStatus.SYNCED.toString());

		// query if already present
		Cursor cursor = null;
		try {
			cursor = provider.query(
					source.toUri(),
					new String[] { OdsSource.COLUMN_ID, OdsSource.COLUMN_SERVER_ID },
					OdsSource.COLUMN_SERVER_ID + " = ?",
					new String[] { serverId },
					null);

			// update data 
			if (cursor.getCount() >= 1) {
				cursor.moveToFirst();
				int id = cursor.getInt(0);
				data.put(OdsSource.COLUMN_ID, id);

				if (cursor.getCount() > 1) {
					Log.warning("Found multiple objects in db with server id " + serverId);
				}

				return ContentProviderOperation
					.newUpdate(source.toUri())
					.withSelection(OdsSource.COLUMN_ID + "=?", new String[] { String.valueOf(id) })
					.withValues(data)
					.build();

			// insert new data
			} else {
				return ContentProviderOperation
					.newInsert(source.toUri())
					.withValues(data)
					.build();
			}
		} finally {
			if (cursor != null) cursor.close();
		}
	}

}
