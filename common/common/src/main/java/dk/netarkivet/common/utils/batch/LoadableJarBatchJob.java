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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * This implementation of FileBatchJob is a bridge to a jar file given as a File
 * object. The given class will be loaded and used to perform the actions of the
 * FileBatchJob class.
 */
public class LoadableJarBatchJob extends FileBatchJob {
    /** The FileBatchJob that this LoadableJarBatchJob is a wrapper for. */
    transient FileBatchJob loadedJob;

    /** The ClassLoader of type ByteJarLoader associated with this job. */
    private ClassLoader multipleClassLoader;

    /** The log. */
    transient Log log = LogFactory.getLog(this.getClass().getName());

    /** The name of the loaded Job. */
    private String jobClass;
    
    /** The arguments for instantiating the batchjob.*/
    private List<String> args;

    /**
     * Load a given class from a jar file.
     * 
     * @param jarFiles The jar file(s) to load from. This file may also contain 
     * other classes required by the FileBatchJob class.
     * @param arguments The arguments for the batchjob.
     * @param jobClass The class to load initially. This must be a subclass of
     * FileBatchJob.
     * @throws ArgumentNotValid If any of the arguments are null.
     */
    public LoadableJarBatchJob(String jobClass, List<String> arguments, 
            File... jarFiles) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(jarFiles, "File jarFile");
        ArgumentNotValid.checkNotNullOrEmpty(jobClass, "String jobClass");
        ArgumentNotValid.checkNotNull(arguments, "List<String> arguments");
        this.jobClass = jobClass;
        this.args = arguments;
        StringBuffer res = new StringBuffer(
                "Loading loadableJarBatchJob using jarfiles: ");
        for (File jarFile : jarFiles) {
            res.append(jarFile.getName());
        }
        res.append(" and jobclass '" + jobClass);
        if(!args.isEmpty()) {
            res.append(", and arguments: '" + args + "'.");
        }
        log.info(res.toString());
        multipleClassLoader = new ByteJarLoader(jarFiles);
        
        // Ensure that the batchjob can be loaded.
        loadBatchJob();
    }
    
    /**
     * Method for initialising the batch job.
     * 
     * @throws IOFailure If the job is not loaded correctly.
     */
    private void loadBatchJob() throws IOFailure {
        try {
            Class batchClass = multipleClassLoader.loadClass(jobClass);
            
            if(args.size() == 0) {
                // just load if no arguments.
                loadedJob = (FileBatchJob) batchClass.newInstance();
            } else {
                // get argument classes (string only).
                Class[] argClasses = new Class[args.size()];
                for(int i = 0; i < args.size(); i++) {
                    argClasses[i] = String.class;
                }

                // extract the constructor and instantiate the batchjob.
                Constructor con = batchClass.getConstructor(argClasses);
                loadedJob = (FileBatchJob) con.newInstance(args.toArray());
                log.debug("Loaded batchjob with arguments: '" + args + "'.");
            }
        } catch (InvocationTargetException e) {
            final String msg = "Not allowed to invoke the batchjob '" 
                + jobClass + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (NoSuchMethodException e) {
            final String msg = "No constructor for the arguments '" + args 
                    + "' can be found for the batchjob '" + jobClass + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (InstantiationException e) {
            final String msg = "Cannot instantiate loaded job class";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (IllegalAccessException e) {
            final String msg = "Cannot access loaded job from byte array";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (ClassNotFoundException e) {
            final String msg = "Cannot create job class from jar file";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Initialize the job before running. This is called before the
     * processFile() calls.
     * 
     * @param os
     *            the OutputStream to which output should be written
     */
    public void initialize(OutputStream os) {
        ArgumentNotValid.checkNotNull(os, "os");
        
        // Initialise the loadedJob.
        loadBatchJob();
        loadedJob.initialize(os);
    }

    /**
     * Process one file stored in the bit archive.
     * 
     * @param file
     *            the file to be processed.
     * @param os
     *            the OutputStream to which output should be written
     * 
     * @return true if the file was successfully processed, false otherwise
     */
    public boolean processFile(File file, OutputStream os) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        return loadedJob.processFile(file, os);
    }

    /**
     * Finish the job. This is called after the last process() call.
     * 
     * @param os
     *            the OutputStream to which output should be written
     */
    public void finish(OutputStream os) {
        ArgumentNotValid.checkNotNull(os, "OutputStream os");
        loadedJob.finish(os);
    }

    /**
     * Human readable representation of this object. Overrides
     * FileBatchJob.toString to include name of loaded jar/class.
     * 
     * @return a Human readable representation of this class
     */
    public String toString() {
        return this.getClass().getName() + " processing " + jobClass + " from "
                + multipleClassLoader.toString();
    }

    /**
     * Override of the default way to serialize this class.
     * 
     * @param out
     *            Stream that the object will be written to.
     * @throws IOException
     *             In case there is an error from the underlying stream, or this
     *             object cannot be serialized.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     * Override of the default way to deserialize an object of this class.
     * 
     * @param in
     *            Stream that the object can be read from.
     * @throws IOException
     *             If there is an error reading from the stream, or the
     *             serialized object cannot be deserialized due to errors in the
     *             serialized form.
     * @throws ClassNotFoundException
     *             If the class definition of the serialized object cannot be
     *             found.
     */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        log = LogFactory.getLog(this.getClass().getName());
    }

    @Override
    public boolean postProcess(InputStream input, OutputStream output) {
        ArgumentNotValid.checkNotNull(input, "InputStream input");
        ArgumentNotValid.checkNotNull(output, "OutputStream output");

        // Let the loaded job handle the post processing. 
        log.debug("Post-processing in the loaded batchjob.");
        loadBatchJob();
        return loadedJob.postProcess(input, output);
    }
    
    /**
     * Method for retrieving the name of the loaded class.
     *  
     * @return The name of the loaded class.
     */
    public String getLoadedJobClass() {
        return jobClass;
    }
}
