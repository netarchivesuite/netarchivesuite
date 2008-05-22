package dk.netarkivet.testutils.preconfigured;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import dk.netarkivet.common.exceptions.PermissionDenied;

public class PreserveStdStreams implements TestConfigurationIF {
    private InputStream origStdIn;
    private PrintStream origStdOut;
    private PrintStream origStdErr;
    private ByteArrayOutputStream myOut;
    private ByteArrayOutputStream myErr;
    private boolean overwrite;

    public PreserveStdStreams(boolean andOverwrite) {
        overwrite = andOverwrite;
    }

    public PreserveStdStreams() {
        this(false);
    }

    public void setUp() {
        origStdIn = System.in;
        origStdOut = System.out;
        origStdErr = System.err;
        if(overwrite) {
            myOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(myOut));
        }
        if(overwrite) {
            myErr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(myErr));
        }
    }

    public void tearDown() {
        System.setIn(origStdIn);
        System.setOut(origStdOut);
        System.setErr(origStdErr);
    }

    public String getOut() {
        if(overwrite) {
            return myOut.toString();
        }
        throw new PermissionDenied("Set overwrite to true to use this facility");
    }

    public String getErr() {
        if(overwrite) {
            return myErr.toString();
        }
        throw new PermissionDenied("Set overwrite to true to use this facility");
    }
}
