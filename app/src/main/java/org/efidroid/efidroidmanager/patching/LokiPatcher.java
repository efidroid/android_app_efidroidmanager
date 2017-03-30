package org.efidroid.efidroidmanager.patching;

import android.content.Context;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.types.FSTabEntry;

import java.util.List;

class LokiPatcher extends Patcher {
    private enum ImageType {
        BOOT("boot", "lokiboot"),
        RECOVERY("recovery", "lokirecovery");

        private final String mName, mFlag;

        ImageType(String name, String flag) {
            mName = name;
            mFlag = flag;
        }

        String getName() {
            return mName;
        }

        String getFlag() {
            return mFlag;
        }
    }

    static {
        // LOKI usable only for ARMv7-based phones
        List<String> abis = Util.getABIs();
        if (abis.contains("armeabi-v7a")) {
            System.loadLibrary("loki_wrapper");
            for (ImageType imageType : ImageType.values()) {
                PatcherStorage.registerPatcher(imageType.getFlag(), LokiPatcher.class);
            }
        }
    }

    private static final String ABootFlag = "lokiaboot";

    private String aBootImage;

    public LokiPatcher(DeviceInfo deviceInfo, Context context) {
        super(deviceInfo, context);
    }

    @Override
    public void prepareEnvironment(String updateDir) throws Exception {
        FSTabEntry aBootEntry = null;
        for (FSTabEntry entry : getDeviceInfo().getFSTab().getFSTabEntries()) {
            if (entry.getFfMgrFlags().contains(ABootFlag)) {
                aBootEntry = entry;
            }
        }
        if (aBootEntry == null) {
            throw new Exception("ABoot " + ABootFlag + " entry not found (bad multiboot.fstab)");
        }
        aBootImage = updateDir + "/aboot.img";
        RootToolsEx.dd(aBootEntry.getBlkDevice(), aBootImage);
    }

    @Override
    public boolean isPatchRequired(FSTabEntry entry) {
        for (ImageType imageType : ImageType.values()) {
            if (entry.getFfMgrFlags().contains(imageType.getFlag())) {
                return true;
            }
        }
        return false;
    }

    private static native boolean nativePatchImage(String imageType, String aBootImage, String in, String out);

    @Override
    public void patchImage(FSTabEntry destEntry, String image) throws Exception {
        boolean isSuccessfulPatch = false;
        String outputImage = null;
        for (ImageType imageType : ImageType.values()) {
            if (destEntry.getFfMgrFlags().contains(imageType.getFlag())) {
                outputImage = image.substring(0, image.lastIndexOf('/')) + "/" + imageType.getName() + ".img";
                isSuccessfulPatch = nativePatchImage(imageType.getName(), aBootImage, image, outputImage);
            }
        }
        if (isSuccessfulPatch) {
            RootTools.copyFile(outputImage, image, false, true);
            RootTools.deleteFileOrDirectory(outputImage, false);
        } else {
            throw new Exception("Image patch error");
        }
    }

    @Override
    public void cleanupEnvironment() {
        RootTools.deleteFileOrDirectory(aBootImage, false);
    }
}
