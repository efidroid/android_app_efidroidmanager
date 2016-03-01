package org.efidroid.efidroidmanager.types;

public class ReturnCodeException extends Exception {
    public ReturnCodeException(int code) {
        super("return code was non zero: "+code);
    }

    public static void check(int code) throws ReturnCodeException{
        if(code!=0)
            throw new ReturnCodeException(code);
    }
}
