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

import org.w3c.dom.Node;

import dk.netarkivet.common.xml.NodeTraverser;
import dk.netarkivet.common.xml.XmlBuilder;

public class OrderXmlBuilder extends XmlBuilder {
    public static final String DEFAULT_ORDE_XML_NAME = "FullSite-order";

    private OrderXmlBuilder() {}
    private OrderXmlBuilder(String name) { super(parseFile(name)); }

    public static OrderXmlBuilder create() {
        return new OrderXmlBuilder();
    }
    public static OrderXmlBuilder createDefault() { return createDefault("order.xml"); }
    public static OrderXmlBuilder createDefault(String name) { return new OrderXmlBuilder(name);}

    public OrderXmlBuilder enableDeduplication() {
        Node deduplicationNode = NodeTraverser.create(xmlDoc).getChildNode("crawl-order", null)
                .getChildNode("controller", null)
                .getChildNode("map", "write-processors")
                .getChildNode("newObject", "DeDuplicator")
                .getChildNode("boolean", "enabled").getNode();
        deduplicationNode.setTextContent("true");
        return this;
    }
}
