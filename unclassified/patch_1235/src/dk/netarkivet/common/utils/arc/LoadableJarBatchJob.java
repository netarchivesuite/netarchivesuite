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
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.common.utils.FileUtils;

/** This implementation of FileBatchJob is a bridge to a jar file given as a
 * File object.
 * The given class will be loaded and used to perform
 * the actions of the FileBatchJob class. */
public class LoadableJarBatchJob extends FileBatchJob {
    transient FileBatchJob loadedJob;
    private ClassLoader multipleClassLoader;
    transient Log log = LogFactory.getLog(this.getClass().getName());
    private String jobClass;

    static class ByteJarLoader extends ClassLoader implements Serializable {
        Map<String, byte[]> binaryData = new HashMap<String, byte[]>();

        public ByteJarLoader(File file) {
            try {
                JarFile jarFile = new JarFile(file);
                for (Enumeration<JarEntry> e = jarFile.entries();
                     e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    String name = entry.getName();
                    InputStream in = jarFile.getInputStream(entry);
                    ByteArrayOutputStream out = new ByteArrayOutputStream((int)entry.getSize());
                    StreamUtils.copyInputStreamToOutputStream(in, out);
                    binaryData.put(name, out.toByteArray());
                }
            } catch (IOException e) {
                throw new IOFailure("Failed to load jar file '" + file + "'");
            }
        }

        public Class findClass(String className) throws ClassNotFoundException {
            if (binaryData.containsKey(className)) {
                final byte[] bytes = binaryData.get(className);
                return defineClass(className, bytes, 0, bytes.length);
            } else {
                return super.findClass(className);
            }
        }
    }

    /** Load a given class from a jar file.
     *
     * @param jarFile The jar file to load from.  This file may also contain
     * other classes required by the FileBatchJob class.
     * @param jobClass The class to load initially.  This must be a
     * subclass of FileBatchJob
     */
    public LoadableJarBatchJob(File jarFile, String jobClass) {
        this.jobClass = jobClass;
        multipleClassLoader = new ByteJarLoader(jarFile);
    }

    /**
     * Initialize the job before runnning. This is called before the
     * processFile() calls.
     *
     * @param os the OutputStream to which output should be written
     */
    public void initialize(OutputStream os) {
        try {
            loadedJob = (FileBatchJob) multipleClassLoader
                    .loadClass(jobClass).newInstance();
        } catch (InstantiationException e) {
            final String msg = "Cannot instantiate loaded job class";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (IllegalAccessException e) {
            final String msg = "Cannot access loaded job from byte array";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (ClassNotFoundException e) {
            final String msg = "Cannout create job class from jar file";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
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
}