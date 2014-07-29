package com.kaiserapps.benkaiser.nodemusicsyncforandroid;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class playlist_selection extends Activity {

    JSONArray playlists;
    ArrayList<CheckBox> chkboxarr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_selection);

        // init the checkbox array
        chkboxarr = new ArrayList<CheckBox>();

        // get a handle on the table
        TableLayout tl = (TableLayout)findViewById(R.id.playlist_table);

        // load the passed playlists
        playlists = new JSONArray();
        try {
            playlists = new JSONArray(getIntent().getStringExtra("playlists"));
        } catch(JSONException e){
            e.printStackTrace();
        }

        // draw the table
        try {
            for (int cnt = 0; cnt < playlists.length(); cnt++) {
                JSONObject playlist = (JSONObject) playlists.get(cnt);
                String title = (String) playlist.get("title");
                // create the table row
                TableRow tr = new TableRow(this);
                TextView titleText = new TextView(this);
                titleText.setText(title);
                chkboxarr.add(new CheckBox(this));
                tr.addView(chkboxarr.get(chkboxarr.size()-1));
                tr.addView(titleText);
                tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
            }
        } catch(JSONException e){
            e.printStackTrace();
        }

        // handle click on sync
        Button send = (Button) findViewById(R.id.syncbutton);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int cnt = 0; cnt < playlists.length(); cnt++) {
                    String title = "";
                    try {
                        JSONObject playlist = (JSONObject) playlists.get(cnt);
                        title = (String) playlist.get("title");
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                    if(chkboxarr.get(cnt).isChecked()) {
                        Log.d("Playlist " + title, "checked!");
                    }
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.playlist_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
