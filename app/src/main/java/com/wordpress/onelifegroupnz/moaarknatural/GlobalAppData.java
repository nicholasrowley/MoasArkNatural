package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.Context.MODE_PRIVATE;

/**
 * This class stores app data that can be accessed by any activity in the app.
 * - Eliminates need to reload certain data types between activities.
 * Created by Nichols Rowley on 1/9/2017.
 */

public class GlobalAppData {
    private static GlobalAppData instance = null;
    private FolderContentLister danceVideoFileLister;
    private FolderContentLister pdfFileLister;
    private FolderContentLister imageFileLister;
    private FolderContentLister foodVideoFileLister;
    private FolderContentLister danceMusicContentLister;
    private List<FileDataListing> danceVideoInfoList;
    private List<FileDataListing> foodVideoInfoList;
    private List<FileDataListing> lastViewingList;
    private PlayListData savedPlayList;
    private int danceVideosLoaded;
    private int foodVideosLoaded;
    public static final String DANCEVIDEOPATH = "/line dance videos/";
    public static final String STEPSHEETPATH = "/steps/";
    public static final String FOODVIDEOPATH = "/food videos/";
    public static final String RECIPEPATH = "/recipes/";
    public static final String CASTIMAGEPATH = "/castimages/";
    public static final String FEATUREDANCETXTPATH = "/feature video/feature dance.txt";
    public static final String FEATUREFOODTXTPATH = "/feature video/feature food.txt";
    public static final String TAGLINETXTPATH = "/tagline/tagline.txt";
    public static final String DANCEMUSICPATH = "/dance music/";
    public static final String ALLVIDEOSCODE = "ALLVIDEOS";
    public static final String PLAYLISTCODE = "PLAYLIST";
    private List<SearchSuggestion> searchSuggestions;
    public static final int DROPBOXTIMEOUTLIMIT = 60000; //Milliseconds
    private Toast toast;

    //name of featured videos & featured video
    private FileDataListing featureDanceInfo;
    private FileDataListing featureFoodInfo;

    //Shared Preferences
    public static final String SHARED_PREFS = "com.wordpress.onelifegroupnz.moasarknatural";

    private GlobalAppData(String directoryRoot, Context context, String searchString) {

        //execute FolderContentLister and get web server directory listing content over https
        refreshIISDirectoryVideoFiles(directoryRoot, context, searchString, ALLVIDEOSCODE);
        populateSearchSuggestions(context);
        initialisePlaylist(context);
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
        danceVideoInfoList = danceVideoFileLister.getLoadData();
        danceVideosLoaded = danceVideoFileLister.getLoadData().size() - danceVideoFileLister.getRemainingLoads();

        //store food videos as fileData
        foodVideoInfoList = foodVideoFileLister.getLoadData();
        foodVideosLoaded = foodVideoFileLister.getLoadData().size() - foodVideoFileLister.getRemainingLoads();
    }

    /*This method connects to a IIS web server to get video data. This method should be run in
    * a separate thread. Note: This method loads the videos from scratch
    * directoryRoot - url to the root folder accessible by the app.
    * Context - Activity that is currently open
    * searchString - search query
    * videoPath - Path to the folder storing the videos.*/
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
            danceVideoFileLister = new FolderContentLister(directoryRoot, DANCEVIDEOPATH, searchString, 0, new ArrayList<FileDataListing>());

            danceVideoInfoList = new ArrayList<>();
            refreshDanceVideoTask.run();
        }

        if (videoPath.equals(FOODVIDEOPATH) || videoPath.equals(ALLVIDEOSCODE)) {
            foodVideoFileLister = new FolderContentLister(directoryRoot, FOODVIDEOPATH, searchString, 0, new ArrayList<FileDataListing>());

            foodVideoInfoList = new ArrayList<>();
            refreshFoodVideoTask.run();
        }
        refreshAllVideoLists();

        setFeatureDanceVideo(directoryRoot);
        setFeatureFoodVideo(directoryRoot);

            try {
                refreshDanceVideoTask.join();
                refreshDanceVideoTask.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        populateSearchSuggestions(context);
    }

    /*This method is for loading web server files in the background until fully loaded. This method
    * uses partially loaded files and is run when more results need to be loaded into the app display.*/
    public void loadIISDirectoryFiles(String directoryRoot, String searchString, String videoPath) {
        if (videoPath.equals(DANCEVIDEOPATH)) {
            danceVideoFileLister = new FolderContentLister(directoryRoot, DANCEVIDEOPATH, searchString, danceVideosLoaded, danceVideoInfoList);
            danceVideoFileLister.execute();

            waitForDanceVideoFileListerExecution(0);
        } else if (videoPath.equals(FOODVIDEOPATH)) {
            foodVideoFileLister = new FolderContentLister(directoryRoot, FOODVIDEOPATH, searchString, foodVideosLoaded, foodVideoInfoList);
            foodVideoFileLister.execute();

            waitForFoodVideoFileListerExecution(0);
        }

        refreshAllVideoLists();
    }

    /*checks if the previous web server connection for a FolderContentLister object was successful
    * String folderPath - target file path on server without root directory mentioned. e.g. /food videos/
    * NOTE: if STEPSHEETPATH or RECIPEPATH are to have different outcomes then the dbsuccess code should be revised in ViewVideo Activity*/
    public boolean dbSuccess(String folderPath) {
        switch (folderPath) {
            case DANCEVIDEOPATH:
                return danceVideoFileLister.httpConnectionSuccessful();
            case FOODVIDEOPATH:
                return foodVideoFileLister.httpConnectionSuccessful();
            case STEPSHEETPATH:
            case RECIPEPATH:
                return pdfFileLister.httpConnectionSuccessful();
            case CASTIMAGEPATH:
                return imageFileLister.httpConnectionSuccessful();
            case DANCEMUSICPATH:
                return danceMusicContentLister.httpConnectionSuccessful();
            default:
                return false;
        }
    }

    /*method which returns the single latest version of the step sheet which contains the given
     name. (if one exists otherwise null)*/
    public FileDataListing getPdfContent(String directoryRoot, String pdfName, String pdfPath) {

        List<FileDataListing> pdfInfoList;

        //Identify whether a Stepsheet or Recipe is needed.
        if (pdfPath.equals(directoryRoot + DANCEVIDEOPATH))
        {
            pdfFileLister = new FolderContentLister(directoryRoot, STEPSHEETPATH, pdfName, 0, new ArrayList<FileDataListing>());
        } else
            if (pdfPath.equals(directoryRoot + FOODVIDEOPATH)) {
                pdfFileLister = new FolderContentLister(directoryRoot, RECIPEPATH, pdfName, 0, new ArrayList<FileDataListing>());
            }
        pdfFileLister.execute();

        waitForPdfFileListerExecution();

        pdfInfoList = pdfFileLister.getLoadData();

        //if no result. declare with empty fields
        if (pdfInfoList.size() == 0)
            pdfInfoList.add(new FileDataListing("", "", "", ""));

        return pdfInfoList.get(0); //returns the latest step sheet
    }

    /*method which returns a single image to display in cast videos (if one exists otherwise returns a blank entry)*/
    public FileDataListing getImageContent(String directoryRoot, String imageName) {

        List<FileDataListing> imageInfoList;

        imageFileLister = new FolderContentLister(directoryRoot, CASTIMAGEPATH, imageName, 0, new ArrayList<FileDataListing>());


        imageFileLister.execute();

        try {
            imageFileLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        imageInfoList = imageFileLister.getLoadData();

        //if no result. declare with empty fields
        if (imageInfoList.size() == 0)
            imageInfoList.add(new FileDataListing("", "", "", ""));

        return imageInfoList.get(0); //returns an image matching the search conditions
    }

    /*method which returns a single music file with the following name (if one exists otherwise returns a blank entry)*/
    public FileDataListing getMusicContent(String directoryRoot, String fileName) {

        List<FileDataListing> musicFileListing;

        danceMusicContentLister = new FolderContentLister(directoryRoot, DANCEMUSICPATH, fileName, 0, new ArrayList<FileDataListing>());


        danceMusicContentLister.execute();

        try {
            danceMusicContentLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        musicFileListing = danceMusicContentLister.getLoadData();

        //if no result. declare with empty fields
        if (musicFileListing.size() == 0) {
            musicFileListing.add(new FileDataListing("", "", "", ""));
        }

        return musicFileListing.get(0); //returns an image matching the search conditions
    }

    public List<SearchSuggestion> getSearchSuggestions(){
        return searchSuggestions;
    }

    /*starts FolderContentLister for dance videos
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

    /*waits for pdf FolderContentLister until it completes or times out.*/
    private void waitForPdfFileListerExecution() {
        try {
            pdfFileLister.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /*starts FolderContentLister for food health investment videos
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
        for (FileDataListing content : danceVideoInfoList) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_dance), content.getName()));
        }
        for (FileDataListing content : foodVideoInfoList) {
            searchSuggestions.add(new SearchSuggestion(context.getString(R.string.search_type_food), content.getName()));
        }
    }

    /* Sets the feature dance video based on what is specified on the server side text file */
    public void setFeatureDanceVideo(String directoryRoot) {
        String featureDance;
        try {
            BufferedReader danceBr = new BufferedReader(new InputStreamReader(new URL(directoryRoot + GlobalAppData.FEATUREDANCETXTPATH).openStream()));

            if ((featureDance = danceBr.readLine()) == null) {
                featureDance = "";
            }
            danceBr.close();
            for (FileDataListing video : danceVideoInfoList) {
                if (featureDance.equals(video.getName())) {
                    featureDanceInfo = video;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("error", "something went wrong with setting the feature dance video.");
        }
    }

    /* Sets the feature food video based on what is specified on the server side text file */
    public void setFeatureFoodVideo(String directoryRoot) {
        String featureFood;
        try {
            BufferedReader foodBr = new BufferedReader(new InputStreamReader(new URL(directoryRoot + GlobalAppData.FEATUREFOODTXTPATH).openStream()));

            if ((featureFood = foodBr.readLine()) == null) {
                featureFood = "";
            }
            foodBr.close();
            for (FileDataListing video : foodVideoInfoList) {
                if (featureFood.equals(video.getName())) {
                    featureFoodInfo = video;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("error", "something went wrong with setting the feature food video.");
        }
    }

    /* returns the data for the feature food video */
    public FileDataListing getFeatureFoodVideo() {
        return featureFoodInfo;
    }

    /* returns the data for the feature dance video */
    public FileDataListing getFeatureDanceVideo() {
        return featureDanceInfo;
    }

    //saves a custom list for video viewing through the next and previous buttons.
    public void setVideoViewList(List<FileDataListing> list) {
        lastViewingList = list;
    }

    //gets the last custom list saved for the video view activity.
    public List<FileDataListing> getLastVideoViewingList() {
        return lastViewingList;
    }

    //method for displaying toast messages in the app and discarding the last message if needed.
    public void showToastMessage(String message, boolean cancelPrevious, Context context ) {
        if (toast != null) {
            if (cancelPrevious) {
                toast.setText(message);
                toast.show();
            }
            else {
                toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void initialisePlaylist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        try {
            long currentSerialVersionID = ObjectStreamClass.lookup(PlayListData.class).getSerialVersionUID();
            Log.d("Initialise Playlist", "Current playlist class serial ID is " + currentSerialVersionID);
            //TODO if Playlist object is updated in the future then use the network to deserialise the playlist
            savedPlayList = (PlayListData) ObjectSerializer.deserialize(prefs.getString("PlayList", ObjectSerializer.serialize(new PlayListData())));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (savedPlayList == null) {
            //TODO avoid overwriting playlist data if corrupt.
            savedPlayList = new PlayListData();
            Log.d("Initialise Playlist", "Playlist not found. Creating blank playlist.");
        } else {
            Log.d("Initialise Playlist", "Playlist initialised. Found " + savedPlayList.getSize() + " entries.");
        }
    }

    public void removeFromPlayList(Context context, String entryName) {
        savedPlayList.removePlayListEntry(entryName);
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
        try {
            editor.putString("PlayList", ObjectSerializer.serialize(savedPlayList));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.apply();
        Log.d("Remove from Playlist", "Playlist entry removed. Found " + savedPlayList.getSize() + " remaining entries.");
    }

    /** adds a new entry to the app playlist and saves it for access when the app is reopened.
     * videoData - the data for the video being accessed.
     * videoType - can be either FOODVIDEOPATH or DANCEVIDEOPATH values.*/
    public void addToPlayList(Context context, FileDataListing videoData, String videoType) {
        if (videoType.equals(FOODVIDEOPATH) || videoType.equals(DANCEVIDEOPATH)) {
            PlaylistEntry entry = new PlaylistEntry(videoData, videoType);
            savedPlayList.addPlayListEntry(entry);
            SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
            try {
                editor.putString("PlayList", ObjectSerializer.serialize(savedPlayList));
            } catch (IOException e) {
                e.printStackTrace();
            }
            editor.apply();
            Log.d("Add to Playlist", "Playlist entry added. Found " + savedPlayList.getSize() + " entries.");
        } else {
            Log.d("Add to Playlist", "Playlist Entry invalid. videoType must be either FOODVIDEOPATH or DANCEVIDEOPATH");
        }
    }

    public PlaylistEntry getPlayListEntry(String entryName) {
        return savedPlayList.getPlayListEntry(entryName);
    }

    public PlaylistEntry getPlayListEntry(int index) {
        return savedPlayList.getPlayListEntry(index);
    }

    public PlayListData getPlaylist() {
        return savedPlayList;
    }


}
