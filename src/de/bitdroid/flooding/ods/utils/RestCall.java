package de.bitdroid.flooding.ods.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.bitdroid.flooding.utils.Assert;
import de.bitdroid.flooding.utils.Log;

public final class RestCall {

	public enum RequestType {
		GET("GET"),
		POST("POST"),
		UPDATE("UPDATE"),
		DELETE("DELETE");

		private final String type;
		RequestType(String type) { this.type = type; }

		@Override
		public String toString() { return type; }
	}


	private final RequestType requestType;
	private final String baseUrl;
	private final List<String> paths;
	private final Map<String, String> parameters;
	private final Map<String, String> headers;


	private RestCall(
				RequestType requestType,
				String baseUrl,
				List<String> paths,
				Map<String, String> parameters,
				Map<String, String> headers) {
		
		this.requestType = requestType;
		this.baseUrl = baseUrl;
		this.paths = paths;
		this.parameters = parameters;
		this.headers = headers;
	}


	public String execute() throws RestException {

		StringBuilder urlBuilder = new StringBuilder(baseUrl);

		// format paths
		for (String path : paths) urlBuilder.append("/" + path);

		// format params
		if (parameters.size() != 0) {
			urlBuilder.append("?");
			boolean firstKey = true;
			for (String key : parameters.keySet()) {
				if (!firstKey) urlBuilder.append("&");
				firstKey = false;

				try {
					urlBuilder.append(
							URLEncoder.encode(key, "UTF-8") + "=" 
							+ URLEncoder.encode(parameters.get(key), "UTF-8"));
				} catch (UnsupportedEncodingException uee) {
					throw new RuntimeException(uee);
				}
			}
		}

		Log.debug("Fetching " + urlBuilder.toString());


		// create connection
		HttpURLConnection conn = null;
		OutputStream outputStream = null;
		BufferedReader dataReader = null;
		try {
			URL url = new URL(urlBuilder.toString());

			conn = (HttpURLConnection) url.openConnection();
			for (String key : headers.keySet()) {
				conn.setRequestProperty(key, headers.get(key));
			}
			conn.setUseCaches(false);
			conn.setRequestMethod(requestType.toString());

			int responseCode = conn.getResponseCode();
			if (responseCode < 200 || responseCode >= 300)
				throw new RestException(responseCode);

			dataReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder dataBuilder = new StringBuilder();
			String line;
			while ((line = dataReader.readLine()) != null)
				dataBuilder.append(line);

			return dataBuilder.toString();
			
		} catch (IOException ie) {
			throw new RestException(ie);

		} finally {
			try {
				if (outputStream != null) outputStream.close();
				if (dataReader != null) dataReader.close();
			} catch (IOException e) { }

			if (conn != null) conn.disconnect();
		}
	}

	

	public static class Builder {
		private final RequestType requestType;
		private final String baseUrl;
		private final List<String> paths = new LinkedList<String>();
		private final Map<String, String> parameters = new HashMap<String, String>();
		private final Map<String, String> headers = new HashMap<String, String>();
		private boolean built = false;

		public Builder(final RequestType requestType, final String baseUrl) {
			Assert.assertNotNull(requestType, baseUrl);
			this.requestType = requestType;
			this.baseUrl = baseUrl;
		}

		public Builder path(String path) {
			Assert.assertNotNull(path);
			Assert.assertFalse(built, "already built");
			paths.add(path);
			return this;
		}

		public Builder parameter(String key, String value) {
			Assert.assertNotNull(key, value);
			Assert.assertFalse(built, "already built");
			parameters.put(key, value);
			return this;
		}

		public Builder header(String key, String value) {
			Assert.assertNotNull(key, value);
			Assert.assertFalse(built, "already built");
			headers.put(key, value);
			return this;
		}

		public RestCall build() {
			Assert.assertFalse(built, "already built");
			built = true;
			return new RestCall(requestType, baseUrl, paths, parameters, headers);
		}
	}

}
