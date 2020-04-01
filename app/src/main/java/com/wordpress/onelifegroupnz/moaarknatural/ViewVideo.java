package com.wordpress.onelifegroupnz.moaarknatural;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaSeekOptions;
import com.google.android.gms.common.images.WebImage;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.STEPSHEETPATH;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;

/*- Plays dropbox videos in Android videoview (Note: all videos must be encoded in H.264 Baseline to guarantee
* playability in Android 5.0 or lower.)
* - Shows a pdf that matches the video content (if there is one)*/
public class ViewVideo extends AppCompatActivity {

    private static final String TAG = "ViewVideo";
    private GlobalAppData appData; //singleton instance of globalAppData
    private FileDataListing videoData; //single video data object
    private FileDataListing pdfData; //single stepsheet data object
    private FileDataListing castImageData; //single cast image data object
    private WebImage castImage;
    private Boolean dialogIsOpen; //ensure that only one video/wifi error dialog is displayed
    private Toolbar toolbar;
    private SharedPreferences settings;

    private ProgressBar progressBar;
    private VideoView videoView;
    private boolean refreshed;
    private boolean portraitView;

    private RelativeLayout mControllers;
    private TextView mStartText;
    private TextView mEndText;
    private SeekBar mSeekbar;
    private ImageView mPlayPause;
    private ImageButton mPlayCircle;
    private Timer mSeekbarTimer;
    private Timer mControllersTimer;
    private PlaybackLocation mLocation;
    private PlaybackState mPlaybackState;
    private Button mPreVidBtn;
    private Button mNextVidBtn;
    private final Handler mHandler = new Handler();
    private int mDuration;
    private boolean mControllersVisible;
    private boolean videoReloadInProgress;
    boolean shouldStartPlayback;
    int startPosition; //the current position to start the video
    private boolean savedInstanceExists;
    private String videoTypePath;
    private int videoIndex;
    private boolean wasSearched;
    private List<FileDataListing> searchList;

    private SearchView searchView;
    private LinearLayout portraitItems;
    private WebView webview;
    private int pdfTestAttempts; //prevents running to many tests
    public static final int PDFLOADRETRIES = 5; //number of retries for loading PDF if test fails
    public static final int PDFLOADTIMEOUT = 5000; //timeout period in milliseconds to avoid pdf load testing too quickly

    private boolean pdfPixelTestOnOrientationChange; //checks if the pdf webview needs to be checked on next orientation change to portrait.
    private LinearLayout videoContainer;

    private CustomSearchFragment searchFragment;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ProgressBar refreshProgressbar;

    boolean pdfIsRedirecting;

    private CastContext mCastContext;
    private MenuItem mediaRouteMenuItem;
    private CastSession mCastSession;
    private SessionManagerListener<CastSession> mSessionManagerListener;
    private boolean castSessionLoading;


    /**
     * indicates whether we are doing a local or a remote playback
     */
    public enum PlaybackLocation {
        LOCAL,
        REMOTE
    }

    /**
     * List of various states that we can be in
     */
    public enum PlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE
    }

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

        //vid view imp onCreate code
        videoView = findViewById(R.id.videoView);
        refreshed = false;

        mControllers = findViewById(R.id.controllers);
        mStartText = (TextView) findViewById(R.id.startText);
        mStartText.setText(formatMillis(0));
        mEndText = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar);
        mPlayPause = (ImageView) findViewById(R.id.imageView);
        mPlayCircle = (ImageButton) findViewById(R.id.play_circle);
        mPlayCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayback();
                videoReloadInProgress = true;
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        setupControlsCallbacks();
        setupCastListener();
        try {
            mCastContext = CastContext.getSharedInstance(this);
            mCastSession = mCastContext.getSessionManager().getCurrentCastSession();
        } catch (RuntimeException re) {
            re.printStackTrace();
            //display message to user.
            Toast.makeText(getApplicationContext(), getString(R.string.play_services_error_toast), Toast.LENGTH_SHORT).show();
        }

        portraitItems = findViewById(R.id.portraitItems);
        videoContainer = findViewById(R.id.videoContainer);
        webview = findViewById(R.id.webview);

        //progress bar shows when video is buffering
        progressBar = findViewById(R.id.progressBar3);

        videoContainer.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mControllersVisible) {
                    updateControllersVisibility(true);
                }
                startControllersTimer();
                return false;
            }
        });



        pdfPixelTestOnOrientationChange = false;

        appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT),
                ViewVideo.this, "");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            videoData = (FileDataListing) extras.getSerializable("videoData");
            videoIndex = extras.getInt("videoIndex", -1);
            //TODO get searchList
            wasSearched = extras.getBoolean("wasSearched", false);

            if (wasSearched)
                searchList = appData.getVideoListFromLastSearchResult();

            shouldStartPlayback = extras.getBoolean("shouldStart");
            startPosition = extras.getInt("startPosition", 0);
        }

        //check if activity refreshed
        if (savedInstanceState != null) {
            startPosition = savedInstanceState.getInt("VideoTime", 0);
            refreshed = true;
        }

        // Whenever the activity is created, set the video position based on bundles.
        this.settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putInt("videoPosition", startPosition);
        editor.apply();

        dialogIsOpen = false;

        setupControlsCallbacks();

        savedInstanceExists = savedInstanceState != null;
        if (!savedInstanceExists) {
            addSearchFragment();
        }

        refreshProgressbar = findViewById(R.id.refreshProgress);

        //set up previous and next video buttons
        mPreVidBtn = findViewById(R.id.preVidBtn);
        mNextVidBtn = findViewById(R.id.nextVidBtn);

        //enable on touch if videos are available
        if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + DANCEVIDEOPATH)) {
            videoTypePath = DANCEVIDEOPATH;
        } else if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + FOODVIDEOPATH)) {
            videoTypePath = FOODVIDEOPATH;
        }

        if (videoTypePath != null) {
            Log.d("index: ", Integer.toString(appData.getVideoData(videoTypePath).indexOf(videoData)));
            if (videoIndex > 0) {
                mPreVidBtn.setTextColor(Color.WHITE);
                //TODO DO NOT USE ONTOUCH!!!!
                mPreVidBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (wasSearched) {
                            Toast.makeText(getApplicationContext(), "Displaying previous video in search results", Toast.LENGTH_SHORT).show();
                        } else if(videoTypePath.equals(DANCEVIDEOPATH)) {
                            Toast.makeText(getApplicationContext(), "Displaying previous video in " + getString(R.string.title_activity_dance_video_gallery), Toast.LENGTH_SHORT).show();
                        } else if(videoTypePath.equals(FOODVIDEOPATH)) {
                            Toast.makeText(getApplicationContext(), "Displaying previous video in " + getString(R.string.title_activity_food_video_gallery), Toast.LENGTH_SHORT).show();
                        }
                        seekToVideoID(videoIndex - 1);
                    }
                });
            } else {
                mPreVidBtn.setTextColor(Color.GRAY);
            }

            if ((!wasSearched && (videoIndex < appData.getVideoData(videoTypePath).size() - 1)) || (wasSearched && (videoIndex < searchList.size() - 1))) {
                mNextVidBtn.setTextColor(Color.WHITE);
                mNextVidBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (wasSearched) {
                            Toast.makeText(getApplicationContext(), "Displaying next video in search results", Toast.LENGTH_SHORT).show();
                        } else if(videoTypePath.equals(DANCEVIDEOPATH)) {
                            Toast.makeText(getApplicationContext(), "Displaying next video in " + getString(R.string.title_activity_dance_video_gallery), Toast.LENGTH_SHORT).show();
                        } else if(videoTypePath.equals(FOODVIDEOPATH)) {
                            Toast.makeText(getApplicationContext(), "Displaying next video in " + getString(R.string.title_activity_food_video_gallery), Toast.LENGTH_SHORT).show();
                        }
                        seekToVideoID(videoIndex + 1);
                    }
                });
            } else {
                mNextVidBtn.setTextColor(Color.GRAY);
            }
        } else {
            mPreVidBtn.setTextColor(Color.GRAY);
            mNextVidBtn.setTextColor(Color.GRAY);
        }

        setOrientation();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            Toast.makeText(getApplicationContext(), "Switch to landscape for fullscreen view", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Switch to portrait to exit fullscreen view", Toast.LENGTH_SHORT).show();

        initialiseAds();

        mPlaybackState = PlaybackState.IDLE;
        updatePlaybackLocation(PlaybackLocation.LOCAL);
        updatePlayButton(mPlaybackState);
        castSessionLoading = false;
        pdfTestAttempts = 0;
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

        if (mCastContext != null) {
            mCastContext.getSessionManager().removeSessionManagerListener(
                    mSessionManagerListener, CastSession.class);
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
        //webview.pauseTimers(); //This also pauses ad banners on other activities
        videoView.pause();

        // Whenever application is paused, save the video position for future sessions.
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putInt("videoPosition", videoView.getCurrentPosition());
        editor.apply();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() was called");
        if (mCastContext != null) {
            mCastContext.getSessionManager().addSessionManagerListener(
                    mSessionManagerListener, CastSession.class);
        }
        if (mCastSession != null && mCastSession.isConnected()) {
            updatePlaybackLocation(PlaybackLocation.REMOTE);
        } else {
            updatePlaybackLocation(PlaybackLocation.LOCAL);
        }
        super.onResume();
        webview.onResume();
        webview.resumeTimers();

        // Gets values from last session of your application, check video position
        // start at 0 if no saved value was found (first session of your app)
        this.settings = getPreferences(MODE_PRIVATE);
        startPosition = settings.getInt("videoPosition", 0);

        if (!videoView.isPlaying()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        pdfIsRedirecting = false;
        Log.d(TAG, "Video start position is " + Integer.toString(startPosition));
        loadActivity();
    }

    // Save UI state changes to the savedInstanceState.
    // This bundle will be passed to onCreate if the process is
    // killed and restarted.
    //Also used for fragments
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save the current position of the video, used when activity is reopened
        outState.putInt("VideoTime", videoView.getCurrentPosition());

        //outState.putBoolean("shouldStart", true);
        outState.putBoolean("fragment_added", true);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        item = menu.findItem(R.id.menu_share_video);
        item.setVisible(true);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        setUpSearchBar(menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu, R.id.media_route_menu_item);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                intent = new Intent(ViewVideo.this, Home.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startNewActivity(intent);
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(ViewVideo.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startNewActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(ViewVideo.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startNewActivity(intent);
                return true;
            case R.id.menu_refresh:
                if(!(refreshProgressbar.getVisibility() == View.VISIBLE)) {
                    refreshProgressbar.setVisibility(View.VISIBLE);
                    loadActivity();
                }
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form on Moa's Ark website
                Uri uri = Uri.parse(getString(R.string.website_contact_form_url).replaceAll(" ", "%20"));
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            case R.id.menu_share_video:
                //Share video url with other apps
                if(videoData != null && !videoData.getfilePathURL().isEmpty()) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Shared " + videoData.getName() + " from Moa\'s Ark Natural NZ app. Download from " + getString(R.string.app_play_store_url) + "\n\n" + videoData.getfilePathURL().replaceAll(" ", "%20"));
                    sendIntent.setType("text/plain");

                    Intent shareIntent = Intent.createChooser(sendIntent, "Share Video URL with...");
                    startActivity(shareIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //video view play method (runs when activity is started/resumed)
    private void PlayVideo() {
        progressBar.setVisibility(View.VISIBLE);
        videoReloadInProgress = true;
        try {
            getWindow().setFormat(PixelFormat.TRANSLUCENT);

            //set up videoView
            final Uri video = Uri.parse(videoData.getfilePathURL().replaceAll(" ", "%20"));
            videoView.setVideoURI(video);
            videoView.requestFocus();
            videoView.pause();

            boolean isConnected = (mCastSession != null)
                    && (mCastSession.isConnected() ||
                    mCastSession.isConnecting());

            if (isConnected) {
                shouldStartPlayback = false;
            }

            if (shouldStartPlayback) {
                mPlaybackState = PlaybackState.PLAYING;
                updatePlaybackLocation(PlaybackLocation.LOCAL);
                updatePlayButton(mPlaybackState);
                if (startPosition > 0) {
                    videoView.seekTo(startPosition);
                }
                videoView.start();
                startControllersTimer();
            } else {
                // we should load the video but pause it
                if (mCastSession != null && mCastSession.isConnected()) {
                    updatePlaybackLocation(PlaybackLocation.REMOTE);
                } else {
                    updatePlaybackLocation(PlaybackLocation.LOCAL);
                }
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);
            }
        } catch (Exception e) {
            System.out.println("Video Play Error :" + e.toString());
            finish();
        }
    }

    //stops the seekbar from updating
    private void stopTrickplayTimer() {
        Log.d(TAG, "Stopped TrickPlay Timer");
        if (mSeekbarTimer != null) {
            mSeekbarTimer.cancel();
        }
    }

    //starts up / restarts the seekbar updates.
    private void restartTrickplayTimer() {
        stopTrickplayTimer();
        mSeekbarTimer = new Timer();
        mSeekbarTimer.scheduleAtFixedRate(new UpdateSeekbarTask(), 100, 1000);
        Log.d(TAG, "Restarted TrickPlay Timer");
    }

    //defines the seekbar updates on each tick.
    private class UpdateSeekbarTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (mLocation == PlaybackLocation.LOCAL) {
                        int currentPos = videoView.getCurrentPosition();
                        updateSeekbar(currentPos, mDuration);
                    }
                }
            });
        }
    }

    //updates the seekbar with new information.
    private void updateSeekbar(int position, int duration) {
        mSeekbar.setProgress(position);
        mSeekbar.setMax(duration);
        mStartText.setText(formatMillis(position));
        mEndText.setText(formatMillis(duration));
    }

    /**
     * updates controllers in response to playback location
     */
    private void updatePlaybackLocation(PlaybackLocation location) {
        mLocation = location;
        if (location == PlaybackLocation.LOCAL) {
            if (mPlaybackState == PlaybackState.PLAYING
                    || mPlaybackState == PlaybackState.BUFFERING) {
                startControllersTimer();
            } else {
                stopControllersTimer();
            }
        } else {
            stopControllersTimer();
            updateControllersVisibility(false);
        }
    }

    // should be called from the main thread
    private void updateControllersVisibility(boolean show) {
        if (show) {
            mControllers.setVisibility(View.VISIBLE);
        } else {
            mControllers.setVisibility(View.INVISIBLE);
        }
    }

    private void togglePlayback() {
        Log.d(TAG, "playback state: " + mPlaybackState.toString());
        Log.d(TAG, "location state: " + mLocation.toString());
        stopControllersTimer();
        switch (mPlaybackState) {
            case PAUSED:
                switch (mLocation) {
                    case LOCAL:
                        shouldStartPlayback = true;
                        videoView.start();
                        Log.d(TAG, "Playing locally...");
                        mPlaybackState = PlaybackState.PLAYING;
                        startControllersTimer();
                        restartTrickplayTimer();
                        updatePlaybackLocation(PlaybackLocation.LOCAL);
                        break;
                    case REMOTE:
                        finish();
                        break;
                    default:
                        break;
                }
                break;

            case PLAYING:
                mPlaybackState = PlaybackState.PAUSED;
                videoView.pause();
                break;

            case IDLE:
                switch (mLocation) {
                    case LOCAL:
                        Log.d(TAG, "Local state detected. Playing locally...");
                        videoView.setVideoURI(Uri.parse(videoData.getfilePathURL().replaceAll(" ", "%20")));
                        videoView.seekTo(0);
                        shouldStartPlayback = true;
                        videoView.start();
                        mPlaybackState = PlaybackState.PLAYING;
                        restartTrickplayTimer();
                        updatePlaybackLocation(PlaybackLocation.LOCAL);
                        break;
                    case REMOTE:
                        if (mCastSession != null && mCastSession.isConnected()) {
                            loadRemoteMedia(mSeekbar.getProgress(), true);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        updatePlayButton(mPlaybackState);
    }

    //cancels timer to show controller
    private void stopControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
    }

    //starts timer to show controller for 5 seconds
    private void startControllersTimer() {
        if (mControllersTimer != null) {
            mControllersTimer.cancel();
        }
        if (mLocation == PlaybackLocation.REMOTE) {
            return;
        }
        mControllersTimer = new Timer();
        mControllersTimer.schedule(new HideControllersTask(), 5000);
    }

    //Hides the media controller
    private class HideControllersTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateControllersVisibility(false);
                    mControllersVisible = false;
                }
            });

        }
    }

    //sets up necessary functions for videoview, seekbar, play/pause button.
    private void setupControlsCallbacks() {
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "OnErrorListener.onError(): VideoView encountered an "
                        + "error, what: " + what + ", extra: " + extra);

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
                                        finish();
                                    } else if (videoData.getFolderPath().equals(getString(R.string.DIRECTORY_ROOT) + FOODVIDEOPATH)) {
                                        //Proceed to Food video gallery
                                        Intent intent = new Intent(ViewVideo.this, VideoGallery.class);
                                        intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                                        startActivity(intent);
                                        finish();
                                    }

                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .show();
                }

                videoView.stopPlayback();
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);
                return true;
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, "onPrepared is reached");
                mDuration = mp.getDuration();
                mEndText.setText(formatMillis(mDuration));
                mSeekbar.setMax(mDuration);
                restartTrickplayTimer();
                if (!castSessionLoading) {
                    progressBar.setVisibility(View.GONE);
                }
                videoReloadInProgress = false;

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

                //set up a still image for the video
                videoView.start();
                if (!shouldStartPlayback || mLocation == PlaybackLocation.REMOTE)
                    videoView.pause();


            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                stopTrickplayTimer();
                mPlaybackState = PlaybackState.IDLE;
                updatePlayButton(mPlaybackState);
            }
        });

        videoView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mControllersVisible) {
                    updateControllersVisibility(true);
                }
                startControllersTimer();
                return false;
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mPlaybackState == PlaybackState.PLAYING) {
                    play(seekBar.getProgress());
                } else if (mPlaybackState != PlaybackState.IDLE) {
                    videoView.seekTo(seekBar.getProgress());
                }
                startControllersTimer();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopTrickplayTimer();
                videoView.pause();
                stopControllersTimer();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                mStartText.setText(formatMillis(progress));
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mLocation == PlaybackLocation.LOCAL) {
                    togglePlayback();
                }
            }
        });
    }

    //plays the video at the specified seekbar position on local or remote video player.
    private void play(int position) {
        startControllersTimer();
        switch (mLocation) {
            case LOCAL:
                videoView.seekTo(position);
                videoView.start();
                break;
            case REMOTE:
                mPlaybackState = PlaybackState.BUFFERING;
                castSessionLoading = true;
                updatePlayButton(mPlaybackState);
                mCastSession.getRemoteMediaClient().seek(new MediaSeekOptions.Builder().setPosition(position*1000).build());
                break;
            default:
                break;
        }
        restartTrickplayTimer();
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

    private void updatePlayButton(PlaybackState state) {
        Log.d(TAG, "Controls: PlayBackState: " + state);
        boolean isConnected = (mCastSession != null)
                && (mCastSession.isConnected() ||
                mCastSession.isConnecting());
        mControllers.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        mPlayCircle.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        switch (state) {
            case PLAYING:
                if(!videoReloadInProgress){
                    progressBar.setVisibility(View.GONE);
                }
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_pause));
                mPlayCircle.setVisibility(isConnected ? View.VISIBLE : View.GONE);
                break;
            case IDLE:
                Log.d(TAG, "Controls: IDLE castSessionLoading = " + Boolean.toString(castSessionLoading));
                mPlayCircle.setVisibility(!castSessionLoading ? View.VISIBLE : View.GONE);
                mControllers.setVisibility(View.GONE);
                break;
            case PAUSED:
                if(!videoReloadInProgress && !castSessionLoading){
                    progressBar.setVisibility(View.GONE);
                }
                mPlayPause.setVisibility(View.VISIBLE);
                mPlayPause.setImageDrawable(
                        getResources().getDrawable(R.drawable.ic_play));
                mPlayCircle.setVisibility(!castSessionLoading && isConnected ? View.VISIBLE : View.GONE);
                break;
            case BUFFERING:
                mPlayPause.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                break;
            default:
                break;
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
                    pdfTestAttempts = 0;
                    loadWebview();
                } else {
                    displayPDFReloadMessage();
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

    /* Prompts the user to try and reload pdf if pdf is unable to load on its own. */
    private void displayPDFReloadMessage() {
        //hide pdf view and display appropriate message.
        TextView noPdfMsg = findViewById(R.id.noSheetMsg);
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

    //loads the pdf webview.
    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebview() {
        //display pdf
        webview.getSettings().setJavaScriptEnabled(true);

        final String pdf = pdfData.getfilePathURL().replaceAll(" ", "%20");
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
    private void testWebview(final WebView view) {
        Log.d(TAG, "TESTING PDF ATTEMPT " + Integer.toString(pdfTestAttempts + 1));
        final Thread pdfWaitTask = new Thread() {
            public void run() {
                try {
                    Thread.sleep(PDFLOADTIMEOUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread pdfTestTask = new Thread() {
            public void run() {
                int pixelDrawTest = toBitmap(view).getPixel(0,0);

                /*In this case the first pixel in the webview is never white so this can be used to test
                if the webview has failed to load in case of a javascript loading issue. */
                if(pixelDrawTest == Color.WHITE) {
                    if (pdfTestAttempts < PDFLOADRETRIES) {
                        findViewById(R.id.pdfProgressBar5).setVisibility(View.VISIBLE);
                        loadWebview();
                        pdfTestAttempts++;
                    } else {
                        //show reload dialog message for pdf.
                        displayPDFReloadMessage();
                    }
                } else {
                    findViewById(R.id.pdfProgressBar5).setVisibility(View.GONE);
                }
            }
        };

        final Thread pdfTestExecuteTask = new Thread() {
            public void run() {
                pdfWaitTask.start();
                try {
                    pdfWaitTask.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(pdfTestTask);
            }
        };
        pdfTestExecuteTask.start();



    }

    /* Starts Google AdMob ads for this activity. */
    private void initialiseAds() {
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

    /**
     * Formats time from milliseconds to hh:mm:ss string format.
     */
    public static String formatMillis(int millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%d:%02d", minutes, seconds);
        }
        return time;
    }

    //sets up listeners for Google Cast
    private void setupCastListener() {
        mSessionManagerListener = new SessionManagerListener<CastSession>() {

            @Override
            public void onSessionEnded(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionResumed(CastSession session, boolean wasSuspended) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionResumeFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarted(CastSession session, String sessionId) {
                onApplicationConnected(session);
            }

            @Override
            public void onSessionStartFailed(CastSession session, int error) {
                onApplicationDisconnected();
            }

            @Override
            public void onSessionStarting(CastSession session) {}

            @Override
            public void onSessionEnding(CastSession session) {}

            @Override
            public void onSessionResuming(CastSession session, String sessionId) {}

            @Override
            public void onSessionSuspended(CastSession session, int reason) {}

            private void onApplicationConnected(CastSession castSession) {
                mCastSession = castSession;
                if (null != videoData) {

                    if (mPlaybackState == PlaybackState.PLAYING) {
                        videoView.pause();
                        loadRemoteMedia(mSeekbar.getProgress(), true);
                        return;
                    } else {
                        mPlaybackState = PlaybackState.IDLE;
                        updatePlaybackLocation(PlaybackLocation.REMOTE);
                    }
                }
                updatePlayButton(mPlaybackState);
                supportInvalidateOptionsMenu();
            }

            private void onApplicationDisconnected() {
                updatePlaybackLocation(PlaybackLocation.LOCAL);
                mPlaybackState = PlaybackState.IDLE;
                mLocation = PlaybackLocation.LOCAL;
                updatePlayButton(mPlaybackState);
                supportInvalidateOptionsMenu();
            }
        };
    }

    /**
     * starts up the remote cast for viewing the video remotely. This is run on another thread to avoid stopping user input.
     */
    private void loadRemoteMedia(int position, boolean autoPlay) {
        castSessionLoading = true;
        if (mCastSession == null) {
            return;
        }
        final RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }
        remoteMediaClient.registerCallback(new RemoteMediaClient.Callback() {
            @Override
            public void onStatusUpdated() {
                Intent intent = new Intent(ViewVideo.this, ExpandedControlsActivity.class);
                startActivity(intent);
                remoteMediaClient.unregisterCallback(this);
            }
        });

        final boolean currentAutoPlayState = autoPlay;
        final int currentPositionState = position;


        //Data load is done here
        final Thread streamingTask = new Thread() {
            public void run() {
                int loadAttempts = 0;
                do {
                    //check for pdf data
                    castImageData = appData.getImageContent(getString(R.string.DIRECTORY_ROOT).replaceAll(" ", "%20"), videoData.getName());
                    loadAttempts++;
                } while (loadAttempts < 5 && !appData.dbSuccess(GlobalAppData.CASTIMAGEPATH));

                if (castImageData.getfilePathURL().equals("")) {
                    castImage = new WebImage(Uri.parse(("https://shoptradenz.com/moasapp/castimages/imagedefault3.jpg").replaceAll(" ", "%20")));
                } else {
                    castImage = new WebImage(Uri.parse(castImageData.getfilePathURL().replaceAll(" ", "%20")));
                }

                if (videoData.getDurationInMilliseconds() == 0L) {
                    videoData.setDuration();
                }
            }
        };

        final Thread startCast = new Thread() {
            public void run() {
                remoteMediaClient.load(new MediaLoadRequestData.Builder()
                        .setMediaInfo(buildMediaInfo(castImage))
                        .setAutoplay(currentAutoPlayState)
                        .setCurrentTime(currentPositionState).build());
            }
        };


        //start background loader in a separate thread
        final Thread startLoad = new Thread() {
            public void run() {
                //attempt a reload up to 5 times for pdfdata if connection fails.
                streamingTask.start();
                try {
                    streamingTask.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(startCast);
                castSessionLoading = false;
            }
        };
        Toast.makeText(getApplicationContext(), "Starting video cast...", Toast.LENGTH_SHORT).show();
        startLoad.start();
    }

    private MediaInfo buildMediaInfo(WebImage castImage) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        //information to display
        movieMetadata.putString(MediaMetadata.KEY_TITLE, videoData.getName());

        movieMetadata.addImage(castImage);

        return new MediaInfo.Builder(videoData.getfilePathURL().replaceAll(" ", "%20"))
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                .setStreamDuration(videoData.getDurationInMilliseconds())
                .build();
    }

    //starts new activity and destroys the current activity
    private void startNewActivity(Intent intent) {
        videoView.pause();
        startActivity(intent);
        finish();
    }

    private void seekToVideoID(int index) {
        //Proceed to View_Video
        Intent intent = new Intent(ViewVideo.this, ViewVideo.class);
        intent.putExtra("videoIndex", index);
        if (wasSearched) {
            appData.setLastSearchResult(searchList);
            intent.putExtra("wasSearched", true);
            intent.putExtra("videoData", searchList.get(index));
        } else {
            intent.putExtra("videoData", appData.getVideoData(videoTypePath).get(index));
        }
        intent.putExtra("shouldStart", true);
        startActivity(intent);
        finish();
    }
}
