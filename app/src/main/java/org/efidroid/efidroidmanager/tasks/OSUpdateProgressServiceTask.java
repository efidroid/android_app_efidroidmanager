package org.efidroid.efidroidmanager.tasks;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.stericson.roottools.RootTools;

import org.efidroid.efidroidmanager.R;
import org.efidroid.efidroidmanager.RootToolsEx;
import org.efidroid.efidroidmanager.Util;
import org.efidroid.efidroidmanager.models.DeviceInfo;
import org.efidroid.efidroidmanager.models.OperatingSystem;
import org.efidroid.efidroidmanager.services.GenericProgressIntentService;
import org.efidroid.efidroidmanager.types.FSTabEntry;
import org.efidroid.efidroidmanager.types.ProgressServiceTask;

import java.util.List;

public class OSUpdateProgressServiceTask extends ProgressServiceTask {
    // args
    public static final String ARG_OPERATING_SYSTEM = "operatingsystem";
    public static final String ARG_DEVICE_INFO = "device_info";

    private OperatingSystem mOperatingSystem = null;
    private boolean mSuccess = false;

    @SuppressWarnings("unused")
    public OSUpdateProgressServiceTask(GenericProgressIntentService service) {
        super(service);
    }

    public void onProcess(Bundle extras) {
        OperatingSystem os = extras.getParcelable(ARG_OPERATING_SYSTEM);
        DeviceInfo deviceInfo = extras.getParcelable(ARG_DEVICE_INFO);
        if(os==null || deviceInfo==null) {
            mSuccess = false;
            publishFinish(mSuccess);
            return;
        }

        mOperatingSystem = os;
        mSuccess = false;

        int progress = 0;
        String romDir = null;
        try {
            if(os.isCreationMode()) {
                progress = publishProgress(1, getService().getString(R.string.creating_os_dir));

                // create multiboot directory
                String multibootDir = os.getLocation().path;
                RootToolsEx.mkdir(multibootDir, true);

                // get available rom directory
                String _romDir = multibootDir + "/" + Util.name2path(os.getName());
                if (RootToolsEx.nodeExists(_romDir)) {
                    long unixTime = System.currentTimeMillis() / 1000L;
                    _romDir += "-" + unixTime;

                    if (RootToolsEx.nodeExists(_romDir)) {
                        throw new Exception(getService().getString(R.string.rom_dir_does_already_exist));
                    }
                }

                // create ROM directory
                RootToolsEx.mkdir(_romDir, false);

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
                    throw new Exception(getService().getString(R.string.cant_write_icon)+" "+e.getLocalizedMessage());
                }
            }
            else if(RootToolsEx.nodeExists(iconPath)) {
                if(!RootTools.deleteFileOrDirectory(iconPath, false)) {
                    throw new Exception(getService().getString(R.string.cant_delete_old_icon));
                }
            }

            // create partitions
            if(os.isCreationMode()) {
                List<OperatingSystem.Partition> partitions = os.getPartitions();

                for (int i=0; i<partitions.size(); i++) {
                    if (getService().shouldStop()) {
                        throw new Exception(getService().getString(R.string.aborted));
                    }

                    OperatingSystem.Partition partition = partitions.get(i);
                    String filename = partition.toIniPath();
                    String filename_abs = romDir+"/"+filename;

                    // publish progress
                    progress = publishProgress(100/partitions.size()*i, getService().getString(R.string.setup_partition, partition.getPartitionName()));

                    switch (partition.getType()) {
                        case OperatingSystem.Partition.TYPE_BIND:
                            RootToolsEx.mkdir(filename_abs, false);
                            break;

                        case OperatingSystem.Partition.TYPE_LOOP:
                            try {
                                long size = partition.getSize();

                                if (partition.getPartitionName().equals("firmware")) {
                                    FSTabEntry fsTabEntry = deviceInfo.getFSTab().getEntryByName(partition.getPartitionName());
                                    if(fsTabEntry==null)
                                        throw new Exception("Can't find "+partition.getPartitionName()+"in fstab.multiboot");

                                    RootToolsEx.createPartitionBackup(getService(), fsTabEntry.getBlkDevice(), filename_abs, size);
                                }
                                else
                                    RootToolsEx.createLoopImage(getService(), filename_abs, size);
                            } catch (InterruptedException e) {
                                throw new Exception(getService().getString(R.string.aborted));
                            } catch (Exception e) {
                                throw new Exception(getService().getString(R.string.cant_create_loop_img, partition.getPartitionName())+e.getLocalizedMessage());
                            }
                            break;
                    }
                }
            }

            mSuccess = true;
        }
        catch (Exception e) {
            mSuccess = false;

            if(os.isCreationMode()) {
                publishProgress(progress, "Undoing changes");

                // delete rom directory
                try {
                    if (romDir != null) {
                        RootTools.deleteFileOrDirectory(romDir, false);
                    }
                } catch (Exception ignored) {
                }
            }

            publishProgress(progress, e.getLocalizedMessage());
        }

        // publish status
        if(mSuccess)
            publishProgress(100, getService().getString(R.string.md_done_label));
        publishFinish(mSuccess);
    }

    @Override
    public String getNotificationProgressTitle() {
        int textid = mOperatingSystem.isCreationMode()?R.string.creating_system:R.string.updating_system;
        return getService().getString(textid, mOperatingSystem.getName());
    }

    @Override
    public String getNotificationResultTitle() {
        if (mSuccess) {
            int textid = mOperatingSystem.isCreationMode()?R.string.created_system:R.string.updated_system;
            return getService().getString(textid, mOperatingSystem.getName());
        } else {
            int textid = mOperatingSystem.isCreationMode()?R.string.error_creating_system:R.string.error_updating_system;
            return getService().getString(textid, mOperatingSystem.getName());
        }
    }
}
