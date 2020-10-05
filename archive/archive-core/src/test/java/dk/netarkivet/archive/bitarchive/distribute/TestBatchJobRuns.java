/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.IOException;
import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;

/**
 * Helper batch job to write number of records processed at finish.
 */
@SuppressWarnings({"serial"})
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
            throw new IOFailure("Error writing to output file: ", e);
        }
        finished = true;
    }
}
