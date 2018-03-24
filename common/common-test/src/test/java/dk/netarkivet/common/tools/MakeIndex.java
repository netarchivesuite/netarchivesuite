/*
 * #%L
 * Netarchivesuite - common - test
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

package dk.netarkivet.common.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;

/**
 * A tool to create index.cdx files.
 * <p>
 * Usage: java dk.netarkivet.common.tools.MakeIndex [filename]
 */
@Deprecated
// what is this used for? /tra
public class MakeIndex {
    public static void main(String[] argv) {
        File indexfile = new File("index.cdx");
        if (argv.length > 1) {
            System.err.println("Too many arguments.");
            dieWithUsage();
        }
        if (argv.length != 0) {
            indexfile = new File(argv[0]);
        }
        ViewerArcRepositoryClient arcrep = null;
        try {
            // Set to one below the number used by the hacos to avoid them
            // eating our reply.
            // Yes, it's a kludge. Anyone care to add me a channel just for this?
            Settings.set(CommonSettings.HTTP_PORT_NUMBER, "" + (Settings.getInt(CommonSettings.HTTP_PORT_NUMBER) - 1));
            System.out.println("Connecting to ArcRepository");
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
            System.out.println("Creating index file '" + indexfile + "'");
            ExtractCDXJob cdxjob = new ExtractCDXJob(false);
            // Do index on the first bitarchive found.
            // String baName = wc.getBitarchiveNames()[0];
            BatchStatus cdxstatus = arcrep.batch(cdxjob, Settings.get(CommonSettings.USE_REPLICA_ID));
            cdxstatus.getResultFile().copyTo(indexfile);
            cdxstatus.getResultFile().cleanup();
            final List<File> filesFailed = new ArrayList<File>(cdxstatus.getFilesFailed());
            if (filesFailed != null && filesFailed.size() != 0) {
                System.out.println("Some files failed to be indexed: " + filesFailed);
            } else {
                System.out.println("Indexed " + cdxstatus.getNoOfFilesProcessed() + " files");
            }
        } catch (NetarkivetException e) {
            System.out.println("Error while making index: " + e);
            e.printStackTrace();
        } finally {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }
    }

    private static void dieWithUsage() {
        System.out.println("Usage: java " + MakeIndex.class.getName() + " [indexfile]");
        System.exit(1);
    }
}
