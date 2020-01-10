package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


import java.io.IOException;

import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.ALLVIDEOSCODE;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;

/** This is the main activity for navigating to different areas of the app.*/
public class Home extends AppCompatActivity {

    private GlobalAppData appData; //singleton instance of globalAppData
    private Button featureDanceVideo;
    private Button featureFoodVideo;
    private TextView blogsTitleText;
    private boolean refreshing;
    private SearchView searchView;
    private RssFragment fragment;
    private boolean savedInstanceExists;

    private CustomSearchFragment searchFragment;

    private ProgressBar refreshProgressbar;
    private RelativeLayout rssView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);

        refreshing = false;

        featureDanceVideo = findViewById(R.id.featureDanceVideoBtn);
        featureFoodVideo = findViewById(R.id.featureFoodVideoBtn);
        blogsTitleText = findViewById(R.id.textBlogsTitle);

        //For fragment implementation
        savedInstanceExists = savedInstanceState != null;
        addSearchFragment();

        refreshProgressbar = findViewById(R.id.refreshProgress);
        rssView = findViewById(R.id.fragment_container);


        Log.d("Test", "test");

        refreshContent();

        initialiseAds();
    }

    //looks for feature videos automatically if required when resuming the Home screen
    @Override
    protected void onResume() {
        super.onResume();
        setFeatureVideoLink();
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
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(Home.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(Home.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_refresh:
                if (!(refreshProgressbar.getVisibility() == View.VISIBLE))
                    refreshContent();
                return true;
            case R.id.menu_contact_form:
                //Proceed to contact form
                intent = new Intent(Home.this, ContactForm.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*This method handles the refreshing of content in other threads while displaying a progress dialog
    * in the current thread.*/
    public void refreshContent() {
        if (!refreshing) {
            refreshing = true;

            //progress bar shows when videos are loading
            refreshProgressbar.setProgress(0);
            refreshProgressbar.setVisibility(View.VISIBLE);

            final ProgressBarAnimation anim = new ProgressBarAnimation(refreshProgressbar, 0, 80);
            anim.setDuration(3040);
            refreshProgressbar.startAnimation(anim);

            final Toast refreshDialog = Toast.makeText(getApplicationContext(), "Feature Videos Refreshed", Toast.LENGTH_SHORT);

            //Data load is done here
            final Thread refreshTask = new Thread() {
                public void run() {
                    try {
                        if (appData == null) {
                            appData = GlobalAppData.getInstance(getString(R.string.ACCESS_TOKEN), Home.this, "");
                        } else {
                            appData.refreshDropboxVideoFiles(getString(R.string.ACCESS_TOKEN), Home.this, "", ALLVIDEOSCODE);

                            refreshDialog.show();
                        }

                        //if data failed to load attempt to reload it.
                        if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                                || appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                            if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                                    && appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                                appData.refreshDropboxVideoFiles(getString(R.string.ACCESS_TOKEN), Home.this, "", ALLVIDEOSCODE);
                            } else if (appData.getVideoData(DANCEVIDEOPATH).size() == 0) {
                                appData.refreshDropboxVideoFiles(getString(R.string.ACCESS_TOKEN), Home.this, "", DANCEVIDEOPATH);
                            } else if (appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                                appData.refreshDropboxVideoFiles(getString(R.string.ACCESS_TOKEN), Home.this, "", FOODVIDEOPATH);
                            }
                        }

                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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

            //UI elements are updated in this thread
            final Thread setTask = new Thread() {
                public void run() {
                    //Connection error will show if any of the video folders return no results.
                    if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                            || appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                        featureDanceVideo.setVisibility(View.GONE);
                        featureFoodVideo.setVisibility(View.GONE);
                        blogsTitleText.setVisibility(View.GONE);

                        new AlertDialog.Builder(Home.this)
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
                    } else {
                        setFeatureVideoLink();
                        blogsTitleText.setVisibility(View.VISIBLE);

                        try {
                            //rss fragment implemented here
                            if (!savedInstanceExists) {
                                addRssFragment();
                            }
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            //This thread waits for refresh then updates UI with handler
            Thread waitForRefresh = new Thread() {
                public void run() {
                    try {
                        refreshTask.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //give the appData extra time to load.
                    if (appData.getVideoData(DANCEVIDEOPATH).size() == 0
                            || appData.getVideoData(FOODVIDEOPATH).size() == 0) {
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
                    //TODO test3
                    try {
                        DropboxShareFolderURLInterpreter.getShareURLFileSystem("https://www.dropbox.com/sh/velh8ofxf1htg4o/AAA4CUgm3jC4v0f67o9ALIODa?dl=0");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            refreshTask.start();
            waitForRefresh.start();
        }
    }

    public void setFeatureVideoLink() {
        featureDanceVideo.setVisibility(View.GONE);
        featureFoodVideo.setVisibility(View.GONE);

        //set the first dance video in the list as the featured video
        if (appData.getVideoData(DANCEVIDEOPATH).size() != 0) {
            String buttonText = getString(R.string.hm_feature_dance_video_aut_text) + "\n" +
                    appData.getVideoData(DANCEVIDEOPATH).get(0).getName().replaceFirst("[.][^.]+$", "");
            featureDanceVideo.setText(buttonText);
            featureDanceVideo.setVisibility(View.VISIBLE);
        }

        //set the first food video in the list as the featured video
        if (appData.getVideoData(FOODVIDEOPATH).size() != 0) {
            String buttonText = getString(R.string.hm_feature_food_video_aut_text) + "\n" +
                    appData.getVideoData(FOODVIDEOPATH).get(0).getName().replaceFirst("[.][^.]+$", "");
            featureFoodVideo.setText(buttonText);
            featureFoodVideo.setVisibility(View.VISIBLE);
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
            new AlertDialog.Builder(Home.this)
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

    public void btnOnClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.danceVideoGalleryBtn:
                //Proceed to Line Dance video gallery
                intent = new Intent(Home.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                break;
            case R.id.foodVideoGalleryBtn:
                //Proceed to Food video gallery
                intent = new Intent(Home.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                break;
            case R.id.featureDanceVideoBtn:
                if (appData.getVideoData(DANCEVIDEOPATH).size() == 0) {
                    new AlertDialog.Builder(Home.this)
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
                } else {
                    //Proceed to ViewVideo
                    intent = new Intent(Home.this, ViewVideo.class);
                    intent.putExtra("videoIndex", appData.getVideoData(DANCEVIDEOPATH).get(0));
                    startActivity(intent);
                }
                break;
            case R.id.featureFoodVideoBtn:
                if (appData.getVideoData(FOODVIDEOPATH).size() == 0) {
                    new AlertDialog.Builder(Home.this)
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
                } else {
                    //Proceed to ViewVideo
                    intent = new Intent(Home.this, ViewVideo.class);
                    intent.putExtra("videoIndex", appData.getVideoData(FOODVIDEOPATH).get(0));
                    startActivity(intent);
                }
                break;
            case R.id.productLinkBtn:
                Uri uri = Uri.parse(getString(R.string.website_shop_url));
                intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                break;
            case R.id.cryptoSignUpBtn:
                intent = new Intent(Home.this, CryptoSignUp.class);
                startActivity(intent);
                break;
        }
    }

    //For Rss News Feed
    private void addRssFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        fragment = new RssFragment();
        transaction.add(rssView.getId(), fragment);
        transaction.commit();
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

    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                AdView mAdView = findViewById(R.id.adView);
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        });
    }

    private void setUpSearchbar(Menu menu) {

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
}
