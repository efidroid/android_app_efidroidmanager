package org.efidroid.efidroidmanager.patching;

import android.content.Context;

import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.types.FSTabEntry;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public final class PatcherStorage {
    // FSTab flag to patcher
    private static final Map<String, Class<? extends Patcher>> patchers = new HashMap<>();

    static void registerPatcher(String flag, Class<? extends Patcher> patcher) {
        patchers.put(flag, patcher);
    }

    // selects patcher using FSTab flags. If no patcher selected, returns a DummyPatcher instance
    public static Patcher selectPatcher(DeviceInfo deviceInfo, Context context) throws Exception {
        for (FSTabEntry entry : deviceInfo.getFSTab().getFSTabEntries()) {
            for (String flag : patchers.keySet()) {
                if (entry.getFfMgrFlags().contains(flag)) {
                    Class<? extends Patcher> patcherClass = patchers.get(flag);
                    if (patcherClass == null) {
                        throw new Exception("Patcher registered but not found");
                    }
                    try {
                        Constructor constructor = patcherClass.getDeclaredConstructor(DeviceInfo.class, Context.class);
                        return (Patcher) constructor.newInstance(deviceInfo, context);
                    } catch (NoSuchMethodException e) {
                        throw new Exception("Cannot instantiate patcher");
                    }
                }
            }
        }
        return new DummyPatcher(deviceInfo, context);
    }

    private PatcherStorage() {
    }
}
