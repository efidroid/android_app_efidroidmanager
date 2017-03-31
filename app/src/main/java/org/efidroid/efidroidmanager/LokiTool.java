package org.efidroid.efidroidmanager;

public class LokiTool {
    public enum PartitionLabel {
        BOOT("boot"),
        RECOVERY("recovery");

        String mLabel;
        PartitionLabel(String s) {
            mLabel=s;
        }
        public String getLabel() {
            return mLabel;
        }
    }

    static {
        System.loadLibrary("loki-wrapper");
    }

    private LokiTool() {}

    private static native int lokiPatch(String partitionLabel, String bootloaderImage,
                                     String inImage, String outImage);
    private static native int lokiFlash(String partitionLabel, String image);

    public static boolean patchImage(PartitionLabel label, String bootloaderImage,
                              String inImage, String outImage) {
        return lokiPatch(label.getLabel(),bootloaderImage,inImage,outImage) == 0;
    }

    public static boolean flashImage(PartitionLabel label, String image) {
        return lokiFlash(label.getLabel(), image) == 0;
    }
}
