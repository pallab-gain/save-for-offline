package jonas.tool.saveForOffline;
import android.app.*;
import android.os.*;
import android.webkit.*;
import android.view.*;
import android.content.*;
import android.net.*;
import android.widget.*;
import android.database.sqlite.*;
import android.util.*;
import java.io.*;
import android.preference.*;

public class ViewActivity extends Activity
{
	private Intent incomingIntent;
	private String title;
	private WebView webview;
	private WebView.HitTestResult result;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.view_activity);
		
		
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		incomingIntent = getIntent();
		actionBar.setSubtitle(incomingIntent.getStringExtra("title"));
		title = incomingIntent.getStringExtra("title");
		
		setProgressBarIndeterminateVisibility(true);
		
		webview = (WebView) findViewById(R.id.webview);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String ua = sharedPref.getString("user_agent", "mobile");
		
		registerForContextMenu(webview);
		

		if (ua.equals("desktop")) {
			webview.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.517 Safari/537.36");
			Log.d("saveActivity", "using desktop ua");
		}
		if (ua.equals("ipad")) {
			webview.getSettings().setUserAgentString("todo:iPad ua");
			Log.w("saveActivity", "iPad ua not implemented yet");
		}
		webview.getSettings().setLoadWithOverviewMode(true);
		webview.getSettings().setUseWideViewPort(true);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setBuiltInZoomControls(true);
		webview.getSettings().setDisplayZoomControls(false);
        
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			//kitkat and up saves webarchives in a different format, which we can read easisly
			setProgressBarIndeterminateVisibility(false);
			webview.loadUrl("file://" + incomingIntent.getStringExtra("fileLocation"));
		} else loadWebView();
    }


	private void loadWebView()
	{
		try {
			FileInputStream is = new FileInputStream(incomingIntent.getStringExtra("fileLocation"));
            //InputStream is = getAssets().open("TestHtmlArchive.xml");
            WebArchiveReader wr = new WebArchiveReader() {
                void onFinished(WebView v) {
                    // we are notified here when the page is fully loaded.
                    continueWhenLoaded(v);
                }
            };
            // To read from a file instead of an asset, use:
            // FileInputStream is = new FileInputStream(fileName);
            if (wr.readWebArchive(is)) {
                wr.loadToWebView(webview);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		super.onStart();
	}
	
	void continueWhenLoaded(WebView webView) {
        
        // Any other code we need to execute after loading a page from a WebArchive...
		setProgressBarIndeterminateVisibility(false);
    }
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.ic_action_settings:
				Intent settings = new Intent(getApplicationContext(), Preferences.class);
				startActivityForResult(settings, 1);

				return true;
			
			case R.id.action_open_in_external:
				
				Intent incomingIntent = getIntent();
				
				
				Uri uri = Uri.parse(incomingIntent.getStringExtra("orig_url"));
				Intent startBrowserIntent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(startBrowserIntent);

				return true;
				
			case R.id.ic_action_about:
				Intent intent = new Intent(this, FirstRunDialog.class);
				startActivity(intent);
				return true;
				
			case R.id.action_delete:
				
				AlertDialog.Builder build;
				build = new AlertDialog.Builder(ViewActivity.this);
				build.setTitle("Delete ?");
				build.setMessage(title);
				build.setPositiveButton("Delete",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog,
											int which) {
							

							DbHelper mHelper = new DbHelper(ViewActivity.this);
							SQLiteDatabase dataBase = mHelper.getWritableDatabase();
							Intent incomingIntent2 = getIntent();

							dataBase.delete(
								DbHelper.TABLE_NAME,
								DbHelper.KEY_ID + "=" + incomingIntent2.getStringExtra("id"), null);

							Log.w("viewActivity", "Deleting !!");

							Toast.makeText(
								getApplicationContext(),
								incomingIntent2.getStringExtra("title") + " is deleted.", Toast.LENGTH_LONG).show();

							finish();
						}
					});

				build.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog,
											int which) {
							dialog.cancel();
						}
					});
				AlertDialog alert = build.create();
				alert.show();
				
				

				return true;
				
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {

	
		result = webview.getHitTestResult();
		
		if (result.getType() == WebView.HitTestResult.ANCHOR_TYPE || result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
			// Menu options for a hyperlink.
			//set the header title to the link url
			menu.setHeaderTitle(result.getExtra());
			menu.add(3, 3, 3, "Save Link");
			menu.add(4, 4, 4, "Share Link");
			menu.add(5, 5, 5, "Open Link");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == 5) {
			Uri uri = Uri.parse(result.getExtra());
			Intent startBrowserIntent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(startBrowserIntent);
		} else if (item.getItemId() == 4) {
			Uri uri = Uri.parse(result.getExtra());
			Intent intent = new Intent(Intent.ACTION_SEND, uri);
			Intent chooser = Intent.createChooser(intent, "Share link via");
			startActivity(chooser);
		} else if (item.getItemId() == 3) {
			Intent intent = new Intent(this, SaveActivity.class);
			intent.putExtra("origurl", result.getExtra());
			startActivity(intent);
		}
		return super.onContextItemSelected(item);
		
	}
	
}