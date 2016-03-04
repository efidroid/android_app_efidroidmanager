package org.efidroid.efidroidmanager.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.DataHelper;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.fragments.EmptyFragment;
import org.efidroid.efidroidmanager.fragments.InstallFragment;
import org.efidroid.efidroidmanager.fragments.OperatingSystemFragment;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.tasks.OSRemovalProgressServiceTask;
import org.efidroid.efidroidmanager.types.MountEntry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OperatingSystemFragment.OnOperatingSystemFragmentInteractionListener,
        DataHelper.DeviceInfoLoadCallback, InstallFragment.OnInstallFragmentInteractionListener {

    // status
    private boolean hasBusybox = false;
    private boolean hasRoot = false;
    private boolean mTouchDisabled = false;
    private int mActiveMenuItemId = 0;
    private MenuItem mPreviousMenuItem;
    private AsyncTask<?, ?, ?> mFragmentLoadingTask = null;

    // data
    private DeviceInfo mDeviceInfo = null;
    private ArrayList<OperatingSystem> mOperatingSystems;

    // args
    private static final String ARG_DEVICE_INFO = "deviceinfo";
    private static final String ARG_OPERATING_SYSTEMS = "operating_systems";
    private static final String ARG_ACTIVEMENU_ID = "activemenu_id";
    private static final String ARG_HAS_BUSYBOX = "has_busybox";
    private static final String ARG_HAS_ROOT = "has_root";

    // request codes
    private static final int REQUEST_EDIT_OS = 0;
    private static final int REQUEST_CREATE_OS = 1;
    private static final int REQUEST_DELETE_OS = 2;

    // UI
    private NavigationView mNavigationView;
    private FloatingActionButton mFab;
    private ProgressBar mFragmentProgress;
    private DrawerLayout mDrawer;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private MaterialDialog mProgressDialog = null;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout mAppBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get views
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mFragmentProgress = (ProgressBar)findViewById(R.id.progressBar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        // set initial toolbar height
        int initialHeight = Util.getStatusBarHeight(this) + Util.getToolBarHeight(this);
        ViewGroup.LayoutParams layoutParams = mAppBarLayout.getLayoutParams();
        layoutParams.height = initialHeight;
        mAppBarLayout.setLayoutParams(layoutParams);
        mAppBarLayout.setExpanded(false, false);

        // actionbar
        setSupportActionBar(mToolbar);

        // drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        // FAB
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, OperatingSystemEditActivity.class);
                intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, new OperatingSystem());
                intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mDeviceInfo);
                startActivityForResult(intent, REQUEST_CREATE_OS);
            }
        });

        // navigation view
        mNavigationView.setNavigationItemSelectedListener(this);

        // SRL
        mSwipeRefreshLayout.setEnabled(false);

        // load data the first time
        if (savedInstanceState == null) {
            // show progress dialog
            mProgressDialog = new MaterialDialog.Builder(this)
                    .title("Loading device info")
                    .content("Please wait")
                    .cancelable(false)
                    .progress(true, 0)
                    .show();

            // load device info
            DataHelper.loadDeviceInfo(this, this);
        }

        else {
            mDeviceInfo = savedInstanceState.getParcelable(ARG_DEVICE_INFO);
            mOperatingSystems = savedInstanceState.getParcelableArrayList(ARG_OPERATING_SYSTEMS);
            mActiveMenuItemId = savedInstanceState.getInt(ARG_ACTIVEMENU_ID);
            hasBusybox = savedInstanceState.getBoolean(ARG_HAS_BUSYBOX);
            hasRoot = savedInstanceState.getBoolean(ARG_HAS_ROOT);
            onLoadUiData();
        }
    }

    public void onDeviceInfoLoadError(Exception e) {
        new MaterialDialog.Builder(this)
                .title("Title")
                .content("Can't load device info. Please check your connection.\n\n"+e.getLocalizedMessage())
                .positiveText("Try again")
                .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                DataHelper.loadDeviceInfo(MainActivity.this, MainActivity.this);
            }
        }).show();
    }

    private void onLoadUiData() {
        if (!hasBusybox && !RootTools.isBusyboxAvailable()) {
            new MaterialDialog.Builder(this)
                    .title("Title")
                    .content("You need BusyBox to use this app.")
                    .positiveText("Install")
                    .neutralText("Try again")
                    .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    RootTools.offerBusyBox(MainActivity.this);
                }
            })
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            onLoadUiData();
                        }
                    }).show();
            return;
        }
        hasBusybox = true;

        if (!hasRoot && !RootToolsEx.isAccessGiven(0, 0)) {
            new MaterialDialog.Builder(this)
                    .title("Title")
                    .content("You need Root access to use this app.")
                    .positiveText("Try again")
                    .cancelable(false).onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            onLoadUiData();
                        }
                    }).show();
            return;
        }

        // restore old tab
        if(mActiveMenuItemId==0)
            mActiveMenuItemId = R.id.nav_operating_systems;

        onNavigationItemSelected(mNavigationView.getMenu().findItem(mActiveMenuItemId));
    }

    @Override
    public void onDeviceInfoLoaded(DeviceInfo deviceInfo) {
        mDeviceInfo = deviceInfo;
        mProgressDialog.dismiss();
        onLoadUiData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // we got paused by offering the busybox, so resume loading now
        if(!hasBusybox) {
            onLoadUiData();
        }

        if(mActiveMenuItemId==R.id.nav_operating_systems && mOperatingSystems==null)
            reloadOperatingSystems();

    }
    @Override
    protected void onPause() {
        super.onPause();

        // cancel current task and disable loading animations
        if(mFragmentLoadingTask!=null && !mFragmentLoadingTask.isCancelled()) {
            mFragmentLoadingTask.cancel(true);
            mSwipeRefreshLayout.setRefreshing(false);
            mFragmentProgress.setVisibility(View.GONE);
            mTouchDisabled = false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_DEVICE_INFO, mDeviceInfo);
        outState.putParcelableArrayList(ARG_OPERATING_SYSTEMS, mOperatingSystems);
        outState.putInt(ARG_ACTIVEMENU_ID, mActiveMenuItemId);
        outState.putBoolean(ARG_HAS_BUSYBOX, hasBusybox);
        outState.putBoolean(ARG_HAS_ROOT, hasRoot);

        super.onSaveInstanceState(outState);
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

    private AsyncTask<Void, Void, Exception> makeOperatingSystemsTask() {
        final ArrayList<OperatingSystem> list = new ArrayList<>();

        return new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    // load mount info
                    MountInfo mountInfo = RootToolsEx.getMountInfo();
                    if (isCancelled()) return null;

                    // load block devices
                    List<String> blockDevices = RootToolsEx.getBlockDevices();

                    for (String blkDevice : blockDevices) {
                        if (isCancelled()) return null;

                        try {
                            // get mountpoint
                            int[] majmin = RootToolsEx.getDeviceNode(blkDevice);
                            MountEntry mountEntry = mountInfo.getByMajorMinor(majmin[0], majmin[1]);
                            if (mountEntry == null)
                                continue;
                            String mountPoint = mountEntry.getMountPoint();

                            // find multiboot directories
                            for(String multibootPath : OperatingSystem.MULTIBOOT_PATHS) {
                                String multibootDir = mountPoint + multibootPath;

                                if (!RootToolsEx.isDirectory(multibootDir))
                                    continue;

                                // get multiboot.ini's
                                List<String> directories = RootToolsEx.getMultibootSystems(multibootDir);
                                for (String directory : directories) {
                                    String path = directory + "/multiboot.ini";

                                    try {
                                        list.add(new OperatingSystem(path));
                                    } catch (Exception ignored){}
                                }
                            }
                        }
                        catch (Exception e) {
                            continue;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
                }
                return null;
            }

            protected void onPostExecute(Exception e) {
                // hide progressbar
                mFragmentProgress.setVisibility(View.GONE);
                mSwipeRefreshLayout.setRefreshing(false);

                if (e != null) {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title("Title")
                            .content("Can't load operating systems. " + e.getLocalizedMessage())
                            .positiveText("ok").show();
                    return;
                }

                mOperatingSystems = list;

                // show fragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.flContent, new OperatingSystemFragment()).commit();

                mTouchDisabled = false;
            }
        };
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mTouchDisabled || super.dispatchTouchEvent(ev);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        // cancel any running loading task
        if(mFragmentLoadingTask!=null && !mFragmentLoadingTask.isCancelled())
            mFragmentLoadingTask.cancel(true);

        // hide progressbar
        mFragmentProgress.setVisibility(View.GONE);

        // disable SwipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener(null);
        mSwipeRefreshLayout.setEnabled(false);

        // hide FAB
        mFab.setVisibility(View.GONE);

        // reset appbar settings
        mCollapsingToolbarLayout.setTitleEnabled(false);
        mCollapsingToolbarLayout.setContentScrimColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, getTheme()));
        mCollapsingToolbarLayout.setStatusBarScrimColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark, getTheme()));

        if (id == R.id.nav_operating_systems) {
            // start loading
            if(mOperatingSystems==null) {
                // show progressbar
                mFragmentProgress.setVisibility(View.VISIBLE);

                // run task
                mFragmentLoadingTask = makeOperatingSystemsTask().execute();
            }
            else {
                fragment = new OperatingSystemFragment();
            }

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mTouchDisabled = true;

                    // run task
                    mFragmentLoadingTask = makeOperatingSystemsTask().execute();
                }
            });
            mSwipeRefreshLayout.setEnabled(true);
        } else if (id == R.id.nav_recovery_tools) {
        } else if (id == R.id.nav_uefi_apps) {
        } else if (id == R.id.nav_plugins) {
        } else if (id == R.id.nav_themes) {
        } else if (id == R.id.nav_install) {
            fragment = new InstallFragment();
        } else if (id == R.id.nav_share) {
        } else if (id == R.id.nav_troubleshoot) {
        } else if (id == R.id.nav_bug_report) {
        } else if (id == R.id.nav_about) {
        }

        mActiveMenuItemId = item.getItemId();

        // use empty fragment
        if(fragment == null) {
            fragment = new EmptyFragment();
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

        // Highlight the selected item and update the title
        item.setChecked(true);
        if (mPreviousMenuItem != null) {
            mPreviousMenuItem.setChecked(false);
        }
        mPreviousMenuItem = item;
        setTitle(item.getTitle());

        // close drawer
        mDrawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onOperatingSystemClicked(OperatingSystem item) {
        Intent intent = new Intent(this, OperatingSystemEditActivity.class);
        intent.putExtra(OperatingSystemEditActivity.ARG_OPERATING_SYSTEM, item);
        intent.putExtra(OperatingSystemEditActivity.ARG_DEVICE_INFO, mDeviceInfo);
        startActivityForResult(intent, REQUEST_EDIT_OS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_EDIT_OS:
            case REQUEST_CREATE_OS:
                if(resultCode==OperatingSystemEditActivity.RESULT_UPDATED)
                    mOperatingSystems = null;
                break;

            case REQUEST_DELETE_OS:
                mOperatingSystems = null;
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onOperatingSystemLongClicked(final OperatingSystem item) {
        new MaterialDialog.Builder(this)
                .title("Delete")
                .content("Do you want to delete '"+item.getName()+"'?")
                .positiveText("Delete")
                .negativeText("Cancel")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Bundle extras = new Bundle();
                Intent intent = GenericProgressActivity.makeIntent(
                        MainActivity.this,
                        OSRemovalProgressServiceTask.class,
                        extras,
                        "Deleting system\n"+item.getName(),
                        R.anim.hold, R.anim.abc_slide_out_right_full,
                        R.anim.hold, R.anim.abc_slide_out_right_full
                );

                extras.putParcelable(OSRemovalProgressServiceTask.ARG_OPERATING_SYSTEM, item);
                startActivityForResult(intent, REQUEST_DELETE_OS);
                overridePendingTransition(R.anim.abc_slide_in_right_full, R.anim.hold);
            }
        }).show();
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    @Override
    public List<OperatingSystem> getOperatingSystems() {
        return mOperatingSystems;
    }

    @Override
    public FloatingActionButton getFAB() {
        return mFab;
    }

    @Override
    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    public CollapsingToolbarLayout getCollapsingToolbarLayout() {
        return mCollapsingToolbarLayout;
    }

    @Override
    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    @Override
    public void reloadOperatingSystems() {
        mOperatingSystems = null;
        if(mActiveMenuItemId>0)
            onNavigationItemSelected(mNavigationView.getMenu().findItem(mActiveMenuItemId));
    }
}
