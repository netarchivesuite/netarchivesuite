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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
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
    /** The arguments for instantiating the batchjob.*/
    private List<String> args;

    /** Create a new batch job that runs the loaded class.
     * @param classFile the classfile for the batch job we want to run.
     * @param arguments The arguments for the batchjobs. This can be null.
     * @throws ArgumentNotValid If the classfile is null.
     */
    public LoadableFileBatchJob(File classFile, List<String> arguments)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(classFile, "File classFile");
        fileContents = FileUtils.readBinaryFile(classFile);
        fileName = classFile.getName();
        if(arguments == null) {
            this.args = new ArrayList<String>();
        } else {
            this.args = arguments;
        }
        
        loadBatchJob();
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
        loadBatchJob();
        loadedJob.initialize(os);
    }
    
    /**
     * Method for initializing the loaded batchjob.
     * @throws IOFailure If the batchjob cannot be loaded.
     */
    protected void loadBatchJob() throws IOFailure {
        ByteClassLoader singleClassLoader = new ByteClassLoader(fileContents);
        try {
            Class batchClass = singleClassLoader.defineClass();
            if(args.size() == 0) {
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
                + fileName + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (NoSuchMethodException e) {
            final String msg = "No constructor for the arguments '" + args 
                    + "' can be found for the batchjob '" + fileName + "'.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        } catch (InstantiationException e) {
            String errMsg = "Cannot instantiate batchjob from byte array";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        } catch (IllegalAccessException e) {
            String errMsg = "Cannot access loaded job from byte array";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
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
        log.trace("Started processing of file '" +  file.getAbsolutePath()
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
    
    @Override
    public boolean postProcess(InputStream input, OutputStream output) {
        ArgumentNotValid.checkNotNull(input, "InputStream input");
        ArgumentNotValid.checkNotNull(output, "OutputStream output");

        // Let the loaded job handle the post processing. 
        loadBatchJob();
        return loadedJob.postProcess(input, output);
    }
}
