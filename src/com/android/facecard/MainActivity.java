package com.android.facecard;

import com.android.facecard.SessionEvents.AuthListener;
import com.android.facecard.SessionEvents.LogoutListener;
import com.android.facecard.app.FaceApp;
import com.android.facecard.data.FacebookUserData.FacebookUser;
import com.android.facecard.remote.FaceService;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class MainActivity extends FragmentActivity implements Constants {
	static final String[] permissions = { "user_about_me", "publish_stream",
			"user_photos", "publish_checkins", "email", "user_about_me",
			"friends_website", "friends_birthday", "friends_photos" };
	private static final String TAG = "FaceMainActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		FragmentTransaction ft = this.getSupportFragmentManager()
				.beginTransaction();
		ft.replace(R.id.main_fm, new LoginFragment());
		ft.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private static final int AUTHORIZE_ACTIVITY_RESULT_CODE = 101;

	public static class LoginFragment extends Fragment {
		LoginButton mLoginButton;
		TextView mText;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			// setContentView(R.layout.activity_main);
			Utility.mFacebook = new Facebook(APP_ID);
			// Instantiate the asynrunner object for asynchronous api calls.
			Utility.mAsyncRunner = new AsyncFacebookRunner(Utility.mFacebook);

			// restore session if one exists
			SessionStore.restore(Utility.mFacebook, getActivity());
			SessionEvents.addAuthListener(new FbAPIsAuthListener());
			SessionEvents.addLogoutListener(new FbAPIsLogoutListener());

			/*
			 * Source Tag: login_tag
			 */
			mLoginButton.init(getActivity(), AUTHORIZE_ACTIVITY_RESULT_CODE,
					Utility.mFacebook, permissions);

			if (Utility.mFacebook.isSessionValid()) {
				requestUserData();
			}
			super.onActivityCreated(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			Log.d(TAG, "createView");
			View login = inflater.inflate(R.layout.login, container, false);
			mText = (TextView) login.findViewById(R.id.message);
			mLoginButton = (LoginButton) login.findViewById(R.id.login);
			return login;
		}

		private void requestUserData() {
			Log.d(TAG, "requestUserData");
			Intent intent = new Intent(FaceService.ACTION_SYNC_FRIENDS);
			intent.setClass(getActivity(), FaceService.class);
			getActivity().startService(intent);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			FriendListFragment fragment = new FriendListFragment();
			ft.replace(R.id.main_fm, fragment);
			ft.commitAllowingStateLoss();
		}

		public class FbAPIsAuthListener implements AuthListener {

			@Override
			public void onAuthSucceed() {
				requestUserData();
			}

			@Override
			public void onAuthFail(String error) {
				mText.setText("Login Failed: " + error);
			}
		}

		/*
		 * The Callback for notifying the application when log out starts and
		 * finishes.
		 */
		public class FbAPIsLogoutListener implements LogoutListener {
			@Override
			public void onLogoutBegin() {
				mText.setText("Logging out...");
			}

			@Override
			public void onLogoutFinish() {
				mText.setText("You have logged out! ");
			}
		}

	}

	// @Override
	// protected void onSaveInstanceState(Bundle outState) {
	// if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
	//
	// } else {
	// super.onSaveInstanceState(outState);
	// }
	// }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + requestCode + " resultCode "
				+ resultCode);
		switch (requestCode) {
		/*
		 * if this is the activity result from authorization flow, do a call
		 * back to authorizeCallback Source Tag: login_tag
		 */
		case AUTHORIZE_ACTIVITY_RESULT_CODE: {
			Utility.mFacebook.authorizeCallback(requestCode, resultCode, data);
			Log.d(TAG, "success");
			break;
		}
		}
	}

	public static class FriendListFragment extends ListFragment implements
			LoaderManager.LoaderCallbacks<Cursor> {

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			// TODO Auto-generated method stub
			super.onListItemClick(l, v, position, id);
		}

		FaceAdapter mAdapter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			mAdapter = new FaceAdapter(
					(FaceApp) getActivity().getApplication(), null);
			setListAdapter(mAdapter);
			getLoaderManager().initLoader(0, null, this);
		}

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			return new CursorLoader(getActivity().getApplicationContext(),
					FacebookUser.CONTENT_URI, null, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			mAdapter.changeCursor(cursor);

		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.changeCursor(null);

		}

	}

	public static class FaceAdapter extends CursorAdapter {
		LayoutInflater mInflater;
		ImageRequester mImageRequester;
		int coverWidth;
		int coverHeight;
		int avatarSize;

		public FaceAdapter(FaceApp app, Cursor c) {
			super(app.getApplicationContext(), c, true);
			mInflater = LayoutInflater.from(app.getApplicationContext());
			mImageRequester = new ImageRequester(app);
			coverWidth = app.getResources().getDisplayMetrics().widthPixels;
			coverHeight = app.getResources().getDimensionPixelSize(
					R.dimen.cover_height);
			avatarSize = app.getResources().getDimensionPixelSize(
					R.dimen.avatar_width);
			Log.d(TAG, "coverWidth " + coverWidth + " coverHeight "
					+ coverHeight + " avatarSize " + avatarSize);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			FacebookUser user = FacebookUser.readFromCursor(cursor);
			ViewHolder holder = (ViewHolder) view.getTag();
			holder.name.setText(user.name);
			mImageRequester.submit(holder.cover, user.cover, coverWidth,
					coverHeight);
			Log.d(TAG, "avatar " + user.avatar);
			mImageRequester.submit(holder.avatar, user.avatar, avatarSize,
					avatarSize);
			//holder.avatar.setBackgroundResource(R.drawable.ic_contact_picture);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View newView = mInflater.inflate(R.layout.facecard_template,
					parent, false);
			ViewHolder holder = new ViewHolder(newView);
			newView.setTag(holder);
			return newView;
		}

	}

	public static class ViewHolder {
		public ImageView cover;
		public ImageView avatar;
		public TextView name;

		public ViewHolder(View convertView) {
			cover = (ImageView) convertView.findViewById(R.id.cover);
			avatar = (ImageView) convertView.findViewById(R.id.avatar);
			name = (TextView) convertView.findViewById(R.id.name);
		}
	}

}
