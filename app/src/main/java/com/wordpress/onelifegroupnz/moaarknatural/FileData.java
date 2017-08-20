package com.wordpress.onelifegroupnz.moaarknatural;

import java.io.Serializable;

/**
 * This object is for storing information on Files that have been fetched from Dropbox.
 * Created by Nicholas Rowley on 1/9/2017.
 */

public class FileData implements Serializable {
    private String name;
    private String tempUrl;
    private String folderPath;
    private String viewStatsName; //log entry for stats
    private static final long serialVersionUID = 1L; //required for Serializable

    public FileData(String fileName, String temporaryUrl, String dbFolderPath)
    {
        name = fileName.replaceFirst("[.][^.]+$", ""); //remove the file extension
        tempUrl = temporaryUrl;
        folderPath = dbFolderPath;
        viewStatsName = "views_" + name.replaceAll(" ", "_").toLowerCase();
    }

    public String getName() {
        return name;
    }

    public String getTempUrl() {
        return tempUrl;
    }

    public String getFolderPath() { return folderPath; }

    public String getVideoStatsName(){
        return viewStatsName;
    }
}
