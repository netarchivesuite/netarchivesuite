/*$Id$
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;

import java.io.IOException;
import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;

public class TestBatchJobRuns extends ARCBatchJob {
    boolean initialized;
    public int records_processed;
    boolean finished;

    public ARCBatchFilter getFilter() {
        return ARCBatchFilter.NO_FILTER;
    }

    public void initialize(OutputStream os) {
        initialized = true;
    }

    public void processRecord(ARCRecord record, OutputStream os) {
        records_processed++;
    }

    public void finish(OutputStream os) {
        try {
            os.write(("Records Processed = " + records_processed).getBytes());
        } catch (IOException e) {
            throw new IOFailure ("Error writing to output file: ", e);
        }
        finished = true;
    }
}
