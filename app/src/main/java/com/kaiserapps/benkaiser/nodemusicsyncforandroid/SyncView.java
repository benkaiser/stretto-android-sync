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
import com.koushikdutta.async.http.socketio.JSONCallback;
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
                Log.d("Logging", host_string);
                SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), host_string, new ConnectCallback() {
                    @Override
                    public void onConnectCompleted(Exception ex, SocketIOClient client) {
                        if (ex != null) {
                            ex.printStackTrace();
                            return;
                        }
                        Log.d("CONNECTED", "YAY!");
                        client.emitEvent("fetch_playlists");
                        client.on("playlists", new EventCallback() {
                            @Override
                            public void onEvent(JSONArray argument, Acknowledge acknowledge) {
                                Log.d("GOT RESULT", "YAY!");
                                JSONArray playlists = new JSONArray();
                                try {
                                    playlists = (JSONArray) (((JSONObject) argument.get(0)).get("playlists"));
                                } catch (JSONException e){
                                    e.printStackTrace();
                                }
                                System.out.println("args: " + playlists.toString());
                                Intent intent = new Intent(SyncView.this, playlist_selection.class);
                                intent.putExtra("playlists", playlists.toString());
                                SyncView.this.startActivity(intent);
                            }
                        });
                    }
                });
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sync_view, menu);
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
