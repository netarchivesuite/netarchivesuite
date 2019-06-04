package dk.netarkivet.harvester.heritrix3;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;

import org.netarchivesuite.heritrix3wrapper.LaunchResultHandlerAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockingCommandLauncher {

    /** The logger for this class. */
       private static final Logger log = LoggerFactory.getLogger(BlockingCommandLauncher.class);

    public ProcessBuilder processBuilder;

    public Map<String, String> env;

    public Process process;

    public BlockingCommandLauncher(File basedir, String[] cmd) {
        processBuilder = new ProcessBuilder(Arrays.asList(cmd));
        processBuilder.directory(basedir);
        env = processBuilder.environment();
    }

    public Integer start(LaunchResultHandlerAbstract resultHandler) throws IOException {

        process = processBuilder.start();

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Thread outputSinkThread = new Thread(new OutputSinkThread(outputReader, resultHandler));
        outputSinkThread.setDaemon(true);
        outputSinkThread.start();

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread errorSinkThread = new Thread(new ErrorSinkThread(errorReader, resultHandler));
        errorSinkThread.setDaemon(true);
        errorSinkThread.start();

        int exitValue = Integer.MIN_VALUE;
        try {
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            log.error("Interrupted error:", e);
        }
        if (resultHandler != null) {
            resultHandler.exitValue(exitValue);
        }
        return exitValue;
    }

    protected class OutputSinkThread implements Runnable {
        LaunchResultHandlerAbstract resultHandler;
        BufferedReader reader;

        protected OutputSinkThread(BufferedReader reader, LaunchResultHandlerAbstract resultHandler) {
            this.reader = reader;
            this.resultHandler = resultHandler;
        }

        @Override
        public void run() {
            try {
                if (resultHandler == null) {
                    return;
                }
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        resultHandler.output(line);
                    }
                } catch (IOException e) {
                    resultHandler.output(getStackTrace(e));
                } finally {
                    resultHandler.closeOutput();
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Exception on closing reader", e);
                }
            }
        }
    }

    protected String getStackTrace(IOException e) {
        String stackTrace = null;
        try (StringWriter stringWriter = new StringWriter();
                PrintWriter writer = new PrintWriter(stringWriter)) {
            e.printStackTrace(writer);
            writer.flush();
            stackTrace = stringWriter.toString();
        } catch (IOException ex) {
            log.warn("Error from string writer...", ex);
        }
        return stackTrace;
    }

    protected class ErrorSinkThread implements Runnable {
        LaunchResultHandlerAbstract resultHandler;
        BufferedReader reader;
        protected ErrorSinkThread(BufferedReader reader, LaunchResultHandlerAbstract resultHandler) {
            this.reader = reader;
            this.resultHandler = resultHandler;
        }

        @Override
        public void run() {
            try {
                if (resultHandler == null) {
                    return;
                }
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        resultHandler.error(line);
                    }
                } catch (IOException e) {
                    resultHandler.error(getStackTrace(e));
                } finally {
                    resultHandler.closeError();
                }
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Exception on closing reader", e);
                }
            }
        }
    }

}
