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
package dk.netarkivet.common.utils.arc;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.FileUtils;

/** This implementation of FileBatchJob is a bridge to a class file given
 * as a File object.
 * The given class will be loaded and used to perform
 * the actions of the FileBatchJob class. */
public class LoadableFileBatchJob extends FileBatchJob {
    transient FileBatchJob loadedJob;
    byte[] fileContents;
    transient Log log = LogFactory.getLog(this.getClass().getName());

    /** Create a new batch job that runs the loaded class. */
    public LoadableFileBatchJob(File classFile) {
        fileContents = FileUtils.readBinaryFile(classFile);
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        log = LogFactory.getLog(this.getClass().getName());
    }

    /**
     * Initialize the job before runnning. This is called before the
     * processFile() calls.
     *
     * @param os the OutputStream to which output should be written
     */
    public void initialize(OutputStream os) {
        ByteClassLoader singleClassLoader = new ByteClassLoader(fileContents);
        try {
            loadedJob = (FileBatchJob) singleClassLoader
                    .defineClass().newInstance();
        } catch (InstantiationException e) {
            log.warn("Cannot load job from byte array", e);
        } catch (IllegalAccessException e) {
            log.warn("Cannot access loaded job from byte array", e);
        }
        loadedJob.initialize(os);
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
        return loadedJob.processFile(file, os);
    }

    /**
     * Finish up the job. This is called after the last process() call.
     *
     * @param os the OutputStream to which output should be written
     */
    public void finish(OutputStream os) {
        loadedJob.finish(os);
    }
}
