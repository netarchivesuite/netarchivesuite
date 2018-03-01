/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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
package dk.netarkivet.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Provides functionality for generating xml document nodes ad-hoc for test purposes. The NodeTraverser
 * uses a builder pattern to chain childnode calls to create multilevel structures. Example <p>
 *     Node deduplicationNode = NodeTraverser.create(orderxmlDoc).getChildNode("crawl-order", null)
 .getChildNode("controller", null).getChildNode("map", "write-processors")
 .getChildNode("newObject", "DeDuplicator").getChildNode("boolean", "enabled").getNode();
 * </p>
 */
public class NodeTraverser {
    private final Document doc;
    private Node currentNode;

    public NodeTraverser(Document doc) {
        this.doc = doc;
        currentNode = doc;
    }

    public Node getNode() {
        return currentNode;
    }

    public static NodeTraverser create(Document doc) {
        return new NodeTraverser(doc);
    }

    /** Will create a NodeTraverser for accessing the indicated node. If the current node does
     * exist it will be created.
     * @param element The tag for the element to make available.
     * @param name A optional name attribute to add to the element if created.
     * @return The NodeTraverser reference, which can be used to create further child nodes.
     */
    public NodeTraverser getChildNode(String element, String name) {
        Node childNode = null;
        NodeList nodes = currentNode.getChildNodes();
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String nameAttr = null;
                if (attributes != null) {
                	Node attrNode = attributes.getNamedItem("name");
                	if (attrNode != null && attrNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                		nameAttr = attrNode.getNodeValue();
                	}
                }
                if (element.equals(node.getNodeName()) && (name == null || name.equals(nameAttr))) {
                    childNode = node;
                    break;
                }
            }
        }
        if (childNode == null) {
            Element newNode = doc.createElement(element);
            if (name != null) {
                newNode.setAttribute("name", name);
            }
            currentNode.appendChild(newNode);
            childNode = newNode;
        }
        currentNode = childNode;

        return this;
    }
}
