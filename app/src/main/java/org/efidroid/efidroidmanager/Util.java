package org.efidroid.efidroidmanager;

import org.efidroid.efidroidmanager.activities.OperatingSystemEditActivity;

import java.util.ArrayList;

public class Util {
    public static OperatingSystemEditActivity.MultibootPartitionInfo getPartitionInfoByName(ArrayList<OperatingSystemEditActivity.MultibootPartitionInfo> list, String name) {
        for(OperatingSystemEditActivity.MultibootPartitionInfo info : list) {
            if(info.name.equals(name))
                return info;
        }

        return null;
    }
}
