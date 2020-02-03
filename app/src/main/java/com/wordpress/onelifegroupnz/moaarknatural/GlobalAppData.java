package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.dropbox.core.v2.files.Metadata;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
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
    private FolderContent danceVideoFileLister;
    private FolderContent pdfFileLister;
    private FolderContent foodVideoFileLister;
    private List<FileDataListing> danceVideoInfoList;
    private List<FileDataListing> foodVideoInfoList;
    private List<FileDataListing> dropboxDanceVideoLoadData; //data for loading remaining dropbox dance videos
    private List<FileDataListing> dropboxFoodVideoLoadData; //data for loading remaining dropbox dance videos
    public static final String DANCEVIDEOPATH = "/line dance videos/";
    public static final String STEPSHEETPATH = "/steps/";
    public static final String FOODVIDEOPATH = "/food videos/";
    public static final String RECIPEPATH = "/recipes/";
    public static final String FEATUREDANCETXTPATH = "/feature video/feature dance.txt";
    public static final String FEATUREFOODTXTPATH = "/feature video/feature food.txt";
    public static final String ALLVIDEOSCODE = "ALLVIDEOS";
    public List<SearchSuggestion> searchSuggestions;
    public static final int DROPBOXTIMEOUTLIMIT = 60000; //Milliseconds

    //name of featured videos & featured video
    private FileDataListing featureDanceInfo;
    private FileDataListing featureFoodInfo;

    private GlobalAppData(String directoryRoot, Context context, String searchString) {

        //execute filelisters and get Dropbox content
        refreshIISDirectoryVideoFiles(directoryRoot, context, searchString, ALLVIDEOSCODE);
        populateSearchSuggestions(context);
    }

    public static GlobalAppData getInstance(String directoryRoot, Context context, String searchString) {
        if (instance == null) {
            instance = new GlobalAppData(directoryRoot, context, searchString);
        }
        return instance;
    }

    public List<FileDataListing> getVideoData(String videoPath) {
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
    public void refreshIISDirectoryVideoFiles(String directoryRoot, Context context, String searchString, String videoPath) {

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
            danceVideoFileLister = new FolderContent(directoryRoot, DANCEVIDEOPATH, searchString, new ArrayList<FileDataListing>(), new ArrayList<FileDataListing>());

            danceVideoInfoList = new ArrayList<>();
            dropboxDanceVideoLoadData = new ArrayList<>();
            refreshDanceVideoTask.run();
        }

        if (videoPath.equals(FOODVIDEOPATH) || videoPath.equals(ALLVIDEOSCODE)) {
            foodVideoFileLister = new FolderContent(directoryRoot, FOODVIDEOPATH, searchString, new ArrayList<FileDataListing>(), new ArrayList<FileDataListing>());

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
    public void loadIISDirectoryFiles(String directoryRoot, String searchString, String videoPath) {
        if (videoPath.equals(DANCEVIDEOPATH)) {
            danceVideoFileLister = new FolderContent(directoryRoot, DANCEVIDEOPATH, searchString, dropboxDanceVideoLoadData, danceVideoInfoList);
            danceVideoFileLister.execute();

            waitForDanceVideoFileListerExecution(0);
        } else if (videoPath.equals(FOODVIDEOPATH)) {
            foodVideoFileLister = new FolderContent(directoryRoot, FOODVIDEOPATH, searchString, dropboxFoodVideoLoadData, foodVideoInfoList);
            foodVideoFileLister.execute();

            waitForFoodVideoFileListerExecution(0);
        }

        refreshAllVideoLists();
    }

    //TODO Needs to be fixed.
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
    public FileDataListing getPdfContent(String directoryRoot, String pdfName, String pdfPath) {

        Log.d("PDFPATH: ", pdfPath);

        List<FileDataListing> pdfInfoList;

        //Identify whether a Stepsheet or Recipe is needed.
        if (pdfPath.equals(directoryRoot + DANCEVIDEOPATH))
        {
            pdfFileLister = new FolderContent(directoryRoot, STEPSHEETPATH, pdfName, new ArrayList<FileDataListing>(), new ArrayList<FileDataListing>());
        } else
            if (pdfPath.equals(directoryRoot + FOODVIDEOPATH)) {
                pdfFileLister = new FolderContent(directoryRoot, RECIPEPATH, pdfName, new ArrayList<FileDataListing>(), new ArrayList<FileDataListing>());
            }
        pdfFileLister.execute();

        waitForPdfFileListerExecution();

        pdfInfoList = pdfFileLister.getFileDatas();

        //if no result. declare with empty fields
        if (pdfInfoList.size() == 0)
            pdfInfoList.add(new FileDataListing("", "", "", ""));

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
        for (FileDataListing content : dropboxDanceVideoLoadData) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_dance), content.getName()));
        }
        for (FileDataListing content : dropboxFoodVideoLoadData) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_food), content.getName()));
        }
    }

    public void setFeatureDanceVideo(String directoryRoot) {
        //TODO set feature videos based on what is specified in the text file
        String featureDance;
        try {
            BufferedReader danceBr = new BufferedReader(new InputStreamReader(new URL(directoryRoot + GlobalAppData.FEATUREDANCETXTPATH).openStream()));

            if ((featureDance = danceBr.readLine()) == null) {
                featureDance = "";
            }
            Log.d("feature dance value",featureDance);
            danceBr.close();
            for (FileDataListing video : danceVideoInfoList) {
                if (featureDance.equals(video.getName())) {
                    featureDanceInfo = video;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("error", "something went wrong with setting the feature dance.");
        }
    }

    public void setFeatureFoodVideo(String directoryRoot) {
        String featureFood;
        try {
            BufferedReader foodBr = new BufferedReader(new InputStreamReader(new URL(directoryRoot + GlobalAppData.FEATUREFOODTXTPATH).openStream()));

            if ((featureFood = foodBr.readLine()) == null) {
                featureFood = "";
            }
            Log.d("feature dance value",featureFood);
            foodBr.close();
            for (FileDataListing video : foodVideoInfoList) {
                if (featureFood.equals(video.getName())) {
                    featureFoodInfo = video;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("error", "something went wrong with setting the feature food.");
        }
    }

    public FileDataListing getFeatureFoodVideo() {
        return featureFoodInfo;
    }

    public FileDataListing getFeatureDanceVideo() {
        return featureDanceInfo;
    }
}
