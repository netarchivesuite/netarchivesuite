/*$Id: TestInfo.java 1338 2010-03-17 15:27:53Z svc $
* $Revision: 1338 $
* $Date: 2010-03-17 16:27:53 +0100 (Wed, 17 Mar 2010) $
* $Author: svc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.webinterface;

import java.io.File;

public class TestInfo {

    public static final File DATA_DIR = new File("tests/dk/netarkivet/common/webinterface/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TEMPDIR = new File(DATA_DIR, "working/");
    
    public static final File BATCH_DIR = new File(WORKING_DIR, "batch");
    
    public static final String CONTEXT_CLASS_NAME = "batchjob";
    
    public static final int GUI_WEB_SERVER_PORT = 4242;
    public static final String GUI_WEB_SERVER_WEBBASE = "/jsp";
    public static final String GUI_WEB_SERVER_JSP_DIRECTORY 
        = "tests/dk/netarkivet/common/webinterface/data/jsp";
    public static final String GUI_WEB_SERVER_SITESECTION_CLASS
        = TestSiteSection.class.getName();
}
