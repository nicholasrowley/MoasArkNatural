package com.wordpress.onelifegroupnz.moaarknatural;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlayListData implements Serializable {
    private Map<String, PlaylistEntry> playlistData;
    private List<String> invalidEntries;
    private boolean placeholder21;

    public PlayListData() {
        playlistData = new LinkedHashMap<>();
        invalidEntries = new ArrayList<>();
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

    /* Fetches playlist data using a string set and updated information for backwards compatibility purposes. */
    public void updatePlaylistData(Set<String> list, List<FileDataListing> updatedDanceVideoList, List<FileDataListing> updatedFoodVideoList) {
        invalidEntries.clear();
        for(String entry : list) {
            boolean validEntry = false;

            //checks if the entry exists in the dance video gallery
            for (FileDataListing danceVideo : updatedDanceVideoList) {
                if (danceVideo.getName().equals(entry)) {
                    addPlayListEntry(new PlaylistEntry(danceVideo, GlobalAppData.DANCEVIDEOPATH));
                    validEntry = true;
                }
            }

            //checks if the entry exists in the food video gallery
            for (FileDataListing foodVideo : updatedFoodVideoList) {
                if (foodVideo.getName().equals(entry)) {
                    addPlayListEntry(new PlaylistEntry(foodVideo, GlobalAppData.FOODVIDEOPATH));
                    validEntry = true;
                }
            }

            //if entry is invalid then mark as false
            if(!validEntry){
                invalidEntries.add(entry);
            }
        }
        //invalidEntries.add("testinvalids");
    }

    public List<String> getInvalidEntries() {
        return invalidEntries;
    }
}
