/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.test.utils;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class OrderXmlBuilder {
    public static final String DEFAULT_ORDE_XML_NAME = "FullSite-order";
    private final Document orderxmlDoc;
    private static DocumentBuilder builder;

    public OrderXmlBuilder() {
        orderxmlDoc = getParser().newDocument();
    }

    public OrderXmlBuilder(Document orderXmlDoc) {
        this.orderxmlDoc = orderXmlDoc;
    }

    public org.dom4j.Document getOrderXml() {
        org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
        return reader.read(orderxmlDoc);
    }

    public static OrderXmlBuilder create() {
        return new OrderXmlBuilder();
    }

    public OrderXmlBuilder enableDeduplication() {
        Node deduplicationNode = NodeTraverser.create(orderxmlDoc).getChildNode("crawl-order", null)
                .getChildNode("controller", null).getChildNode("map", "write-processors")
                .getChildNode("newObject", "DeDuplicator").getChildNode("boolean", "enabled").getNode();
        deduplicationNode.setTextContent("true");
        return this;
    }

    private static synchronized DocumentBuilder getParser() {
        if (builder == null) {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setNamespaceAware(true);
                builder = documentBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return builder;
    }

    /**
     * Creates a default orderXmlDoc based on the order.xml file on the classpath.
     */
    public static synchronized OrderXmlBuilder createDefault() {
        try {
            return new OrderXmlBuilder(getParser().parse(
                    OrderXmlBuilder.class.getClassLoader().getResourceAsStream("order.xml")));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read order.xml from path " +
                    OrderXmlBuilder.class.getClassLoader().getResource("order.xml"), e);
        }
    }

    @Override
    public String toString() {
        try {
            DOMSource domSource = new DOMSource(orderxmlDoc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = null;
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(domSource, result);
            writer.flush();
            return writer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
