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

package dk.netarkivet.monitor.tools;

import java.lang.management.ManagementFactory;

import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.monitor.jmx.HostForwarding;
import dk.netarkivet.monitor.logging.SingleLogRecord;

/**
 * This tool will simply reregister all MBeans that matches the given query 
 * from the JMX hosts read in settings, using its own platformmbeanserver.
 * It will then wait forever.
 *
 * It can then be connected to with any JMX client, e.g. jconsole.
 * Start this tool with -Dcom.sun.management.jmxremote
 *
 */
public class JMXProxy extends ToolRunnerBase {

    /** Run the tool as described in class comment.
     * @param args Should take one arg, the query.
     */
    public static void main(String[] args) {
        new JMXProxy().runTheTool(args);
    }

    /**
     * Factory method. Creates and returns the actual workhorse that
     * does the job.
     *
     * @return An implementation of the SimpleCmdlineTool interface.
     */
    protected SimpleCmdlineTool makeMyTool() {
        return new SimpleCmdlineTool() {
            /**
             * Check (command line) arguments. There should be one, the query
             *
             * @param args The command line arguments passed directly
             *             from a public static void main(String[] args)
             *             method.
             * @return True, if parameters are size 1. False if not.
             */
            public boolean checkArgs(String... args) {
                if (args.length != 1) {
                    System.err.println("This tool takes one argument");
                    return false;
                }
                return true;
            }

            /**
             * Does nothing.
             *
             * @param args Not used.
             */
            public void setUp(String... args) {
            }

            /**
             * Does nothing.
             */
            public void tearDown() {
            }

            /**
             * Run the tool. Simply forward all logging mbeans to the local
             * mbean server. Then waits.
             *
             * @param args Not used.
             */
            public void run(String... args) {
                String query = args[0];
                HostForwarding.getInstance(
                        SingleLogRecord.class,
                        ManagementFactory.getPlatformMBeanServer(),
                        query);
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        //end the tool nicely on wakeup.
                    }
                }
            }

            /**
             * Describes the parameters that this tool accepts.
             * One argument is needed: The query,
             * e.g dk.netarkivet.common.logging:*
             *
             * @return The parameter description in a String object.
             */
            public String listParameters() {
                return "query";
            }
        };
    }
}
