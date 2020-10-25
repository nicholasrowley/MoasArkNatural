package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;

import java.io.Serializable;

/** Stores a single entry in a playlist */
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
