package org.efidroid.efidroidmanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Base64;

import com.stericson.rootshell.RootShell;
import com.stericson.rootshell.execution.Command;
import com.stericson.rootshell.execution.Shell;
import com.stericson.roottools.RootTools;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.efidroid.efidroidmanager.models.MountInfo;
import org.efidroid.efidroidmanager.services.IntentServiceEx;
import org.efidroid.efidroidmanager.types.MountEntry;
import org.efidroid.efidroidmanager.types.Pointer;
import org.efidroid.efidroidmanager.types.ReturnCodeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public final class RootToolsEx {
    private static String BUSYBOX = null;

    public interface MountInfoLoadedCallback {
        void onError(Exception e);
        void onSuccess(List<MountEntry> mountEntry);
    }

    public static void commandWait(Shell shell, Command cmd) throws Exception {
        while (!cmd.isFinished()) {

            RootShell.log(RootShell.version, shell.getCommandQueuePositionString(cmd));
            RootShell.log(RootShell.version, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.");

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
    }

    public static void commandWaitService(Shell shell, Command cmd, int pid, IntentServiceEx service) throws Exception {
        while (!cmd.isFinished()) {
            if(service.shouldStop()) {
                shell.close();
                ReturnCodeException.check(kill(pid));
                throw new InterruptedException("command got killed");
            }

            RootShell.log(RootShell.version, shell.getCommandQueuePositionString(cmd));
            RootShell.log(RootShell.version, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.");

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    RootShell.log(RootShell.version, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * @return <code>true</code> if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    public static boolean isAccessGiven(int timeout, int retry) {
        final Set<String> ID = new HashSet<String>();
        final int IAG = 158;

        try {
            RootShell.log("Checking for Root access");

            Command command = new Command(IAG, false, "busybox id") {
                @Override
                public void commandOutput(int id, String line) {
                    if (id == IAG) {
                        ID.addAll(Arrays.asList(line.split(" ")));
                    }

                    super.commandOutput(id, line);
                }
            };

            Shell.startRootShell(timeout, retry).add(command);
            commandWait(Shell.startRootShell(timeout, retry), command);
            ReturnCodeException.check(command.getExitCode());

            //parse the userid
            for (String userid : ID) {
                RootShell.log(userid);

                if (userid.toLowerCase().contains("uid=0")) {
                    RootShell.log("Access Given");
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static MountInfo getMountInfo() throws Exception {
        final ArrayList<MountEntry> mountinfos = new ArrayList<>();

        final Command command = new Command(0, false, "busybox cat /proc/1/mountinfo")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);

                String[] fields = line.split(" ");
                String[] majmin = fields[2].split(":");
                mountinfos.add(new MountEntry(
                        Integer.parseInt(fields[0]), // mountID
                        Integer.parseInt(fields[1]), // parentID
                        Integer.parseInt(majmin[0]), // major
                        Integer.parseInt(majmin[1]), // minor
                        fields[3], // root
                        fields[4], // mountPoint
                        fields[5], // mountOptions
                        fields[6], // optionalFields
                        fields[7], // separator
                        fields[8], // fsType
                        fields[9], // mountSource
                        fields[10] // superOptions
                ));
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return new MountInfo(mountinfos);
    }

    public static List<String> getBlockDevices() throws Exception {
        final ArrayList<String> devices = new ArrayList<>();

        final Command command = new Command(0, false, "busybox blkid")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String blkDevice = line.split(":")[0];
                devices.add(blkDevice);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return devices;
    }

    public static int[] getDeviceNode(String path) throws Exception {
        final Pointer<Integer> major = new Pointer<>(0);
        final Pointer<Integer> minor = new Pointer<>(0);

        final Command command = new Command(0, false, "busybox stat -Lt \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String[] parts = line.split(" ");

                major.value = Integer.parseInt(parts[9], 16);
                minor.value = Integer.parseInt(parts[10], 16);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return new int[]{major.value, minor.value};
    }

    public static List<String> getMultibootSystems(String path) throws Exception {
        final ArrayList<String> directories = new ArrayList<>();

        final Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 1 -maxdepth 1")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                directories.add(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return directories;
    }

    public static boolean isDirectory(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        final Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 0 -maxdepth 0 -type d")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode()==0 && exists.value;
    }

    public static boolean isFile(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 0 -maxdepth 0 -type f")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode()==0 && exists.value;
    }

    public static boolean nodeExists(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        Command command = new Command(0, false, "busybox find \""+path+"\" -mindepth 0 -maxdepth 0")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode()==0 && exists.value;
    }

    public static boolean mkdir(String path, boolean parents) throws Exception {
        final Command command = new Command(0, false, "busybox mkdir "+(parents?"-p":"")+ " \""+path+"\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        return command.getExitCode()==0;
    }

    public static long getDeviceSize(String path) throws Exception {
        final Pointer<Long> size = new Pointer<>(new Long(-1));

        final Command command = new Command(0, false, "busybox blockdev --getsize64 \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                size.value = Long.parseLong(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return size.value;
    }

    public static long getFileSize(String path) throws Exception {
        final Pointer<Long> size = new Pointer<>(new Long(-1));

        final Command command = new Command(0, false, "busybox stat -L -c %s \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                size.value = Long.parseLong(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        return size.value;
    }

    public static String readFile(String path) throws Exception {
        final StringWriter stringWriter = new StringWriter();

        final Command command = new Command(0, false, "busybox cat \""+path+"\"")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                stringWriter.write(line);
                stringWriter.write("\n");
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        if(command.getExitCode()!=0)
            return null;

        return stringWriter.getBuffer().toString();
    }

    public static byte[] readBinaryFile(String path) throws Exception {
        final StringBuffer sb = new StringBuffer();

        final Command command = new Command(0, false, "busybox cat \""+path+"\" | busybox base64")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sb.append(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        if(command.getExitCode()!=0)
            return null;

        return Base64.decode(sb.toString(), Base64.DEFAULT);
    }

    public static byte[] readBinaryFileEx(String path, long offset, long size) throws Exception {
        final StringBuffer sb = new StringBuffer();

        final Command command = new Command(0, false, "busybox dd if=\""+path+"\" bs=1 count="+size+" skip="+offset+" status=none | busybox base64")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sb.append(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());

        if(command.getExitCode()!=0)
            return null;

        return Base64.decode(sb.toString(), Base64.NO_WRAP);
    }

    public static void copyFileNoRoot(String source, String destination) throws Exception {
        final Command command = new Command(0, false, "su -c 'busybox cat \""+source+"\"' > \""+destination+"\"");

        Shell shell = RootTools.getShell(false);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());
    }

    public static File copyFileToTemp(Context context, String path) throws Exception {
        String cacheDir = context.getCacheDir().getAbsolutePath();
        File cacheFile = new File(cacheDir+"/"+ UUID.randomUUID().toString() + ".tmp");

        copyFileNoRoot(path, cacheFile.getAbsolutePath());

        return cacheFile;
    }

    public static int chmod(String file, String perm) throws Exception {
        final Command command = new Command(0, false, "busybox chmod \""+perm+"\" \""+file+"\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode();
    }

    public static int kill(int pid) throws Exception {
        final Command command = new Command(0, false, "busybox kill -SIGKILL "+pid);
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode();
    }

    public static int runServiceCommand(IntentServiceEx service, final Command command) throws Exception {
        final Pointer<Integer> pid = new Pointer<>(0);

        final Command pidcommand = new Command(0, false, 0, command.getCommand()+" &\n echo $!")
        {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                pid.value = Integer.parseInt(line);
            }
        };

        // run command async
        Shell shell = RootTools.getShell(true);
        shell.add(pidcommand);
        commandWait(shell, pidcommand);
        ReturnCodeException.check(pidcommand.getExitCode());

        // wait for async command to finish
        final Command waitcommand = new Command(0, false, 0, "wait "+pid.value);
        shell = RootTools.getShell(true);
        shell.add(waitcommand);
        commandWaitService(shell, waitcommand, pid.value, service);
        return waitcommand.getExitCode();
    }

    public static void createLoopImage(IntentServiceEx service, String filename, long size) throws Exception {
        final Command command = new Command(0, false, 0, "busybox dd if=/dev/zero of=\""+filename+"\" bs=512 count="+(Util.ROUNDUP(size,512)/512)+"");
        int rc = runServiceCommand(service, command);
        ReturnCodeException.check(rc);
    }

    public static void createPartitionBackup(IntentServiceEx service, String device, String filename) throws Exception {
        long size = getDeviceSize(device);

        final Command command = new Command(0, false, 0, "busybox dd if=\""+device+"\" of=\""+filename+"\" bs=512 count="+(Util.ROUNDUP(size,512)/512)+"");
        int rc = runServiceCommand(service, command);
        ReturnCodeException.check(rc);
    }

    public static class RootFile extends File {
        private boolean mIsDir;

        public RootFile(String path, boolean isDir) {
            super(path);
            mIsDir = isDir;
        }

        public RootFile(String path) {
            super(path);
            try {
                mIsDir = RootToolsEx.isDirectory(path);
            } catch (Exception e) {
                mIsDir = false;
            }
        }

        @Override
        public boolean isDirectory() {
            return mIsDir;
        }

        @Override
        public boolean isFile() {
            return !mIsDir;
        }

        @Override
        public File getParentFile() {
            return new RootFile(super.getParent());
        }

        @Override
        public File[] listFiles() {
            final String path = getAbsolutePath();
            final ArrayList<RootFile> list = new ArrayList<>();
            final String prefix = path.charAt(path.length()-1)=='/'?path:path+"/";
            final Pointer<Boolean> first = new Pointer<>(true);

            final Command command = new Command(0, false, "busybox ls -lL \""+path+"\"")
            {
                @Override
                public void commandOutput(int id, String line) {
                    super.commandOutput(id, line);
                    String[] parts = line.split(" ");

                    if(first.value) {
                        first.value = false;
                        return;
                    }

                    boolean is_dir = parts[0].charAt(0)=='d';
                    String name = parts[parts.length-1];
                    list.add(new RootFile(prefix+name, is_dir));
                }
            };

            try {
                Shell shell = RootTools.getShell(true);
                shell.add(command);
                commandWait(shell, command);
                if(command.getExitCode()!=0)
                    list.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list.toArray(new RootFile[]{});
        }
    }

    public static void copyFile(String source, String destination) throws Exception {
        final Command command = new Command(0, false, "busybox cat \""+source+"\" > \""+destination+"\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());
    }

    public static void writeDataToFile(Context context, String filename, String data) throws Exception {
        String cacheDir = context.getCacheDir().getAbsolutePath();
        File cacheFile = new File(cacheDir+"/"+ FilenameUtils.getName(filename));

        // write data to cache file
        FileOutputStream os = new FileOutputStream(cacheFile);
        os.write(data.getBytes());
        os.close();

        // copy cache file to destination
        copyFile(cacheFile.getAbsolutePath(), filename);

        // delete cache file
        cacheFile.delete();
    }

    public static void writeBitmapToPngFile(Context context, String filename, Bitmap bitmap) throws Exception {
        String cacheDir = context.getCacheDir().getAbsolutePath();
        File cacheFile = new File(cacheDir+"/"+ FilenameUtils.getName(filename));

        // write data to cache file
        Util.savePng(cacheFile, bitmap);

        // copy cache file to destination
        copyFile(cacheFile.getAbsolutePath(), filename);

        // delete cache file
        cacheFile.delete();
    }

    public static int unzip(String zip, String destination) throws Exception {
        mkdir(destination, true);

        final Command command = new Command(0, false, "busybox unzip -oq \""+zip+"\" -d \""+destination+"\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        return command.getExitCode();
    }

    public static void dd(String source, String destination) throws Exception {
        final Command command = new Command(0, false, "busybox dd if=\""+source+"\" of=\""+destination+"\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        ReturnCodeException.check(command.getExitCode());
    }

    public static void init(Context context) {
        try {
            InputStream is = context.getAssets().open(Build.CPU_ABI+"/busybox");
            File out = new File(context.getFilesDir(), "/busybox");
            if(!out.exists())
                FileUtils.copyInputStreamToFile(is, out);
            out.setExecutable(true, false);
            BUSYBOX = out.getAbsolutePath();
            RootShell.shellPathExtension = context.getFilesDir().getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
