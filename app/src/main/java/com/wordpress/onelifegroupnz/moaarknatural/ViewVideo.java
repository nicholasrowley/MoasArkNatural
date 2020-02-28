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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

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
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.STEPSHEETPATH;

/*- Plays dropbox videos in Android videoview (Note: all videos must be encoded in H.264 Baseline to guarantee
* playability in Android 5.0 or lower.)
* - Shows a pdf that matches the video content (if there is one)*/
public class ViewVideo extends AppCompatActivity {

    private GlobalAppData appData; //singleton instance of globalAppData
    private FileDataListing videoData; //single video data object
    private FileDataListing pdfData; //single stepsheet data object
    private Boolean dialogIsOpen; //ensure that only one video/wifi error dialog is displayed
    private Toolbar toolbar;

    private ProgressBar progressBar;
    private VideoView videoView;
    private Integer savedVideoPosition; //the current position of the video
    private boolean refreshed;
    private boolean portraitView;

    private SearchView searchView;
    private LinearLayout portraitItems;
    private WebView webview;
    private boolean pdfPixelTestOnOrientationChange; //checks if the pdf webview needs to be checked on next orientation change to portrait.
    private LinearLayout videoContainer;

    private CustomSearchFragment searchFragment;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ProgressBar refreshProgressbar;

    boolean pdfIsRedirecting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_video);
        setTitle(R.string.app_name);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_green);
        findViewById(R.id.search_fragment).setVisibility(View.GONE);

        findViewById(R.id.pdfReloadMessage).setVisibility(View.GONE);

        portraitItems = findViewById(R.id.portraitItems);
        videoContainer = findViewById(R.id.videoContainer);
        webview = findViewById(R.id.webview);

        //vid view imp onCreate code
        videoView = findViewById(R.id.videoView);
        refreshed = false;

        pdfPixelTestOnOrientationChange = false;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            videoData = (FileDataListing) extras.getSerializable("videoIndex");
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
                                    if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + DANCEVIDEOPATH)) {
                                        //Proceed to Line Dance video gallery
                                        Intent intent = new Intent(ViewVideo.this, VideoGallery.class);
                                        intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                                        startActivity(intent);
                                    } else if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + FOODVIDEOPATH)) {
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

        appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT),
                ViewVideo.this, "");

        addSearchFragment();

        refreshProgressbar = findViewById(R.id.refreshProgress);

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
            if (inm != null) {
                inm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }

        webview.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
        webview.clearCache(true);

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        webview.loadUrl("about:blank");

        webview.onPause();
        webview.destroyDrawingCache();

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call webview.resumeTimers().
        webview.pauseTimers();
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webview.onResume();
        webview.resumeTimers();
        videoView.resume();
        if (!videoView.isPlaying()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        pdfIsRedirecting = false;
        loadActivity();
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

        setUpSearchBar(menu);

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
                    loadPdf();
                }
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form on Moa's Ark website
                Uri uri = Uri.parse(getString(R.string.website_contact_form_url));
                intent = new Intent(Intent.ACTION_VIEW, uri);
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

            final Uri video = Uri.parse(videoData.getfilePathURL());
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

    /* Prepares the activity based on its orientation. Run this after a orientation change.*/
    public void setOrientation() {
        portraitView = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        View decorView = getWindow().getDecorView();

        ViewGroup.LayoutParams contParams = videoContainer.getLayoutParams();
        ViewGroup.LayoutParams params = videoView.getLayoutParams();

        TextView videoTitle;
        //Check if screen is in portrait or landscape mode.
        if (portraitView) {

            videoTitle = findViewById(R.id.txtVideoTitle);
            videoTitle.setText(videoData.getName());
            toolbar.setVisibility(View.VISIBLE);
            findViewById(R.id.toolbarUnderline).setVisibility(View.VISIBLE);
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            portraitItems.setVisibility(View.VISIBLE);

            contParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    200, getResources().getDisplayMetrics());
            params.height = MATCH_PARENT;

            videoContainer.setLayoutParams(contParams);
            videoView.setLayoutParams(params);

            //Webview testing is done on another thread to avoid pausing the UI Thread.
            final Thread waitForWebview = new Thread() {
                public void run() {
                        //wait for webview to draw itself before testing webview.
                        while(!(webview.getMeasuredHeight() > 0)) {
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            };

            //Webview testing is done on another thread to avoid pausing the UI Thread.
            final Thread webviewTestThread = new Thread() {
                public void run() {
                    testWebview(webview);
                }
            };

            final Thread webviewPixelTest = new Thread() {
                public void run() {
                    waitForWebview.start();
                    try {
                        waitForWebview.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(webviewTestThread);
                }
            };
        if (pdfPixelTestOnOrientationChange) {
            webviewPixelTest.start();
            pdfPixelTestOnOrientationChange = false;
        }
        } else {
            //hide toolbar and status bar
            toolbar.setVisibility(View.GONE);
            findViewById(R.id.toolbarUnderline).setVisibility(View.GONE);
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

    /* Loads the pdf from the server from scratch and displays it in the Android Webview */
    private void loadPdf() {
        //Data load is done here
        final Thread loadTask = new Thread() {
            public void run() {
                int loadAttempts = 0;
                do {
                    //check for pdf data
                    pdfData = appData.getPdfContent(getString(R.string.DIRECTORY_ROOT), videoData.getName(), videoData.getFolderPath());
                    loadAttempts++;
                } while (loadAttempts < 5 && !appData.dbSuccess(STEPSHEETPATH)); //same result regardless of using STEPSHEETPATH or RECIPEPATH
            }
        };

        //Loading UI Elements in this thread
        final Thread setTask = new Thread() {
            @SuppressLint("SetJavaScriptEnabled")
            public void run() {
                TextView noPdfMsg = findViewById(R.id.noSheetMsg);
                noPdfMsg.setVisibility(View.GONE);
                if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + DANCEVIDEOPATH)) {
                    noPdfMsg.setText(R.string.stepsheet_not_found);
                } else if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + FOODVIDEOPATH)) {
                    noPdfMsg.setText(R.string.recipe_not_found);
                }

                //if a matching pdf is found
                if (!pdfData.getName().equals("")) {
                    findViewById(R.id.pdfReloadMessage).setVisibility(View.GONE);
                    noPdfMsg.setVisibility(View.GONE);

                    findViewById(R.id.pdfProgressBar5).setVisibility(View.VISIBLE);
                    loadWebview();
                } else {
                    //hide pdf view and display appropriate message.
                    webview.setVisibility(View.GONE);

                    boolean requireStepsheet = videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + DANCEVIDEOPATH);
                    boolean requireRecipe = videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + FOODVIDEOPATH);

                    if ((!appData.dbSuccess(GlobalAppData.STEPSHEETPATH) && requireStepsheet)
                            || (!appData.dbSuccess(GlobalAppData.RECIPEPATH) && requireRecipe) ) {
                        findViewById(R.id.pdfReloadMessage).setVisibility(View.VISIBLE);
                        TextView pdfReloadText = findViewById(R.id.pdfReloadText);
                        if (requireStepsheet)
                            pdfReloadText.setText(getString(R.string.pdf_reload_text_stepsheet));
                        else if (requireRecipe)
                            pdfReloadText.setText(getString(R.string.pdf_reload_text_recipe));
                    }
                    else {
                        noPdfMsg.setVisibility(View.VISIBLE);
                        findViewById(R.id.pdfReloadMessage).setVisibility(View.GONE);
                    }
                }
                findViewById(R.id.pdfProgressBar5).setVisibility(View.GONE);
            }
        };

        //start background loader in a separate thread
        final Thread startLoad = new Thread() {
            public void run() {
                //attempt a reload up to 5 times for pdfdata if connection fails.
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

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebview() {
        //display pdf
        webview.getSettings().setJavaScriptEnabled(true);

        final String pdf = pdfData.getfilePathURL();
        webview.loadUrl("https://docs.google.com/viewer?url=" + pdf);
        webview.setVisibility(View.VISIBLE);
        webview.getSettings().setBuiltInZoomControls(true); //allows zoom controls and pinch zooming.
        webview.getSettings().setDisplayZoomControls(false); //hides webview zoom controls
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                //If redirecting then we don't want to reload the webview if it hasn't loaded.
                if (!pdfIsRedirecting) {
                    //we don't want to test the webview if it is not visible on the screen.
                    if (view.getMeasuredHeight() > 0) {
                        testWebview(view);
                    } else {
                        //if not visible then postpone the test until orientation is changed to portrait.
                        pdfPixelTestOnOrientationChange = true;
                    }
                }
            }

            //This method runs on all versions before Nougat
            //@SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                pdfIsRedirecting = true; //activity should be flagged as redirecting to avoid testing the webview when the activity is inactive.
                view.reload();
                //urls format spaces as %20 which needs to be done during comparison.
                if (url.contains("print=true") || url.replaceAll(" ", "%20").equals(pdf.replaceAll(" ", "%20"))) {
                    //run the following if print or open original document are pressed.
                    Uri uri = Uri.parse(pdf);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } else if (url.contains("google.com/ServiceLogin")) {
                    //run the following if sign in is pressed.
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
                return false;
            }

            //This method runs on all versions after and including Nougat
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                pdfIsRedirecting = true; //activity should be flagged as redirecting to avoid testing the webview when the activity is inactive.
                Uri url = request.getUrl();
                view.reload();
                //urls format spaces as %20 which needs to be done during comparison.
                if (url.toString().contains("print=true") || url.toString().replaceAll(" ", "%20").equals(pdf.replaceAll(" ", "%20"))) {
                    //run the following if print or open original document are pressed.
                    Uri uri = Uri.parse(pdf);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } else if (url.toString().contains("google.com/ServiceLogin")) {
                    //run the following if sign in is pressed.
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(intent);
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Make a note about the failed load.
                webview.setVisibility(View.GONE);
                findViewById(R.id.pdfReloadMessage).setVisibility(View.VISIBLE);
            }
        });
    }

    //Converts a View to Bitmap
    private static Bitmap toBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /*Tests the webview for an image. Should only run when webview is on the screen.
    * This is based on the assumption that the first pixel is never white when the webview loads successfully.*/
    private void testWebview(WebView view) {
        int pixelDrawTest = toBitmap(view).getPixel(0,0);

        /*In this case the first pixel in the webview is never white so this can be used to test
        if the webview has failed to load in case of a javascript loading issue. */
        if(pixelDrawTest == Color.WHITE) {
            loadWebview();
        }
    }

    /* Starts Google AdMob ads for this activity. */
    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id));

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    /* Sets up the search options so that it is ready to be used by the search bar. Should be run before the search bar is expanded the first time. */
    private void addSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        searchFragment = new CustomSearchFragment();
        transaction.add(R.id.search_fragment, searchFragment);
        transaction.commit();
    }

    /* Provides the setup necessary to get the search bar working.*/
    private void setUpSearchBar(Menu menu) {
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (androidx.appcompat.widget.SearchView) menu.findItem(R.id.search).getActionView();
        if (searchManager != null) {
            searchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getComponentName()));
        }

        //disable default search icon next to search box
        ImageView searchImage = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
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
                if (imm != null) {
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
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

    /* onClick method for the pdf refresh button */
    public void onClickPDFRefresh(View v) {
        findViewById(R.id.pdfReloadMessage).setVisibility(View.GONE);
        loadPdf();
    }
}
