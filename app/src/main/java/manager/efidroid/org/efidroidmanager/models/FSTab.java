package manager.efidroid.org.efidroidmanager.models;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import manager.efidroid.org.efidroidmanager.types.FSTabEntry;

public class FSTab {
    private ArrayList<FSTabEntry> mFSTabEntries = new ArrayList<>();

    public FSTab(String data) throws IOException {
        List<String> lines = IOUtils.readLines(new StringReader(data));

        for(String line : lines) {
            String[] parts = line.split(" ");

            for(String part : parts) {
                mFSTabEntries.add(new FSTabEntry(parts[0], parts[1], parts[2], parts[3], parts[4]));
            }
            break;
        }
    }

    public List<FSTabEntry> getFSTabEntries() {
        return mFSTabEntries;
    }
}
