package com.hkurokawa.example.multiloader;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.hkurokawa.multiloader.OnCreateLoader;


public class MainActivity extends Activity {

    public static final int LOADER_ID_LIST1 = 0;
    private static final String TAG_NAME = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnCreateLoader(LOADER_ID_LIST1)
    public void onCreateList1Loader(int id, Bundle args) {
        Log.i(TAG_NAME, "Creating a list loader.");
    }

//    @Override
//    public Loader<Object> onCreateLoader(int id, Bundle args) {
//        return null;
//    }
//
//    @Override
//    public void onLoadFinished(Loader<Object> loader, Object data) {
//
//    }
//
//    @Override
//    public void onLoaderReset(Loader<Object> loader) {
//
//    }
}
