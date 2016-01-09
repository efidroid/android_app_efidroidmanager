package org.efidroid.efidroidmanager.models;

import android.os.Parcel;
import android.os.Parcelable;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OperatingSystem implements Parcelable {
    private final Ini mIni;

    public static class Partition {
        public String name;
        public String path;

        public Partition(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }

    public OperatingSystem(Ini ini) {
        mIni = ini;
    }

    protected OperatingSystem(Parcel in) {
        StringReader stringReader = new StringReader(in.readString());
        try {
            mIni = new Ini(stringReader);
        } catch (IOException e) {
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
        for (Map.Entry<String, String> entry : list.entrySet()) {
            partitions.add(new Partition(entry.getKey(), entry.getValue()));
        }

        return partitions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        StringWriter stringWriter = new StringWriter();
        try {
            mIni.store(stringWriter);
        } catch (IOException e) {
            throw new RuntimeException("Can't store Operating System in Parcelable");
        }

        dest.writeString(stringWriter.getBuffer().toString());
    }
}
