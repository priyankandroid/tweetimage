package com.example.tweetimage;

import java.io.File;
import java.net.URL;

import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import twitter4j.media.MediaProvider;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.example.tweetimage.TwitterApp.TwDialogListener;

public class MainActivity extends Activity {

	private static final int PICK_FROM_FILE = 2;
	private String mPath;
	private Uri mImageCaptureUri;
	private ProgressDialog mProgressDialog;
	private TwitterApp mTwitter;
	private CheckBox mTwitterBtn;

	private static final String twitter_consumer_key = "GFp4jjQGzjTxgQAK1j9GA";
	private static final String twitter_secret_key = "c0vDK74P1lPBZ8GJlvIvZkYABlF1NQwKEvJ2lmU0bk8";
	private static final String twitpic_api_key = "b3dacc0dbb2c26a2caf73b2e1be86a05";

	private static final String TAG = "AndroidTwitpic";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTwitterBtn = (CheckBox) findViewById(R.id.twitterCheck);

		mTwitterBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onTwitterClick();
			}
		});

		Button selBtn=(Button) findViewById(R.id.selectbtn);

		selBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSelectClick();
			}
		});

		mTwitter = new TwitterApp(this, twitter_consumer_key,
				twitter_secret_key, twitpic_api_key);

		mTwitter.setListener(mTwLoginDialogListener);

		if (mTwitter.hasAccessToken()) {
			mTwitterBtn.setChecked(true);

			String username = mTwitter.getUsername();
			username = (username.equals("")) ? "Unknown" : username;

			mTwitterBtn.setText("  Twitter (" + username + ")");
			mTwitterBtn.setTextColor(Color.BLACK);
		}

		
	}

	private void onTwitterClick() {
		if (mTwitter.hasAccessToken()) {
			new ImageSender().execute();

		} else {
			mTwitterBtn.setChecked(false);

			mTwitter.authorize();
			//new ImageSender().execute();
		}
	}

	private void onSelectClick() {
		Intent intent = new Intent();
		Log.d(TAG, " msg1");
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);

		startActivityForResult(
				Intent.createChooser(intent, "Complete action using"),
				PICK_FROM_FILE);
	}

	private final TwDialogListener mTwLoginDialogListener = new TwDialogListener() {
		@Override
		public void onComplete(String value) {
			String username = mTwitter.getUsername();
			username = (username.equals("")) ? "No Name" : username;

			mTwitterBtn.setText("  Twitter  (" + username + ")");
			mTwitterBtn.setChecked(true);
			mTwitterBtn.setTextColor(Color.WHITE);

			Toast.makeText(MainActivity.this,
					"Connected to Twitter as " + username, Toast.LENGTH_LONG)
					.show();
		}

		@Override
		public void onError(String value) {
			mTwitterBtn.setChecked(false);

			Toast.makeText(MainActivity.this, "Twitter connection failed",
					Toast.LENGTH_LONG).show();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void tweetClick(View v) {
		new ImageSender().execute();

	}

	private class ImageSender extends AsyncTask<URL, Integer, Long> {
		private String url;

		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(MainActivity.this, "",
					"Sending image...", true);

			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		protected Long doInBackground(URL... urls) {

			long result = 0;
			
			if (!mTwitter.hasAccessToken()) {
				mTwitterBtn.setChecked(false);

				mTwitter.authorize();

			} 

			// TwitterSession twitterSession = new
			// TwitterSession(SendImageActivity.this);
			AccessToken accessToken = mTwitter.getAccessToken();

			Configuration conf = new ConfigurationBuilder()
					.setOAuthConsumerKey(twitter_consumer_key)
					.setOAuthConsumerSecret(twitter_secret_key)
					.setOAuthAccessToken(accessToken.getToken())
					.setOAuthAccessTokenSecret(accessToken.getTokenSecret())
					.setMediaProviderAPIKey(twitpic_api_key).build();

			ImageUpload upload = new ImageUploadFactory(conf)
					.getInstance(MediaProvider.TWITPIC);

			Log.d(TAG, "Start sending image...");

			try {

				url = upload.upload(new File(mPath));
				Log.d(TAG, "Image uploaded, Twitpic url is " + url);
				mTwitter.updateStatus(url.toString());
				result = 1;

			} catch (Exception e) {
				Log.e(TAG, "Failed to send image");

				e.printStackTrace();
			}

			return result;

		}

		protected void onProgressUpdate(Integer... progress) {
		}

		protected void onPostExecute(Long result) {
			mProgressDialog.cancel();

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;

		if (requestCode == PICK_FROM_FILE) {
			mImageCaptureUri = data.getData();
			mPath = getRealPathFromURI(mImageCaptureUri); // from Gallery
		}

	}

	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);

		if (cursor == null)
			return null;

		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

		cursor.moveToFirst();

		return cursor.getString(column_index);
	}
}
