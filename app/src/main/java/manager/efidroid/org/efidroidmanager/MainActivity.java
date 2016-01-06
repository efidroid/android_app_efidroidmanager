package manager.efidroid.org.efidroidmanager;

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

import manager.efidroid.org.efidroidmanager.fragments.OperatingSystemFragment;
import manager.efidroid.org.efidroidmanager.fragments.dummy.DummyContent;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OperatingSystemFragment.OnListFragmentInteractionListener {

    private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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
        Class fragmentClass;
        Fragment fragment = null;
        fragmentClass = OperatingSystemFragment.class;
       /* if (id == R.id.nav_camera) {
            // Handle the camera action
            fragmentClass = OperatingSystemFragment.class;
        } else if (id == R.id.nav_gallery) {
            fragmentClass = OperatingSystemFragment.class;
        } else if (id == R.id.nav_slideshow) {
            fragmentClass = OperatingSystemFragment.class;
        } else if (id == R.id.nav_manage) {
            fragmentClass = OperatingSystemFragment.class;
        } else if (id == R.id.nav_share) {
            fragmentClass = OperatingSystemFragment.class;
        } else if (id == R.id.nav_send) {
            fragmentClass = OperatingSystemFragment.class;
        }
        else {
            fragmentClass = OperatingSystemFragment.class;
        }*/

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
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

    public void onListFragmentInteraction(DummyContent.DummyItem item) {

    }
}
