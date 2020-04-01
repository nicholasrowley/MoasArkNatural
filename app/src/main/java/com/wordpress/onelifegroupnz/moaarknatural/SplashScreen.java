package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

/** Loading screen when app is opened. Also can process notification payloads*/
public class SplashScreen extends AppCompatActivity {

    private GlobalAppData appData;
    private static final String TAG = "SplashScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initialiseAds();

        final Thread warningThread = new Thread(){
            public void run(){
                try{
                    sleep(30000);
                    Thread warningMessage = new Thread() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Slow Internet Detected", Toast.LENGTH_SHORT).show();
                        }
                    };
                    runOnUiThread(warningMessage);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        };

        Thread timerThread = new Thread(){
            public void run(){
                try{
                    warningThread.start();
                    sleep(100);
                    appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), SplashScreen.this, "");
                    sleep(100);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }finally{
                    //cancel warning
                    warningThread.interrupt();
                    //checks for notification then starts activity
                    startApplication();
                }
            }
        };
        timerThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    //checks the notification for any readable data then starts the application.
    public void startApplication() {
        FileDataListing video = null;
        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
                for ( FileDataListing fileData : appData.getVideoData(GlobalAppData.DANCEVIDEOPATH)) {
                    if (value != null)
                        if (key.toLowerCase().equals("LDVIDEO".toLowerCase()) &&
                                value.toString().toLowerCase()
                                        .equals(fileData.getName().replaceFirst("[.][^.]+$", "")
                                                .toLowerCase())) {
                            video = fileData;
                        }
                }
                for ( FileDataListing fileData : appData.getVideoData(GlobalAppData.FOODVIDEOPATH)) {
                    if (value != null)
                        if (key.toLowerCase().equals("FVIDEO".toLowerCase()) &&
                                value.toString().toLowerCase()
                                        .equals(fileData.getName().replaceFirst("[.][^.]+$", "")
                                                .toLowerCase())) {
                            video = fileData;
                        }
                }
            }
        }
        // [END handle_data_extras]

        //if video data was defined in the data payload
        if (video != null) {
            //Proceed to View_Video
            Intent intent = new Intent(this, ViewVideo.class);
            intent.putExtra("videoData", video);
            intent.putExtra("shouldStart", true);
            startActivity(intent);
        }
        else
        {
            //startup like normal
            Intent intent = new Intent(SplashScreen.this, Home.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    /* Starts up Google AdMob Ads */
    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
    }

}
