package com.wordpress.onelifegroupnz.moaarknatural;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home);

        //find all textview fields
        emailField = (TextView) findViewById(R.id.emailField);
        confEmailField = (TextView) findViewById(R.id.emailConField);
        fnameField = (TextView) findViewById(R.id.fnameField);
        lnameField = (TextView) findViewById(R.id.lnameField);
        usernameField = (TextView) findViewById(R.id.usernameField);
        cityField = (TextView) findViewById(R.id.cityField);
        postcodeField = (TextView) findViewById(R.id.postCodeField);
        mobNumField = (TextView) findViewById(R.id.mobNumField);
        homNumField = (TextView) findViewById(R.id.homNumField);

        //sets country options
        countrySpinner = (Spinner) findViewById(R.id.countrySpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_item,
                getResources().getStringArray(R.array.country_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        countrySpinner.setAdapter(adapter);


    }

}
