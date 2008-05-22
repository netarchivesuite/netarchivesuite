/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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

package dk.netarkivet.common.tools;

/**
 * A very abstracted interface for simple command line tools. Allows for setup,
 * teardown, argument checking, usage listing and (of course) running.
 *
 */
public interface SimpleCmdlineTool {

    /**
     * Check (command line) arguments.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     * @return True, if parameters are usable. False if not.
     */
    public abstract boolean checkArgs(String... args);

    /**
     * Create any resource which may requires an explicit teardown. Implement
     * teardown in the teardown method.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     */
    public abstract void setUp(String... args);

    /**
     * Teardown any resource which requires an explicit teardown. Implement
     * creation of these in the setup method. Note that not all objects may be
     * created in case of an exception during setup, so check for null!!!
     */
    public abstract void tearDown();

    /**
     * Run the tool. Any resources that can be managed without reliable teardown
     * may be created here.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     */
    public abstract void run(String... args);

    /**
     * Describes the parameters that this tool accepts.
     *
     * @return The parameter description in a String object.
     */
    public abstract String listParameters();
}
