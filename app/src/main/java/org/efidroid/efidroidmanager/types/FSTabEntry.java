package org.efidroid.efidroidmanager.types;

public class FSTabEntry {
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
}
