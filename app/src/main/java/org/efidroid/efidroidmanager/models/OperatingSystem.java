package org.efidroid.efidroidmanager.models;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.io.FilenameUtils;
import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OperatingSystem implements Parcelable {
    // data: common
    // parsed on edit, data on creation
    private Ini mIni = null;

    // data: edit mode
    private String mFilename = null;

    // data: creation mode
    private boolean mCreationMode = false;
    private Uri mIconUri = null;
    private OperatingSystemEditActivity.MultibootDir mLocation = null;
    private ArrayList<Partition> mPartitions = new ArrayList<>();

    // parsed
    private List<CmdlineItem> mCmdline = new ArrayList<>();

    // listeners
    private ArrayList<OperatingSystemChangeListener> mListeners = new ArrayList<>();

    // os types
    public static final String OSTYPE_ANDROID = "android";
    public static final String OSTYPE_UBUNTU  = "ubuntu";

    // os type lists
    public static final ArrayList<String> ALL_OS_TYPES = new ArrayList<>();
    public static final ArrayList<Integer> ALL_OS_TYPES_NAMES = new ArrayList<>();

    public static class Partition implements Parcelable {
        private String mPartitionName;
        private String mFileName;
        private int mType;
        private long mSize = -1;

        public static final int TYPE_BIND = 0;
        public static final int TYPE_LOOP = 1;
        public static final int TYPE_DYNFILEFS= 2;

        private Partition(String name, String value) {
            mPartitionName = name;
            mFileName = FilenameUtils.removeExtension(value);

            String ext = FilenameUtils.getExtension(value);
            switch(ext) {
                case "img":
                    mType = TYPE_LOOP;
                    break;

                case "dyn":
                    mType = TYPE_DYNFILEFS;
                    break;

                default:
                    mType = TYPE_BIND;
                    break;
            }
        }

        public Partition(String partitionName, String fileName, int type) {
            mPartitionName = partitionName;
            mFileName = fileName;
            mType = type;
        }

        public Partition(String partitionName, String fileName, int type, long size) {
            this(partitionName, fileName, type);
            mSize = size;
        }

        protected Partition(Parcel in) {
            mPartitionName = in.readString();
            mFileName = in.readString();
            mType = in.readInt();
            mSize = in.readLong();
        }

        public static final Creator<Partition> CREATOR = new Creator<Partition>() {
            @Override
            public Partition createFromParcel(Parcel in) {
                return new Partition(in);
            }

            @Override
            public Partition[] newArray(int size) {
                return new Partition[size];
            }
        };

        public String getPartitionName() {
            return mPartitionName;
        }

        public void setPartitionName(String name) {
            mPartitionName = name;
        }

        public String getFileName() {
            return mFileName;
        }

        public void setFileName(String name) {
            mFileName = name;
        }

        public int getType() {
            return mType;
        }

        public void setType(int type) {
            mType = type;
        }

        public long getSize() {
            return mSize;
        }

        public void setSize(long size) {
            mSize = size;
        }

        public String toIniPath() {
            StringBuilder sb = new StringBuilder();
            sb.append(mFileName);

            switch(mType) {
                case TYPE_LOOP:
                    sb.append(".img");
                    break;

                case TYPE_DYNFILEFS:
                    sb.append(".dyn");
                    break;
            }

            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mPartitionName);
            dest.writeString(mFileName);
            dest.writeInt(mType);
            dest.writeLong(mSize);
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

    static {
        ALL_OS_TYPES.add(OSTYPE_ANDROID);
        ALL_OS_TYPES.add(OSTYPE_UBUNTU);

        ALL_OS_TYPES_NAMES.add(R.string.ostype_android);
        ALL_OS_TYPES_NAMES.add(R.string.ostype_ubuntu);
    }

    public static boolean isBindAllowed(String fsType) {
        switch(fsType) {
            case "ext2":
            case "ext3":
            case "ext4":
            case "f2fs":
                return true;
        }

        return false;
    }

    private void init(String filename) throws Exception {
        // create new ini
        if(filename.equals("")) {
            mFilename = filename;
            mIni = new Ini();
            return;
        }

        // read data
        String data = RootToolsEx.readFile(filename);
        if (data == null)
            throw new Exception("no data");

        // parse ini
        mFilename = filename;
        mIni = new Ini(new StringReader(data));

        initCmdline();
    }

    private void initCmdline() {
        // parse cmdline
        String cmdlineStr = mIni.get("replacements", "cmdline");
        if(cmdlineStr!=null) {
            String[] parts = cmdlineStr.split(" ");
            for(String part : parts) {
                if(part.equals(""))
                    continue;

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

    public OperatingSystem() {
        mCreationMode = true;
        try {
            init("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected OperatingSystem(Parcel in) {
        // filename
        mFilename = in.readString();

        // ini data
        String data = in.readString();
        try {
            mIni = new Ini(new StringReader(data));
        } catch (IOException e) {
            throw new RuntimeException("Can't read ini from buffer");
        }
        initCmdline();

        // creation mode
        mCreationMode = in.readByte() != 0;

        if (mCreationMode) {
            // icon uri
            if (in.readByte() == 0) {
                mIconUri = null;
            } else {
                mIconUri = Uri.parse(in.readString());
            }

            // location
            if (in.readByte() == 0) {
                mLocation = null;
            } else {
                mLocation = in.readParcelable(OperatingSystemEditActivity.MultibootDir.class.getClassLoader());
            }

            // partitions
            in.readList(mPartitions, Partition.class.getClassLoader());
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

        // write ini data
        StringWriter writer = new StringWriter();
        try {
            mIni.store(writer);
        } catch (IOException e) {
           throw new RuntimeException("Can't store ini to buffer");
        }
        dest.writeString(writer.getBuffer().toString());

        // creation mode
        dest.writeByte((byte) (mCreationMode ? 1 : 0));

        if(mCreationMode) {
            // icon uri
            dest.writeByte((byte) (mIconUri != null ? 1 : 0));
            if (mIconUri != null)
                dest.writeString(mIconUri.toString());

            // location
            dest.writeByte((byte) (mLocation != null ? 1 : 0));
            if (mLocation != null)
                dest.writeParcelable(mLocation, flags);

            // partitions
            dest.writeList(mPartitions);
        }
    }

    void saveToFile() throws IOException{
        File f = new File(mFilename);

        FileOutputStream os = new FileOutputStream(f);
        mIni.store(os);
        os.close();
    }

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

    public static List<String> getLocalizedOSTypeList(Context context) {
        ArrayList<String> list = new ArrayList<>();
        for(Integer id : ALL_OS_TYPES_NAMES) {
            list.add(context.getResources().getString(id));
        }

        return list;
    }

    public String getFilename() {
        return mFilename;
    }
    public void setFilename(String filename) {
        mFilename = filename;
    }

    public String getDirectory() {
        return new File(mFilename).getParent();
    }

    public String getName() {
        return mIni.get("config", "name");
    }

    public String getOperatingSystemType() {
        return mIni.get("config", "type");
    }

    public void setOperatingSystemType(String type) {
        mIni.put("config", "type", type);
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
        if(mCreationMode) {
            partitions.addAll(mPartitions);
            return partitions;
        }

        Profile.Section list = mIni.get("partitions");
        if(list!=null) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(list).entrySet()) {
                partitions.add(new Partition(entry.getKey(), entry.getValue()));
            }
        }

        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        Profile.Section list = mIni.get("partitions");
        if(list!=null) {
            mIni.remove(list);
        }

        for (Partition p : partitions) {
            mIni.put("partitions", p.getPartitionName(), p.toIniPath());
        }

        if(mCreationMode) {
            mPartitions.clear();
            mPartitions.addAll(partitions);
        }
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
        if(s==null)
            mIni.remove("replacements", "kernel");
        else
            mIni.put("replacements", "kernel", s);
    }

    public void setReplacementRamdisk(String s) {
        if(s==null)
            mIni.remove("replacements", "ramdisk");
        else
            mIni.put("replacements", "ramdisk", s);
    }

    public void setReplacementDT(String s) {
        if(s==null)
            mIni.remove("replacements", "dt");
        else
            mIni.put("replacements", "dt", s);
    }

    public boolean isCreationMode() {
        return mCreationMode;
    }

    public Uri getIconUri() {
        return mIconUri;
    }

    public void setIconUri(Uri uri) {
        mIconUri = uri;
    }

    public OperatingSystemEditActivity.MultibootDir getLocation() {
        return mLocation;
    }

    public void setLocation(OperatingSystemEditActivity.MultibootDir location) {
        mLocation = location;
    }
}
