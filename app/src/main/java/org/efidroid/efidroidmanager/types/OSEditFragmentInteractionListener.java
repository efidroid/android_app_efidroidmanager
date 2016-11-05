package org.efidroid.efidroidmanager.types;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;

import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.view.CustomViewPager;

import java.util.ArrayList;

public interface OSEditFragmentInteractionListener {
    class MultibootPartitionInfo implements Parcelable {
        public final String name;
        public final long size;

        public MultibootPartitionInfo(String name, long size) {
            this.name = name;
            this.size = size;
        }

        protected MultibootPartitionInfo(Parcel in) {
            this.name = in.readString();
            this.size = in.readLong();
        }

        public static final Creator<MultibootPartitionInfo> CREATOR = new Creator<MultibootPartitionInfo>() {
            @Override
            public MultibootPartitionInfo createFromParcel(Parcel in) {
                return new MultibootPartitionInfo(in);
            }

            @Override
            public MultibootPartitionInfo[] newArray(int size) {
                return new MultibootPartitionInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeLong(size);
        }
    }

    class MultibootDir implements Parcelable {
        public static final Creator<MultibootDir> CREATOR = new Creator<MultibootDir>() {
            @Override
            public MultibootDir createFromParcel(Parcel in) {
                return new MultibootDir(in);
            }

            @Override
            public MultibootDir[] newArray(int size) {
                return new MultibootDir[size];
            }
        };
        public final String path;
        public final MountEntry mountEntry;

        public MultibootDir(String path, MountEntry mountEntry) {
            this.path = path;
            this.mountEntry = mountEntry;
        }

        protected MultibootDir(Parcel in) {
            this.path = in.readString();
            this.mountEntry = in.readParcelable(MountEntry.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(path);
            dest.writeParcelable(mountEntry, flags);
        }

        @Override
        public String toString() {
            return path + " (" + mountEntry.getFsType() + ")";
        }
    }

    interface CommitListener {
        boolean onCommit();
    }

    // data methods
    DeviceInfo getDeviceInfo();

    OperatingSystem getOperatingSystem();

    ArrayList<MultibootDir> getMultibootDirectories();

    ArrayList<MultibootPartitionInfo> getMultibootPartitionInfo();

    // callbacks
    void onPartitionItemClicked(OperatingSystem.Partition item);

    // listeners
    void addOnCommitListener(CommitListener listener);

    void removeOnCommitListener(CommitListener listener);

    // UI
    CustomViewPager getViewPager();

    TabLayout getTabLayout();

    void setFabVisible(final boolean visible);
}
