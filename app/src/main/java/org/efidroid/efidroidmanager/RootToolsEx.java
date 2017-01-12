package org.efidroid.efidroidmanager;

import android.content.Context;
import android.content.SharedPreferences;
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
import org.efidroid.efidroidmanager.types.CommandEx;
import org.efidroid.efidroidmanager.types.MountEntry;
import org.efidroid.efidroidmanager.types.Pointer;
import org.efidroid.efidroidmanager.types.ReturnCodeException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
            if (service.shouldStop()) {
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

        final CommandEx command = new CommandEx(0, false, "busybox cat /proc/1/mountinfo") {
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
                        "", // optionalFields
                        fields[fields.length - 3], // fsType
                        fields[fields.length - 2], // mountSource
                        fields[fields.length - 1] // superOptions
                ));
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return new MountInfo(mountinfos);
    }

    public static List<String> getBlockDevices() throws Exception {
        final ArrayList<String> devices = new ArrayList<>();

        String deviceWildcard = "/multiboot/dev/block/* /dev/block/*";
        final CommandEx command = new CommandEx(0, false, "busybox blkid -c /dev/null " + deviceWildcard) {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                String blkDevice = line.split(":")[0];
                if (blkDevice.startsWith("/multiboot") || devices.indexOf("/multiboot" + blkDevice) < 0)
                    devices.add(blkDevice);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return devices;
    }

    public static int[] getDeviceNode(String path) throws Exception {
        final Pointer<Integer> major = new Pointer<>(0);
        final Pointer<Integer> minor = new Pointer<>(0);

        final CommandEx command = new CommandEx(0, false, "busybox stat -Lt \"" + path + "\"") {
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
        command.checkReturnCode();

        return new int[]{major.value, minor.value};
    }

    public static List<String> getMultibootSystems(String path) throws Exception {
        final ArrayList<String> directories = new ArrayList<>();

        final CommandEx command = new CommandEx(0, false, "busybox find \"" + path + "\" -mindepth 1 -maxdepth 1") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                directories.add(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return directories;
    }

    public static boolean isDirectory(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        final Command command = new Command(0, false, "busybox find \"" + path + "\" -mindepth 0 -maxdepth 0 -type d") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode() == 0 && exists.value;
    }

    public static boolean isFile(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        Command command = new Command(0, false, "busybox find \"" + path + "\" -mindepth 0 -maxdepth 0 -type f") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode() == 0 && exists.value;
    }

    public static boolean nodeExists(String path) throws Exception {
        final Pointer<Boolean> exists = new Pointer<>(false);

        Command command = new Command(0, false, "busybox find \"" + path + "\" -mindepth 0 -maxdepth 0") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                exists.value = true;
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode() == 0 && exists.value;
    }

    public static void mkdir(String path, boolean parents) throws Exception {
        final CommandEx command = new CommandEx(0, false, "busybox mkdir " + (parents ? "-p" : "") + " \"" + path + "\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static long getDeviceSize(String path) throws Exception {
        final Pointer<Long> size = new Pointer<>(new Long(-1));

        final CommandEx command = new CommandEx(0, false, "busybox blockdev --getsize64 \"" + path + "\"") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                size.value = Long.parseLong(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return size.value;
    }

    public static long getFileSize(String path) throws Exception {
        final Pointer<Long> size = new Pointer<>(new Long(-1));

        final CommandEx command = new CommandEx(0, false, "busybox stat -L -c %s \"" + path + "\"") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                size.value = Long.parseLong(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return size.value;
    }

    public static String realpath(String path) throws Exception {
        final StringBuffer sb = new StringBuffer();

        final Command command = new Command(0, false, "busybox realpath \"" + path + "\"") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sb.append(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode() == 0 ? sb.toString() : null;
    }

    public static String readFile(String path) throws Exception {
        final StringWriter stringWriter = new StringWriter();

        final CommandEx command = new CommandEx(0, false, "busybox cat \"" + path + "\"") {
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
        command.checkReturnCode();

        return stringWriter.getBuffer().toString();
    }

    public static byte[] readBinaryFile(String path) throws Exception {
        final StringBuffer sb = new StringBuffer();

        final CommandEx command = new CommandEx(0, false, "busybox cat \"" + path + "\" | busybox base64") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sb.append(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return Base64.decode(sb.toString(), Base64.DEFAULT);
    }

    public static byte[] readBinaryFileEx(String path, long offset, long size) throws Exception {
        final StringBuffer sb = new StringBuffer();

        final CommandEx command = new CommandEx(0, false, "busybox dd if=\"" + path + "\" bs=1 count=" + size + " skip=" + offset + " status=none 2>/dev/null | busybox base64") {
            @Override
            public void commandOutput(int id, String line) {
                super.commandOutput(id, line);
                sb.append(line);
            }
        };

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();

        return Base64.decode(sb.toString(), Base64.NO_WRAP);
    }

    public static void copyFileNoRoot(String source, String destination) throws Exception {
        final CommandEx command = new CommandEx(0, false, "su -c 'busybox cat \"" + source + "\"' > \"" + destination + "\"");

        Shell shell = RootTools.getShell(false);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static File copyFileToTemp(Context context, String path) throws Exception {
        String cacheDir = context.getCacheDir().getAbsolutePath();
        File cacheFile = new File(cacheDir + "/" + UUID.randomUUID().toString() + ".tmp");

        copyFileNoRoot(path, cacheFile.getAbsolutePath());

        return cacheFile;
    }

    public static void chmod(String file, String perm) throws Exception {
        final CommandEx command = new CommandEx(0, false, "busybox chmod \"" + perm + "\" \"" + file + "\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static int kill(int pid) throws Exception {
        final Command command = new Command(0, false, "busybox kill -SIGKILL " + pid);
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode();
    }

    public static void runServiceCommand(IntentServiceEx service, String sCommand) throws Exception {
        final Pointer<Integer> pid = new Pointer<>(0);
        final File tmpfile = File.createTempFile("servicecmd", null);

        final Command pidcommand = new Command(0, false, 0, sCommand + " &>> " + tmpfile.getAbsolutePath() + " &\n echo $!") {
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
        final Command waitcommand = new Command(0, false, 0, "wait " + pid.value);
        shell = RootTools.getShell(true);
        shell.add(waitcommand);
        commandWaitService(shell, waitcommand, pid.value, service);

        // read output
        StringBuilder outputBuffer = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(tmpfile));
            for (String line; (line = br.readLine()) != null; ) {
                outputBuffer.append(line);
            }
        } catch (Exception e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }

        // delete tmpfile
        tmpfile.delete();

        // check return code
        int rc = waitcommand.getExitCode();
        if (rc != 0) {
            String s = outputBuffer.toString();
            if (s.trim().length() == 0)
                throw new ReturnCodeException(rc);
            else
                throw new Exception(s);
        }
    }

    public static void createLoopImage(IntentServiceEx service, String filename, long size) throws Exception {
        runServiceCommand(service, "busybox truncate -s " + size + " \"" + filename + "\"");
    }

    public static void createPartitionBackup(IntentServiceEx service, String device, String filename, long size) throws Exception {
        if (size == -1)
            size = getDeviceSize(device);

        runServiceCommand(service, "busybox dd if=\"" + device + "\" of=\"" + filename + "\" bs=512 count=" + (Util.ROUNDUP(size, 512) / 512) + "");
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
            final String prefix = path.charAt(path.length() - 1) == '/' ? path : path + "/";
            final Pointer<Boolean> first = new Pointer<>(true);

            final Command command = new Command(0, false, "busybox ls -lL \"" + path + "\"") {
                @Override
                public void commandOutput(int id, String line) {
                    super.commandOutput(id, line);
                    String[] parts = line.split(" ");

                    if (first.value) {
                        first.value = false;
                        return;
                    }

                    boolean is_dir = parts[0].charAt(0) == 'd';
                    String name = parts[parts.length - 1];
                    list.add(new RootFile(prefix + name, is_dir));
                }
            };

            try {
                Shell shell = RootTools.getShell(true);
                shell.add(command);
                commandWait(shell, command);
                if (command.getExitCode() != 0)
                    list.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return list.toArray(new RootFile[]{});
        }
    }

    public static void copyFile(String source, String destination) throws Exception {
        final CommandEx command = new CommandEx(0, false, "busybox cat \"" + source + "\" > \"" + destination + "\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static void writeDataToFile(Context context, String filename, String data) throws Exception {
        String cacheDir = context.getCacheDir().getAbsolutePath();
        File cacheFile = new File(cacheDir + "/" + FilenameUtils.getName(filename));

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
        File cacheFile = new File(cacheDir + "/" + FilenameUtils.getName(filename));

        // write data to cache file
        Util.savePng(cacheFile, bitmap);

        // copy cache file to destination
        copyFile(cacheFile.getAbsolutePath(), filename);

        // delete cache file
        cacheFile.delete();
    }

    public static void unzip(String zip, String destination) throws Exception {
        mkdir(destination, true);

        final CommandEx command = new CommandEx(0, false, "busybox unzip -oq \"" + zip + "\" -d \"" + destination + "\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static void dd(String source, String destination) throws Exception {
        final CommandEx command = new CommandEx(0, false, "busybox dd if=\"" + source + "\" of=\"" + destination + "\"");
        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);
        command.checkReturnCode();
    }

    public static void init(Context context) {
        SharedPreferences sp = context.getSharedPreferences(AppConstants.SHAREDPREFS_GLOBAL, Context.MODE_PRIVATE);
        int lastVersionCode = sp.getInt(AppConstants.SHAREDPREFS_GLOBAL_LAST_APP_VERSION, 0);

        try {
            ArrayList<String> abis = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                abis.addAll(Arrays.asList(Build.SUPPORTED_ABIS));
            }
            else {
                abis.add(Build.CPU_ABI);
                abis.add(Build.CPU_ABI2);
            }

            InputStream is = null;
            for(String abi : abis) {
                try {
                    is = context.getAssets().open(abi + "/busybox");
                }
                catch (FileNotFoundException ignored) {
                }
            }
            if(is==null) {
                throw new IOException("can't find compatible busybox");
            }

            File out = new File(context.getFilesDir(), "/busybox");
            if (!out.exists() || lastVersionCode != BuildConfig.VERSION_CODE)
                FileUtils.copyInputStreamToFile(is, out);
            out.setExecutable(true, false);
            out.setReadable(true, false);
            RootShell.shellPathExtension = context.getFilesDir().getAbsolutePath();

            sp.edit().putInt(AppConstants.SHAREDPREFS_GLOBAL_LAST_APP_VERSION, BuildConfig.VERSION_CODE).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
