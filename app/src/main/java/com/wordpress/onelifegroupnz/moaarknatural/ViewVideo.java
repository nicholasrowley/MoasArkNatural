package com.wordpress.onelifegroupnz.moaarknatural;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;

/*- Plays dropbox videos in Android videoview (Note: all videos must be encoded in H.264 Baseline to guarantee
* playability in Android 5.0 or lower.)
* - Shows a pdf that matches the video content (if there is one)*/
public class ViewVideo extends AppCompatActivity {

    private GlobalAppData appData; //singleton instance of globalAppData
    private FileData videoData; //single video data object
    private FileData pdfData; //single stepsheet data object
    private Boolean dialogIsOpen; //ensure that only one video/wifi error dialog is displayed
    private Toolbar toolbar;

    //private static ProgressDialog progressDialog; progress bar will replace dialog
    private ProgressBar progressBar;
    private VideoView videoView;
    private Integer savedVideoPosition; //the current position of the video
    private boolean refreshed;
    private boolean portraitView;

    private SearchView searchView;
    private LinearLayout portraitItems;
    private WebView webview;
    private LinearLayout videoContainer;

    private CustomSearchFragment searchFragment;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ProgressBar refreshProgressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_video);
        setTitle(R.string.app_name);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home);

        portraitItems = findViewById(R.id.portraitItems);
        videoContainer = findViewById(R.id.videoContainer);
        webview = findViewById(R.id.webview);

        //vid view imp onCreate code
        videoView = findViewById(R.id.videoView);
        refreshed = false;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            videoData = (FileData) extras.getSerializable("videoIndex");
        }

        //check if activity refreshed
        if (savedInstanceState != null) {
            savedVideoPosition = savedInstanceState.getInt("VideoTime");
            refreshed = true;
        }
        dialogIsOpen = false;

        //progress bar shows when video is buffering
        progressBar = findViewById(R.id.progressBar3);

        //set up lister to handle VideoView errors
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (!dialogIsOpen) {
                    dialogIsOpen = true;
                    new AlertDialog.Builder(ViewVideo.this)
                            .setTitle("Video can't be played")
                            .setMessage("Please check your connection and reload video")
                            .setPositiveButton("Reload Video", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //Reload ViewVideo
                                    dialogIsOpen = false;
                                    loadActivity();
                                }
                            })
                            .setNegativeButton("Return to Gallery", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialogIsOpen = false;
                                    if (videoData.getFolderPath().equals(DANCEVIDEOPATH)) {
                                        //Proceed to Line Dance video gallery
                                        Intent intent = new Intent(ViewVideo.this, VideoGallery.class);
                                        intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                                        startActivity(intent);
                                    } else if (videoData.getFolderPath().equals(FOODVIDEOPATH)) {
                                        //Proceed to Food video gallery
                                        Intent intent = new Intent(ViewVideo.this, VideoGallery.class);
                                        intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                                        startActivity(intent);
                                    }

                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .show();
                }
                return true;
            }
        });

        appData = GlobalAppData.getInstance(getString(R.string.ACCESS_TOKEN),
                ViewVideo.this, "");

        addSearchFragment();

        refreshProgressbar = findViewById(R.id.refreshProgress);

        loadActivity();

        setOrientation();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            Toast.makeText(getApplicationContext(), "Switch to landscape for fullscreen view", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Switch to portrait to exit fullscreen view", Toast.LENGTH_SHORT).show();

        initialiseAds();
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager inm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focusedView = this.getCurrentFocus();
        if (focusedView != null)
            inm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.VISIBLE);
    }


    // Save UI state changes to the savedInstanceState.
    // This bundle will be passed to onCreate if the process is
    // killed and restarted.
    //Also used for fragments
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save the current position of the video, used when orientation changes
        outState.putInt("VideoTime", videoView.getCurrentPosition());

        outState.putBoolean("fragment_added", true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        setUpSearchbar(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                startActivity(new Intent(ViewVideo.this, Home.class));
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(ViewVideo.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(ViewVideo.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_refresh:
                if(!(refreshProgressbar.getVisibility() == View.VISIBLE)) {
                    refreshProgressbar.setVisibility(View.VISIBLE);
                    loadActivity();
                }
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form
                intent = new Intent(ViewVideo.this, ContactForm.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //video view imp play method
    private void PlayVideo() {
        progressBar.setVisibility(View.VISIBLE);
        try {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);
            MediaController mediaController;

            //define media controller behaviour based on screen orientation
            if (portraitView) {
                mediaController = new MediaController(this) {
                    public boolean dispatchKeyEvent(KeyEvent event) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction()
                                == KeyEvent.ACTION_UP)
                            ((Activity) getContext()).finish();

                        return super.dispatchKeyEvent(event);
                    }

                    @Override
                    public void show() {
                        super.show(5000);
                    }
                };
            } else { //if in landscape view
                mediaController = new MediaController(ViewVideo.this) {
                    //hide after 5 seconds
                    @Override
                    public void show() {
                        super.show(5000);
                    }
                };
            }

            //set up videoView
            mediaController.setAnchorView(videoView);

            final Uri video = Uri.parse(videoData.getTempUrl());
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(video);
            videoView.requestFocus();
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                //@Override
                public void onPrepared(MediaPlayer mp) {

                    //if refreshed continue from last know position
                    if (savedVideoPosition != null && refreshed) {
                        videoView.seekTo(savedVideoPosition);
                        refreshed = false;
                    }

                    //Simulates the onTouchEvent to show the Media controller
                    if (portraitView) {
                        videoView.dispatchTouchEvent(MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis() + 100,
                                MotionEvent.ACTION_DOWN,
                                0.0f,
                                0.0f,
                                0
                                )
                        );
                    }

                    progressBar.setVisibility(View.GONE);

                    videoView.start();

                    //set the video frame to match the video
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    ViewGroup.LayoutParams params = videoView.getLayoutParams();
                    ViewGroup.LayoutParams contParams = videoContainer.getLayoutParams();
                    if (portraitView) {
                        params.height = MATCH_PARENT;
                        contParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                200, getResources().getDisplayMetrics());
                    } else {
                        params.height = displayMetrics.heightPixels;
                        contParams.height = displayMetrics.heightPixels;
                    }

                    videoView.setLayoutParams(params);
                    videoContainer.setLayoutParams(contParams);

                    // Obtain the FirebaseAnalytics instance.
                    mFirebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());

                    //log when the video starts
                    Bundle vsparams = new Bundle();
                    vsparams.putDouble(FirebaseAnalytics.Param.VALUE, 1.0);
                    mFirebaseAnalytics.logEvent(videoData.getVideoStatsName(), vsparams);
                }
            });
        } catch (Exception e) {
            System.out.println("Video Play Error :" + e.toString());
            finish();
        }
    }

    //Opens the app setting so the user can turn notifications on or off
    public void openAppSettings() {
        String packageName = getString(R.string.package_name);

        try {
            //Open the specific App Info page:
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(ViewVideo.this)
                    .setTitle("Notification Settings Not Available")
                    .setMessage("Unable to open the apps settings screen, please try again later")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setOrientation();
    }

    public void setOrientation() {
        portraitView = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        View decorView = getWindow().getDecorView();

        ViewGroup.LayoutParams contParams = videoContainer.getLayoutParams();
        ViewGroup.LayoutParams params = videoView.getLayoutParams();

        TextView videoTitle;
        //Check if in portrait or landscape
        if (portraitView) {
            videoTitle = findViewById(R.id.txtVideoTitle);
            videoTitle.setText(videoData.getName());
            toolbar.setVisibility(View.VISIBLE);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            portraitItems.setVisibility(View.VISIBLE);

            contParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    200, getResources().getDisplayMetrics());
            params.height = MATCH_PARENT;

            videoContainer.setLayoutParams(contParams);
            videoView.setLayoutParams(params);
        } else {
            //hide toolbar and status bar
            toolbar.setVisibility(View.GONE);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            portraitItems.setVisibility(View.GONE);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            contParams.height = displayMetrics.heightPixels;
            params.height = displayMetrics.heightPixels;

            videoContainer.setLayoutParams(contParams);
            videoView.setLayoutParams(params);
        }
    }

    private void loadPdf() {
        //Data load is done here
        final Thread loadTask = new Thread() {
            public void run() {
                try {
                    //check for pdf data
                    pdfData = appData.getPdfContent(getString(R.string.ACCESS_TOKEN), videoData.getName(), videoData.getFolderPath());
                    //refreshDialog.show(); //generally bad to show a toast with something that can be executed repetitively
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Loading UI Elements in this thread
        final Thread setTask = new Thread() {
            @SuppressLint("SetJavaScriptEnabled")
            public void run() {
                TextView noPdfMsg = findViewById(R.id.noSheetMsg);
                if (videoData.getFolderPath().equals(DANCEVIDEOPATH)) {
                    noPdfMsg.setText(R.string.stepsheet_not_found);
                } else if (videoData.getFolderPath().equals(FOODVIDEOPATH)) {
                    noPdfMsg.setText(R.string.recipe_not_found);
                }

                //if a matching pdf is found
                if (!pdfData.getName().equals("")) {
                    noPdfMsg.setVisibility(View.GONE);
                    //display pdf
                    webview.getSettings().setJavaScriptEnabled(true);

                    final String pdf = pdfData.getTempUrl();
                    webview.setVisibility(View.GONE);
                    webview.loadUrl("https://docs.google.com/viewer?url=" + pdf);
                    webview.reload();
                    webview.setVisibility(View.VISIBLE);
                    webview.getSettings().setBuiltInZoomControls(true); //allows zoom controls and pinch zooming.
                    webview.getSettings().setDisplayZoomControls(false); //hides webview zoom controls
                    webview.setWebViewClient(new WebViewClient() {

                        @SuppressWarnings("deprecation")
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            view.reload();
                            if (url.contains("print=true") || url.equals(pdf)) {
                                Uri uri = Uri.parse(pdf);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            } else if (url.contains("google.com/ServiceLogin")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(intent);
                            }
                            return true;
                        }

                        @TargetApi(Build.VERSION_CODES.N)
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            Uri url = request.getUrl();
                            view.reload();
                            if (url.toString().contains("print=true") || url.toString().equals(pdf)) {
                                Uri uri = Uri.parse(pdf);
                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                            } else if (url.toString().contains("google.com/ServiceLogin")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, url);
                                startActivity(intent);
                            }
                            return true;
                        }
                    });
                } else {
                    //hide pdf view and display appropriate message.
                    webview.setVisibility(View.GONE);
                    noPdfMsg.setVisibility(View.VISIBLE);
                }
            }
        };

        //start background loader in a separate thread
        final Thread startLoad = new Thread() {
            public void run() {
                loadTask.start();
                try {
                    loadTask.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(setTask);
            }

        };

        startLoad.start();
    }

    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id_live));

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void addSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        searchFragment = new CustomSearchFragment();
        transaction.add(R.id.search_fragment, searchFragment);
        transaction.commit();
    }

    private void setUpSearchbar(Menu menu) {
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (android.support.v7.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //disable default search icon next to search box
        ImageView searchImage = searchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        ViewGroup LayoutSearchView =
                (ViewGroup) searchImage.getParent();
        LayoutSearchView.removeView(searchImage);

        final LinearLayout searchFragmentLayout = findViewById(R.id.search_fragment);
        searchFragmentLayout.setVisibility(View.GONE);

        MenuItem searchItem = menu.findItem(R.id.search);

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchFragmentLayout.setVisibility(View.GONE);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchFragmentLayout.setVisibility(View.VISIBLE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                searchView.setIconifiedByDefault(false);
                searchView.setFocusable(true);
                searchView.requestFocusFromTouch();

                //set width of search view
                searchView.setMaxWidth(Integer.MAX_VALUE);

                return true;
            }
        });
    }

    /* Begins loading for elements in the activity.*/
    public void loadActivity() {
        refreshProgressbar.setProgress(0);
        refreshProgressbar.setVisibility(View.VISIBLE);

        final ProgressBarAnimation anim = new ProgressBarAnimation(refreshProgressbar, 0, 80);
        anim.setDuration(3040);
        refreshProgressbar.startAnimation(anim);

        loadPdf();

        PlayVideo();

        finishLoading();
    }

    /* A method for completing the progress bar animation on the toolbar*/
    private void finishLoading() {
        final Thread finishLoading = new Thread() {
            public void run() {
                ProgressBarAnimation anim5 = new ProgressBarAnimation(refreshProgressbar, refreshProgressbar.getProgress(), 100);
                anim5.setDuration(1000);
                refreshProgressbar.startAnimation(anim5);
            }
        };

        final Thread setProgressComplete = new Thread() {
            public void run() {
                refreshProgressbar.setVisibility(View.GONE);
            }
        };

        Thread waitForRefresh = new Thread() {
            public void run() {
                runOnUiThread(finishLoading);
                try {
                    finishLoading.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(setProgressComplete);
            }
        };
        waitForRefresh.start();
    }
}
