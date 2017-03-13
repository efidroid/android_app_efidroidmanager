package org.efidroid.efidroidmanager.types;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;

import java.util.Arrays;

public class InstallationEntry implements Parcelable {
    // common data
    private FSTabEntry mFSTabEntry = null;
    private int mStatus = STATUS_INVALID;

    // parsed data
    private long mTimeStamp;
    private long mEfiSpecVersion;
    private long mEFIDroidReleaseVersion;
    private String mDeviceName = null;
    private String mManifest = null;

    public static final int STATUS_INVALID = -1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_NOT_INSTALLED = 1;
    public static final int STATUS_WRONG_DEVICE = 2;
    public static final int STATUS_ESP_MISSING = 3;
    public static final int STATUS_ESP_ONLY = 4;

    private byte[] readBytes(byte[] data, Pointer<Integer> pPos, int size) {
        byte[] ret = Arrays.copyOfRange(data, pPos.value, pPos.value + size);
        pPos.value += size;
        return ret;
    }

    private long uint32FromByteArray(byte[] bytes) {
        return (long) (((bytes[0] & 0xFF)) |
                ((bytes[1] & 0xFF) << 8) |
                ((bytes[2] & 0xFF) << 16) |
                ((bytes[3] & 0xFF) << 24));
    }

    private long readU32(byte[] data, Pointer<Integer> pPos) {
        return uint32FromByteArray(readBytes(data, pPos, 4));
    }

    private byte[] getMetaData(Pointer<Integer> pMetaOffset) throws Exception {
        // read header
        Pointer<Integer> pPos = new Pointer<>(0);
        byte[] header = RootToolsEx.readBinaryFileEx(mFSTabEntry.getBlkDevice(), 0, 2048);

        // magic
        String magic = new String(readBytes(header, pPos, 8));
        if (!magic.equals("ANDROID!"))
            throw new UnsupportedOperationException("Invalid magic");

        long kernel_size = readU32(header, pPos);
        long kernel_addr = readU32(header, pPos);

        long ramdisk_size = readU32(header, pPos);
        long ramdisk_addr = readU32(header, pPos);

        long second_size = readU32(header, pPos);
        long second_addr = readU32(header, pPos);

        long tags_addr = readU32(header, pPos);
        long page_size = readU32(header, pPos);
        long dt_size = readU32(header, pPos);

        // calculate offsets
        long off_kernel = page_size;
        long off_ramdisk = off_kernel + Util.ROUNDUP(kernel_size, page_size);
        long off_second = off_ramdisk + Util.ROUNDUP(ramdisk_size, page_size);
        long off_tags = off_second + Util.ROUNDUP(second_size, page_size);
        long off_meta = off_tags + Util.ROUNDUP(dt_size, page_size);

        // read meta
        byte[] meta = RootToolsEx.readBinaryFileEx(mFSTabEntry.getBlkDevice(), off_meta, 36);
        pMetaOffset.value = (int) off_meta;
        return meta;
    }

    public InstallationEntry(FSTabEntry fsTabEntry, DeviceInfo deviceInfo) {
        String espDir = deviceInfo.getESPDir(true);
        boolean has_espfile = false;

        try {
            mFSTabEntry = fsTabEntry;

            if (espDir != null && RootToolsEx.isFile(espDir + "/partition_" + mFSTabEntry.getName() + ".img"))
                has_espfile = true;

            // read meta data
            Pointer<Integer> pPos = new Pointer<>(0);
            Pointer<Integer> pMetaOffset = new Pointer<>(0);
            byte[] meta = getMetaData(pMetaOffset);

            // magic
            String magic = new String(readBytes(meta, pPos, 8));
            if (!magic.equals("EFIDroid"))
                throw new UnsupportedOperationException("Invalid magic");

            long hdr_version = readU32(meta, pPos);

            mTimeStamp = readU32(meta, pPos);
            mEfiSpecVersion = readU32(meta, pPos);
            mEFIDroidReleaseVersion = readU32(meta, pPos);

            long device_name_size = readU32(meta, pPos);
            long device_name_offset = readU32(meta, pPos);
            long manifest_size = readU32(meta, pPos);
            long manifest_offset = readU32(meta, pPos);

            // read device_name
            mDeviceName = new String(RootToolsEx.readBinaryFileEx(fsTabEntry.getBlkDevice(), pMetaOffset.value + device_name_offset, device_name_size - 1));

            // read manifest
            mManifest = new String(RootToolsEx.readBinaryFileEx(fsTabEntry.getBlkDevice(), pMetaOffset.value + manifest_offset, manifest_size));

            if (!mDeviceName.equals(deviceInfo.getDeviceName()))
                mStatus = STATUS_WRONG_DEVICE;
            else if (!has_espfile)
                mStatus = STATUS_ESP_MISSING;
            else
                mStatus = STATUS_OK;
        } catch (Exception e) {
            if (has_espfile)
                mStatus = STATUS_ESP_ONLY;
            else
                mStatus = STATUS_NOT_INSTALLED;
        }
    }

    protected InstallationEntry(Parcel in) {
        mFSTabEntry = (FSTabEntry) in.readValue(FSTabEntry.class.getClassLoader());
        mStatus = in.readInt();
        mTimeStamp = in.readLong();
        mEfiSpecVersion = in.readLong();
        mEFIDroidReleaseVersion = in.readLong();
        mDeviceName = (String) in.readValue(String.class.getClassLoader());
        mManifest = (String) in.readValue(String.class.getClassLoader());
    }

    public static final Creator<InstallationEntry> CREATOR = new Creator<InstallationEntry>() {
        @Override
        public InstallationEntry createFromParcel(Parcel in) {
            return new InstallationEntry(in);
        }

        @Override
        public InstallationEntry[] newArray(int size) {
            return new InstallationEntry[size];
        }
    };

    public FSTabEntry getFsTabEntry() {
        return mFSTabEntry;
    }

    public int getStatus() {
        return mStatus;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public long getEfiSpecVersion() {
        return mEfiSpecVersion;
    }

    public long getEfiSpecVersionMajor() {
        return ((mEfiSpecVersion & 0xffff0000) >> 16);
    }

    public long getEfiSpecVersionMinor() {
        return (mEfiSpecVersion & 0x0000ffff);
    }

    public long getEFIDroidReleaseVersion() {
        return mEFIDroidReleaseVersion;
    }

    public String getEFIDroidReleaseVersionString() {
        String str = "";
        long ver_0 = ((mEFIDroidReleaseVersion & 0xff000000) >> 24);
        long ver_1 = ((mEFIDroidReleaseVersion & 0x00ff0000) >> 16);
        long ver_2 = ((mEFIDroidReleaseVersion & 0x0000ff00) >> 8);
        long ver_3 = ((mEFIDroidReleaseVersion & 0x000000ff));

        if (ver_3 > 0)
            str = "." + ver_3 + str;
        if (ver_2 > 0 || str.length() > 0)
            str = "." + ver_2 + str;

        str = "." + ver_1 + str;
        str = ver_0 + str;

        return str;
    }

    public String getManifest() {
        return mManifest;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mFSTabEntry);
        dest.writeInt(mStatus);
        dest.writeLong(mTimeStamp);
        dest.writeLong(mEfiSpecVersion);
        dest.writeLong(mEFIDroidReleaseVersion);
        dest.writeValue(mDeviceName);
        dest.writeValue(mManifest);
    }
}