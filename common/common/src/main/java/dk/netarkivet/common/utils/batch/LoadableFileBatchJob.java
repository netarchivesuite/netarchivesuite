/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

/** This implementation of FileBatchJob is a bridge to a class file given
 * as a File object.
 * The given class will be loaded and used to perform
 * the actions of the FileBatchJob class. */
public class LoadableFileBatchJob extends FileBatchJob {
    /** The class logger. */
    transient Log log = LogFactory.getLog(this.getClass().getName());

    /** The job loaded from file. */
    transient FileBatchJob loadedJob;
    /** The binary contents of the file before they are turned into a class. */
    byte[] fileContents;
    /** The name of the file before they are turned into a class. */    
    String fileName;

    /** Create a new batch job that runs the loaded class.
     * @param classFile the classfile for the batch job we want to run.
     */
    public LoadableFileBatchJob(File classFile) {
        ArgumentNotValid.checkNotNull(classFile, "File classFile");
        fileContents = FileUtils.readBinaryFile(classFile);
        fileName = classFile.getName();
    }

    /** Override of the default toString to include name of loaded class.
     * @return string representation of this class. */
   public String toString() {
       return this.getClass().getName() + " processing " + fileName;
   }

    /** Override of the default way to serialize this class.
     *
     * @param out Stream that the object will be written to.
     * @throws IOException In case there is an error from the underlying stream,
     * or this object cannot be serialized.
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
    }

    /** Override of the default way to unserialize an object of this class.
     *
     * @param in Stream that the object can be read from.
     * @throws IOException If there is an error reading from the stream, or
     * the serialized object cannot be deserialized due to errors in the
     * serialized form.
     * @throws ClassNotFoundException If the class definition of the
     * serialized object cannot be found.
     */
    private void readObject(ObjectInputStream in)
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
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
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
        log.debug("Started processing of file '" +  file.getAbsolutePath()
                + "'.");
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        return loadedJob.processFile(file, os);
    }

    /**
     * Finish up the job. This is called after the last process() call.
     *
     * @param os the OutputStream to which output should be written
     */
    public void finish(OutputStream os) {
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        loadedJob.finish(os);
    }
}
