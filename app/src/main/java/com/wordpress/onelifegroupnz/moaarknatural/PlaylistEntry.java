package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;

import java.io.Serializable;
import java.util.List;

//TODO check integrity of playlist entry when user tries to open it.
public class PlaylistEntry implements Serializable {
    private FileDataListing playlistMediaData;
    private String videoPath;

    public PlaylistEntry(FileDataListing entry, String videoType) {
        playlistMediaData = entry;
        videoPath = videoType;
    }

    public FileDataListing getFileData() {
        return playlistMediaData;
    }

    public String getVideoType() {
        return videoPath;
    }
}
