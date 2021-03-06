package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.ALLVIDEOSCODE;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastButtonFactory;

/**
 * Description:
 * This class manages and sets up the video Gallery for
 * -Food Videos
 * -Line Dance videos
 */

public class VideoGallery extends AppCompatActivity {
    private GlobalAppData appData;
    private boolean refreshing;
    private Button loadMore;
    private SearchView searchView;
    private ProgressBar progressBar;
    private CustomSearchFragment searchFragment;
    private String targetFolder;
    private ProgressBar refreshProgressbar;
    private int galleryViewButtonsLoaded;
    private boolean savedInstanceExists;

    private CastContext mCastContext;
    private MenuItem mediaRouteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_gallery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_green);
        findViewById(R.id.search_fragment).setVisibility(View.GONE);

        try {
            mCastContext = CastContext.getSharedInstance(this);
        } catch (RuntimeException re) {
            re.printStackTrace();
            //display message to user.
            Toast.makeText(getApplicationContext(), getString(R.string.play_services_error_toast), Toast.LENGTH_SHORT).show();
        }

        refreshing = false;
        //load more button
        loadMore = findViewById(R.id.loadMoreBtn);
        progressBar = findViewById(R.id.progressBar2);

        //determine which videos to show.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            targetFolder = (String) extras.getSerializable("videoPath");
        }
        //prepare gallery based on target folder
        TextView galleryTitle = findViewById(R.id.galleryTitle);
        TextView subTitle = findViewById(R.id.videoGalleryText);
        if (targetFolder.equals(DANCEVIDEOPATH)) {
            galleryTitle.setText(getString(R.string.title_activity_dance_video_gallery));
            subTitle.setText(R.string.text_video_gallery_desc_dance);
        } else
            if (targetFolder.equals(FOODVIDEOPATH)) {
                galleryTitle.setText(getString(R.string.title_activity_food_video_gallery));
                subTitle.setText(R.string.text_video_gallery_desc_food);
            }

        galleryViewButtonsLoaded = 0;

        savedInstanceExists = savedInstanceState != null;
        if (!savedInstanceExists) {
            addSearchFragment();
        }

        refreshProgressbar = findViewById(R.id.refreshProgress);

        refreshContent();

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
    }

    @Override
    protected void onResume(){
        super.onResume();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        if (targetFolder.equals(DANCEVIDEOPATH)) {
            item = menu.findItem(R.id.menu_dance_video_gallery);
            item.setVisible(false);
        } else
            if (targetFolder.equals(FOODVIDEOPATH)) {
                item = menu.findItem(R.id.menu_food_video_gallery);
                item.setVisible(false);
            }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                intent = new Intent(VideoGallery.this, Home.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(VideoGallery.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                finish();
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(VideoGallery.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                finish();
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_refresh:
                if(!(refreshProgressbar.getVisibility() == View.VISIBLE)) {
                    refreshProgressbar.setVisibility(View.VISIBLE);
                    refreshContent();
                }
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form on Moa's Ark website
                Uri uri = Uri.parse(getString(R.string.website_contact_form_url).replaceAll(" ", "%20"));
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            case R.id.menu_playlist_gallery:
                //Proceed to playlist gallery
                intent = new Intent(VideoGallery.this, PlaylistGallery.class);
                startActivity(intent);
                return true;
            case R.id.menu_rate_app:
                //Navigates to Google Play
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(VideoGallery.this, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    //This method loads the buttons for the Video Gallery after video data is found.
    public void loadGallery(boolean loadFromScratch) {
        List<Button> galleryLinks;
        LinearLayout galleryView;

        //create video gallery buttons
        galleryView = findViewById(R.id.gallery);

        if (loadFromScratch) {
            //clears the linearlayout for the video buttons
            galleryView.removeAllViews();
            galleryViewButtonsLoaded = 0;
        }

        galleryLinks = new ArrayList<>();
        int buttonsToBeLoaded;

        //checks how many buttons need to be loaded up to the LOADAMOUNT specified by FolderContentLister
        if (galleryViewButtonsLoaded + FolderContentLister.LOADAMOUNT > appData.getVideoData(targetFolder).size()) {
            buttonsToBeLoaded = appData.getVideoData(targetFolder).size();
        } else {
            buttonsToBeLoaded = galleryViewButtonsLoaded + FolderContentLister.LOADAMOUNT;
        }

        for (int i = galleryViewButtonsLoaded; i < buttonsToBeLoaded; i++) {
            //create the button for the video link with the correct characteristics
            Button newButton = new Button(this);
            final int sdk = android.os.Build.VERSION.SDK_INT;
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                //noinspection deprecation
                newButton.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.button) );
            } else {
                newButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button));
            }
            LinearLayout.LayoutParams parameter =  (LinearLayout.LayoutParams) galleryView.getLayoutParams();
            parameter.setMargins(5, 5, 5, 5); // left, top, right, bottom margins
            newButton.setLayoutParams(parameter);

            galleryLinks.add(newButton);

            String buttonText = appData.getVideoData(targetFolder).get(i).getName();
            galleryLinks.get(i % FolderContentLister.LOADAMOUNT).setText(buttonText);
            galleryLinks.get(i % FolderContentLister.LOADAMOUNT).setId(i);

            //set button size
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(MATCH_PARENT,
                            MATCH_PARENT);
            layoutParams.setMargins(0, 0, 0, 20);
            galleryLinks.get(i % FolderContentLister.LOADAMOUNT).setLayoutParams(layoutParams);

            galleryView.addView(galleryLinks.get(i % FolderContentLister.LOADAMOUNT));

            //set the link for the video button
            galleryLinks.get(i % FolderContentLister.LOADAMOUNT).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Proceed to ViewVideo
                    Intent intent = new Intent(VideoGallery.this, ViewVideo.class);
                    intent.putExtra("videoData", appData.getVideoData(targetFolder).get(view.getId()));
                    intent.putExtra("videoIndex", view.getId());
                    intent.putExtra("shouldStart", true);
                    startActivity(intent);
                }
            });
            galleryViewButtonsLoaded++;
        }

        //end of loading
        progressBar.setVisibility(View.GONE);

        //if there are no more videos left to load.
        if (appData.getVideoData(targetFolder).size() == galleryViewButtonsLoaded) {
            loadMore.setVisibility(View.GONE);
        } else {
            loadMore.setVisibility(View.VISIBLE);
        }
    }

    /*Checks server side for videos in another thread and shows a progress bar.
    * Run when tbe activity needs to be loaded from scratch when opened or by refresh button. */
    public void refreshContent() {
        if (!refreshing) {
            refreshing = true;
            findViewById(R.id.gallery).setVisibility(View.GONE);
            findViewById(R.id.loadMoreBtn).setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            //progress bar shows when videos are loading
            refreshProgressbar.setProgress(0);
            refreshProgressbar.setVisibility(View.VISIBLE);

            final ProgressBarAnimation anim = new ProgressBarAnimation(refreshProgressbar, 0, 80);
            anim.setDuration(3040);
            refreshProgressbar.startAnimation(anim);

            final Toast refreshDialog = Toast.makeText(getApplicationContext(), "Gallery refreshed", Toast.LENGTH_SHORT);

            //Data load is done here
            final Thread refreshTask = new Thread() {
                public void run() {
                    try {
                        if (appData == null)
                            appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "");
                        else {
                            appData.refreshIISDirectoryVideoFiles(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "", targetFolder);
                            refreshDialog.show();
                        }
                        //if data failed to load attempt to reload it.
                        if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                                || appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                            if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                                    && appData.getVideoData(FOODVIDEOPATH).size() == 0)
                                appData.refreshIISDirectoryVideoFiles(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "", ALLVIDEOSCODE);
                            else if (appData.getVideoData(DANCEVIDEOPATH).size() == 0)
                                appData.refreshIISDirectoryVideoFiles(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "", DANCEVIDEOPATH);
                            else if (appData.getVideoData(FOODVIDEOPATH).size() == 0)
                                appData.refreshIISDirectoryVideoFiles(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "", FOODVIDEOPATH);
                        }
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            //Loading UI Elements in this thread
            final Thread setTask = new Thread() {
                public void run() {
                    loadGallery(true);
                    findViewById(R.id.gallery).setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);

                    //if server side connection has failed.
                    if (!appData.dbSuccess(targetFolder)) {
                        new AlertDialog.Builder(VideoGallery.this)
                                .setTitle(getString(R.string.server_connection_error_title))
                                .setMessage(getString(R.string.server_connection_error))
                                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        refreshContent();
                                    }
                                })
                                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .show();
                    }
                }
            };

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
                    refreshing = false;
                }
            };

            //This thread waits for refresh then loads ui elements in handler.
            Thread waitForRefresh = new Thread() {
                public void run() {
                    try {
                        refreshTask.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //give the appData extra time to load.
                    if (!appData.dbSuccess(targetFolder)) {
                        try {
                            sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(setTask);

                    try {
                        setTask.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

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
            refreshTask.start();
            waitForRefresh.start();
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
            new AlertDialog.Builder(VideoGallery.this)
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

    /* loads more video links based on videos already listed in the file lister.
     Its purpose is to lessen the time spent waiting for the urls to load.*/
    public void loadInBackground() {
        final Toast refreshDialog = Toast.makeText(getApplicationContext(), "Loading Complete", Toast.LENGTH_SHORT);

        //Data load is done here
        final Thread loadTask = new Thread() {
            public void run() {
                try {
                    if (appData == null)
                        appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), VideoGallery.this, "");
                    else {
                        appData.loadIISDirectoryFiles(getString(R.string.DIRECTORY_ROOT), "", targetFolder);
                        refreshDialog.show();
                    }
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Loading UI Elements in this thread
        final Thread setTask = new Thread() {
            public void run() {
                loadGallery(false);
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

    /* This is the onClick method for buttons in the video gallery activity. */
    public void galleryOnClick(View v) {
        switch (v.getId()) {
            case R.id.loadMoreBtn:
                loadMore.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                loadInBackground();
                break;
        }
    }

    /* Starts Google AdMob ads for this activity. */
    private void initialiseAds() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("fragment_added", true);
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
    private void setUpSearchBar(Menu menu ) {
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
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener()  {
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
                searchView.setMaxWidth( Integer.MAX_VALUE );

                return true;
            }
        });
    }
}