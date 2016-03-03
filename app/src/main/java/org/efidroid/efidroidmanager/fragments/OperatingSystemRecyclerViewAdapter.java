package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.fragments.OperatingSystemFragment.OnListFragmentInteractionListener;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.MountEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link OperatingSystem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 */
public class OperatingSystemRecyclerViewAdapter extends RecyclerView.Adapter<OperatingSystemRecyclerViewAdapter.ViewHolder> {

    private final List<OperatingSystem> mValues;
    private final OnListFragmentInteractionListener mListener;

    private void loadIconsAsync(final Context context) {
        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for(int i=0; i<mValues.size(); i++) {
                    OperatingSystem os = mValues.get(i);

                    try {
                        if(!os.hasLoadedIcon()) {
                            os.getIconBitmap(context);
                            publishProgress(i);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                for(int pos : values) {
                    notifyItemChanged(pos);
                }
            }
        }.execute();
    }

    public OperatingSystemRecyclerViewAdapter(List<OperatingSystem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_operatingsystem_item, parent, false);

        loadIconsAsync(view.getContext());

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        OperatingSystem os = mValues.get(position);

        holder.mItem = os;
        holder.mTitleView.setText(os.getName());

        Bitmap iconBitmap = null;
        if(os.hasLoadedIcon()) {
            try {
                iconBitmap = os.getIconBitmap(holder.mView.getContext());
                if (iconBitmap != null)
                    holder.mImageView.setImageBitmap(iconBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (iconBitmap == null) {
            Drawable icon = ResourcesCompat.getDrawable(
                    holder.mView.getResources(),
                    android.R.mipmap.sym_def_app_icon,
                    holder.mView.getContext().getTheme()
            );
            holder.mImageView.setImageDrawable(icon);
        }

        String desc = os.getDescription();
        if(desc!=null && desc.length()>0) {
            holder.mSubtitleView.setText(desc);
            holder.mSubtitleView.setVisibility(View.VISIBLE);
        }
        else {
            holder.mSubtitleView.setVisibility(View.GONE);
        }

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mSubtitleView;
        public final ImageView mImageView;
        public OperatingSystem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.text);
            mSubtitleView = (TextView) view.findViewById(R.id.text2);
            mImageView = (ImageView) view.findViewById(R.id.image);
        }
    }
}
