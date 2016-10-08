package org.efidroid.efidroidmanager.types;

import android.os.Parcel;
import android.os.Parcelable;

public class MountEntry implements Parcelable {
    private final int mMountID;
    private final int mParentID;
    private final int mMajor;
    private final int mMinor;
    private final String mRoot;
    private final String mMountPoint;
    private final String mMountOptions;
    private final String mOptionalFields;
    private final String mFsType;
    private final String mMountSource;
    private final String mSuperOptions;

    public MountEntry(int mountID, int parentID, int major, int minor, String root, String mountPoint, String mountOptions, String optionalFields, String fsType, String mountSource, String superOptions) {
        mMountID = mountID;
        mParentID = parentID;
        mMajor = major;
        mMinor = minor;
        mRoot = root;
        mMountPoint = mountPoint;
        mMountOptions = mountOptions;
        mOptionalFields = optionalFields;
        mFsType = fsType;
        mMountSource = mountSource;
        mSuperOptions = superOptions;
    }

    protected MountEntry(Parcel in) {
        mMountID = in.readInt();
        mParentID = in.readInt();
        mMajor = in.readInt();
        mMinor = in.readInt();
        mRoot = in.readString();
        mMountPoint = in.readString();
        mMountOptions = in.readString();
        mOptionalFields = in.readString();
        mFsType = in.readString();
        mMountSource = in.readString();
        mSuperOptions = in.readString();
    }

    public static final Creator<MountEntry> CREATOR = new Creator<MountEntry>() {
        @Override
        public MountEntry createFromParcel(Parcel in) {
            return new MountEntry(in);
        }

        @Override
        public MountEntry[] newArray(int size) {
            return new MountEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMountID);
        dest.writeInt(mParentID);
        dest.writeInt(mMajor);
        dest.writeInt(mMinor);
        dest.writeString(mRoot);
        dest.writeString(mMountPoint);
        dest.writeString(mMountOptions);
        dest.writeString(mOptionalFields);
        dest.writeString(mFsType);
        dest.writeString(mMountSource);
        dest.writeString(mSuperOptions);
    }

    public int getMountID() {
        return mMountID;
    }

    public int getParentID() {
        return mParentID;
    }

    public int getMajor() {
        return mMajor;
    }

    public int getMinor() {
        return mMinor;
    }

    public String getRoot() {
        return mRoot;
    }

    public String getMountPoint() {
        return mMountPoint;
    }

    public String getMountOptions() {
        return mMountOptions;
    }

    public String getOptionalFields() {
        return mOptionalFields;
    }

    public String getFsType() {
        return mFsType;
    }

    public String getMountSource() {
        return mMountSource;
    }

    public String getSuperOptions() {
        return mSuperOptions;
    }
}
