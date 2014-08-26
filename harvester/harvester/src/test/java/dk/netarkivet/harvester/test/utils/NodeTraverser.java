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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author tra
 */
public class NodeTraverser {
    private final Document doc;
    private Node currentNode;

    /**
     *
     * @param doc
     */
    public NodeTraverser(Document doc) {
        this.doc = doc;
        currentNode = doc;
    }

    /**
     *
     * @return
     */
    public Node getNode() {
        return currentNode;
    }

    /**
     *
     * @param doc
     * @return
     */
    public static NodeTraverser create(Document doc) {
        return new NodeTraverser(doc);
    }

    /**
     *
     * @param element
     * @param name
     * @return
     */
    public NodeTraverser getChildNode(String element, String name) {
        Node childNode = null;
        NodeList nodes = currentNode.getChildNodes();
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if ((name == null || name.equals(node.getNodeName()))
                        && element.equals(node.getAttributes().getNamedItem("name"))) {
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
