package com.wordpress.onelifegroupnz.moaarknatural;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

/**
 * Description:
 * Creates the connection and authentication with the Dropbox account
 * Created by Nicholas Rowley on 1/26/2017.
 */
public class DropboxClient {

    public static DbxClientV2 getClient(String ACCESS_TOKEN) {
        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder("MoaArkNatural/v1.0").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        return client;
    }
}
