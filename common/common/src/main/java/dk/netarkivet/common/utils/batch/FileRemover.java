/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
import java.io.OutputStream;


/**
 * This class implements a batchjob that enables you to delete files
 * from an archive. Note that the default Java Security Policy distributed
 * with NetarchiveSuite does not allow this.
 * 
 * @author ngiraud
 * @author svc
 */
public class FileRemover extends FileBatchJob {

    
    /**
     * The method to initialize the batchjob.
     * @param os The OutputStream to which output should be written
     * @see FileBatchJob#initialize(OutputStream)
     */
	@Override
	public void initialize(OutputStream os) {
	}
    
    /**
     * This method deletes the file in the argument list. Note that the default
     * Java Security Policy distributed with NetarchiveSuite does not
     * allow this.
     * @param file The file to be processed 
     * @param os The OutputStream to which output should be written
     * @see FileBatchJob#processFile(File, OutputStream)
     */	
	@Override
	public boolean processFile(File file, OutputStream os) {
		return file.delete();
	}

    /**
     * The method to finish the batchjob.
     * @param os The OutputStream to which output should be written
     * @see FileBatchJob#finish(OutputStream)
     */
	@Override
	public void finish(OutputStream os) {
	}
}
