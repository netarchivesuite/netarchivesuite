/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.NoSuchMethodException;
import java.lang.IllegalAccessException;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/** This class is used for loading by LoadableFileBatchJobTester.  The compiled
 * class file should be placed under data/originals.
 * Create the jar file with jar cvf <jarfilename> <names of compiled classes> */
public class LoadableTestJob extends FileBatchJob {
    String ourName = "me";

    public LoadableTestJob() {
    }

    public LoadableTestJob(boolean innerClass) {
        if (innerClass) {
            ourName = new InnerClass().innerClassName();
        }
    }

    /**
     * Initialize the job before runnning. This is called before the
     * processFile() calls
     *
     * @param os the OutputStream to which output should be written
     */
    public void initialize(OutputStream os) {
        try {
            os.write(("initialize() called on " + this + "\n").getBytes());
        } catch (IOException e) {
            throw new IOFailure("Error in initializing " + this + ": ", e);
        }
    }

    /**
     * Process one file stored in the bit archive.
     *
     * @param file the file to be processed.
     * @param os   the OutputStream to which output should be written
     *
     * @return true if the file was successfully processed, false otherwise
     */
    public boolean processFile(File file, OutputStream os) {
        try {
            os.write(("processFile() called on " + this + " with " + file.getName() + "\n").getBytes());
            if (file.getName().endsWith("hack")) {
                // Try to break out of jail
                os.write("Trying direct breakout.\n".getBytes());
                new File("fnord").createNewFile();
                return true;
            } else if (file.getName().endsWith("break")) {
                // Try to usurp trusted utilities to break out of jail
                os.write("Trying indirect breakout.\n".getBytes());
                FileUtils.writeBinaryFile(new File("escape"), "free".getBytes());
                return true;
            } else if (file.getName().endsWith("climb")) {
                os.write("Trying backwards breakout.\n".getBytes());
                try {
                    ClassLoader loader = os.getClass().getClassLoader();
                    if (loader == null) {
                        loader = ClassLoader.getSystemClassLoader();
                    }
                    Class c = loader.loadClass("dk.netarkivet.utils.FileUtils");
                    c.getMethod("writeBinaryFile", File.class, String.class).invoke(null, new File("free"), "gotout".getBytes());
                } catch (ClassNotFoundException e) {
                    os.write(e.toString().getBytes());
                    return false;
                } catch (NoSuchMethodException e) {
                    os.write(e.toString().getBytes());
                    return false;
                } catch (IllegalAccessException e) {
                    os.write(e.toString().getBytes());
                    return false;
                } catch (InvocationTargetException e) {
                    os.write(e.toString().getBytes());
                    return false;
                }
                return true;
            } else if (file.getName().endsWith("smash")) {
                // Try to usurp trusted utilities to break out of jail
                os.write("Trying vandalism.\n".getBytes());
                FileUtils.writeBinaryFile(file, "sign".getBytes());
                return true;
            } else if (file.getName().endsWith("exec")) {
                // Try to usurp trusted utilities to break out of jail
                os.write("Trying external process.\n".getBytes());
                Runtime.getRuntime().exec("ls");
                return true;
            } else {
                return file.length() % 2 == 1;
            }
        } catch (IOException e) {
            throw new IOFailure("Error in processing " + file
                                + " with " + this + ": ", e);
        }
    }

    /**
     * Finish up the job. This is called after the last process() call.
     *
     * @param os the OutputStream to which output should be written
     */
    public void finish(OutputStream os) {
        try {
            os.write(("finish() called on " + this + "\n").getBytes());
        } catch (IOException e) {
            throw new IOFailure("Error in finishing " + this + ": ", e);
        }
    }

    public String toString() {
        return ourName;
    }

    class InnerClass {
        public String innerClassName() {
            return "inner";
        }
    }

    public static class InnerBatchJob extends FileBatchJob {
        public InnerBatchJob() {
        }

        /**
         * Initialize the job before runnning. This is called before the
         * processFile() calls
         *
         * @param os the OutputStream to which output should be written
         */
        public void initialize(OutputStream os) {
            try {
                os.write(("initialize() called on " + this + "\n").getBytes());
            } catch (IOException e) {
                throw new IOFailure("Error in initializing " + this + ": ", e);
            }
        }

        /**
         * Process one file stored in the bit archive.
         *
         * @param file the file to be processed.
         * @param os   the OutputStream to which output should be written
         *
         * @return true if the file was successfully processed, false otherwise
         */
        public boolean processFile(File file, OutputStream os) {
            try {
                os.write(("processFile() called on " + this + " with " + file.getName() + "\n").getBytes());
                return true;
            } catch (IOException e) {
                throw new IOFailure("Error in processing " + file
                                    + " with " + this + ": ", e);
            }
        }

        /**
         * Finish up the job. This is called after the last process() call.
         *
         * @param os the OutputStream to which output should be written
         */
        public void finish(OutputStream os) {
            try {
                os.write(("finish() called on " + this + "\n").getBytes());
            } catch (IOException e) {
                throw new IOFailure("Error in finishing " + this + ": ", e);
            }
        }

        public String toString() {
            return "inner";
        }
    }
}
