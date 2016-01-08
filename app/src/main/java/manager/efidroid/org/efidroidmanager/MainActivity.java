package manager.efidroid.org.efidroidmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import manager.efidroid.org.efidroidmanager.fragments.OperatingSystemFragment;
import manager.efidroid.org.efidroidmanager.models.DeviceInfo;
import manager.efidroid.org.efidroidmanager.models.OperatingSystem;
import manager.efidroid.org.efidroidmanager.services.ResourceLoaderIntentService;
import org.ini4j.Ini;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OperatingSystemFragment.OnListFragmentInteractionListener,
        DataHelper.DeviceInfoLoadCallback{

    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private MaterialDialog mProgressDialog = null;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String string = bundle.getString("FILEPATH");
                Toast.makeText(MainActivity.this, string,
                        Toast.LENGTH_LONG).show();
            }

            removeStickyBroadcast(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        onNavigationItemSelected(mNavigationView.getMenu().getItem(0));

        mProgressDialog = new MaterialDialog.Builder(this)
                .title("Loading device info")
                .content("Please wait")
                .progress(true, 0)
                .show();
        DataHelper.loadDeviceInfo(this, this);
    }

    public void onDeviceInfoLoadError(Exception e) {
        new MaterialDialog.Builder(this)
                .title("Title")
                .content("Can't load device info. Please check your connection.\n\n"+e.getLocalizedMessage())
                .positiveText("Try again")
                .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(MaterialDialog dialog, DialogAction which) {
                DataHelper.loadDeviceInfo(MainActivity.this, MainActivity.this);
            }
        }).show();
    }

    @Override
    public void onDeviceInfoLoaded(DeviceInfo deviceInfo) {
        mProgressDialog.dismiss();
    }

    @Override
    public void onDeviceInfoStartLoading() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter("NOTIFICATION"));
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_operating_systems) {
            fragment = new OperatingSystemFragment();
        } else if (id == R.id.nav_recovery_tools) {
        } else if (id == R.id.nav_uefi_apps) {
        } else if (id == R.id.nav_plugins) {
        } else if (id == R.id.nav_install) {
        } else if (id == R.id.nav_share) {
        } else if (id == R.id.nav_troubleshoot) {
        } else if (id == R.id.nav_bug_report) {
        } else if (id == R.id.nav_about) {
        }

        if(fragment == null) {
            return false;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        if(item.getGroupId()==R.id.nav_group_main){
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_main,true,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_setup,false,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_communicate,false,true);
        }
        else if(item.getGroupId()==R.id.nav_group_setup){
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_main,false,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_setup,true,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_communicate,false,true);
        }
        else if(item.getGroupId()==R.id.nav_group_communicate){
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_main,false,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_setup,false,true);
            mNavigationView.getMenu().setGroupCheckable(R.id.nav_group_communicate,true,true);
        }

        // Highlight the selected item and update the title
        item.setChecked(true);
        setTitle(item.getTitle());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onListFragmentInteraction(OperatingSystem item) {

    }
}
