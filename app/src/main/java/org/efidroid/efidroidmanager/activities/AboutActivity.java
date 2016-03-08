package org.efidroid.efidroidmanager.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.efidroid.efidroidmanager.BuildConfig;
import org.efidroid.efidroidmanager.R;

import java.text.DateFormat;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView textVersion = (TextView)findViewById(R.id.textVersion);
        textVersion.setText("App Version " + BuildConfig.VERSION_NAME);

        DateFormat format = DateFormat.getDateTimeInstance();
        TextView textBuildinfo = (TextView)findViewById(R.id.textBuildInfo);
        textBuildinfo.setText("Compiled by "+BuildConfig.USERNAME+"@"+BuildConfig.HOSTNAME+" on "+format.format(BuildConfig.TIMESTAMP));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
