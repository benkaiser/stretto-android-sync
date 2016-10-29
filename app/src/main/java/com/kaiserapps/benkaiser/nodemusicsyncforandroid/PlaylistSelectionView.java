package com.kaiserapps.benkaiser.strettoandroidsync;

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


public class PlaylistSelectionView extends Activity {

    JSONArray playlists;
    ArrayList<CheckBox> chkboxarr;
    private SyncApplication syncState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_selection);

        // init the checkbox array
        chkboxarr = new ArrayList<CheckBox>();

        // get a handle on the table
        TableLayout tl = (TableLayout)findViewById(R.id.playlist_table);

        // load the playlists
        syncState = (SyncApplication) getApplication();
        playlists = syncState.getPlaylists();

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
                JSONArray marked_lists = new JSONArray();
                for (int cnt = 0; cnt < playlists.length(); cnt++) {
                    String title = "";
                    try {
                        JSONObject playlist = (JSONObject) playlists.get(cnt);
                        title = (String) playlist.get("title");
                        if(chkboxarr.get(cnt).isChecked()) {
                            Log.d("Playlist " + title, " marked for syncing!");
                            marked_lists.put((JSONObject)playlists.get(cnt));
                        }
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
                syncState.setMarked_playlists(marked_lists);
                Intent intent = new Intent(PlaylistSelectionView.this, ProcessSyncView.class);
                PlaylistSelectionView.this.startActivity(intent);
            }
        });
    }

}
