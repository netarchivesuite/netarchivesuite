/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.testutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;

/**
 * A local-file based arc repository client.  Given one or more directories
 * with ARC files, this client will serve them out like a normal arcrepository.
 *
 */

public class TestArcRepositoryClient extends JMSArcRepositoryClient {
    public File arcDir;
    /** How many times batch has been called */
    public int batchCounter;
    /** Whether the batch call should die with an exception */
    public boolean batchMustDie;
    /** How many milliseconds the batch should pause before running */
    public int batchPauseMilliseconds;
    public File tmpDir;

    public TestArcRepositoryClient(File arcdir) {
        super();
        this.arcDir = arcdir;
        tmpDir = FileUtils.getTempDir();
    }

    public void getFile(String arcfilename, Replica replica, File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(replica, "replica");
        ArgumentNotValid.checkNotNull(toFile, "toFile");
        File actualFile = new File(arcDir, arcfilename);
        try {
            InputStream in = null;
            try {
                in = new FileInputStream(actualFile);
                FileUtils.writeStreamToFile(in, toFile);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error reading file " + actualFile, e);
        }
    }

    public BitarchiveRecord get(String arcfile, long index)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(arcfile, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");

        File in = new File(arcDir, arcfile);
        try {
            ARCReader arcReader = ARCReaderFactory.get(in);
            ARCRecord arc = (ARCRecord) arcReader.get(index);
            BitarchiveRecord result = new BitarchiveRecord(arc);
            return result;
        } catch (IOException e) {
            throw new IOFailure("Error reading record from " + arcfile + " offset " + index, e);
        }
    }


    public BatchStatus batch(FileBatchJob job, String replicaId) {
        batchCounter++;
        if (batchMustDie) {
            throw new IOFailure("Committing suicide as ordered, SIR!");
        }
        if (batchPauseMilliseconds > 0) {
            try {
                Thread.sleep(batchPauseMilliseconds);
            } catch (InterruptedException e) {
                // Don't care.
            }
        }

        File f = new File(tmpDir, "batchOutput");
        OutputStream os = null;
        try {
            os = new FileOutputStream(f);
        } catch (IOException e) {
            return new BatchStatus(replicaId,
                    new ArrayList<File>(), 0, null, job.getExceptions());
        }
        File[] files = arcDir.listFiles(
                new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".arc");
                    }
                }
        );
        job.initialize(os);
        int processed = 0;
        List<File> failures = new ArrayList<File>();
        for (File f1 : files) {
            if (job.getFilenamePattern().matcher(f1.getName()).matches()) {
                processed ++;
                if (!job.processFile(f1, os)) {
                    failures.add(f1);
                }
            }
        }
        job.finish(os);
        try {
            os.close();
        } catch (IOException e) {
            throw new IOFailure("Error in close", e);
        }
        return new BatchStatus(replicaId,
                failures, processed, 
                new TestRemoteFile(f, batchMustDie,
                        batchMustDie,
                        batchMustDie),
                        job.getExceptions());
    }
}
