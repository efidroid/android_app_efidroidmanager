package org.efidroid.efidroidmanager.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.io.IOUtils;
import org.efidroid.efidroidmanager.types.FSTabEntry;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FSTab implements Parcelable {
    // data
    private ArrayList<FSTabEntry> mFSTabEntries = new ArrayList<>();

    public FSTab(String data) throws IOException {
        List<String> lines = IOUtils.readLines(new StringReader(data));

        for(String line : lines) {
            String[] parts = line.split(" ");
            ArrayList<String> al = new ArrayList<>(Arrays.asList(parts));
            al.removeAll(Collections.singleton(""));
            mFSTabEntries.add(new FSTabEntry(al.get(0), al.get(1), al.get(2), al.get(3), al.get(4)));
        }
    }

    protected FSTab(Parcel in) {
        in.readList(mFSTabEntries, FSTabEntry.class.getClassLoader());
    }

    public static final Creator<FSTab> CREATOR = new Creator<FSTab>() {
        @Override
        public FSTab createFromParcel(Parcel in) {
            return new FSTab(in);
        }

        @Override
        public FSTab[] newArray(int size) {
            return new FSTab[size];
        }
    };

    public List<FSTabEntry> getFSTabEntries() {
        return mFSTabEntries;
    }

    public FSTabEntry getEntryByName(String name) {
        for (FSTabEntry entry : getFSTabEntries()) {
            if(entry.getName().equals(name))
                return entry;
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mFSTabEntries);
    }
}
