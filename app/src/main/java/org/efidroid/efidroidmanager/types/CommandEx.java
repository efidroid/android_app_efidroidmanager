package org.efidroid.efidroidmanager.types;

import com.stericson.rootshell.execution.Command;

public class CommandEx extends Command {
    private StringBuffer mErrorBuffer = new StringBuffer();

    public CommandEx(int id, String... command) {
        super(id, command);
    }

    public CommandEx(int id, boolean handlerEnabled, String... command) {
        super(id, handlerEnabled, command);
    }

    public CommandEx(int id, int timeout, String... command) {
        super(id, timeout, command);
    }

    public CommandEx(int id, boolean handlerEnabled, int timeout, String... command) {
        super(id, handlerEnabled, timeout, command);
    }

    @Override
    public void commandOutput(int id, String line) {
        super.commandOutput(id, line);
        mErrorBuffer.append(line);
    }

    public String getErrorBuffer() {
        return mErrorBuffer.toString();
    }

    public void checkReturnCode() throws Exception {
        if (getExitCode() != 0) {
            String s = getErrorBuffer();
            if (s.trim().length() == 0)
                throw new ReturnCodeException(getExitCode());
            else
                throw new Exception(s);
        }
    }
}
