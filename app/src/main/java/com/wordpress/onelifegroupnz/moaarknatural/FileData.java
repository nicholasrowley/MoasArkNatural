package com.wordpress.onelifegroupnz.moaarknatural;

import java.io.Serializable;

/**
 * This object is for storing information on Files that have been fetched from Dropbox.
 * Created by Nicholas Rowley on 1/9/2017.
 */

public class FileData implements Serializable {
    private String name;
    private String tempUrl;
    private String dropboxUri;
    private String folderPath;
    private String viewStatsName; //TODO check if still wanted. not currently implemented
    private static final long serialVersionUID = 1L; //required for Serializable

    public FileData(String fileName, String temporaryUrl, String dbUri, String dbFolderPath)
    {
        name = fileName.replaceFirst("[.][^.]+$", ""); //remove the file extension
        tempUrl = temporaryUrl;
        dropboxUri = dbUri;
        folderPath = dbFolderPath;
        viewStatsName = "views_" + name.replaceAll(" ", "_").toLowerCase();
    }

    public String getDbUri() { return dropboxUri; }

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
