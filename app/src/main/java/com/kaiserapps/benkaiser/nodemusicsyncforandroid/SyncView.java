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
import com.koushikdutta.async.http.socketio.EventEmitter;
import com.koushikdutta.async.http.socketio.SocketIOClient;

import io.socket.client.*;
import io.socket.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.app.PendingIntent.getActivity;

public class SyncView extends Activity {

    final private static String PREF_KEY = "recentConnections";
    EditText host_ip;
    Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity currentActivity = this;

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
                if (!host_string.startsWith("http://") && !host_string.startsWith("https://")) {
                    host_string = "http://".concat(host_string);
                }

                // save the url
                SyncApplication syncApp = (SyncApplication) getApplication();
                syncApp.setUrl(host_string);

                Log.d("Logging", host_string);
                final String test = host_string;

                try {
                    // options for connection
                    IO.Options opts = new IO.Options();
                    opts.reconnection = false;

                    // initialise the socket
                    socket = IO.socket(host_string, opts);

                    // emit the requests for data
                    socket.emit("fetch_playlists");
                    socket.emit("fetch_songs");

                    // add handlers for the data
                    socket.once("playlists", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            JSONArray playlists = new JSONArray();
                            try {
                                playlists = (JSONArray) (((JSONObject) args[0]).get("playlists"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Playlists: " + playlists.length());
                            // set the playlists
                            SyncApplication syncApp = (SyncApplication) getApplication();
                            syncApp.setPlaylists(playlists);
                            // load the next activity, don't wait for the songs
                            Intent intent = new Intent(SyncView.this, PlaylistSelectionView.class);
                            SyncView.this.startActivity(intent);

                            Log.d("STARTED", "Loading Platlist View");
                        }
                    });

                    socket.once("songs", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            JSONArray songs = new JSONArray();
                            try {
                                songs = (JSONArray) (((JSONObject) args[0]).get("songs"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Songs: " + songs.length());
                            // set the songs
                            SyncApplication syncApp = (SyncApplication) getApplication();
                            syncApp.setSongs(songs);
                        }
                    });

                    // on a successful connection, save the connection string to the history
                    socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            addToRecentConnections(test);
                        }
                    });

                    // on a successful connection, save the connection string to the history
                    socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            currentActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(SyncView.this);
                                    builder.setTitle("Error")
                                            .setMessage("Failed to connect to server.");
                                    builder.create().show();
                                }
                            });
                        }
                    });

                    // start the connection
                    socket.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    AlertDialog.Builder builder = new AlertDialog.Builder(SyncView.this);
                    builder.setTitle("Error")
                            .setMessage("Host not found.");
                    builder.create().show();
                }
            }
        });

        recent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final Context context = SyncView.this;

                final ArrayList<String> connections = getRecentConnections();

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
        final ArrayList<String> connections = getRecentConnections();

        if(!connections.contains(connection)) {
            SharedPreferences prefs = SyncView.this.getSharedPreferences("NodeMusicSyncApp", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            connections.add(0, connection);

            Log.d("Added: ", connection);

            editor.putString(PREF_KEY, TextUtils.join(",", connections));
            editor.commit();
        }
    }

    public ArrayList<String> getRecentConnections() {
        SharedPreferences prefs = SyncView.this.getSharedPreferences("NodeMusicSyncApp", Context.MODE_PRIVATE);
        String serialized = prefs.getString(PREF_KEY, null);

        if(serialized != null) {
            ArrayList<String> list = new ArrayList<String>(Arrays.asList(TextUtils.split(serialized, ",")));
            return list;
        } else {
            return new ArrayList<String>();
        }

    }

}
