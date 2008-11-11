/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
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
package dk.netarkivet.common.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.dom.DOMDocument;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Utility class to load and save data from/to XML files using a
 * very simple XML format.
 */
public class SimpleXml {
    protected final Log log = LogFactory.getLog(getClass().getName());

    /** The underlying XML Document object that we give access to. */
    private Document xmlDoc;

    /** The file that this XML was read from, or a fixed string if it
     * was created from scratch.
     */
    private String source;

    /** Create a new SimpleXml object by loading a file.
     *
     * @param f XML file to load
     */
    public SimpleXml(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        load(f);
    }

    /** Create a new SimpleXml just containing the root element.
     *
     * @param rootElement Name of the root element
     */
    public SimpleXml(String rootElement) {
        ArgumentNotValid.checkNotNullOrEmpty(rootElement, "String rootElement");
        xmlDoc = new DOMDocument();
        xmlDoc.addElement(rootElement);
        source = "Newly creating XML file with root '" + rootElement + "'";
    }

    /** Create a new SimpleXml object by loading a file.
     *
     * @param resourceAsStream XML file to load
     */
    public SimpleXml(InputStream resourceAsStream) {
        ArgumentNotValid.checkNotNull(resourceAsStream,
                "InputStream resourceAsStream");
        load(resourceAsStream);
    }

    /**
     * Loads an xml stream.
     *
     * @param resourceAsStream a XML stream to load.
     */
    private void load(InputStream resourceAsStream) {
        xmlDoc = XmlUtils.getXmlDoc(resourceAsStream);
        source = "XML file from input stream '" + resourceAsStream + "'";
    }

    /**
     * Loads an xml file.
     *
     * @param f a XML file
     */
    private void load(File f) {
        source = f.toString();

        if (!f.exists()) {
            log.warn("XML file '" + f.getAbsolutePath()
                      + "' does not exist");
            throw new IOFailure("XML file '" + f.getAbsolutePath()
                    + "' does not exist");
        }

        xmlDoc = XmlUtils.getXmlDoc(f);
    }

    /**
     * Add entries to the current set of settings.  If a node with this
     * key already exists in the XML, the new nodes are added after that,
     * otherwise the new nodes are added at the end.
     *
     * @param key
     *            the key to add
     * @param values
     *            the values to add
     * @throws ArgumentNotValid
     *             if the key is null or empty, or the value is null
     */
    public void add(String key, String... values) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        ArgumentNotValid.checkNotNull(values, "values");

        //find values to set
        List<String> allValues = new ArrayList<String>(getList(key));
        allValues.addAll(Arrays.asList(values));

        //ensure the key exists
        Element newNode = addParents(key.split("\\."));

        //set values
        update(key, allValues.toArray(new String[]{}));
    }

    /** Add all the necessary parents to have the given elements available,
     * and add a new Element node at the lowest level.
     *
     * @param elementNames A list of tags, must start with the document root.
     * @return The last element added.
     */
    private Element addParents(String... elementNames) {
        ArgumentNotValid.checkTrue(elementNames.length >= 2,
                "Must have at least root element and final element in "
                        + "element names, not just "
                        + Arrays.asList(elementNames));
        Element currentNode = xmlDoc.getRootElement();
        if (!currentNode.getName().equals(elementNames[0])) {
            throw new ArgumentNotValid("Document has root element '"
                    + currentNode.getName() + "', not '"
                    + elementNames[0] + "'");
        }
        for (int i = 1; i < elementNames.length - 1; i++) {
            String elementName = elementNames[i];
            List<Element> nodes
                    = currentNode.elements(elementName);
            if (nodes == null || nodes.size() == 0) {
                // Element not found, add at end
                currentNode = currentNode.addElement(elementName);
            } else {
                currentNode = nodes.get(nodes.size() - 1);
            }
        }
        return addAfterSameElement(currentNode,
                elementNames[elementNames.length - 1]);
    }

    /** Add another element either right after the last of its kind in
     * currentNode or at the end of currentNode.
     *
     * @param currentNode A node that the new element will be a sub-node of
     * @param elementName The name of the new element
     * @return The new element, which is now placed under currentNode
     */
    private Element addAfterSameElement(Element currentNode,
            String elementName) {
        Element newElement = currentNode.addElement(elementName);
        newElement.detach();
        // If there are already nodes of this type, add straight after them.
        List<Element> existingNodes = currentNode.elements();
        for (int i = existingNodes.size() - 1; i >= 0; i--) {
            if (existingNodes.get(i).getName().equals(elementName)) {
                existingNodes.add(i + 1, newElement);
                return newElement;
            }
        }
        // Otherwise add at the end.
        existingNodes.add(newElement);
        return newElement;
    }

    /**
     * Removes current settings for a key and adds new values
     * for the same key. Calling update() is equivalent to calling
     * delete() and add(), except the old value does not get destroyed on
     * errors and order of the elements are kept.
     * If no values are given, the key is removed.
     *
     * @param key The key for which the value should be updated.
     * @param values The new values that should be set for the key.
     * @throws UnknownID
     *             if the key does not exist
     * @throws ArgumentNotValid
     *             if the key is null or empty, or any of the values are null
     */
    public void update(String key, String... values) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "String key");
        ArgumentNotValid.checkNotNull(values, "String... values");
        for (int i = 0; i < values.length; i++) {
            ArgumentNotValid.checkNotNull(
                    values[i], "String values[" + i + "]");
        }
        if (!hasKey(key)) {
            throw new UnknownID("No key registered with the name: '" + key
                    + "' in '" + source + "'");
        }

        List<Node> nodes = getXPath(key).selectNodes(xmlDoc);
        int i = 0;
        for (; i < nodes.size() && i < values.length; i++) {
            nodes.get(i).setText(values[i]);
        }
        if (i < nodes.size()) {
            // Delete nodes if there were more nodes than values
            for (; i < nodes.size(); i++) {
                nodes.get(i).detach();
            }
        } else {
            // Add nodes if there were fewer nodes than values
            for (; i < values.length; i++) {
                Element newNode = addParents(key.split("\\."));
                newNode.setText(values[i]);
            }
        }
    }

    /**
     * Get the first entry that matches the key. Keys are constructed as a dot
     * separated path of xml tag names. Example: The following XML definition of
     * a user name &lt;dk&gt;&lt;netarkivet&gt;&lt;user&gt;ssc&lt;/user&gt;
     * &lt;/netarkivet&gt;&lt;/dk&gt; is
     * accessed using the path: "dk.netarkivet.user"
     *
     * @param key
     *            the key of the entry.
     * @return the first entry that matches the key.
     * @throws UnknownID
     *             if no element matches the key
     * @throws ArgumentNotValid
     *             if the key is null or empty
     */
    public String getString(String key) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");

        XPath xpath = getXPath(key);
        List<Node> nodes = xpath.selectNodes(xmlDoc);
        if (nodes == null || nodes.size() == 0) {
            throw new UnknownID("No elements exists for the path '" + key
                    + "' in '" + source + "'");
        }
        Node first = nodes.get(0);
        return first.getStringValue().trim();
    }

    /**
     * Checks if a setting with the specified key exists.
     *
     * @param key a key for a setting
     * @return true if the key exists
     * @throws ArgumentNotValid
     *             if key is null or empty
     */
    public boolean hasKey(String key) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");

        final List<Node> nodes = (List<Node>) getXPath(key).selectNodes(xmlDoc);
        return nodes != null && nodes.size() > 0;
    }

    /**
     * Get list of all items matching the key. If no items exist matching the
     * key, an empty list is returned.
     *
     * @param key
     *            the path down to elements to get
     * @return a list of items that match the supplied key
     */
    public List<String> getList(String key) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");

        List<Node> nodes = (List<Node>) getXPath(key).selectNodes(xmlDoc);
        if (nodes == null || nodes.size() == 0) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<String>(nodes.size());
        for (Node node : nodes) {
            results.add(node.getText());
        }
        return results;
    }

    /**
     * Save the current settings as an XML file.
     *
     * @param f
     *            the file to write the XML to.
     */
    public void save(File f) {
        ArgumentNotValid.checkNotNull(f, "f");
        XmlUtils.writeXmlToFile(xmlDoc, f);
    }


    /** Return a tree structure reflecting the XML and trimmed values.
     * @param path Dotted path into the xml.
     * @return A tree reflecting the xml at the given path.
     * @throws UnknownID If the path does not exist in the tree or is ambiguous
     */
    public StringTree<String> getTree(String path) {
        ArgumentNotValid.checkNotNullOrEmpty(path, "String path");
        XPath xpath = getXPath(path);
        List<Node> nodes = xpath.selectNodes(xmlDoc);
        if (nodes == null || nodes.size() == 0) {
            throw new UnknownID("No path '" + path + "' in XML document '"
                                + source + "'");
        } else if (nodes.size() > 1) {
            throw new UnknownID("More than one candidate for path '" + path
                                   + "' in XML document '" + source + "'");
        }
        return XmlTree.getStringTree(nodes.get(0));
    }

    /** Get an XPath version of the given dotted path.  A dotted path
     * foo.bar.baz corresponds to the XML node &lt;foo&gt;&lt;bar&gt;&lt;baz&gt;
     *  &lt;/baz&gt;&lt;/bar&gt;&lt;/foo&gt;
     *
     * Implementation note: If needed, this could be optimized by keeping a
     * HashMap cache of the XPaths, since they don't change.
     *
     * @param path A dotted path
     * @return An XPath that matches the dotted path equivalent, using
     * "dk:" as namespace prefix for all but the first element.
     */
    private XPath getXPath(String path) {
        String[] pathParts = path.split("\\.");
        StringBuilder result = new StringBuilder();
        result.append("/");
        result.append(pathParts[0]);
        for (int i = 1; i < pathParts.length; i++) {
            result.append("/dk:");
            result.append(pathParts[i]);
        }
        XPath xpath = xmlDoc.createXPath(result.toString());
        Namespace nameSpace = xmlDoc.getRootElement().getNamespace();
        Map<String, String> namespaceURIs = new HashMap<String, String>(1);
        namespaceURIs.put("dk", nameSpace.getURI());
        xpath.setNamespaceURIs(namespaceURIs);
        return xpath;
    }

}
