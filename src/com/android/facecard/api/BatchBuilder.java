package com.android.facecard.api;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.FacebookError;

import android.text.TextUtils;

public class BatchBuilder {
	public static class Batch {
		public String mName;
		public String mMethod;
		public String mRelativeUrl;
		public boolean omitResponse;
		public HashMap<String, String> mBody;
	}

	List<Batch> mBatches;

	public BatchBuilder(int size) {
		mBatches = new ArrayList<Batch>(size);
	}

	public void add(Batch batch) {
		mBatches.add(batch);
	}

	public void replace(int position, Batch batch) {
		Batch b = mBatches.get(position);
		b.mName = batch.mName;
		b.mMethod = batch.mMethod;
		b.mRelativeUrl = batch.mRelativeUrl;
		b.omitResponse = batch.omitResponse;
		b.mBody = batch.mBody;
	}

	public String build() {
		JSONArray array = new JSONArray();
		try {
			for (Batch run : mBatches) {
				JSONObject object = new JSONObject();
				object.put("method", run.mMethod);
				object.put("relative_url", run.mRelativeUrl);
				if (!TextUtils.isEmpty(run.mName)) {
					object.put("name", run.mName);
				}
				if (run.mBody != null) {
					StringBuilder builder = new StringBuilder();
					appendEncodeUrl(builder, run.mBody);
					object.put("body", builder.toString());
				}
				object.put("omit_response_on_success", run.omitResponse);
				array.put(object);
			}
		} catch (JSONException e) {
			throw new FacebookError("JsonException");
		}
		return array.toString();
	}

	public static void appendEncodeUrl(StringBuilder sb,
			HashMap<String, String> parameters) {
		if (parameters == null) {
			return;
		}

		boolean first = true;
		for (String key : parameters.keySet()) {
			if (first)
				first = false;
			else
				sb.append("&");
			sb.append(URLEncoder.encode(key) + "="
					+ URLEncoder.encode(parameters.get(key)));
		}
	}
}
