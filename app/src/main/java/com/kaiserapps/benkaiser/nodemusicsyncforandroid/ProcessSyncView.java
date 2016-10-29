package com.kaiserapps.benkaiser.strettoandroidsync;

import android.app.Activity;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class ProcessSyncView extends Activity {

    private final int MAX_PROGRESS = 100;

    // view element handles
    ProgressBar progressBar;
    TextView sync_update;
    // existing data
    SyncApplication app;
    JSONArray marked_playlists;
    JSONArray songs;
    String url;
    String sync_folder = "stretto";
    String output_dir;
    // generated data
    String[] syncSongs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_sync_view);

        // get handles
        sync_update = (TextView) findViewById(R.id.sync_update);
        progressBar = (ProgressBar) findViewById(R.id.sync_progress);
        // get data
        app = (SyncApplication) getApplication();
        marked_playlists = app.getMarked_playlists();
        songs = app.getSongs();
        url = app.getUrl();

        output_dir = getMusicStorageDir(sync_folder).getAbsolutePath();

        sync_update.setText("Working out songs to download...");
        syncSongs = getUniqueSongs();
        // start the download
        downloadSongs(0);
    }

    private String[] getUniqueSongs(){
        LinkedHashSet<String> songs = new LinkedHashSet<String>();
        // get the marked playlists
        try {
            for (int x = 0; x < marked_playlists.length(); x++) {
                JSONArray songs_in_list = (JSONArray)((JSONObject) marked_playlists.get(x)).get("songs");
                for(int y = 0; y < songs_in_list.length(); y++){
                    songs.add((((JSONObject)songs_in_list.get(y)).get("_id")).toString());
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
        return songs.toArray(new String[songs.size()]);
    }

    private void downloadSongs(final int index){
        if(index < syncSongs.length){
            // update UI
            String song_id = syncSongs[index];
            JSONObject extended_song_info = getSongFromId(song_id);
            if(extended_song_info != null) {
                try {
                    sync_update.setText("Fetching song: " + extended_song_info.get("title"));
                } catch (JSONException e){
                    e.printStackTrace();
                }
            } else {
                sync_update.setText("");
            }
            progressBar.setProgress(MAX_PROGRESS/syncSongs.length*index);
            // run the download task
            new DownloadSongAsync().execute(new Integer(index));
        } else {
            sync_update.setText("Download Finished");
            progressBar.setProgress(MAX_PROGRESS);
            finishDownload();
        }
    }

    public File getMusicStorageDir(String filename) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), sync_folder);
        // make the parent if it isn't there already
        file.mkdirs();
        return file;
    }

    public JSONObject getSongFromId(String id){
        JSONObject retObj = null;
        try{
            for(int cnt = 0; cnt < songs.length(); cnt++){
                JSONObject songObj = (JSONObject) songs.get(cnt);
                if(((String)songObj.get("_id")).equals(id)){
                    retObj = songObj;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            return retObj;
        }
    }

    private void finishDownload(){
        sync_update.setText("Adding music to local library...");
        new AddContentToResolver().execute();
    }

    private void assignPlaylists(){
        sync_update.setText("Adding playlists to local library...");
        new AssignPlaylistTask().execute();
    }

    private void finished(){
        sync_update.setText("Finished");
    }

    private class DownloadSongAsync extends AsyncTask<Integer, Boolean, Boolean>{

        int index;
        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                index = params[0];
                // get the required info for download
                String song_id = syncSongs[index];
                String url_of_file = url + "/songs/" + song_id;
                String write_to_file = output_dir + "/" + song_id;
                try {
                    JSONObject extended_song_info = getSongFromId(song_id);
                    if(extended_song_info == null){
                        return true;
                    }
                    if(extended_song_info.has("location")){
                        String location = (String) extended_song_info.get("location");
                        write_to_file = output_dir + location;
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                // download the song
                File out_file = new File(write_to_file);
                if(out_file.exists()){
                    // skip, it has already been downloaded
                    return true;
                } else {
                    // download the file
                    Future<File> result = AsyncHttpClient.getDefaultInstance().executeFile(new AsyncHttpGet(url_of_file), write_to_file, null);
                    Log.d("STATUS", "File is available at: " + result.get().getAbsolutePath());
                }
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            downloadSongs(index + 1);
        }
    }

    private class AddContentToResolver extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                for (int cnt = 0; cnt < syncSongs.length; cnt++) {
                    String song_id = syncSongs[cnt];
                    JSONObject song_info = getSongFromId(song_id);
                    if(song_info == null){
                        continue;
                    }
                    // where did we save the file
                    String file_location = output_dir + "/" + song_id;
                    if(song_info.has("location")){
                        String location = (String) song_info.get("location");
                        file_location = output_dir + location;
                    }
                    // build the content values
                    ContentValues values = new ContentValues();

                    values.put(MediaStore.Audio.Media.TITLE, song_info.get("title").toString());
                    values.put(MediaStore.Audio.Media.ARTIST, song_info.get("display_artist").toString());
                    values.put(MediaStore.Audio.Media.ALBUM, song_info.get("album").toString());
                    values.put(MediaStore.Audio.Media.DURATION, Float.parseFloat(song_info.get("duration").toString()) * 1000);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    values.put(MediaStore.Audio.Media.DATA, file_location);
                    Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
                    if(uri != null) {
                        long id = Long.parseLong(uri.getLastPathSegment());
                        Log.d("ADDED TRACK", "Title: " + song_info.get("title").toString() + " id: " + id);
                    }
                }
            } catch (JSONException e){
                e.printStackTrace();
            } finally {
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            assignPlaylists();
        }
    }

    private class AssignPlaylistTask extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                for (int cnt = 0; cnt < marked_playlists.length(); cnt++) {
                    JSONObject this_list = (JSONObject) marked_playlists.get(cnt);
                    long playlist_id = getOrCreatePlaylist(this_list.get("title").toString());
                    JSONArray songs_in_list = (JSONArray)this_list.get("songs");
                    for(int cnt2 = 0; cnt2 < songs_in_list.length(); cnt2++) {
                        String song_id = (((JSONObject)songs_in_list.get(cnt2)).get("_id")).toString();
                        JSONObject song = getSongFromId(song_id);
                        if(song != null) {
                            addToPlaylist(playlist_id, output_dir + song.get("location").toString());
                        }
                    }
                }
            } catch (JSONException e){
                e.printStackTrace();
            } finally {
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            finished();
        }
    }

    private long getOrCreatePlaylist(String name){
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists._ID },
                MediaStore.Audio.Playlists.NAME+ "=? ",
                new String[] { name }, null);
        if(cursor != null && cursor.moveToFirst()){
            // found existing playlist
            return cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
        } else {
            // create a new playlist
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.NAME, name);
            Uri uri = getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
            return Long.parseLong(uri.getLastPathSegment());
        }
    }

    private boolean addToPlaylist(long playlist_id, String data_location){
        // get the last play order
        Uri playlist_uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlist_id);
        String[] playlist_projection = new String[] { MediaStore.Audio.Playlists.Members.PLAY_ORDER };
        Cursor cursor = getContentResolver().query(playlist_uri, playlist_projection, null, null, null);
        int base = 0;
        if (cursor.moveToLast())
            base = cursor.getInt(0) + 1;
        cursor.close();

        // get the id of the song
        Uri song_uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] song_projection = new String[] { MediaStore.Audio.Media._ID };
        cursor = getContentResolver().query(song_uri, song_projection,
                MediaStore.Audio.Media.DATA + "=? ",
                new String[] { data_location }, null);
        if (cursor != null && cursor.moveToFirst()) {
            // successfully found the song
            int song_id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            // check if the song is already in the playlist
            cursor = getContentResolver().query(playlist_uri, playlist_projection,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=? ",
                    new String[] { Integer.toString(song_id) }, null);
            if(cursor == null || !cursor.moveToFirst()) {
                // add the song to the playlist
                ContentValues value = new ContentValues(2);
                value.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Integer.valueOf(base + 1));
                value.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, song_id);
                getContentResolver().insert(playlist_uri, value);
                return true;
            } else {
                // already in playlist
                return false;
            }
        } else {
            Log.e("ERROR", "Unable to find song for location: " + data_location);
            return false;
        }

    }
}
