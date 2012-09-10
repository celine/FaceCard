package com.android.facecard.remote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;

import com.android.facecard.Constants;
import com.android.facecard.SessionStore;
import com.android.facecard.api.GetFriendsData;
import com.facebook.android.Facebook;

import android.app.IntentService;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

public class FaceService extends IntentService implements Constants {
	private static final String TAG = "FaceService";

	public FaceService() {
		super("FaceService");
	}

	public static String ACTION_SYNC_FRIENDS = FaceService.class.getPackage()
			.getName() + "/" + FaceService.class.getSimpleName();

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent " + intent.getAction());
		if (ACTION_SYNC_FRIENDS.equals(intent.getAction())) {
			Facebook mFacebook = new Facebook(APP_ID);
			SessionStore.restore(mFacebook, getApplicationContext());
			GetFriendsData getFriendsData = new GetFriendsData(mFacebook,
					getApplicationContext());
			try {
				getFriendsData.startSync();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
