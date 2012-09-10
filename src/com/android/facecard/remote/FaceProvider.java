/*
 * Copyright (C) 2008 Romain Guy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.facecard.remote;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;
import android.net.Uri;
import android.text.TextUtils;
import android.app.SearchManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.android.facecard.data.FacebookUserData.FacebookUser;

public class FaceProvider extends ContentProvider {
	private static final String LOG_TAG = "FaceProvider";

	private static final String DATABASE_NAME = "faces.db";
	private static final int DATABASE_VERSION = 3;

	private static final int SEARCH = 1;
	private static final int FacebookUsers = 2;
	private static final int FacebookUser_ID = 3;
	private static final int RAWQUERY = 4;
	public static final String AUTHORITY = "facecards";
	public static final Uri RAWQUERY_URI = Uri.parse("content://" + AUTHORITY
			+ "/rawquery");

	private static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
				SEARCH);
		URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY
				+ "/*", SEARCH);
		URI_MATCHER.addURI(AUTHORITY, FacebookUser.TABLE, FacebookUsers);
		URI_MATCHER.addURI(AUTHORITY, FacebookUser.TABLE + "/#",
				FacebookUser_ID);
		URI_MATCHER.addURI(AUTHORITY, "rawquery", RAWQUERY);
	}

	private static final HashMap<String, String> SUGGESTION_PROJECTION_MAP;
	static {
		SUGGESTION_PROJECTION_MAP = new HashMap<String, String>();
		SUGGESTION_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
				FacebookUser.NAME + " AS "
						+ SearchManager.SUGGEST_COLUMN_TEXT_1);

		SUGGESTION_PROJECTION_MAP.put(
				SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, FacebookUser._ID
						+ " AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
		SUGGESTION_PROJECTION_MAP.put(FacebookUser._ID, FacebookUser._ID);
	}

	private SQLiteOpenHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		boolean rawquery = false;
		;
		switch (URI_MATCHER.match(uri)) {
		case SEARCH:
			qb.setTables(FacebookUser.TABLE);
			String query = uri.getLastPathSegment();
			if (!TextUtils.isEmpty(query)) {
				qb.appendWhere(FacebookUser.NAME + " LIKE ");
				qb.appendWhereEscapeString('%' + query + '%');
			}
			qb.setProjectionMap(SUGGESTION_PROJECTION_MAP);
			break;
		case FacebookUsers:
			qb.setTables(FacebookUser.TABLE);
			break;
		case FacebookUser_ID:
			qb.setTables(FacebookUser.TABLE);
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;
		case RAWQUERY:
			rawquery = true;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = FacebookUser.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		if (rawquery) {
			return db.rawQuery(selection, selectionArgs);
		}
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	private static final String TAG = "FaceProvider";

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int change = 0;
		db.beginTransaction();
		try {
			for (ContentValues v : values) {

				Log.d(TAG, "name " + v.getAsString(FacebookUser.NAME));
				db.replace(FacebookUser.TABLE, null, v);
				change++;
				db.yieldIfContendedSafely();
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return change;
	}

	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case FacebookUsers:
			return "vnd.android.cursor.dir/vnd.org.curiouscreature.provider.fusers";
		case FacebookUser_ID:
			return "vnd.android.cursor.FacebookUser/vnd.org.curiouscreature.provider.fusers";
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	public Uri insert(Uri uri, ContentValues initialValues) {
		ContentValues values;

		if (initialValues != null) {
			values = initialValues;
		} else {
			values = new ContentValues();
		}

		if (URI_MATCHER.match(uri) != FacebookUsers) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		final long rowId = db.replace(FacebookUser.TABLE, FacebookUser.NAME,
				values);
		if (rowId > 0) {
			Uri insertUri = ContentUris.withAppendedId(
					FacebookUser.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			return insertUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		int count;
		switch (URI_MATCHER.match(uri)) {
		case FacebookUsers:
			count = db.delete(FacebookUser.TABLE, selection, selectionArgs);
			break;
		case FacebookUser_ID:
			String segment = uri.getPathSegments().get(1);
			count = db.delete(FacebookUser.TABLE, FacebookUser._ID
					+ "="
					+ segment
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection
							+ ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		Context mContext;

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + FacebookUser.TABLE + " ("
					+ FacebookUser._ID + " INTEGER PRIMARY KEY, "
					+ FacebookUser.NAME + " TEXT NOT NULL UNIQUE, "
					+ FacebookUser.FIRSTNAME + " TEXT, "
					+ FacebookUser.LASTNAME + " TEXT, " + FacebookUser.LINK
					+ " TEXT," + FacebookUser.BIRTHDAY + " TEXT,"
					+ FacebookUser.AVATAR + " TEXT," + FacebookUser.COVER
					+ " TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS " + FacebookUser.TABLE);
			onCreate(db);
		}
	}
}
