package com.wordpress.onelifegroupnz.moaarknatural;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
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
import android.telephony.TelephonyManager;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Locale;

import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.DANCEVIDEOPATH;
import static com.wordpress.onelifegroupnz.moaarknatural.GlobalAppData.FOODVIDEOPATH;
/** Activity class for the one coin registration form*/
public class CryptoRegister extends AppCompatActivity {

    private SearchView searchView;

    private TextView emailField;
    private TextView fnameField;
    private TextView lnameField;
    private TextView usernameField;
    private TextView cityField;
    private TextView postcodeField;
    private TextView mobNumField;
    private TextView homNumField;

    private TextView countryDesc;

    private Spinner countrySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto_register);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home);

        //find all textview fields
        emailField = findViewById(R.id.emailField);
        fnameField = findViewById(R.id.fnameField);
        lnameField = findViewById(R.id.lnameField);
        usernameField = findViewById(R.id.usernameField);
        cityField = findViewById(R.id.cityField);
        postcodeField = findViewById(R.id.postCodeField);
        mobNumField = findViewById(R.id.mobNumField);
        homNumField = findViewById(R.id.homNumField);

        String[] countryList = getResources().getStringArray(R.array.country_options);

        //sets network location as the default country
        if (this.getSystemService(Context.TELEPHONY_SERVICE) != null) {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

            Locale l = null;
            if (tm != null) {
                l = new Locale("", tm.getNetworkCountryIso());
            }
            if (l != null) {
                countryList[0] = l.getDisplayCountry();
            }
        }

        //sets country options
        countrySpinner = findViewById(R.id.countrySpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_item,
                countryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        countrySpinner.setAdapter(adapter);

        //for placing the error text if country is not selected.
        countryDesc = findViewById(R.id.countryDesc);



        addSearchFragment();

        initialiseAds();

        finishLoading();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        setUpSearchBar(menu);

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
                startActivity(new Intent(CryptoRegister.this, Home.class));
                return true;
            case R.id.action_notification:
                openAppSettings();
                return true;
            case R.id.menu_dance_video_gallery:
                //Proceed to Line Dance video gallery
                intent = new Intent(CryptoRegister.this, VideoGallery.class);
                intent.putExtra("videoPath", DANCEVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
                return true;
            case R.id.menu_food_video_gallery:
                //Proceed to Food video gallery
                intent = new Intent(CryptoRegister.this, VideoGallery.class);
                intent.putExtra("videoPath", FOODVIDEOPATH); //using video path to set the gallery
                startActivity(intent);
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

        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(CryptoRegister.this)
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

    private void addSearchFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        CustomSearchFragment searchFragment = new CustomSearchFragment();
        transaction.add(R.id.search_fragment, searchFragment);
        transaction.commit();
    }

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

    /*
    Description:
        Provides validation for incorrectly filled fields checked on submission.
    */
    public boolean validateFields() {

        boolean allFieldsValid = true;

        //Email Validation.
        if (emailField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(emailField, "Email is Required");
        else if (!Patterns.EMAIL_ADDRESS.matcher(emailField.getText()).matches())
            allFieldsValid = setErrorMessage(emailField, "Input needs to be an email");
        else
            emailField.setError(null);

        //First Name Validation.
        if (fnameField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(fnameField, "First Name is Required");
        else
            fnameField.setError(null);

        //Last name validation
        if (lnameField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(lnameField, "Last Name is Required");
        else
            lnameField.setError(null);

        //username validation
        if (usernameField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(usernameField, "Username is Required");
        else if (!usernameField.getText().toString().matches("^[a-zA-Z0-9]{2,30}$"))
            allFieldsValid = setErrorMessage(usernameField, "Username must only contain alphanumeric " +
                    "characters and contain at least 2 letters and no more than 30 characters");
        else if(!usernameField.getText().toString().matches("^.*[a-zA-Z].*[a-zA-Z].*$"))
            allFieldsValid = setErrorMessage(usernameField, "Username must only contain alphanumeric " +
                    "characters and contain at least 2 letters and no more than 30 characters");
        else
            usernameField.setError(null);

        //city validation
        if (cityField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(cityField, "City is Required");
        else
            cityField.setError(null);

        //country validation
        if (countrySpinner.getSelectedItem().toString().equals(getString(R.string.text_country)))
            allFieldsValid = setErrorMessage(countryDesc, "Please select your Country");
        else
            countryDesc.setError(null);

        //postcode validation
        if (postcodeField.getText().toString().equals(""))
            allFieldsValid = setErrorMessage(postcodeField, "Postcode is Required");
        else if (postcodeField.getText().toString().length() < 4) {
            allFieldsValid = setErrorMessage(postcodeField, "Postcode must be 4 or more characters long");
        } else
            postcodeField.setError(null);

        //phone number field validation
        if (mobNumField.getText().toString().equals("") && homNumField.getText().toString().equals("")) {
            setErrorMessage(mobNumField, "A least one Mobile or Home number is required");
            allFieldsValid = setErrorMessage(homNumField, "A least one Mobile or Home number is required");
        } else {
            if ((!Patterns.PHONE.matcher(mobNumField.getText()).matches()) && (!mobNumField.getText().toString().equals("")))
                allFieldsValid = setErrorMessage(mobNumField, "This is not a valid phone number");
            else
                mobNumField.setError(null);
            if ((!Patterns.PHONE.matcher(homNumField.getText()).matches()) && (!homNumField.getText().toString().equals("")))
                allFieldsValid = setErrorMessage(homNumField, "This is not a valid phone number");
            else
                homNumField.setError(null);
        }

        //return true if all fields pass or false if a field fails validation
        return allFieldsValid;
    }

    /*Called to set an error message for fields that fail validation. Do not use to set null
    * use setError on the View directly.*/
    private boolean setErrorMessage(TextView forView, String message) {
        forView.setError(message);
        //boolean indicates that validation failed.
        return false;
    }

    /*
    Description:
        Gathers form data and sends it to email intent
        intent opens email client chooser
    */
    public void sendFeedback(View view) {

        String subject = "One Coin Package Account Registration for " + usernameField.getText();
        String message = "Email Address: " + emailField.getText().toString() + "\n"
                + "First Name: " + fnameField.getText().toString() + "\n"
                + "Last Name: " + lnameField.getText().toString() + "\n"
                + "User Name: " + usernameField.getText().toString() + "\n"
                + "Country of Residence: " + countrySpinner.getSelectedItem().toString() + "\n"
                + "City of Residence: " + cityField.getText().toString() + "\n"
                + "Postcode: " + postcodeField.getText().toString() + "\n";

        if(!mobNumField.getText().toString().equals("")) {
            message += "Mobile Phone: " + mobNumField.getText().toString() + "\n";
        }
        if(!homNumField.getText().toString().equals("")) {
            message += "Home Phone: " + homNumField.getText().toString() + "\n";
        }

        message += "\nSent from the Moa's Ark Natural Android Application\n";

        //intent creates chooser for only email apps
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + getString(R.string.sales_email)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);
        try {
            startActivity(Intent.createChooser(emailIntent, "Send registration form using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(CryptoRegister.this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    public void btnOnClick(View v) {
        switch (v.getId()) {
            case R.id.submitBtn:
                //checks whether there are error messages is set
                //runs email intent if no errors
                if(validateFields())
                    sendFeedback(v);
                break;
        }
    }

}
