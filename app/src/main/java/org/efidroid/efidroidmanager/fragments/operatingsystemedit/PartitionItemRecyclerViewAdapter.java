package org.efidroid.efidroidmanager.fragments.operatingsystemedit;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.commons.io.FileUtils;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.types.OSEditFragmentInteractionListener;

import java.util.List;

public class PartitionItemRecyclerViewAdapter extends RecyclerView.Adapter<PartitionItemRecyclerViewAdapter.ViewHolder> implements OperatingSystem.OperatingSystemChangeListener {
    private List<OperatingSystem.Partition> mValues;
    private final OSEditFragmentInteractionListener mListener;
    private final OperatingSystem mOperatingSystem;

    public PartitionItemRecyclerViewAdapter(OperatingSystem os, OSEditFragmentInteractionListener listener) {
        mOperatingSystem = os;
        mListener = listener;
        rebuild();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_partitionitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        OperatingSystem.Partition partition = mValues.get(position);

        // data
        holder.mItem = partition;

        // text
        holder.mIdView.setText(partition.getPartitionName());

        // description
        String description = partition.toIniPath();
        if (partition.getType() != OperatingSystem.Partition.TYPE_BIND) {
            description += " (" + FileUtils.byteCountToDisplaySize(partition.getSize()) + ")";
        }
        holder.mContentView.setText(description);

        // icon letter
        String letter = "";
        switch (partition.getType()) {
            case OperatingSystem.Partition.TYPE_BIND:
                letter = "B";
                break;
            case OperatingSystem.Partition.TYPE_LOOP:
                letter = "L";
                break;
            case OperatingSystem.Partition.TYPE_DYNFILEFS:
                letter = "D";
                break;
        }
        holder.mIconLetter.setText(letter);

        // onclick
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onPartitionItemClicked(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private void rebuild() {
        mValues = mOperatingSystem.getPartitions();
    }

    @Override
    public void onOperatingSystemChanged() {
        rebuild();
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mOperatingSystem.addChangeListener(this);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mOperatingSystem.removeChangeListener(this);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public final ImageView mImageView;
        public final TextView mIconLetter;
        public OperatingSystem.Partition mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(android.support.design.R.id.text);
            mContentView = (TextView) view.findViewById(android.support.design.R.id.text2);
            mImageView = (ImageView) view.findViewById(android.support.design.R.id.image);
            mIconLetter = (TextView) view.findViewById(R.id.icon_letter);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
