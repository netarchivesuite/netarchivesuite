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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class OrderXmlBuilder {
    private final Document orderxmlDoc;
    private static DocumentBuilder builder;

    public OrderXmlBuilder() {
        orderxmlDoc = getParser().newDocument();
    }

    public org.dom4j.Document getOrderXml() {
        org.dom4j.io.DOMReader reader = new org.dom4j.io.DOMReader();
        return reader.read(orderxmlDoc);
    }

    public static OrderXmlBuilder create() {
        return new OrderXmlBuilder();
    }

    public OrderXmlBuilder enableDeduplication() {
        Node deduplicationNode = NodeTraverser.create(orderxmlDoc)
                .getChildNode("crawl-order", null)
                .getChildNode("controller", null)
                .getChildNode("map", "write-processors")
                .getChildNode("newObject", "DeDuplicator")
                .getChildNode("boolean", "enabled")
                .getNode();
        deduplicationNode.setTextContent("true");
        return this;
    }

    private static synchronized DocumentBuilder getParser() {
        if(builder == null) {
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        } return builder;
    }
}
