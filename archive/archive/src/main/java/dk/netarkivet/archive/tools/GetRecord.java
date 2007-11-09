/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ARCLookup;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;

/**
 * A command-line tool to get ARC records from the bitarchive. Requires an
 * Lucene index file
 *
 * Usage: java dk.netarkivet.archive.tools.GetRecord indexfile uri >
 * myrecord.arcrec
 */

public class GetRecord extends ToolRunnerBase {

    /**
     * Main method. Reads a record from the bitarchive and copies it to stdout.
     * Setup, teardown and run is delegated to the GetRecordTool class.
     * Management of this, exception handling etc. is delegated to
     * ToolRunnerBase class.
     *
     * @param argv Takes two command line paramers: - indexdir (the Lucene index
     *             directory) - uri (the URI to get the record from)
     */
    public static void main(String[] argv) {
        GetRecord instance = new GetRecord();
        instance.runTheTool(argv);
    }

    protected SimpleCmdlineTool makeMyTool() {
        return new GetRecordTool();
    }

    private static class GetRecordTool implements SimpleCmdlineTool {
        /**
         * This instance is declared outside of run method to ensure reliable teardown
         * in case of exceptions during execution.
         */
        private ViewerArcRepositoryClient arcrep;

        /**
         * Accept only exactly 2 parameters.
         *
         * @param args the arguments
         * @return true, if length of args list is 2; returns false otherwise
         */
        public boolean checkArgs(String... args) {
            return args.length == 2;
        }

        /**
         * Create the ArcRepositoryClient instance here for reliable execution
         * of close method in teardown.
         *
         * @param args the arguments (not used)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
            ArgumentNotValid.checkNotNull(arcrep, "arcrep");
        }

        /**
         * Ensure reliable execution of the ArcRepositoryClient.close() method.
         * Remember to check if arcrep was actually created. Also reliably clean up
         * JMSConnection.
         */
        public void tearDown() {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }

        /**
         * Perform the actual work. Procure the necessary information to run the
         * ARCArchiveAccess from command line parameters and system settings, and
         * perform the write. Creating and closing the ArcRepositoryClient (arcrep) is
         * done in setup and teardown methods.
         *
         * @param args the arguments
         */
        public void run(String... args) {
            try {
                String indexPath = args[0];
                String uri = args[1];
                ARCLookup lookup = new ARCLookup(arcrep,
                                                 new File(new File(
                                                         indexPath).getParentFile(),
                                                          "luceneIndexUnzipped"));
                lookup.setIndex(new File(indexPath));
                InputStream is = lookup.lookup(new URI(uri));
                if (is == null) {
                    throw new IOFailure(
                            "Resource missing in index or repository for '"
                            + uri + "' in '" + indexPath + "'");
                }
                processRecord(is);
            } catch (NetarkivetException e) {
                throw new IOFailure(
                        "NetarkivetException while performing ARCArchiveAccess.lookup",
                        e);
            } catch (URISyntaxException e) {
                throw new IOFailure("URI has illegal syntax", e);
            }
        }

        /**
         * Copy a record from content to System.out.
         *
         * @param content The InputStream containing the record to copy to
         *                System.out.
         */
        private static void processRecord(InputStream content) {
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(content));
                try {
                    int i;
                    while ((i = br.read()) != -1) {
                        System.out.append((char) i);
                    }
                } finally {
                    br.close();
                }
            } catch (IOException e) {
                throw new IOFailure(
                        "Internal error: Could not read InputStream from repository",
                        e);
            }
        }

        /**
         * Return the list of parameters accepted by the GetRecordTool class.
         *
         * @return the list parameters accepted.
         */
        public String listParameters() {
            return "indexfile uri";
        }

    }
}
