/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.junit.Assert;
import org.junit.Test;

import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.H1HeritrixTemplate;
import dk.netarkivet.harvester.test.utils.HarvestInfoXmlBuilder;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;

// TODO Possibly check for "Error missing metadata" in log output when no metadata is present in order xml.
public class WARCWriterProcessorTester {

    private static final String DISK_PATH_XPATH = "//crawl-order/controller" + "/string[@name='disk-path']";

    private String metadataItemsStr = ""
    		/*
    		+ "<stringList name=\"metadata-items\">\n"
    		+ "  <string>Vilhelm</string>\n"
    		//+ "  <string>harvestInfo.version=Vilhelm</string>\n"
            + "</stringList>\n";
            */
            + "<map name=\"metadata-items\">\n"
            + "  <string name=\"harvestInfo.version\">Vilhelm</string>\n"
            + "  <string name=\"harvestInfo.jobId\">Caroline</string>\n"
            + "  <string name=\"harvestInfo.channel\">Login</string>\n"
            + "  <string name=\"harvestInfo.harvestNum\">ffff</string>\n"
            + "  <string name=\"harvestInfo.origHarvestDefinitionID\">ffff</string>\n"
            + "  <string name=\"harvestInfo.maxBytesPerDomain\">ffff</string>\n"
            + "  <string name=\"harvestInfo.maxObjectsPerDomain\">ffff</string>\n"
            + "  <string name=\"harvestInfo.orderXMLName\">Default Orderxml</string>\n"
            + "  <string name=\"harvestInfo.origHarvestDefinitionName\">ddddd</string>\n"
            + "  <string name=\"harvestInfo.scheduleName\">Every Hour</string>\n"
            + "  <string name=\"harvestInfo.harvestFilenamePrefix\">1-1</string>\n"
            + "  <string name=\"harvestInfo.jobSubmitDate\">NOW</string>\n"
            + "  <string name=\"harvestInfo.performer\">performer</string>\n"
            + "  <string name=\"harvestInfo.audience\">audience</string>\n"
            + "</map>\n";

    @Test
    public void testWriteWarcInfoWithScheduleName() {
    	Document orderXML;
        WARCWriterProcessor p;
    	/*
    	 * WithOut.
    	 */
    	orderXML = OrderXmlBuilder.createDefault().getDoc();
        Assert.assertNotNull(orderXML);
        try {
            Node controllerNode = orderXML.selectSingleNode(H1HeritrixTemplate.WARCWRITERPROCESSOR_XPATH);
			Node metadataItemsXml = XmlUtils.documentFromString(metadataItemsStr);
			Element controllerElement = (Element) controllerNode;
			controllerElement.add(metadataItemsXml.getDocument().getRootElement().detach());
        } catch (DocumentException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
        File orderWithOut = new File("target/order_for_testing_warcinfo.xml");
        XmlUtils.setNode(orderXML, DISK_PATH_XPATH, orderWithOut.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(orderXML, orderWithOut);

        p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWithOut);
        /*
         * Default.
         */
        orderXML = HarvestInfoXmlBuilder.createDefault().getDoc();
        File orderWith = new File("target/harvestInfo.xml");
        XmlUtils.writeXmlToFile( orderXML, orderWith);

        p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWith);
    }

    @Test
    public void testWriteWarcInfoWithoutScheduleName() {
    	Document orderXML;
        WARCWriterProcessor p;
    	/*
    	 * WithOut.
    	 */
        orderXML = OrderXmlBuilder.createDefault().getDoc();
        Assert.assertNotNull(orderXML);
        try {
            Node controllerNode = orderXML.selectSingleNode(H1HeritrixTemplate.WARCWRITERPROCESSOR_XPATH);
			Node metadataItemsXml = XmlUtils.documentFromString(metadataItemsStr);
			Element controllerElement = (Element) controllerNode;
			controllerElement.add(metadataItemsXml.getDocument().getRootElement().detach());
        } catch (DocumentException e) {
			e.printStackTrace();
			Assert.fail("Unexpected exception!");
		}
        File orderWithOut = new File("target/order_for_testing_warcinfo.xml");
        XmlUtils.setNode(orderXML, DISK_PATH_XPATH, orderWithOut.getParentFile().getAbsolutePath());
        XmlUtils.writeXmlToFile(orderXML, orderWithOut);

        p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWithOut);
        /*
         * Default.
         */
        orderXML = HarvestInfoXmlBuilder.createDefault("harvestInfo-snapshot.xml").getDoc();
        File orderWith = new File("target/harvestInfo.xml");
        XmlUtils.writeXmlToFile(orderXML, orderWith);

        p = new WARCWriterProcessor("testing");
        p.getFirstrecordBody(orderWith);
    }

}
