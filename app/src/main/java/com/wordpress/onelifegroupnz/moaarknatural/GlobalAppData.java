package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class stores app data that can be accessed by any activity in the app.
 * - Eliminates need to reload certain data types between activities.
 * Created by Nichols Rowley on 1/9/2017.
 */

public class GlobalAppData {
    private static GlobalAppData instance = null;
    private FileLister danceVideoFileLister;
    private FileLister pdfFileLister;
    private FileLister foodVideoFileLister;
    private List<FileData> danceVideoInfoList;
    private List<FileData> foodVideoInfoList;
    private List<Metadata> dropboxDanceVideoLoadData; //data for loading remaining dropbox dance videos
    private List<Metadata> dropboxFoodVideoLoadData; //data for loading remaining dropbox dance videos
    public static final String DANCEVIDEOPATH = "/line dance videos/";
    public static final String STEPSHEETPATH = "/steps/";
    public static final String FOODVIDEOPATH = "/food videos/";
    public static final String RECIPEPATH = "/recipes/";
    public static final String ALLVIDEOSCODE = "ALLVIDEOS";
    public List<SearchSuggestion> searchSuggestions;
    public static final int DROPBOXTIMEOUTLIMIT = 60000; //Milliseconds

    private GlobalAppData(String ACCESS_TOKEN, Context context, String searchString) {

        //checks if access token is not set.
        if (ACCESS_TOKEN.equals("ACCESS_TOKEN")) {
            new AlertDialog.Builder(context)
                    .setTitle("WARNING: ACCESS TOKEN NOT SET")
                    .setMessage("Invalid access token detected. Without a valid token this " +
                            "application will not run properly. If you are a user please reinstall " +
                            "the app. If you are still experiencing this issue please contact " +
                            "support.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            //execute filelisters and get Dropbox content
            refreshDropboxVideoFiles(ACCESS_TOKEN, context, searchString, ALLVIDEOSCODE);

            populateSearchSuggestions(context);
        }
    }

    public static GlobalAppData getInstance(String ACCESS_TOKEN, Context context, String searchString) {
        if (instance == null) {
            instance = new GlobalAppData(ACCESS_TOKEN, context, searchString);
        }
        return instance;
    }

    public List<FileData> getVideoData(String videoPath) {
        if (videoPath.equals(DANCEVIDEOPATH)) {
            return danceVideoInfoList;
        } else if (videoPath.equals(FOODVIDEOPATH)) {
            return foodVideoInfoList;
        }
        return null;
    }

    private void refreshAllVideoLists() {
        //store dance videos as fileData
            danceVideoInfoList = danceVideoFileLister.getFileDatas();
            dropboxDanceVideoLoadData = danceVideoFileLister.getLoadData();

        //store food videos as fileData
            foodVideoInfoList = foodVideoFileLister.getFileDatas();
            dropboxFoodVideoLoadData = foodVideoFileLister.getLoadData();
    }

    /*This method connects to the dropbox servers to get video data. This method should be run in
    * a separate thread. Note: This method loads the videos from scratch
    * ACCESS_TOKEN - unique token for connecting to dropbox for the videos
    * Context - Activity that is currently open
    * searchString - search query
    * videoPath - Path to the folder storing the videos. If a valid one is not specified all videos will load instead*/
    public void refreshDropboxVideoFiles(String ACCESS_TOKEN, Context context, String searchString, String videoPath) {

        Thread refreshDanceVideoTask = new Thread() {
            public void run() {
                danceVideoFileLister.execute();
                waitForDanceVideoFileListerExecution(DROPBOXTIMEOUTLIMIT);
            }
        };
        Thread refreshFoodVideoTask = new Thread() {
            public void run() {
                foodVideoFileLister.execute();
                waitForFoodVideoFileListerExecution(DROPBOXTIMEOUTLIMIT);
            }
        };

        if (videoPath.equals(DANCEVIDEOPATH) || videoPath.equals(ALLVIDEOSCODE)) {
            danceVideoFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), new ArrayList<Metadata>(), new ArrayList<FileData>(), searchString, DANCEVIDEOPATH);

            danceVideoInfoList = new ArrayList<>();
            dropboxDanceVideoLoadData = new ArrayList<>();
            refreshDanceVideoTask.run();
        }

        if (videoPath.equals(FOODVIDEOPATH) || videoPath.equals(ALLVIDEOSCODE)) {
            foodVideoFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), new ArrayList<Metadata>(), new ArrayList<FileData>(), searchString, FOODVIDEOPATH);

            foodVideoInfoList = new ArrayList<>();
            dropboxFoodVideoLoadData = new ArrayList<>();
            refreshFoodVideoTask.run();
        }

            try {
                refreshDanceVideoTask.join();
                refreshDanceVideoTask.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        refreshAllVideoLists();

        populateSearchSuggestions(context);
    }

    /*This method is for loading dropbox files in the background until fully loaded. This method
    * uses partially loaded files and doesn't load new files stored on dropbox servers*/
    public void loadDropboxFiles(String ACCESS_TOKEN, String searchString, String videoPath) {
        if (videoPath.equals(DANCEVIDEOPATH)) {
            danceVideoFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), dropboxDanceVideoLoadData, danceVideoInfoList, searchString, DANCEVIDEOPATH);
            danceVideoFileLister.execute();

            waitForDanceVideoFileListerExecution(0);
        } else if (videoPath.equals(FOODVIDEOPATH)) {
            foodVideoFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), dropboxFoodVideoLoadData, foodVideoInfoList, searchString, FOODVIDEOPATH);
            foodVideoFileLister.execute();

            waitForFoodVideoFileListerExecution(0);
        }

        refreshAllVideoLists();
    }

    /*checks the number of videos that have not been fully loaded from dropbox servers*/
    public int loadsRemaining(String folderPath) {
        if (folderPath.equals(DANCEVIDEOPATH)) {
            return danceVideoFileLister.getRemainingLoads();
        } else if (folderPath.equals(FOODVIDEOPATH)) {
            return foodVideoFileLister.getRemainingLoads();
        }
        return 0;
    }

    /*checks if the previous dropbox connection for a filelister object was successful
    * String folderPath - target file path on server*/
    public boolean dbSuccess(String folderPath) {
        if (folderPath.equals(DANCEVIDEOPATH)) {
            return danceVideoFileLister.dbConnectionSuccessfull();
        } else if (folderPath.equals(FOODVIDEOPATH)) {
            return foodVideoFileLister.dbConnectionSuccessfull();
        } else {
            if (danceVideoFileLister.dbConnectionSuccessfull() && foodVideoFileLister.dbConnectionSuccessfull()) {
                return true;
            } else
                return false;
        }
    }

    /*method which returns the single latest version of the step sheet which contains the given
     name. (if one exists otherwise null)*/
    public FileData getPdfContent(String ACCESS_TOKEN, String pdfName, String pdfPath) {

        List<FileData> pdfInfoList;

        //Identify whether a Stepsheet or Recipe is needed.
        if (pdfPath.equals(DANCEVIDEOPATH))
        {
            pdfFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), new ArrayList<Metadata>(), new ArrayList<FileData>(), pdfName, STEPSHEETPATH);
        } else
            if (pdfPath.equals(FOODVIDEOPATH)) {
                pdfFileLister = new FileLister(DropboxClient.getClient(ACCESS_TOKEN), new ArrayList<Metadata>(), new ArrayList<FileData>(), pdfName, RECIPEPATH);
            }
        pdfFileLister.execute();

        waitForPdfFileListerExecution();

        pdfInfoList = pdfFileLister.getFileDatas();

        //if no result. declare with empty fields
        if (pdfInfoList.size() == 0)
            pdfInfoList.add(new FileData("", "", ""));

        return pdfInfoList.get(0); //returns the latest step sheet
    }

    public List<SearchSuggestion> getSearchSuggestions(){
        return searchSuggestions;
    }

    /*starts fileLister for dance videos
    * milliseconds - time in milliseconds allowed to get information from server.*/
    private void waitForDanceVideoFileListerExecution( int milliseconds ) {
        try {
            if (milliseconds > 0)
                danceVideoFileLister.get(milliseconds, TimeUnit.MILLISECONDS);
            else
                danceVideoFileLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /*waits for pdf fileLister until it completes or times out.*/
    private void waitForPdfFileListerExecution() {
        try {
            pdfFileLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /*starts fileLister for food health investment videos
    * milliseconds - time in milliseconds allowed to get information from server.*/
    private void waitForFoodVideoFileListerExecution( int milliseconds ) {
        try {
            if (milliseconds > 0)
                foodVideoFileLister.get(milliseconds, TimeUnit.MILLISECONDS);
            else
                foodVideoFileLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    /*lists the search suggestions to list*/
    private void populateSearchSuggestions(Context context) {
        //populate the suggestions
        searchSuggestions = new ArrayList<>();
        for (Metadata content : dropboxDanceVideoLoadData) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_dance), content.getName().replaceFirst("[.][^.]+$", "")));
        }
        for (Metadata content : dropboxFoodVideoLoadData) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_food), content.getName().replaceFirst("[.][^.]+$", "")));
        }
    }
}
