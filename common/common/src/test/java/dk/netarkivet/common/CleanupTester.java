/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.common;

import java.io.File;
import java.io.FileFilter;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;


public class CleanupTester extends TestCase {
	
	private File tmpdir;
	
	@Override
    public void setUp() throws Exception {
        super.setUp();
        tmpdir = FileUtils.getTempDir();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Remove files in FileUtils.getTempDir();
     */
    public void testThatMostTmpFilesGone() {
    	
    	File[] files = tmpdir.listFiles(new SvnFileFilter());
    	for (File f: files) {
    		FileUtils.removeRecursively(f);
    	}
    } 
    
    class SvnFileFilter implements FileFilter {	
    		public boolean accept(File f) {
    			return !f.getName().equalsIgnoreCase(".svn");
    		}
    }
}
