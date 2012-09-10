package com.android.facecard.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.android.facecard.api.BatchBuilder.Batch;
import com.android.facecard.data.AvatarData;
import com.android.facecard.data.AvatarData.Avatar;
import com.android.facecard.data.CoverData;
import com.android.facecard.data.CoverData.Cover;
import com.android.facecard.data.FacebookUserData;
import com.android.facecard.data.FacebookUserData.FacebookUser;
import com.android.facecard.remote.FaceProvider;
import com.facebook.android.Facebook;
import com.google.gson.Gson;

public class GetFriendsData {
	private static final String TAG = "GetFriendsData";
	ContentProviderClient mClient;

	Facebook mFb;

	public GetFriendsData(Facebook fb, Context context) {
		mFb = fb;
		mClient = context.getContentResolver().acquireContentProviderClient(
				FaceProvider.AUTHORITY);
	}

	public void startSync() throws FileNotFoundException,
			MalformedURLException, IOException, JSONException, RemoteException {
		BatchBuilder builder = null;
		int offset = 0;
		int limit = 100;
		do {
			Batch friends = new Batch();
			friends.mName = "get-friends";
			friends.mMethod = "GET";
			friends.omitResponse = false;
			friends.mRelativeUrl = "me/friends?fields=id,first_name,last_name,link,birthday,location&offset="
					+ offset + "&limit=" + limit;
			if (builder == null) {
				builder = new BatchBuilder(3);
				builder.add(friends);
				Batch cover = new Batch();
				cover.mRelativeUrl = "method/fql.query?query=select object_id, src_big,owner from photo where object_id in (select cover_object_id from album where name='Cover Photos' and owner in ({result=get-friends:$.data.*.id}))";
				cover.mMethod = "POST";
				builder.add(cover);
				Batch avatar = new Batch();
				avatar.mRelativeUrl = "method/fql.query?query=select object_id, src_big,owner from photo where object_id in (select cover_object_id from album where type='profile' and owner in ({result=get-friends:$.data.*.id}))";
				avatar.mMethod = "POST";
				builder.add(avatar);
			} else {
				builder.replace(0, friends);
			}
			Bundle params = new Bundle();
			Log.d(TAG, "builder " + builder.build());
			params.putString("batch", builder.build());
			String result = mFb.request("", params, "POST");
			Log.d(TAG, "result " + result);
			JSONArray jResult = new JSONArray(result);
			Gson gson = new Gson();
			Log.d(TAG, "userData " + jResult.getJSONObject(2).toString());
			if (jResult.getJSONObject(0).getInt("code") == 200) {
				FacebookUserData userData = gson.fromJson(jResult
						.getJSONObject(0).getString("body"),
						FacebookUserData.class);
				HashMap<String, FacebookUser> userMap = new HashMap<String, FacebookUser>();
				for (FacebookUser user : userData.data) {
					userMap.put(user.id, user);
				}
				if (jResult.getJSONObject(1).getInt("code") == 200) {

					Avatar[] aData = gson.fromJson(jResult.getJSONObject(2)
							.getString("body"), Avatar[].class);
					for (Avatar avatar : aData) {
						FacebookUser user = userMap.get(avatar.owner);
						user.avatar = avatar.src_big;
					}
				}
				if (jResult.getJSONObject(2).getInt("code") == 200) {
					Cover[] cData = gson.fromJson(jResult.getJSONObject(1)
							.getString("body"), Cover[].class);

					for (Cover cover : cData) {
						FacebookUser user = userMap.get(cover.owner);
						user.cover = cover.src_big;
					}
				}
				ContentValues values[] = new ContentValues[userMap.size()];
				int i = 0;
				for (String id : userMap.keySet()) {
					FacebookUser user = userMap.get(id);
					if (TextUtils.isEmpty(user.name)) {
						user.name = user.first_name + " " + user.last_name;
					}
					values[i++] = user.getInsertValues();
				}
				Log.d(TAG, "get " + userMap.size());
				mClient.bulkInsert(FacebookUser.CONTENT_URI, values);
				offset += limit;

				if (userMap.size() < limit) {
					break;
				}
			} else {
				break;
			}
		} while (true);
	}
}
