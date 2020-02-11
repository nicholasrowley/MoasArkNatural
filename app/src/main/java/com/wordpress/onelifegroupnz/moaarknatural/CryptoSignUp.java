package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;

/**Activity class for the Crypto Sign ups*/
public class CryptoSignUp extends AppCompatActivity {

    private SearchView searchView;
    private TextView formEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_sign_up);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_green);

        formEmail = findViewById(R.id.cryptoSubmitEmail);

        addSearchFragment();

        initialiseAds();

        finishLoading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        setUpSearchbar(menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;
        item = menu.findItem(R.id.menu_refresh);
        item.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                startActivity(new Intent(CryptoSignUp.this, Home.class));
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(CryptoSignUp.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(CryptoSignUp.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.action_notification:
                openAppSettings();
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

    /*Click events in XML are assigned to this method*/
    public void btnOnClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.paFormBtn:
                String paFormUrl = getString(R.string.bgp_agreement_url);
                //String paFormFallbackUrl = getString(R.string.Crypto_Agreement_shortcodeurl);

                loadPdfFile(paFormUrl/*, paFormFallbackUrl*/);
                break;
            case R.id.cryptoSharesBtn:
                String bgpSharesUrl = getString(R.string.BGP_Shares_url);
                //String bgpSharesFallbackUrl = getString(R.string.Crypto_Shares_shortcodeurl);

                loadPdfFile(bgpSharesUrl/*, bgpSharesFallbackUrl*/);
                break;
            case R.id.copyBtn:
                ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Share Link", formEmail.getText());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                break;
            case R.id.cryptoRegisterBtn:
                //Proceed to contact form
                intent = new Intent(CryptoSignUp.this, CryptoRegister.class);
                startActivity(intent);
                break;
        }
    }

    /*Runs a connection test on another thread before opening*/
    private void loadPdfFile(final String URLName/*, final String fallbackURL*/){
        //disable pdf button
        final Thread checkConnection = new Thread() {
            public void run() {
                if (urlCanConnect(URLName)){
                    runOnUiThread(loadPdf);
                } else {
                    Toast.makeText(getApplicationContext(), "404 - Page not found", Toast.LENGTH_SHORT).show();
                    //runOnUiThread(connectFailed);
                }
            }

            final Thread loadPdf = new Thread() {
                public void run() {
                    Intent intent;
                    Uri pdfUri = Uri.parse(URLName);
                    intent = new Intent(Intent.ACTION_VIEW, pdfUri);
                    startActivity(intent);
                }
            };
            /* Disabled as it is useless
            final Thread connectFailed = new Thread() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Page not found - loading web page", Toast.LENGTH_SHORT).show();
                    Intent intent;
                    Uri pdfUri = Uri.parse(fallbackURL);
                    intent = new Intent(Intent.ACTION_VIEW, pdfUri);
                    startActivity(intent);
                }
            };
            */
        };

        checkConnection.start();
    }

    /*Checks connection (Cannot run in UI Thread)*/
    private boolean urlCanConnect(String urlString){
        try
        {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url
                    .openConnection();
            int responseCode = urlConnection.getResponseCode();
            urlConnection.disconnect();
            return responseCode != 404; //page not found CODE 404
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
            return false;
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /*Loads ads for the page*/
    private void initialiseAds() {
        //initialise ads
        MobileAds.initialize(this, getString(R.string.banner_ad_unit_id_live));

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
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
            new AlertDialog.Builder(CryptoSignUp.this)
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

    /*loads the search options when search is expanded*/
    private void addSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        CustomSearchFragment searchFragment = new CustomSearchFragment();
        transaction.add(R.id.search_fragment, searchFragment);
        transaction.commit();
    }

    /*Loads the search bar when search is expanded*/
    private void setUpSearchbar( Menu menu ) {
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

    /* A method for completing the progress bar animation on the toolbar*/
    private void finishLoading() {
        final ProgressBar refreshProgressbar = findViewById(R.id.refreshProgress);
        if (refreshProgressbar.getVisibility() == View.VISIBLE) {
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


}
