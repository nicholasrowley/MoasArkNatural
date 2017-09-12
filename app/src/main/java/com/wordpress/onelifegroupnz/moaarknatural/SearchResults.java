package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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

import com.dropbox.core.v2.files.Metadata;
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

/* This activity is for listing the results of a search.*/
public class SearchResults extends AppCompatActivity {

    private GlobalAppData appData;
    private boolean refreshing;
    private Button loadMore;
    private FileLister searchVideoLister; //can be either dance or food lister
    private List<FileData> videoInfoResults;
    private List<Metadata> dropboxSearchData; //data for loading remaining dropbox videos
    private String searchInput;
    private String folderPathCode;
    private SearchView searchView;
    private ProgressBar progressBar;
    private CustomSearchFragment searchFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);
        setTitle(R.string.app_name);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home);

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

        //load more button
        loadMore = (Button) findViewById(R.id.loadMoreBtn);
        loadMore.setVisibility(View.GONE);

        progressBar = (ProgressBar) findViewById(R.id.progressBar4);

        //For fragment implementation
        addSearchFragment();

        refreshContent();

        initialiseAds();
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager inm = (InputMethodManager)  this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focusedView = this.getCurrentFocus();
        if(focusedView != null)
            inm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
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
                startActivity(new Intent(SearchResults.this, Home.class));
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(SearchResults.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(SearchResults.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_refresh:
                refreshContent();
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form
                intent = new Intent(SearchResults.this, ContactForm.class);
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
    public void loadResults() {
        List<Button> resultLinks;
        LinearLayout resultsView;

        //create video gallery buttons
        resultsView = (LinearLayout) findViewById(R.id.resultsView);
        //clears the linearlayout for the video buttons
        resultsView.removeAllViews();
        resultLinks = new ArrayList<>();
        int i = 0;

        TextView title = (TextView) findViewById(R.id.resultsDescription);

        Integer numResults;

        numResults = searchVideoLister.getTotal();

        title.setText(getString(R.string.msg_results_found, numResults.toString(), searchInput));

        //if there are no more videos left to load.
        if(searchVideoLister.getRemainingLoads() == 0 && videoInfoResults.size() != 0)
        {
            loadMore.setVisibility(View.GONE);
        } else if (searchVideoLister.getRemainingLoads() == 0) {
            //if there are no results or loads
            title.setText(getString(R.string.msg_no_results, searchInput));
        }
        else {
            loadMore.setVisibility(View.VISIBLE);
        }

        for (FileData link : videoInfoResults) {
            //create the button for the video link
            resultLinks.add(new Button(this));

            String buttonText = link.getName();
            resultLinks.get(i).setText(buttonText);
            resultLinks.get(i).setId(i);

            //set button size
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(0, 0, 0, 20);
            resultLinks.get(i).setLayoutParams(layoutParams);

            //use this for pre v21 devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                //noinspection deprecation
                resultLinks.get(i).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }

            resultsView.addView(resultLinks.get(i));

            //set the link for the video button
            resultLinks.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Proceed to View_Video
                    Intent intent = new Intent(SearchResults.this, ViewVideo.class);
                    intent.putExtra("videoIndex", videoInfoResults.get(view.getId()));
                    startActivity(intent);
                }
            });
            i++;
        }

        progressBar.setVisibility(View.GONE);
    }

    /*Checks Dropbox for videos in another thread and shows a progress dialog in the main thread.
    * Run when tbe activity needs to be loaded from scratch when opened or by refresh button. */
    public void refreshContent() {
        if (!refreshing) {
            refreshing = true;
            //progress dialog shows when videos are loading
            final ProgressDialog progressDialog = ProgressDialog.show(SearchResults.this, "", "Loading Videos...", true);
            final Toast refreshDialog = Toast.makeText(getApplicationContext(), "Results refreshed", Toast.LENGTH_SHORT);
            final Handler mHandler = new Handler();

            //Data load is done here
            final Thread refreshTask = new Thread() {
                public void run() {
                    try {
                        if (appData == null)
                            appData = GlobalAppData.getInstance(getString(R.string.ACCESS_TOKEN), SearchResults.this, "");

                        searchVideoLister = new FileLister(DropboxClient.getClient(getString(R.string.ACCESS_TOKEN)),
                                new ArrayList<Metadata>(), new ArrayList<FileData>(),
                                searchInput, folderPathCode);

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
                        dropboxSearchData = new ArrayList<>();

                        videoInfoResults = searchVideoLister.getFileDatas();
                        dropboxSearchData = searchVideoLister.getLoadData();

                        refreshDialog.show();
                        sleep(100);
                    } catch (InterruptedException e) {
                        progressDialog.dismiss();
                        e.printStackTrace();
                    }
                }
            };

            //Loading UI Elements in this thread
            final Thread setTask = new Thread() {
                public void run() {
                    loadResults();
                    //if Dropbox connection has failed.
                    if(!searchVideoLister.dbConnectionSuccessfull())
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

                    progressDialog.dismiss();
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
                    mHandler.post(setTask);
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
                        appData = GlobalAppData.getInstance(getString(R.string.ACCESS_TOKEN), SearchResults.this, "");

                    searchVideoLister = new FileLister(DropboxClient.getClient(getString(R.string.ACCESS_TOKEN)), dropboxSearchData, videoInfoResults,
                            searchInput, folderPathCode);

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
                    dropboxSearchData = new ArrayList<>();

                    videoInfoResults = searchVideoLister.getFileDatas();
                    dropboxSearchData = searchVideoLister.getLoadData();

                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        //Loading UI Elements in this thread
        final Thread setTask = new Thread() {
            public void run() {
                loadResults();
                //if Dropbox connection has failed.
                if(!searchVideoLister.dbConnectionSuccessfull())
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

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.loadMoreBtn:
                loadMore.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                loadInBackground();
                break;
        }
    }

    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id_live));

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("fragment_added", true);
    }

    private void addSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        searchFragment = new CustomSearchFragment();
        transaction.add(R.id.search_fragment, searchFragment);
        transaction.commit();
    }

    private void setUpSearchbar( Menu menu ) {
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (android.support.v7.widget.SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //disable default search icon next to search box
        ImageView searchImage = (ImageView)searchView.findViewById(android.support.v7.appcompat.R.id.search_mag_icon);
        ViewGroup LayoutSearchView =
                (ViewGroup) searchImage.getParent();
        LayoutSearchView.removeView(searchImage);

        final LinearLayout searchFragmentLayout = (LinearLayout) findViewById(R.id.search_fragment);
        searchFragmentLayout.setVisibility(View.GONE);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
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
                searchView.setMaxWidth( Integer.MAX_VALUE );

                return true;
            }
        });
    }
}
