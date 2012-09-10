package com.android.facecard.data;

import com.android.facecard.remote.FaceProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class FacebookUserData {
	public FacebookUser[] data;

	public static class FacebookUser implements BaseColumns {
		public static String TABLE = "faceUser";
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ FaceProvider.AUTHORITY + "/" + TABLE);
		public static String DEFAULT_SORT_ORDER = "name asc";
		public String id;
		public String name;
		public String first_name;
		public String last_name;
		public String link;
		public String birthday;
		public Location location;
		public String avatar;
		public String cover;
		public static final String NAME = "name";
		public static final String FIRSTNAME = "first_name";
		public static final String LASTNAME = "last_name";
		public static final String LINK = "link";
		public static final String BIRTHDAY = "birthday";
		public static final String AVATAR = "avatar";
		public static final String COVER = "cover";

		public static class Location {
			String id;
			String name;
		}

		public static FacebookUser readFromCursor(Cursor cursor) {
			FacebookUser user = new FacebookUser();
			user.id = cursor.getString(cursor.getColumnIndexOrThrow(_ID));
			user.name = cursor.getString(cursor.getColumnIndexOrThrow(NAME));
			user.first_name = cursor.getString(cursor
					.getColumnIndexOrThrow(FIRSTNAME));
			user.last_name = cursor.getString(cursor
					.getColumnIndexOrThrow(LASTNAME));
			user.link = cursor.getString(cursor.getColumnIndexOrThrow(LINK));
			user.birthday = cursor.getString(cursor
					.getColumnIndexOrThrow(BIRTHDAY));
			user.avatar = cursor
					.getString(cursor.getColumnIndexOrThrow(AVATAR));
			user.cover = cursor.getString(cursor.getColumnIndexOrThrow(COVER));
			return user;
		}

		private static final String TAG = "FacebookUser";

		public ContentValues getInsertValues() {
			ContentValues values = new ContentValues();
			values.put(_ID, id);
			values.put(NAME, name);
			values.put(FIRSTNAME, first_name);
			values.put(LASTNAME, last_name);
			values.put(LINK, link);
			values.put(BIRTHDAY, birthday);
			values.put(COVER, cover);
			values.put(AVATAR, avatar);

			return values;
		}
	}
}