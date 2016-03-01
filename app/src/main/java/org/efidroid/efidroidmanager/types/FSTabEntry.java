package org.efidroid.efidroidmanager.types;

import android.os.Parcel;
import android.os.Parcelable;

public class FSTabEntry implements Parcelable {
    final String mBlkDevice;
    final String mMountPoint;
    final String mFsType;
    final String mMountFlags;
    final String mFfMgrFlags;

    public FSTabEntry(String blkDevice, String mountPoint, String fsType, String mountFlags, String fsMgrFlags) {
        mBlkDevice = blkDevice;
        mMountPoint = mountPoint;
        mFsType = fsType;
        mMountFlags = mountFlags;
        mFfMgrFlags = fsMgrFlags;
    }

    protected FSTabEntry(Parcel in) {
        mBlkDevice = in.readString();
        mMountPoint = in.readString();
        mFsType = in.readString();
        mMountFlags = in.readString();
        mFfMgrFlags = in.readString();
    }

    public static final Creator<FSTabEntry> CREATOR = new Creator<FSTabEntry>() {
        @Override
        public FSTabEntry createFromParcel(Parcel in) {
            return new FSTabEntry(in);
        }

        @Override
        public FSTabEntry[] newArray(int size) {
            return new FSTabEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBlkDevice);
        dest.writeString(mMountPoint);
        dest.writeString(mFsType);
        dest.writeString(mMountFlags);
        dest.writeString(mFfMgrFlags);
    }

    public String getBlkDevice() {
        return mBlkDevice;
    }

    public String getMountPoint() {
        return mMountPoint;
    }

    public String getFsType() {
        return mFsType;
    }

    public String getMountFlags() {
        return mMountFlags;
    }

    public String getFfMgrFlags() {
        return mFfMgrFlags;
    }

    public boolean isMultiboot() {
        String[] parts = mFfMgrFlags.split(",");
        for(String part : parts) {
            if(part.equals("multiboot"))
                return true;
        }

        return false;
    }
}
