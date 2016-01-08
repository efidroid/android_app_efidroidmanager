package manager.efidroid.org.efidroidmanager;

import android.util.Log;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import manager.efidroid.org.efidroidmanager.models.MountInfo;
import manager.efidroid.org.efidroidmanager.types.MountEntry;
import manager.efidroid.org.efidroidmanager.types.Pointer;

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

    /**
     * @return <code>true</code> if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    public static boolean isAccessGiven(int timeout, int retry) {
        final Set<String> ID = new HashSet<String>();
        final int IAG = 158;

        try {
            RootShell.log("Checking for Root access");

            Command command = new Command(IAG, false, "id") {
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

        final Command command = new Command(0, false, "cat /proc/1/mountinfo")
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

        return new int[]{major.value, minor.value};
    }

    public static boolean fileExists(String path) throws Exception {
        Command command = new Command(0, false, "busybox ls \""+path+"\"");

        Shell shell = RootTools.getShell(true);
        shell.add(command);
        commandWait(shell, command);

        return command.getExitCode()==0;
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

        return directories;
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

        if(command.getExitCode()!=0)
            return null;

        return stringWriter.getBuffer().toString();
    }
}
