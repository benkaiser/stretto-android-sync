package com.kaiserapps.benkaiser.nodemusicsyncforandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.ConnectCallback;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.app.PendingIntent.getActivity;

public class SyncView extends Activity {

    final private static String PREF_KEY = "recentConnections";
    EditText host_ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // show view
        setContentView(R.layout.activity_sync_view);

        host_ip = (EditText) findViewById(R.id.hostIP);
        Button send = (Button) findViewById(R.id.getplaylists);
        Button recent = (Button) findViewById(R.id.recentBtn);

        // add the click listener
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handle click
                String host_string = host_ip.getText().toString();
                // if they left off the protocol, add it
                if (!host_string.startsWith("http://")) {
                    host_string = "http://".concat(host_string);
                }

                // save the url
                SyncApplication syncApp = (SyncApplication) getApplication();
                syncApp.setUrl(host_string);

                Log.d("Logging", host_string);
                final String test = host_string;

                SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), host_string, new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, SocketIOClient client) {
                        if (ex != null) {
                            ex.printStackTrace();
                            AlertDialog.Builder builder = new AlertDialog.Builder(SyncView.this);
                                builder.setTitle("Error")
                                        .setMessage("Host not found.");
                            builder.create().show();

                            return;
                        }

                        Log.d("CONNECTED", "YAY!");

                        // Add to recent connections
                        addToRecentConnections(test);

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
                                } catch (JSONException e) {
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
                                } catch (JSONException e) {
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

        recent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final Context context = SyncView.this;

                final List<String> connections = getRecentConnections();

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Recent connections");

                if(connections != null) {
                    builder.setItems(connections.toArray(new CharSequence[connections.size()]),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String con = connections.get(which);
                                    host_ip.setText(con);
                                }
                            });
                } else {
                    builder.setMessage("No recent connections");
                }

                builder.create().show();
            }
        });

    }

    public void addToRecentConnections(final String connection) {
        final List<String> connections = getRecentConnections();

        if(connections == null || !connections.contains(connection)) {
            SharedPreferences prefs = SyncView.this.getSharedPreferences("NodeMusicSyncApp", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            List<String> list = new ArrayList<String>();
            list.add(connection);

            editor.putString(PREF_KEY, TextUtils.join(",", list));
            editor.commit();
        }
    }

    public List<String> getRecentConnections() {
        SharedPreferences prefs = SyncView.this.getSharedPreferences("NodeMusicSyncApp", Context.MODE_PRIVATE);
        String serialized = prefs.getString(PREF_KEY, null);

        if(serialized != null) {
            List<String> list = Arrays.asList(TextUtils.split(serialized, ","));
            return list;
        } else {
            return null;
        }

    }

}
