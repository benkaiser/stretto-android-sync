package com.kaiserapps.benkaiser.nodemusicsyncforandroid;

import android.app.Application;

import org.json.JSONArray;

/**
 * Created by benkaiser on 31/07/14.
 */
public class SyncApplication extends Application {

    private JSONArray playlists;
    private JSONArray marked_playlists;
    private JSONArray songs;
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONArray getSongs() {
        return songs;
    }

    public void setSongs(JSONArray songs) {
        this.songs = songs;
    }

    public SyncApplication() {
        this.playlists = new JSONArray();
        this.marked_playlists = new JSONArray();
    }

    public JSONArray getPlaylists() {
        return playlists;
    }

    public void setPlaylists(JSONArray playlists) {
        this.playlists = playlists;
    }

    public JSONArray getMarked_playlists() {
        return marked_playlists;
    }

    public void setMarked_playlists(JSONArray marked_playlists) {
        this.marked_playlists = marked_playlists;
    }



}
