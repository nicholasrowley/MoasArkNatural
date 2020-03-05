package com.wordpress.onelifegroupnz.moaarknatural;

import android.media.MediaMetadataRetriever;

import java.io.Serializable;
import java.util.HashMap;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * This is a simple data object for storing information on Files that have been fetched from web server.
 * Created by Nicholas Rowley on 03/02/2020.
 */
public class FileDataListing implements Serializable {
    private String name;
    private String filePath;
    private String folderPath;
    private String dateLastModified;
    private String timeLastModified;
    private String viewStatsName; //log entry for stats
    private long duration; //duration in milliseconds
    private static final long serialVersionUID = 1L; //required for Serializable

    public FileDataListing(String fileName, String httpFolderPath, String dateDDMMYYYY, String timeAmPm) {
        name = fileName.replaceFirst("[.][^.]+$", ""); //remove the file extension
        filePath = httpFolderPath + fileName;
        folderPath = httpFolderPath;
        dateLastModified = dateDDMMYYYY;
        timeLastModified = timeAmPm;
        viewStatsName = "views_" + name.replaceAll(" ", "_").toLowerCase();
    }

    /* Get methods for each field of data.*/
    public String getName() {
        return name;
    }

    public String getfilePathURL() {
        return filePath;
    }

    public String getFolderPath() { return folderPath; }

    public String getVideoStatsName(){
        return viewStatsName;
    }

    public void setDuration() {
        //sets the video duration in milliseconds
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(filePath, new HashMap<String, String>());
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        duration = Long.parseLong(time);
        retriever.release();
    }

    public long getDurationInMilliseconds() { return duration; }

    /*Unused functions*/
    public String getDate() { return dateLastModified; }

    public String getTime() { return timeLastModified; }
}
