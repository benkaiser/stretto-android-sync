package com.kaiserapps.benkaiser.nodemusicsyncforandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SyncView extends Activity {

    EditText host_ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // show view
        setContentView(R.layout.activity_sync_view);

        // add the click listener
        Button send = (Button) findViewById(R.id.getplaylists);
        host_ip = (EditText) findViewById(R.id.hostIP);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handle click
                String host_string = host_ip.getText().toString();
                // if they left off the protocol, add it
                if(!host_string.startsWith("http://")){
                    host_string = "http://".concat(host_string);
                }
                // save the url
                SyncApplication syncApp = (SyncApplication) getApplication();
                syncApp.setUrl(host_string);
                Log.d("Logging", host_string);
                SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), host_string, new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, SocketIOClient client) {
                        if (ex != null) {
                            ex.printStackTrace();
                            return;
                        }
                        Log.d("CONNECTED", "YAY!");
                        // fetch the data
                        client.emitEvent("fetch_playlists");
                        client.emitEvent("fetch_songs");
                        // respond to the playlists
                        client.on("playlists", new EventCallback() {
                            @Override
                            public void onEvent(JSONArray argument, Acknowledge acknowledge) {
                                JSONArray playlists = new JSONArray();
                                try {
                                    playlists = (JSONArray) (((JSONObject) argument.get(0)).get("playlists"));
                                } catch (JSONException e){
                                    e.printStackTrace();
                                }
                                System.out.println("Playlists: " + playlists.length());
                                // set the playlists
                                SyncApplication syncApp = (SyncApplication) getApplication();
                                syncApp.setPlaylists(playlists);
                                // load the next activity
                                Intent intent = new Intent(SyncView.this, PlaylistSelectionView.class);
                                SyncView.this.startActivity(intent);
                            }
                        });
                        // respond to the songs, but don't wait for them
                        client.on("songs", new EventCallback() {
                            @Override
                            public void onEvent(JSONArray argument, Acknowledge acknowledge) {
                                JSONArray songs = new JSONArray();
                                try {
                                    songs = (JSONArray) (((JSONObject) argument.get(0)).get("songs"));
                                } catch (JSONException e){
                                    e.printStackTrace();
                                }
                                System.out.println("Songs: " + songs.length());
                                // set the songs
                                SyncApplication syncApp = (SyncApplication) getApplication();
                                syncApp.setSongs(songs);
                            }
                        });
                    }
                });
            }
        });
    }


}
