package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DROPBOXTIMEOUTLIMIT;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastButtonFactory;

/* This activity is for listing the results of a search.*/
public class SearchResults extends AppCompatActivity {

    private GlobalAppData appData;
    private boolean refreshing;
    private Button loadMore;
    private FolderContentLister searchVideoLister; //can be either dance or food lister
    private List<FileDataListing> videoInfoResults;
    private int searchResultsLoaded;
    private String searchInput;
    private String folderPathCode;
    private SearchView searchView;
    private ProgressBar progressBar;
    private CustomSearchFragment searchFragment;
    private boolean savedInstanceExists;

    private ProgressBar refreshProgressbar;

    private int searchResultsDisplayed;

    private CastContext mCastContext;
    private MenuItem mediaRouteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        setTitle(R.string.app_name);
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

        searchInput = "";
        String searchType = "";

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            searchInput = extras.getString("searchInput");
            searchType = extras.getString("searchType");
        }

        //Generate folder path code based on search preference
        if (searchType != null) {
            if (searchType.equals(getString(R.string.search_type_dance)))
                folderPathCode = DANCEVIDEOPATH;
            else if (searchType.equals(getString(R.string.search_type_food))) {
                folderPathCode = FOODVIDEOPATH;
            }
        }
        searchResultsDisplayed = 0;

        //load more button
        loadMore = findViewById(R.id.loadMoreBtn);
        loadMore.setVisibility(View.GONE);

        progressBar = findViewById(R.id.progressBar4);

        refreshProgressbar = findViewById(R.id.refreshProgress);

        //For fragment implementation
        savedInstanceExists = savedInstanceState != null;
        if (!savedInstanceExists) {
            addSearchFragment();
        }

        refreshContent();

        initialiseAds();
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager inm = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focusedView = this.getCurrentFocus();
        if(focusedView != null)
            if (inm != null) {
                inm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
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
                intent = new Intent(SearchResults.this, Home.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(SearchResults.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                finish();
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(SearchResults.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                finish();
                return true;
            case R.id.menu_refresh:
                if(!(refreshProgressbar.getVisibility() == View.VISIBLE)){
                    refreshProgressbar.setVisibility(View.VISIBLE);
                    refreshContent();
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

    //Opens the app setting so the user can turn notifications on or off
    public void openAppSettings() {
        String packageName = getString(R.string.package_name);

        try {
            //Open the specific App Info page:
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);

        } catch ( ActivityNotFoundException e ) {
            new AlertDialog.Builder(SearchResults.this)
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

    //This method loads the buttons for the Video Gallery after video data is found.
    public void loadResults(boolean loadFromScratch) {
        List<Button> resultLinks;
        LinearLayout resultsView;

        //create video gallery buttons
        resultsView = findViewById(R.id.resultsView);
        if (loadFromScratch) {
            //clears the linearlayout for the video buttons
            resultsView.removeAllViews();
            searchResultsDisplayed = 0;
        }
        resultLinks = new ArrayList<>();

        TextView title = findViewById(R.id.resultsDescription);

        int numResults;

        numResults = searchVideoLister.getTotal();

        title.setText(getString(R.string.msg_results_found, Integer.toString(numResults), searchInput));

        //check how many videos need to be loaded up to the LOADAMOUNT specified in FolderContentLister
        int buttonsToBeLoaded;
        if (searchResultsDisplayed + FolderContentLister.LOADAMOUNT > videoInfoResults.size()) {
            buttonsToBeLoaded = videoInfoResults.size();
        } else {
            buttonsToBeLoaded = searchResultsDisplayed + FolderContentLister.LOADAMOUNT;
        }

        for (int i = searchResultsDisplayed; i < buttonsToBeLoaded; i++) {
            //create the button for the video link with the correct characteristics
            Button newButton = new Button(this);
            final int sdk = android.os.Build.VERSION.SDK_INT;
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                //noinspection deprecation
                newButton.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.rounded_button) );
            } else {
                newButton.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_button));
            }
            LinearLayout.LayoutParams parameter =  (LinearLayout.LayoutParams) resultsView.getLayoutParams();
            parameter.setMargins(5, 5, 5, 5); // left, top, right, bottom margins
            newButton.setLayoutParams(parameter);

            resultLinks.add(newButton);

            String buttonText = videoInfoResults.get(i).getName();
            resultLinks.get(i % FolderContentLister.LOADAMOUNT).setText(buttonText);
            resultLinks.get(i % FolderContentLister.LOADAMOUNT).setId(i);

            //set button size
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(0, 0, 0, 20);
            resultLinks.get(i % FolderContentLister.LOADAMOUNT).setLayoutParams(layoutParams);

            //use this for pre v21 devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //noinspection deprecation
                resultLinks.get(i % FolderContentLister.LOADAMOUNT).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }

            resultsView.addView(resultLinks.get(i % FolderContentLister.LOADAMOUNT));

            //set the link for the video button
            resultLinks.get(i % FolderContentLister.LOADAMOUNT).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Proceed to View_Video
                    Intent intent = new Intent(SearchResults.this, ViewVideo.class);
                    intent.putExtra("videoIndex", videoInfoResults.get(view.getId()));
                    intent.putExtra("shouldStart", true);
                    startActivity(intent);
                }
            });
            searchResultsDisplayed++;
        }

        //if there are no more videos left to load.
        if(videoInfoResults.size() == searchResultsDisplayed && videoInfoResults.size() != 0)
        {
            loadMore.setVisibility(View.GONE);
        } else if (videoInfoResults.size() == 0) {
            //if there are no results or loads
            title.setText(getString(R.string.msg_no_results, searchInput));
        }
        else {
            loadMore.setVisibility(View.VISIBLE);
        }

        progressBar.setVisibility(View.GONE);
    }

    /*Checks server side for videos in another thread and shows a progress bar
    * Run when tbe activity needs to be loaded from scratch when opened or by refresh button. */
    public void refreshContent() {
        if (!refreshing) {
            refreshing = true;
            //progress bar shows when videos are loading
            refreshProgressbar.setProgress(0);
            refreshProgressbar.setVisibility(View.VISIBLE);

            final ProgressBarAnimation anim = new ProgressBarAnimation(refreshProgressbar, 0, 80);
            anim.setDuration(3040);
            refreshProgressbar.startAnimation(anim);

            final Toast refreshDialog = Toast.makeText(getApplicationContext(), "Results refreshed", Toast.LENGTH_SHORT);

            //Data load is done here
            final Thread refreshTask = new Thread() {
                public void run() {
                    try {
                        if (appData == null)
                            appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), SearchResults.this, "");

                        searchVideoLister = new FolderContentLister(getString(R.string.DIRECTORY_ROOT), folderPathCode, searchInput,
                                0, new ArrayList<FileDataListing>());

                        searchVideoLister.execute();
                        try {
                            searchVideoLister.get(DROPBOXTIMEOUTLIMIT, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }

                        videoInfoResults = new ArrayList<>();

                        videoInfoResults = searchVideoLister.getLoadData();
                        searchResultsLoaded = searchVideoLister.getLoadData().size() - searchVideoLister.getRemainingLoads();

                        refreshDialog.show();
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            //Loading UI Elements in this thread
            final Thread setTask = new Thread() {
                public void run() {
                    loadResults(true);
                    //if server side connection has failed.
                    if(!searchVideoLister.httpConnectionSuccessful())
                    {
                        new AlertDialog.Builder(SearchResults.this)
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

    /* loads more video links based on videos already listed in the file lister.
     Its purpose is to lessen the time spent waiting for the urls to load.*/
    public void loadInBackground() {
        //Data load is done here
        final Thread loadTask = new Thread() {
            public void run() {
                try {
                    if (appData == null)
                        appData = GlobalAppData.getInstance(getString(R.string.DIRECTORY_ROOT), SearchResults.this, "");

                    searchVideoLister = new FolderContentLister(getString(R.string.DIRECTORY_ROOT), folderPathCode, searchInput, searchResultsLoaded, videoInfoResults);

                    searchVideoLister.execute();
                    try {
                        searchVideoLister.get(DROPBOXTIMEOUTLIMIT, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }

                    videoInfoResults = new ArrayList<>();

                    videoInfoResults = searchVideoLister.getLoadData();
                    searchResultsLoaded = searchVideoLister.getLoadData().size() - searchVideoLister.getRemainingLoads();

                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Loading UI Elements in this thread
        final Thread setTask = new Thread() {
            public void run() {
                loadResults(false);
                //if connection to server has failed.
                if(!searchVideoLister.httpConnectionSuccessful())
                {
                    new AlertDialog.Builder(SearchResults.this)
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

    /* Handles all button onlick functions for this activity. */
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.loadMoreBtn:
                loadMore.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                loadInBackground();
                break;
        }
    }

    /* Starts Google AdMob Ads */
    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id));

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
                searchView.setMaxWidth( Integer.MAX_VALUE );

                return true;
            }
        });
    }
}
