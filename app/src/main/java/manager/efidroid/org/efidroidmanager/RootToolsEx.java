package manager.efidroid.org.efidroidmanager;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class RootToolsEx {
    private static void commandWait(Shell shell, Command cmd) throws Exception {
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
}
