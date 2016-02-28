package org.efidroid.efidroidmanager.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.GeneralFragment;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.PartitionItemFragment;
import org.efidroid.efidroidmanager.fragments.operatingsystemedit.ReplacementItemFragment;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.FABListener;
import org.efidroid.efidroidmanager.view.CustomViewPager;

public class OperatingSystemEditActivity extends AppCompatActivity implements PartitionItemFragment.OnListFragmentInteractionListener {
    private OperatingSystem mOperatingSystem;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private FloatingActionButton mFab;
    private TabLayout mTabLayout;

    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private CustomViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operating_system_edit);

        if (savedInstanceState != null)
            mOperatingSystem = savedInstanceState.getParcelable(ARG_OPERATING_SYSTEM);
        else
            mOperatingSystem = getIntent().getParcelableExtra(ARG_OPERATING_SYSTEM);
        setTitle(mOperatingSystem.getName());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (CustomViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
                final boolean show = (mSectionsPagerAdapter.getRegisteredFragment(position) instanceof FABListener);

                if(show && mFab.getVisibility()==View.GONE) {
                    mFab.setVisibility(View.VISIBLE);
                    mFab.setAlpha(0f);
                }
                mFab.animate().setDuration(shortAnimTime).alpha(
                        show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFab.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = mViewPager.getCurrentItem();
                Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(position);
                if ((fragment instanceof FABListener)) {
                    ((FABListener)fragment).onFABClicked();
                }
            }
        });

        boolean showFab = (mSectionsPagerAdapter.getRegisteredFragment(0) instanceof FABListener);
        mFab.setVisibility(showFab ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(ARG_OPERATING_SYSTEM, mOperatingSystem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPartitionItemClicked(OperatingSystem.Partition item) {

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private SparseArray<Fragment> registeredFragments = new SparseArray();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GeneralFragment.newInstance(OperatingSystemEditActivity.this.mOperatingSystem);
                case 1:
                    return PartitionItemFragment.newInstance(OperatingSystemEditActivity.this.mOperatingSystem);
                case 2:
                    return ReplacementItemFragment.newInstance(OperatingSystemEditActivity.this.mOperatingSystem);
            }
            return null;

        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "GENERAL";
                case 1:
                    return "PARTITIONS";
                case 2:
                    return "REPLACEMENTS";
                case 3:
                    return "PLUGINS";
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        public Fragment getRegisteredFragment(int position) {
            return this.registeredFragments.get(position);
        }
    }

    public OperatingSystem getOperatingSystem() {
        return mOperatingSystem;
    }
    public CustomViewPager getViewPager() {
        return mViewPager;
    }

    public TabLayout getTabLayout() {
        return mTabLayout;
    }
}
