/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback;

import java.io.File;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.wayback.batch.ExtractWaybackCDXBatchJob;

/**
 * csr forgot to comment this!
 *
 * @author csr
 * @since Jul 6, 2009
 */

public class ExtractWaybackCDXBatchJobTester extends TestCase {

     private BatchLocalFiles blaf;

    public void setUp() throws Exception {
        super.setUp();
        File file = new File(
                "tests/dk/netarkivet/wayback/data/originals/arcfile_withredirects.arc");
        assertTrue("File should exist: '"+file.getAbsolutePath()+"'",file.exists());
        blaf = new BatchLocalFiles(new File[] {file});
    }

    public void testProcess() throws IOException {
        ARCBatchJob job = new ExtractWaybackCDXBatchJob();
        OutputStream os = new ByteArrayOutputStream();
        blaf.run(job,os);
        os.flush();
        System.out.println(os);
        assertFalse("expect a non-empty output", os.toString() == null || os.toString().length()==0);
    }

}
