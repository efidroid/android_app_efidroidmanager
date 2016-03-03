package org.efidroid.efidroidmanager.types;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;

import java.util.List;

public class OSUpdateProgressServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";

    private OperatingSystem mOperatingSystem = null;
    private boolean mSuccess = false;

    @SuppressWarnings("unused")
    public OSUpdateProgressServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    public void onProcess(Bundle extras) {
        OperatingSystem os = extras.getParcelable(ARG_OPERATING_SYSTEM);
        if(os==null) {
            mSuccess = false;
            publishFinish(mSuccess);
            return;
        }

        mOperatingSystem = os;
        mSuccess = false;

        int progress = 0;
        try {
            String romDir;
            if(os.isCreationMode()) {
                progress = publishProgress(1, "Creating OS directory");

                // create multiboot directory
                String multibootDir = os.getLocation().path;
                if (!RootToolsEx.isDirectory(multibootDir)) {
                    if (!RootToolsEx.mkdir(multibootDir, true)) {
                        throw new Exception("Can't create multiboot directory");
                    }
                }

                // get available rom directory
                String _romDir = multibootDir + "/" + Util.name2path(os.getName());
                if (RootToolsEx.nodeExists(_romDir)) {
                    long unixTime = System.currentTimeMillis() / 1000L;
                    _romDir += "-" + unixTime;

                    if (RootToolsEx.nodeExists(_romDir)) {
                        throw new Exception("ROM directory does already exist");
                    }
                }

                // create ROM directory
                if (!RootToolsEx.mkdir(_romDir, false)) {
                    throw new Exception("Can't create ROM directory");
                }

                romDir = _romDir;
            }
            else {
                romDir = os.getDirectory();
            }

            // write multiboot.ini
            os.saveToFile(getService().getApplicationContext(), romDir+"/multiboot.ini");

            // write icon
            Bitmap iconBitmap = os.getIconBitmap(getService());
            String iconPath = romDir + "/icon.png";
            // 960 x 540
            if(iconBitmap!=null) {
                try {
                    double width = iconBitmap.getWidth();
                    double height = iconBitmap.getHeight();

                    // scale down
                    if (width > 192) {
                        height = height / width * 192f;
                        width = 192f;
                    }
                    if (height > 192) {
                        width = width / height * 192f;
                        height = 192f;
                    }

                    iconBitmap = Bitmap.createScaledBitmap(iconBitmap, (int)width, (int)height, false);
                    RootToolsEx.writeBitmapToPngFile(getService(), iconPath, iconBitmap);
                }
                catch (Exception e) {
                    throw new Exception("Can't write icon: "+e.getLocalizedMessage());
                }
            }
            else if(RootToolsEx.nodeExists(iconPath)) {
                if(!RootTools.deleteFileOrDirectory(iconPath, false)) {
                    throw new Exception("Can't delete old icon");
                }
            }

            // create partitions
            if(os.isCreationMode()) {
                List<OperatingSystem.Partition> partitions = os.getPartitions();

                for (int i=0; i<partitions.size(); i++) {
                    if (getService().shouldStop()) {
                        throw new Exception("Aborted");
                    }

                    OperatingSystem.Partition partition = partitions.get(i);
                    String filename = partition.toIniPath();
                    String filename_abs = romDir+"/"+filename;

                    // publich progress
                    progress = publishProgress(100/partitions.size()*i, "setup partition '"+partition.getPartitionName()+"'");

                    switch (partition.getType()) {
                        case OperatingSystem.Partition.TYPE_BIND:
                            if (!RootToolsEx.mkdir(filename_abs, false)) {
                                throw new Exception("Can't create directory '"+filename+"'");
                            }
                            break;

                        case OperatingSystem.Partition.TYPE_DYNFILEFS:
                            try {
                                RootToolsEx.createDynFileFsImage(getService(), filename_abs, partition.getSize());
                            } catch (InterruptedException e) {
                                throw new Exception("Aborted");
                            } catch (Exception e) {
                                throw new Exception("Can't create DynfileFS2 image '"+filename+"': "+e.getLocalizedMessage());
                            }
                            break;

                        case OperatingSystem.Partition.TYPE_LOOP:
                            try {
                                RootToolsEx.createLoopImage(getService(), filename_abs, partition.getSize());
                            } catch (InterruptedException e) {
                                throw new Exception("Aborted");
                            } catch (Exception e) {
                                throw new Exception("Can't create loop image '"+filename+"': "+e.getLocalizedMessage());
                            }
                            break;
                    }
                }
            }

            mSuccess = true;
        }
        catch (Exception e) {
            mSuccess = false;
            publishProgress(progress, e.getLocalizedMessage());
        }

        // publish status
        if(mSuccess)
            publishProgress(100, "Done");
        publishFinish(mSuccess);

        mOperatingSystem = null;
    }

    @Override
    public String getNotificationProgressTitle() {
        return (mOperatingSystem.isCreationMode() ? "Creating" : "Updating") + " System '" + mOperatingSystem.getName() + "'";
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            return (mOperatingSystem.isCreationMode() ? "Created" : "Updated") + " System '" + mOperatingSystem.getName() + "'";
        } else {
            return "Error " + (mOperatingSystem.isCreationMode() ? "Creating" : "Updating") + " System '" + mOperatingSystem.getName() + "'";
        }
    }
}
