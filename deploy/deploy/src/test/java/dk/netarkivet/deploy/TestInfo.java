/*$Id$
* $Revision$
* $Date$
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
package dk.netarkivet.deploy;

import java.io.File;

public class TestInfo {
    public static final File DATA_DIR = new File("tests/dk/netarkivet/deploy/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TMPDIR = new File(WORKING_DIR, "tmpdir");
    public static final File TARGETDIR = new File(WORKING_DIR,"target");

    public static final File IT_CONF_FILE  = new File(WORKING_DIR,"it_conf_dev.xml");
    public static final File SETTINGS_FILE = new File(WORKING_DIR,"settings.xml");
    public static final String TEST_SYSTEM_ADMIN_HOSTNAME = "kb-dev-adm-001.kb.dk";
    public static final int TEST_JMX_HOSTS = 9;
    public static final String TEST_JMX_PASSWORD = "test";
    public static final File IT_CONF_SINGLE_FILE  = new File(WORKING_DIR,"it_conf_single.xml");
    public static final File SINGLE_TARGET_DIR = new File(WORKING_DIR,"single_target");
}
