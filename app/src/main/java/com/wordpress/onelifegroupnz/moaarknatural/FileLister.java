package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Async class used to fetch metadata on files stored on Dropbox
 * Used for the following:
 * -Fetching dropbox videos
 * -Fetching pdf
 * Created by Nicholas Rowley on 1/9/2017.
 */

public class FileLister extends AsyncTask<Object, Void, Object> {
    private DbxClientV2 dbxClient;
    private List<FileData> fileInfoList;
    private List<Metadata> folderContents;
    private String folderPath;
    private Context context;
    private int filesLoaded;
    private int remainingLoads;
    private boolean dbSuccess;
    private String searchString;
    private boolean searchEnabled;
    private static final int LOADAMOUNT = 5; //number of files loaded with a single execution of the class. Less is faster.

    FileLister(DbxClientV2 dbxClient, Context context, List<Metadata> dropboxLoadData,
               List<FileData> loadedVideos, String searchInput, String path) {
        this.dbxClient = dbxClient;
        this.context = context;
        folderContents = dropboxLoadData;
        fileInfoList = loadedVideos;
        folderPath = path;

        filesLoaded = fileInfoList.size();
        remainingLoads = (int) Math.ceil((folderContents.size() - filesLoaded) / (float) LOADAMOUNT);
        searchString = searchInput;

        if (searchString.equals("")) {
            searchEnabled = false;
        } else
        {
            searchEnabled = true;
        }
        dbSuccess = false;
    }

    /*Async method that connects to Dropbox and gets all videos stored in the videos folder in
    Dropbox*/
    @Override
    protected Object doInBackground(Object[] params) {
        try {
            if(!searchEnabled) {
                if (filesLoaded == 0) {
                    //contains metadata for all contents in the folder such as the URI links to each file.
                    folderContents = dbxClient.files().listFolder(folderPath).getEntries();
                }
            } else {
                if (filesLoaded == 0) {
                    //contains metadata for all contents in the folder such as the URI links to each file.
                    folderContents = dbxClient.files().listFolder(folderPath).getEntries();

                    //go through and delete all irrelevant results
                    List<Metadata> resultsList = new ArrayList<>();
                    CharSequence searchSequence = searchString.toLowerCase();
                    for(Metadata file : folderContents){
                            if (file.getName().toLowerCase().contains(searchSequence)) {
                                if (folderPath.equals(GlobalAppData.DANCEVIDEOPATH)
                                        || file.getName().toLowerCase().replaceFirst("[.][^.]+$", "")
                                        .equals(searchSequence))
                                    //for stepsheet pdfs the name must be exactly the same as the video
                                    resultsList.add(file);
                            }
                    }
                    folderContents = resultsList;
                }
            }

            //create temporary links for the next few files in the folder
            for (int i = 0; i < LOADAMOUNT; i++) {
                //to store temporary urls into a list from newest to oldest (date added to folder)
                if (filesLoaded < folderContents.size()) {
                    int entryToLoad = folderContents.size() - 1 - filesLoaded; //load in reverse
                    fileInfoList.add(new FileData(folderContents.get(entryToLoad).getName(), dbxClient.files()
                            .getTemporaryLink(folderContents.get(entryToLoad).getPathLower()).getLink(),
                            folderContents.get(entryToLoad).getPathLower(), folderPath));
                    filesLoaded++;
                    remainingLoads = (int) Math.ceil((folderContents.size() - filesLoaded) / (float) LOADAMOUNT);
                }
            }
            dbSuccess = true;

            remainingLoads = (int) Math.ceil((folderContents.size() - filesLoaded) / (float) LOADAMOUNT);

            Log.d("Create Links", "Success");
        } catch (DbxException e) {
            e.printStackTrace();
            remainingLoads = (int) Math.ceil((folderContents.size() - filesLoaded) / (float) LOADAMOUNT);
            dbSuccess = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

    public void setContext(Context context){
        this.context = context;
    }

    public List<FileData> getFileDatas() {
        return fileInfoList;
    }

    public List<Metadata> getLoadData() { return folderContents; }

    public int getRemainingLoads() { return remainingLoads; }

    public int getTotal() { return folderContents.size(); }

    public boolean dbConnectionSuccessfull(){
        return dbSuccess;
    }
}
