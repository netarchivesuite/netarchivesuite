/* File:        $Id: CrawlLogLinesMatchingRegexp.java 2577 2012-12-13 18:47:05Z svc $
 * Revision:    $Revision: 2577 $
 * Author:      $Author: svc $
 * Date:        $Date: 2012-12-13 19:47:05 +0100 (Thu, 13 Dec 2012) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;
import dk.netarkivet.viewerproxy.reporting.CrawlLogLinesMatchingRegexp;

/** Tester for the class CrawlLogLinesMatchingRegexp used in 
 * Reporting.getCrawlLoglinesMatchingRegexp(jobid, regexp);
 *
 */
public class TestCrawlLogLinesMatchingRegexp {

    public static void main(String[] args) throws FileNotFoundException {
        FileBatchJob cJob = new CrawlLogLinesMatchingRegexp(".*netarkivet\\.dk.*");
        
        File f1 = new File("/home/svc/TESTFILES/1-metadata-1.warc");
        File f = new File("/home/svc/TESTFILES/1-metadata-1.arc");
        File[] files = new File[]{f1,f};
        BatchLocalFiles blf = new BatchLocalFiles(files);
        blf = new BatchLocalFiles(files);
        OutputStream os2 = new FileOutputStream("tmp1");
        blf.run(cJob, os2);
        
        System.out.println(cJob.getNoOfFilesProcessed());
        if (cJob.getFilesFailed().size() > 0) {
            System.out.println(cJob.getFilesFailed().size() + " failed");
        }
        for (ExceptionOccurrence e: cJob.getExceptions()) {
            System.out.println(e.getException());
        }
    }
}
