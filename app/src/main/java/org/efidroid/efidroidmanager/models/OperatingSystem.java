package org.efidroid.efidroidmanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.efidroid.efidroidmanager.RootToolsEx;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OperatingSystem implements Parcelable {
    private String mFilename;
    private Ini mIni;
    private List<CmdlineItem> mCmdline;
    private ArrayList<OperatingSystemChangeListener> mListeners = new ArrayList<>();

    public interface OperatingSystemChangeListener {
        void onOperatingSystemChanged();
    }

    public void addChangeListener(OperatingSystemChangeListener listener) {
        mListeners.add(listener);
    }

    public void removeChangeListener(OperatingSystemChangeListener listener) {
        mListeners.remove(listener);
    }

    public void notifyChange() {
        for(OperatingSystemChangeListener listener : mListeners) {
            listener.onOperatingSystemChanged();
        }
    }

    public static class Partition {
        public String name;
        public String path;

        public Partition(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public static class CmdlineItem {
        public String name;
        public String value;

        public CmdlineItem(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private void init(String filename) throws Exception {
        String data = RootToolsEx.readFile(filename);
        if (data == null)
            throw new Exception("no data");

        mFilename = filename;
        mIni = new Ini(new StringReader(data));
        mCmdline = new ArrayList<>();
        String cmdlineStr = mIni.get("replacements", "cmdline");
        if(cmdlineStr!=null) {
            String[] parts = cmdlineStr.split(" ");
            for(String part : parts) {
                String[] kv = part.split("=");
                String name = kv[0];
                String value = null;

                if(kv.length>1)
                    value = kv[1];

                mCmdline.add(new CmdlineItem(name, value));
            }
        }
    }

    public OperatingSystem(String filename) throws Exception {
        init(filename);
    }

    protected OperatingSystem(Parcel in) {
        String filename = in.readString();
        try {
            init(filename);
        } catch (Exception e) {
            throw new RuntimeException("Can't create Operating System from Parcelable");
        }
    }

    public static final Creator<OperatingSystem> CREATOR = new Creator<OperatingSystem>() {
        @Override
        public OperatingSystem createFromParcel(Parcel in) {
            return new OperatingSystem(in);
        }

        @Override
        public OperatingSystem[] newArray(int size) {
            return new OperatingSystem[size];
        }
    };

    public String getFilename() {
        return mFilename;
    }

    public String getDirectory() {
        return new File(mFilename).getParent();
    }

    public String getName() {
        return mIni.get("config", "name");
    }

    public void setName(String name) {
        mIni.put("config", "name", name);
    }

    public String getDescription() {
        return mIni.get("config", "description");
    }

    public void setDescription(String description) {
        mIni.put("config", "description", description);
    }

    public List<Partition> getPartitions() {
        ArrayList<Partition> partitions = new ArrayList<>();
        Profile.Section list = mIni.get("partitions");
        for (Map.Entry<String, String> entry : new TreeMap<String,String>(list).entrySet()) {
            partitions.add(new Partition(entry.getKey(), entry.getValue()));
        }

        return partitions;
    }

    public List<CmdlineItem> getCmdline() {
        return mCmdline;
    }

    public String getReplacementKernel() {
        return mIni.get("replacements", "kernel");
    }

    public String getReplacementRamdisk() {
        return mIni.get("replacements", "ramdisk");
    }

    public String getReplacementDT() {
        return mIni.get("replacements", "dt");
    }

    public void setReplacementKernel(String s) {
        mIni.put("replacements", "kernel", s);
    }

    public void setReplacementRamdisk(String s) {
        mIni.put("replacements", "ramdisk", s);
    }

    public void setReplacementDT(String s) {
        mIni.put("replacements", "dt", s);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // write cmdline back to ini
        StringWriter cmdlineWriter = new StringWriter();
        for (CmdlineItem item : mCmdline) {
            cmdlineWriter.write(" "+item.name+"="+item.value);
        }
        mIni.put("replacements", "cmdline", cmdlineWriter.getBuffer().toString());

        // write filename to parcel
        dest.writeString(mFilename);
    }
}
