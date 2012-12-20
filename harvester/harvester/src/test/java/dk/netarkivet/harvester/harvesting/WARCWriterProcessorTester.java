/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

import org.dom4j.Document;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.testutils.TestFileUtils;
import junit.framework.TestCase;

public class WARCWriterProcessorTester extends TestCase {

    private static final String DISK_PATH_XPATH =
            "//crawl-order/controller"
            + "/string[@name='disk-path']";
    
    @Override
    public void setUp(){
        TestFileUtils.copyDirectoryNonCVS(TestInfo.WARCPROCESSORFILES_DIR,
                TestInfo.WORKING_DIR);
    }
    
    @Override
    public void tearDown(){
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }
    
    
    public void testWriteWarcInfo() {
        // Change disk-path of order.xml to WORKING_DIR
        Document doc = XmlUtils.getXmlDoc(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        
        XmlUtils.setNode(doc, DISK_PATH_XPATH,
                TestInfo.ORDER_FOR_TESTING_WARCINFO.getParentFile().getAbsolutePath()
                );
        XmlUtils.writeXmlToFile(doc, TestInfo.ORDER_FOR_TESTING_WARCINFO);

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        //String output = p.getFirstrecordBody(TestInfo.ORDER_FOR_TESTING_WARCINFO);
        //System.out.println(output);
    }
}
