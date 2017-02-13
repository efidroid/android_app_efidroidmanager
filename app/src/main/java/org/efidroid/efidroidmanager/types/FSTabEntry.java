package org.efidroid.efidroidmanager.types;

import android.os.Parcel;
import android.os.Parcelable;

import org.efidroid.efidroidmanager.RootToolsEx;

import java.util.Arrays;
import java.util.List;

public class FSTabEntry implements Parcelable {
    final String mBlkDevice;
    final String mMountPoint;
    final String mFsType;
    final String mMountFlags;
    final String mFfMgrFlags;

    public FSTabEntry(String blkDevice, String mountPoint, String fsType, String mountFlags, String fsMgrFlags) {
        mMountPoint = mountPoint;
        mFsType = fsType;
        mMountFlags = mountFlags;
        mFfMgrFlags = fsMgrFlags;

        // use backup node if it exists
        String new_blkDevice = blkDevice;
        try {
            String backup = "/multiboot/dev/replacement_backup_" + getName();
            if (RootToolsEx.nodeExists(backup)) {
                new_blkDevice = backup;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBlkDevice = new_blkDevice;
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
        try {
            if (RootToolsEx.isDirectory("/multiboot"))
                return "/multiboot" + RootToolsEx.realpath(mBlkDevice);
        } catch (Exception e) {
        }

        return mBlkDevice;
    }

    public String getMountPoint() {
        return mMountPoint;
    }

    public String getName() {
        return getMountPoint().substring(1);
    }

    public String getFsType() {
        return mFsType;
    }

    public String getMountFlags() {
        return mMountFlags;
    }

    public List<String> getFfMgrFlags() {
        return Arrays.asList(mFfMgrFlags.split(","));
    }

    public boolean isMultiboot() {
        return getFfMgrFlags().contains("multiboot");
    }

    public boolean isUEFI() {
        return getFfMgrFlags().contains("uefi");
    }

    public String getESP() {
        for (String part : getFfMgrFlags()) {
            if (!part.startsWith("esp"))
                continue;

            return part.substring(4);
        }

        return null;
    }
}
