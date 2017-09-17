package com.wordpress.onelifegroupnz.moaarknatural;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedList;

public class OneCoinRegister extends AppCompatActivity {

    private TextView emailField;
    private TextView confEmailField;
    private TextView fnameField;
    private TextView lnameField;
    private TextView usernameField;
    private TextView cityField;
    private TextView postcodeField;
    private TextView mobNumField;
    private TextView homNumField;

    private Spinner countrySpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_coin_register);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home);

        //find all textview fields
        emailField = findViewById(R.id.emailField);
        confEmailField = findViewById(R.id.emailConField);
        fnameField = findViewById(R.id.fnameField);
        lnameField = findViewById(R.id.lnameField);
        usernameField = findViewById(R.id.usernameField);
        cityField = findViewById(R.id.cityField);
        postcodeField = findViewById(R.id.postCodeField);
        mobNumField = findViewById(R.id.mobNumField);
        homNumField = findViewById(R.id.homNumField);

        String[] countryList = getResources().getStringArray(R.array.country_options);
        //countryList[0] = "This is a string";

        //sets country options
        countrySpinner = findViewById(R.id.countrySpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_item,
                countryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        countrySpinner.setAdapter(adapter);

        finishLoading();

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
