/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;

import org.dom4j.Document;
import org.junit.Test;

import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.test.utils.HarvestInfoXmlBuilder;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;

public class WARCWriterProcessorTester {
    private static final String DISK_PATH_XPATH = "//crawl-order/controller" + "/string[@name='disk-path']";

    @Test
    public void testWriteWarcInfoWithScheduleName() {
        Document orderXML = OrderXmlBuilder.createDefault().getDoc();

        File orderWithOut = new File("target/order_for_testing_warcinfo.xml");
        XmlUtils.setNode(orderXML, DISK_PATH_XPATH, orderWithOut.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(orderXML, orderWithOut);
        XmlUtils.writeXmlToFile(HarvestInfoXmlBuilder.createDefault().getDoc(), new File("target/harvestinfo.xml"));

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWithOut);
    }

    @Test
    public void testWriteWarcInfoWithoutScheduleName() {
        Document orderXML = OrderXmlBuilder.createDefault().getDoc();

        File orderWithOut = new File("target/order_for_testing_warcinfo.xml");
        XmlUtils.setNode(orderXML, DISK_PATH_XPATH, orderWithOut.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(orderXML, orderWithOut);
        XmlUtils.writeXmlToFile(HarvestInfoXmlBuilder.createDefault("harvestinfo-snapshot.xml").getDoc(), new File("target/harvestinfo.xml"));

        WARCWriterProcessor p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWithOut);
    }
}
