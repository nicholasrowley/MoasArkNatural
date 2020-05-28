package com.wordpress.onelifegroupnz.moaarknatural;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlayListData implements Serializable {
    private Map<String, PlaylistEntry> playlistData;

    public PlayListData() {
        playlistData = new LinkedHashMap<>();
    }

    /* Stores filedata in the playlist with the file name as the unique key */
    public void addPlayListEntry(PlaylistEntry entry) {
        playlistData.put(entry.getFileData().getName(), entry);
    }

    /** Removes from the playlist based on the name of the video file */
    public void removePlayListEntry(String entryName) {
        //if (playlistData.get(entryName) != null) {
            playlistData.remove(entryName);
        //}
    }

    public int getSize() {
        return playlistData.size();
    }

    public PlaylistEntry getPlayListEntry(String entryName) {
        return playlistData.get(entryName);
    }

    public PlaylistEntry getPlayListEntry (int index) {
        //TODO reduce overhead and use something like a linkedhashmap
        return playlistData.get(playlistData.keySet().toArray()[index].toString());
    }

    public List<FileDataListing> getVideoListFromPlaylistData() {
        List<FileDataListing> playlistVideoData = new ArrayList<>();

        for (Map.Entry<String, PlaylistEntry> entry : playlistData.entrySet()) {
            playlistVideoData.add(entry.getValue().getFileData());
        }

        return playlistVideoData;
    }
}
