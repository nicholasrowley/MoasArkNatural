package com.wordpress.onelifegroupnz.moaarknatural;

import android.os.Debug;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class FolderContent {

    public static List<FileDataListing> getShareURLFileSystem(String url) throws IOException {
        // Build and set timeout values for the request.
        URLConnection connection = (new URL(url)).openConnection();
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

        String sourceToParse = html.toString();
        //Trim off irrelevant information in source code for directory listing.
        sourceToParse = sourceToParse.replaceFirst(".+?(?:<br><br>)", "");

        List<FileDataListing> fileDataListing = new ArrayList<>();
        String currentlistingDate;
        String currentlistingTime;
        String currentlistingName;

        //check loop condition and if more listings are available and if the end is reached </pre> then there is no more source to parse.
        while (!sourceToParse.startsWith("</pre>")) {
            //Select the characters for the date and save it as a string

            Log.d("pattern test 1:", sourceToParse);
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
            fileDataListing.add(new FileDataListing(currentlistingName, url, currentlistingDate, currentlistingTime));


            /*if(sourceToParse.startsWith("</pre>")) {
                sourceToParse = "";
            }*/
        }
        for (FileDataListing item:fileDataListing
             ) {
            Log.d("listing item name:", item.getName());
            Log.d("listing item date:", item.getDate());
            Log.d("listing item time:", item.getTime());
            Log.d("listing item urlpath:", item.getfilePathURL());
        }
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
        return fileDataListing;
    }

}
