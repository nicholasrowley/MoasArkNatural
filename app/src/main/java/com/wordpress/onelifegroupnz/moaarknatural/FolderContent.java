package com.wordpress.onelifegroupnz.moaarknatural;

import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

//TODO Sort FileDataListing "currentDirectoryListing" list so that the list is sorted by date
public class FolderContent extends AsyncTask<Object, Void, Object> {

    private String folderPathRoot;
    private String searchString;
    private List<FileDataListing> currentDirectoryListing;
    private List<FileDataListing> currentDirectoryListingLoaded;
    private boolean isValid; //checks if the url source was loaded properly
    private String sourceToParse; //remaining string to interpret.
    private int remainingLoads; //remaining number of times new items will be available through the load button.
    private static final int LOADAMOUNT = 5; //number of files loaded with a single execution of the class. Less is faster.

    public FolderContent(String urlDirectoryRoot, String folderPath, String searchInput, List<FileDataListing> loadedVideos, List<FileDataListing> sourceDirectoryData){
        folderPathRoot = urlDirectoryRoot + folderPath;
        searchString = searchInput;
        currentDirectoryListingLoaded = loadedVideos;
        currentDirectoryListing = sourceDirectoryData;
        remainingLoads = (int) Math.ceil((currentDirectoryListing.size() - currentDirectoryListingLoaded.size()) / (float) LOADAMOUNT);
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            getShareURLFileSystem();
        } catch (IOException e) {
            isValid = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

    public void getShareURLFileSystem() throws IOException {
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
        isValid = true;
    }

    private void loadItemsFromSourceCode() throws IOException{
        List<FileDataListing> fileDataListing = new ArrayList<>();
        String currentlistingDate;
        String currentlistingTime;
        String currentlistingName;

        //check loop condition and if more listings are available and if the end is reached </pre> then there is no more source to parse.
        while (!sourceToParse.startsWith("</pre>")) {
            //Select the characters for the date and save it as a string

            //Log.d("pattern test 1:", sourceToParse);
            Pattern pd = Pattern.compile(".{10}");
            Matcher md = pd.matcher(sourceToParse);
            if (md.find()){
                currentlistingDate = md.group();
            } else {
                throw new IOException("currentlistingDate returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".{11}", "");
            //Select the characters for the time and save it as a string
            Pattern pt = Pattern.compile(".{8}");
            Matcher mt = pt.matcher(sourceToParse);
            if (mt.find()){
                currentlistingTime = mt.group();
            } else {
                throw new IOException("currentlistingTime returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".+?(?:\">)", "");
            //Select the characters for the name of the listing and save it as a string
            Pattern pn = Pattern.compile(".+?(?=</)");
            Matcher mn = pn.matcher(sourceToParse);
            if (mn.find()){
                currentlistingName = mn.group();
            } else {
                throw new IOException("currentlistingName returned nothing while a value was expected.");
            }
            //Trim off the string up to the next item in directory listing
            sourceToParse = sourceToParse.replaceFirst(".+?(?:<br>)", "");

            //save the currently found listing to the file data list
            fileDataListing.add(new FileDataListing(currentlistingName, folderPathRoot, currentlistingDate, currentlistingTime));


            /*if(sourceToParse.startsWith("</pre>")) {
                sourceToParse = "";
            }*/

        }
        /*for (FileDataListing item:fileDataListing
        ) {
            Log.d("listing item name:", item.getName());
            Log.d("listing item date:", item.getDate());
            Log.d("listing item time:", item.getTime());
            Log.d("listing item urlpath:", item.getfilePathURL());
        }*/
        // regex to select everything before "> : .+?(?:">)
        // regex to select everything before and excluding </ : .+?(?=</)
        // regex to select 10 characters : .{10}
        /*String result = html.toString().replaceFirst(".+?(?:\\[To Parent Directory\\])", "");
        Pattern p = Pattern.compile(".+?(?=</)");
        Matcher m = p.matcher(result);
        if (m.find()){
            Log.d("source code length:", m.group());
        }


        Log.d("source code length:", String.valueOf(html.toString().length()));
        //Log.d("source code indexed:", result.replaceFirst("\",(.*)", ""));
        Log.d("source code:", result);
        return html.toString();*/
        currentDirectoryListing = fileDataListing;

        //go through and delete all irrelevant results based on the search string
        List<FileDataListing> resultsList = new ArrayList<>();
        CharSequence searchSequence = searchString.toLowerCase();
        for(FileDataListing file : currentDirectoryListing){
            if (file.getName().toLowerCase().contains(searchSequence)){
                //for stepsheet pdfs the name must be exactly the same as the video
                resultsList.add(file);
            }
        }
        currentDirectoryListing = resultsList;

        loadItemsFromCurrentList();
    }

    public void loadItemsFromCurrentList() {
        int itemsToLoad = LOADAMOUNT;
        while (itemsToLoad != 0 && currentDirectoryListing.size() != currentDirectoryListingLoaded.size()){
            int loadPosition = currentDirectoryListingLoaded.size();
            currentDirectoryListingLoaded.add(currentDirectoryListing.get(loadPosition));
            itemsToLoad--;
        }
        remainingLoads = (int) Math.ceil((currentDirectoryListing.size() - currentDirectoryListingLoaded.size()) / (float) LOADAMOUNT);
    }

    public List<FileDataListing> getFileDatas() {
        return currentDirectoryListingLoaded;
    }

    public List<FileDataListing> getLoadData() { return currentDirectoryListing; }

    public int getRemainingLoads() { return remainingLoads; }

    public int getTotal() { return currentDirectoryListing.size(); }

    /*dbSuccess - last connection to dropbox servers was successful*/
    public boolean dbConnectionSuccessfull(){
        return isValid;
    }
}
