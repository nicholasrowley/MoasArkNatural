package com.wordpress.onelifegroupnz.moaarknatural;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Async class used to fetch metadata on files stored on the IIS web server's directory listing by reading html source code.
 *  * Used for the following:
 *  * -Fetching application videos
 *  * -Fetching pdfs for videos
 * Created by Nicholas Rowley on  03/02/2020.
 */
public class FolderContentLister extends AsyncTask<Object, Void, Object> {

    private String folderPathRoot;
    private String searchString;
    private List<FileDataListing> currentDirectoryListing;
    private boolean isValid; //checks if the url source was loaded properly
    private String sourceToParse; //remaining string to interpret.
    private int loadedFiles; //videos that have already been loaded
    public static final int LOADAMOUNT = 5; //number of files loaded with a single execution of the class. Less is faster.

    /* Initialises the FolderContentLister so that it is ready to be executed. Must be run before each time the Lister is executed */
    public FolderContentLister(String urlDirectoryRoot, String folderPath, String searchInput, int filesLoaded, List<FileDataListing> sourceDirectoryData){
        folderPathRoot = urlDirectoryRoot + folderPath;
        searchString = searchInput;
        currentDirectoryListing = sourceDirectoryData;
        loadedFiles = filesLoaded;
    }

    @Override
    protected Object doInBackground(Object[] params) {

        try {
            if (currentDirectoryListing.isEmpty()) {
                getShareURLFileSystem();
            }
            isValid = true;
            loadItemsFromCurrentList();

            //if the first entry has empty fields then assume connection has failed.
            if (!currentDirectoryListing.isEmpty())
                if (currentDirectoryListing.get(0).getName().equals("")) {
                    Log.d("Lister Error", "could not connect to directory.");
                    isValid = false;
                }
        } catch (IOException e) {
            isValid = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

    /* Gets the HTML source code and writes it to string then executes loadItemsFromSourceCode */
    private void getShareURLFileSystem() throws IOException {
        // Build and set timeout values for the request.
        URLConnection connection = (new URL(folderPathRoot)).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        // Read and store the result line by line then return the entire string.
        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            html.append(line);
        }
        in.close();

        sourceToParse = html.toString();
        //Trim off irrelevant information in source code up to the first listing in directory listing.
        sourceToParse = sourceToParse.replaceFirst(".+?(?:<br><br>)", "");

        loadItemsFromSourceCode();
    }

    /* reads relevant data from source code and discards irrelevant information.
    * The following information is collected from source code:
    *   File Name
    *   File Date modified
    *   File Time modified
    *   File URL */
    private void loadItemsFromSourceCode() throws IOException{
        List<FileDataListing> fileDataListing = new ArrayList<>();
        String currentListingDate;
        String currentListingTime;
        String currentListingName;

        //check loop condition and if more listings are available and if the end is reached </pre> then there is no more source to parse.
        while (!sourceToParse.startsWith("</pre>")) {
            //Select the characters for the date and save it as a string

            //Log.d("pattern test 1:", sourceToParse);
            Pattern pd = Pattern.compile(".{10}");
            Matcher md = pd.matcher(sourceToParse);
            if (md.find()){
                currentListingDate = md.group();
            } else {
                throw new IOException("currentListingDate returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".{11}", "");
            //Select the characters for the time and save it as a string
            Pattern pt = Pattern.compile(".{8}");
            Matcher mt = pt.matcher(sourceToParse);
            if (mt.find()){
                currentListingTime = mt.group();
            } else {
                throw new IOException("currentListingTime returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".+?(?:\">)", "");
            //Select the characters for the name of the listing and save it as a string
            Pattern pn = Pattern.compile(".+?(?=</)");
            Matcher mn = pn.matcher(sourceToParse);
            if (mn.find()){
                currentListingName = mn.group();
            } else {
                throw new IOException("currentListingName returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".+?(?:<br>)", "");

            //save the currently found listing to the file data list
            fileDataListing.add(new FileDataListing(currentListingName, folderPathRoot, currentListingDate, currentListingTime));
        }

        currentDirectoryListing = fileDataListing;

        //go through and delete all irrelevant results based on the search string
        List<FileDataListing> resultsList = new ArrayList<>();
        CharSequence searchSequence = searchString.toLowerCase();
        for(FileDataListing file : currentDirectoryListing){
            if (file.getName().toLowerCase().contains(searchSequence)){
                resultsList.add(file);
            }
        }
        currentDirectoryListing = resultsList;
    }

    /* Ensures that only a set number of files are displayed by the app per execution*/
    public void loadItemsFromCurrentList() {
        if ((loadedFiles + LOADAMOUNT) > currentDirectoryListing.size() ) {
            loadedFiles = currentDirectoryListing.size();
        } else {
            loadedFiles += LOADAMOUNT;
        }
    }

    //gets the directory listing data collected over http
    public List<FileDataListing> getLoadData() { return currentDirectoryListing; }

    //get the remaining number of files that will be available through the load button.
    public int getRemainingLoads() { return loadedFiles - currentDirectoryListing.size(); }

    //get the number of files found on http side.
    public int getTotal() { return currentDirectoryListing.size(); }

    /* returns the result of the last connection to web server directory listing*/
    public boolean httpConnectionSuccessful(){
        return isValid;
    }
}
