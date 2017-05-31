package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Context;

import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DROPBOXTIMEOUTLIMIT;

/**
 * Created by Nicholas on 19/03/2017.
 */

public class RefreshDropboxThread implements Runnable {

    private String token;
    private Context activityContext;
    private String stringSearch;
    private FileLister lister;
    private String path;

    public RefreshDropboxThread(String ACCESS_TOKEN, Context context, String searchString,
                                String folderPath) {
        // store parameter for later user
        token = ACCESS_TOKEN;
        activityContext = context;
        stringSearch = searchString;
        path = folderPath;

    }

    public void run() {
        lister = new FileLister(DropboxClient.getClient(token),
                activityContext, new ArrayList<Metadata>(), new ArrayList<FileData>(), stringSearch, path);
        lister.execute();
        try {
            lister.get(DROPBOXTIMEOUTLIMIT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
