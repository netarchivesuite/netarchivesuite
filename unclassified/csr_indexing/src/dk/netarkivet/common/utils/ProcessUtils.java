/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Various utilities for running processes -- not exactly Java's forte.
 *
 */

public class ProcessUtils {
    /** The logger. */
    private static Log log = LogFactory.getLog(ProcessUtils.class);

    /** Runs an external process that takes no input, discarding its output.
     *
     * @param environment An environment to run the process in (may be null)
     * @param programAndArgs The program and its arguments.
     * @return The return code of the process.
     */
    public static int runProcess(String[] environment,
            String... programAndArgs) {
        try {
            Process p = Runtime.getRuntime().exec(programAndArgs, environment);
            discardProcessOutput(p.getInputStream());
            discardProcessOutput(p.getErrorStream());
            while (true) {
                try {
                    return p.waitFor();
                } catch (InterruptedException e) {
                    // Ignoring interruptions, we just want to try waiting
                    // again.
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Failure while running " 
                    + Arrays.toString(programAndArgs), e);
        }
    }

    /** Runs an external process that takes no input, discarding its output.
     * This is a convenience wrapper for runProcess(environment, programAndArgs)
     * @param programAndArgs The program to run and its arguments
     * @return The return code of the process.
     */
    public static int runProcess(String... programAndArgs) {
        return runProcess(null, programAndArgs);
    }

    /**
     * Read the output from a process. Due to oddities in the Process
     * handling, this has to be done char by char. This method just implements
     * a consumer thread to eat the output of a process and so prevent
     * blocking.
     *
     * @param inputStream
     *            A stream to read up to end of file. This stream is closed at
     *            some point in the future, but not necessarily before this
     *            method returns.
     */
    public static void discardProcessOutput(final InputStream inputStream) {
        makeCollectorThread(inputStream,
                            new DiscardingOutputStream(), -1).start();
    }

    /** Collect all output from an inputstream, up to maxCollect bytes,
     * in an output object. This will eventually close the given InputStream,
     * but not necessarily before the method returns.  The thread created
     * is placed in a thread set, and should be removed once all output
     * has been collected.  While only a limited amount may be written to
     * the output object, the entire output will be read from the inputStream
     * unless the thread or the inputStream is destroyed first.
     *
     * @param inputStream The inputstream to read contents from
     * @param maxCollect The maximum number of bytes to collect, or -1 for no
     *  limit
     * @param collectionThreads Set of threads that concurrently collect output
     * @return An object that collects the output.  Once the thread returned
     * is finished, the object will no longer be written to.  The collected
     * output can be retrieved with the toString method.
     */
    public static Object collectProcessOutput(
            final InputStream inputStream, final int maxCollect,
            Set<Thread> collectionThreads) {
        final OutputStream stream = new ByteArrayOutputStream();
        Thread t = makeCollectorThread(inputStream, stream, maxCollect);
        t.start();
        collectionThreads.add(t);
        return stream;
    }

    /** Collect all output from an inputstream, appending it to a file.
     * This will eventually close the given InputStream,
     * but not necessarily before the method returns.  The thread created
     * is placed in a thread set, and should be removed once all output
     * has been collected.
     *
     * @param inputStream The inputstream to read contents from
     * @param outputFile The file that output should be appended to.
     * @param collectionThreads Set of threads that concurrently collect output
     */
    public static void writeProcessOutput(final InputStream inputStream,
                                            final File outputFile,
                                            Set<Thread> collectionThreads) {
        final OutputStream stream;
        try {
            stream = new FileOutputStream(outputFile, true);
        } catch (FileNotFoundException e) {
            throw new IOFailure("Cannot create file '" + outputFile
                                + " to write process output to.", e);
        }
        Thread t = makeCollectorThread(inputStream, stream, -1);
        t.start();
        collectionThreads.add(t);
    }

    /** Collect all output from an inputstream, writing it to an output stream,
     * using a separate thread. This will eventually close the given InputStream
     * and OutputStream, but not necessarily before the method returns. While
     * only a limited amount may be written to the output object, the entire
     * output will be read fron the inputStream unless the thread or the
     * inputStream is destroyed first.
     *
     * @param inputStream The inputstream to read contents from
     * @param outputStream An stream to write the output to.
     * @param maxCollect The maximum number of bytes to collect, or -1 for no
     *  limit
     * @return The thread that will collect the output.
     */
    private static Thread makeCollectorThread(final InputStream inputStream,
                                              final OutputStream outputStream,
                                              final int maxCollect) {
        return new Thread() {
            public void run() {
                try {
                    InputStream reader = null;
                    OutputStream writer = null;
                    try {
                        reader = new BufferedInputStream(inputStream);
                        writer = new BufferedOutputStream(outputStream);
                        copyContents(reader, writer, maxCollect);
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                        if (writer != null) {
                            writer.close();
                        }
                    }
                } catch (IOException e) {
                    // This seems ugly
                    throw new RuntimeException("Couldn't close streams for "
                            + "process.", e);
                }
            }
        };
    }

    /** Reads all contents from a stream, writing some or all to another.
     *
     * @param in InputStream to read from
     * @param out OutputStream to write to
     * @param maxCollect Maximum number of bytes to write to out
     * @throws IOFailure If there are problems reading or writing.
     */
    private static void copyContents(InputStream in, OutputStream out,
                                     int maxCollect) {
        int bytesRead;
        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        int totalBytesRead = 0;
        try {
            while ((bytesRead = in.read(buffer, 0,
                                        Constants.IO_BUFFER_SIZE))
                   != -1) {
                if (maxCollect == -1) {
                    out.write(buffer, 0, bytesRead);
                } else if (totalBytesRead < maxCollect) {
                    out.write(buffer, 0,
                              Math.min(bytesRead,
                                       maxCollect - totalBytesRead));
                }
                // Close early if applicable
                if (maxCollect != -1
                    && totalBytesRead < maxCollect
                    && totalBytesRead + bytesRead > maxCollect) {
                    out.close();
                }
                totalBytesRead += bytesRead;
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading or writing process output", e);
        }
    }

    /** Wait for the end of a process, but only for a limited time.  This
     * method takes care of the ways waitFor can get interrupted.
     *
     * @param p Process to wait for
     * @param maxWait The maximum number of milliseconds to wait for the
     * process to exit.
     * @return Exit value for process, or null if the process didn't exit
     * within the expected time.
     */
    public static Integer waitFor(final Process p, long maxWait) {
        ArgumentNotValid.checkNotNull(p, "Process p");
        ArgumentNotValid.checkPositive(maxWait, "long maxWait");
        long startTime = System.currentTimeMillis();
        Timer timer = new Timer(true);
        final Thread waitThread = Thread.currentThread();
        boolean wakeupScheduled = false;
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        while (System.currentTimeMillis() < startTime + maxWait) {
            try {
                if (!wakeupScheduled) {
                    // First time in here, we need to start the wakup thread,
                    // but be sure it doesn't notify us too early or too late.
                    synchronized(waitThread) {
                        timer.schedule(new TimerTask() {
                            public void run() {
                                synchronized(waitThread) {
                                    if (!doneWaiting.get()) {
                                        waitThread.interrupt();
                                    }
                                }
                            }
                        }, maxWait);
                        wakeupScheduled = true;
                    }
                }

                p.waitFor();
                break;
            } catch (InterruptedException e) {
                // May happen for a number of reasons.  We just check if we've
                // timed out yet when we go through the loop again.
            }
        }
        synchronized (waitThread) {
            timer.cancel();
            doneWaiting.set(true);
            Thread.interrupted(); // In case the timer task interrupted.
        }
        try {
            return p.exitValue();
        } catch (IllegalThreadStateException e) {
            log.warn("Process '" + p + "' did not exit within "
                     + (System.currentTimeMillis() - startTime)
                     + " milliseconds");
            return null;
        }
    }
}
