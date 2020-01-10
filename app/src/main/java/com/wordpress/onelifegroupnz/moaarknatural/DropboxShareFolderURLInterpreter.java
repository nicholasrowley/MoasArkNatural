package com.wordpress.onelifegroupnz.moaarknatural;

import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class DropboxShareFolderURLInterpreter {

    public static String getShareURLFileSystem(String url) throws IOException {
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

        String result = html.toString().replaceFirst("^.*(?=(https://www.dropbox.com/sh/velh8ofxf1htg4o))", "");

        Log.d("source code length:", String.valueOf(html.toString().length()));
        Log.d("source code indexed:", result.replaceFirst("\",(.*)", ""));
        return html.toString();
    }

}
