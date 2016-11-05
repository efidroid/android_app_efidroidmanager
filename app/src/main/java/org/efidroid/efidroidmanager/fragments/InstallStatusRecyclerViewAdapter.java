package org.efidroid.efidroidmanager.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.models.OperatingSystem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link OperatingSystem} and makes a call to the
 * specified {@link OperatingSystemFragment.OnOperatingSystemFragmentInteractionListener}.
 */
public class InstallStatusRecyclerViewAdapter extends RecyclerView.Adapter<InstallStatusRecyclerViewAdapter.ViewHolder> {

    private final List<Item> mValues;

    public static class Item {
        public String title;
        public String subtitle;

        public Item(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public InstallStatusRecyclerViewAdapter(List<Item> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_replacemenitem, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Item item = mValues.get(position);

        holder.mItem = item;
        holder.mTitleView.setText(item.title);

        if (item.subtitle != null && item.subtitle.length() > 0) {
            holder.mSubtitleView.setText(item.subtitle);
            holder.mSubtitleView.setVisibility(View.VISIBLE);
        } else {
            holder.mSubtitleView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mSubtitleView;
        public Item mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.text);
            mSubtitleView = (TextView) view.findViewById(R.id.text2);
        }
    }
}
