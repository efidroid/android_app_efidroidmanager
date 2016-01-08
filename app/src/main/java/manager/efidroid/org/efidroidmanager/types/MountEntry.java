package manager.efidroid.org.efidroidmanager.types;

public class MountEntry {
    private final int mMountID;
    private final int mParentID;
    private final int mMajor;
    private final int mMinor;
    private final String mRoot;
    private final String mMountPoint;
    private final String mMountOptions;
    private final String mOptionalFields;
    private final String mSeparator;
    private final String mFsType;
    private final String mMountSource;
    private final String mSuperOptions;

    public MountEntry(int mountID, int parentID, int major, int minor, String root, String mountPoint, String mountOptions, String optionalFields, String separator, String fsType, String mountSource, String superOptions) {
        mMountID = mountID;
        mParentID = parentID;
        mMajor = major;
        mMinor = minor;
        mRoot = root;
        mMountPoint = mountPoint;
        mMountOptions = mountOptions;
        mOptionalFields = optionalFields;
        mSeparator = separator;
        mFsType = fsType;
        mMountSource = mountSource;
        mSuperOptions = superOptions;
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

    public String getSeparator() {
        return mSeparator;
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