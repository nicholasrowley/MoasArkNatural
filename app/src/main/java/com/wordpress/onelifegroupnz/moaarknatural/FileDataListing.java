package com.wordpress.onelifegroupnz.moaarknatural;

public class FileDataListing {
    private String name;
    private String filePath;
    private String folderPath;
    private String dateLastModified;
    private String timeLastModified;
    private String viewStatsName; //log entry for stats
    private static final long serialVersionUID = 1L; //required for Serializable

    public FileDataListing(String fileName, String httpFolderPath, String dateDDMMYYYY, String timeAmPm) {
        name = fileName.replaceFirst("[.][^.]+$", ""); //remove the file extension
        filePath = httpFolderPath + fileName;
        folderPath = httpFolderPath;
        dateLastModified = dateDDMMYYYY;
        timeLastModified = timeAmPm;
        viewStatsName = "views_" + name.replaceAll(" ", "_").toLowerCase();
    }

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

    public String getDate() { return dateLastModified; }

    public String getTime() { return timeLastModified; }
}
