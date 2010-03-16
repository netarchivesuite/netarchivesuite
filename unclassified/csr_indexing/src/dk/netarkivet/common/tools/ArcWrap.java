/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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
package dk.netarkivet.common.tools;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;

import org.archive.io.arc.ARCWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Command line tool for creating an ARC file from given data. Uses
 * ToolRunnerBase and SimpleCmdlineTool to coordinate task. Usage: java
 * dk.netarkivet.common.tools.ArcWrap input_file uri mime-type > myarchive.arc
 * Note: Does not depend on logging - communicates failure on stderr
 */
public class ArcWrap extends ToolRunnerBase {
    /**
     * Main method. Reads given content and outputs an ARC file on stdout. The
     * output ARC file has two records: the ARC file header and one record
     * containing the given content. The uri and mimetype of the latter record
     * are specified as command line parameters. Setup, teardown and run is
     * delegated to the ArcWrapTool class. Management of this, exception
     * handling etc. is delegated to ToolRunnerBase class.
     *
     * @param args
     *            Takes three command line parameters: - input file (the content
     *            to archive) - uri (the name to record the content by) -
     *            mime-type (the type to record for the content)
     */
    public static void main(String[] args) {
        ArcWrap instance = new ArcWrap();
        instance.runTheTool(args);
    }

    protected SimpleCmdlineTool makeMyTool() {
        return new ArcWrapTool();
    }

    private static class ArcWrapTool implements SimpleCmdlineTool {

        /**
         * This instance is declared outside of run method to ensure reliable
         * teardown in case of exceptions during execution.
         */
        private ARCWriter aw;

        /**
         * Accept only exactly 3 parameters.
         *
         * @param args
         *            the arguments
         * @return true, if length of argument list is 3; returns false
         *         otherwise
         */
        public boolean checkArgs(String... args) {
            return args.length == 3;
        }

        /**
         * Create the ARCWriter instance here for reliable execution of close
         * method in teardown.
         *
         * @param args
         *            the arguments (presently not used)
         */
        public void setUp(String... args) {
            try {
                aw = ARCUtils.getToolsARCWriter(System.out,
                        new File("dummy.arc"));
            } catch (IOException e) {
                throw new IOFailure(e.getMessage());
            }
        }

        /**
         * Ensure reliable execution of the ARCWriter.close() method. Remember
         * to check if aw was actually created.
         */
        public void tearDown() {
            try {
                if (aw != null) {
                    aw.close();
                }
            } catch (IOException e) {
                throw new IOFailure(e.getMessage());
            }
        }

        /**
         * Perform the actual work. Procure the necessary information to run the
         * ARCWriter from command line parameters and system settings, and
         * perform the write. Creating and closing the ARCWriter is done in
         * setup and teardown methods.
         *
         * @param args
         *            the arguments
         */
        public void run(String... args) {
            try {
                // Prepare metadata...
                File content = FileUtils.makeValidFileFromExisting(args[0]);
                // ...and write the given file's content into the writer passing
                // as parameters URI, MIME type, IP, Timestamp, Length and IS
                String uri = args[1];
                String mimetype = args[2];
                aw.write(uri, mimetype, SystemUtils.getLocalIP(), 
                        content.lastModified(),
                        content.length(), new FileInputStream(content));
            } catch (IOException e) {
                throw new IOFailure(e.getMessage());
            }
        }

        /**
         * Return the list of parameters accepted by the ArcWrapTool class.
         * @return the list of parameters accepted
         */
        public String listParameters() {
            return "file uri mimetype";
        }

    }
}
