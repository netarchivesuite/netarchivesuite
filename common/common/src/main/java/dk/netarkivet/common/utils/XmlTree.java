/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;

/** A class that implements the StringTree<T> interface by backing it with
 * XML. The name of each XML node corresponds to the identifier of a node
 * in the tree.
 */
public class XmlTree<T> implements StringTree<T> {
    /** This matches string values that are valid for identifying a field. */
    private static final Pattern LEGAL_FIELD_NAME
            = Pattern.compile("[a-zA-Z0-9.-]*");

    /**
     * This interface defines how the value of an xml leaf is parsed to get a
     * value of type T.
     */
    interface ValueParser<T> {
        T parse(String s);
    };

    /**
     * A value parser that simply converts an XML node to a string, by trimming
     * the text contents.
     */
    private static final ValueParser<String> TRIMMING_STRING_PARSER
            = new ValueParser<String>() {
        public String parse(String s) {
            return s.trim();
        }
    };

    /** The element we are encapsulating. */
    private final Element element;

    /** If this tree is based on a Document, this is non-null. */
    private final Document root;

    /** The parser for contents of leaf nodes. */
    private final ValueParser<T> parser;

    /**
     * Initialise a node in an XML tree.
     *
     * @param n The XML node for this node
     * @param parser The parser that can convert a leaf node to a value of type
     * T.
     * @throws ArgumentNotValid on null argument, or if n is not of type
     * element or document.
     */
    private XmlTree(Node n, ValueParser<T> parser) {
        ArgumentNotValid.checkNotNull(n, "Node n");
        ArgumentNotValid.checkNotNull(parser, "ValueParser<T> parser");
        if (n.getNodeType() == Node.DOCUMENT_NODE) {
            root = (Document) n;
            element = null;
        } else if (n.getNodeType() == Node.ELEMENT_NODE) {
            element = (Element) n;
            root = null;
        } else {
            throw new ArgumentNotValid("Invalid XML node type '"
                                       + n.getNodeTypeName() + "'");
        }
        this.parser = parser;
    }

    /** Returns a StringTree&lt;String&gt; view of the given XML node.
     *
     * @param n A part of an XML document structure
     * @return A StringTree&lt;String&gt; backed by the given XML part.
     *
     * @throws ArgumentNotValid on null argument.
     */
    public static StringTree<String> getStringTree(Node n) {
        ArgumentNotValid.checkNotNull(n, "Node n");
        return new XmlTree<String>(n, TRIMMING_STRING_PARSER);
    }

    /**
     * Returns true if this object is a leaf, and thus if getValue is legal.
     *
     * @return True if the implementing object is a leaf, false otherwise.
     */
    public boolean isLeaf() {
        return element != null && elementIsLeaf(element);
    }

    /**
     * Get the value of a named sub-leaf.
     *
     * @param name Name of the sub-leaf to get the value of. These are strings,
     * and as a shorthand may specify subtrees of subtrees by separating each
     * level with '.', i.e. getSubtrees("subtree.subsubtree").
     * @return The value of the named leaf of this Tree, if it exists.
     * @throws IllegalState if this StringTree does not have exactly one
     * leaf sub-node with the given name.
     * @throws ArgumentNotValid if argument is null or empty.
     */
    public T getValue(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        Element n = selectSingleNode(name);
        if (!elementIsLeaf(n)) {
            throw new IllegalState("Subtree '" + name + "' is not a leaf");
        }
        return parseLeafNode(n);
    }

    /**
     * Get the value of a leaf.
     *
     * @return The value of this Tree, if it is a leaf.
     * @throws IllegalState if this Tree is a node.
     */
    public T getValue() {
        if (!isLeaf()) {
            throw new IllegalState("Node is not text, but "
                                       + (element != null?
                                          element.getNodeTypeName():
                                          root.getNodeTypeName()));
        }
        return parseLeafNode(element);
    }

    /**
     * Get the only subtree with the given name.
     *
     * @param name The name of the subtree. These are strings, and as a
     * shorthand may specify subtrees of subtrees by separating each level with
     * '.', i.e. getSubtrees("subtree.subsubtree").
     * @return The single subtree with the given name.
     * @throws IllegalState if this object is a leaf, or there is not
     * exactly one subtree with the given name.
     * @throws ArgumentNotValid if argument is null or empty.
     */
    public StringTree<T> getSubTree(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        final Element n = selectSingleNode(name);
        return new XmlTree<T>(n, parser);
    }

    /**
     * Get the named subtrees.
     *
     * @param name The name of the subtrees. These are strings, and as a
     * shorthand may specify subtrees of subtrees by separating each level with
     * '.', i.e. getSubtrees("subtree.subsubtree").
     * @return All subtrees with the given name, or an empty list for none.
     * @throws IllegalState if this object is a leaf.
     * @throws ArgumentNotValid if argument is null or empty.
     */
    public List<StringTree<T>> getSubTrees(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        List<Element> nodeList = selectMultipleNodes(name);
        List<StringTree<T>> resultList
                = new ArrayList<StringTree<T>>(nodeList.size());
        for (Element n : nodeList) {
            resultList.add(new XmlTree<T>(n, parser));
        }
        return resultList;
    }

    /**
     * Get a map of all the children of this node.
     *
     * @return Map of children of this node.
     * @throws IllegalState if this object is a leaf.
     */
    public Map<String, List<StringTree<T>>> getChildMultimap() {
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        Map<String, List<StringTree<T>>> children =
                new HashMap<String, List<StringTree<T>>>();
        List<Element> nodeList = getChildNodes();
        if (nodeList != null) {
            for (Element n : nodeList) {
                List<StringTree<T>> childList = children.get(n.getName());
                if (childList == null) {
                    childList = new ArrayList<StringTree<T>>();
                    children.put(n.getName(), childList);
                }
                childList.add(new XmlTree<T>(n, parser));
            }
        }
        return children;
    }

    /**
     * Get a map of all direct subtrees, assuming that all subtrees are uniquely
     * named.
     *
     * @return Map of all subtrees.
     * @throws IllegalState if this object is a leaf, or if the subtrees are not
     * uniquely named.
     */
    public Map<String, StringTree<T>> getChildMap() {
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        Map<String, List<StringTree<T>>> map = getChildMultimap();
        return convertMultimapToMap(map);
    }

    /**
     * Get a multimap of the names and values of all subtrees, assuming that all
     * subtrees are leafs.
     *
     * @return Multimap from subtree names to values of their leaves.
     *
     * @throws ArgumentNotValid if this object is not a node, or if any of its
     *                          children are not leaves.
     */
    public Map<String, List<T>> getLeafMultimap() {
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        Map<String, List<T>> children =
                new HashMap<String, List<T>>();
        List<Element> nodeList = getChildNodes();
        if (nodeList != null) {
            for (Element n : nodeList) {
                if (!elementIsLeaf(n)) {
                    throw new IllegalState("Child " + n.getName()
                                           + " is not a leaf");
                }
                List<T> childList = children.get(n.getName());
                if (childList == null) {
                    childList = new ArrayList<T>();
                    children.put(n.getName(), childList);
                }
                childList.add(parseLeafNode(n));
            }
        }
        return children;
    }

    /**
     * Get a map of the names and values of all subtrees, assuming that all
     * subtrees are leafs and are uniquely named
     *
     * @return Map from subtree names to values of their leaves.
     *
     * @throws IllegalState if this object is a leaf or if the subtrees are not
     * uniquely named, or if any of its children are not leaves.
     */
    public Map<String, T> getLeafMap() {
        if (isLeaf()) {
            throw new IllegalState("Cannot find subtrees in a leaf");
        }
        Map<String, List<T>> map = getLeafMultimap();
        return convertMultimapToMap(map);
    }

    /** Select a list of nodes from the current tree, resolving dotted paths.
     * Currently, if any part of the dotted path has multiple subtrees with
     * the given name, this method will find all that match.
     *
     * @param name Name of a node (tree or leaf), possibly via a dotted path
     * @return A list of nodes found via the given name from the root of this
     * tree, mapping names to nodes.
     * @throws ArgumentNotValid on null or empty argument, or if the path is
     * illegal
     */
    private List<Element> selectMultipleNodes(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        ArgumentNotValid.checkTrue(LEGAL_FIELD_NAME.matcher(name).matches(),
                                   "Name must contain only alphanumeric, dash"
                                   + " and period");
        // parts contains the dotted path, split into individual components
        String[] parts = name.split("\\.");
        // elements contains the root elements to find children from.
        List<Element> elements = new ArrayList<Element>();
        int i = 0;
        if (root != null) {
            if (parts[i].equals(root.getRootElement().getName())) {
                elements.add(root.getRootElement());
                i++;
            } // We allow zero-element results here, so a no-match is okay.
        } else {
            elements.add(element);
        }
        for (; i < parts.length; i++) {
            // newElements contains the children matching the name from the
            // dotted path
            List<Element> newElements = new ArrayList<Element>();
            final String subname = parts[i];
            for (Element n : elements) {
                List<Element> childList = n.elements(subname);
                if (childList != null) {
                    newElements.addAll(childList);
                }
            }
            // loop to find children of the found nodes with the next name in
            // the dotted paths
            elements = newElements;
        }
        return elements;
    }

    /** Select a single node from the current tree, resolving dotted paths.
     *
     * @param name Name of a node (tree or leaf), possibly via a dotted path
     * @return A single node found via the given name from the root of this
     * tree.
     * @throws ArgumentNotValid on null or empty argument, or if the path is
     * illegal
     * @throws IllegalState if there is no such
     * node, or if there is more than one candidate for the node.
     */
    private Element selectSingleNode(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        List<Element> elements = selectMultipleNodes(name);
        if (elements.size() == 0) {
            throw new IllegalState("No subtree with name '" + name + "'");
        }
        if (elements.size() > 1) {
            throw new IllegalState("Multiple subtrees with name '"
                                   + name + "'");
        }
        return elements.get(0);
    }

    /** Parse the contents of this leaf node according to the parser.
     *
     * @param e A leaf node to parse.
     * @return The parsed contents of the node.
     */
    private T parseLeafNode(Element e) {
        return parser.parse(e.getText());
    }

    /** Returns true if the given node is a leaf (i.e. has only text).
     *
     * @param e A node to check
     * @return True if the given node is a leaf.
     */
    private boolean elementIsLeaf(Element e) {
        return e.isTextOnly();
    }

    /** Get the nodes that are children of the current node.  This works
     * for both Documents and Elements.
     *
     * @return List of Element objects that are children of the current node.
     */
    private List<Element> getChildNodes() {
        List<Element> nodeList;
        if (root != null) {
            nodeList = new ArrayList<Element>(1);
            nodeList.add(root.getRootElement());
        } else {
            nodeList = (List<Element>) element.elements();
        }
        return nodeList;
    }

    /** Convert a multimap into a map, checking that there is only one value for
     * each key.
     *
     * @param map The multimap to convert from
     * @return The map to convert to.
     *
     * @throws IllegalState if the map does not contain exactly one value for
     * each key.
     */
    private static <K, V> Map<K, V> convertMultimapToMap(
            Map<K, List<V>> map) {
        Map<K, V> result =
                new HashMap<K, V>();
        for (Map.Entry<K, List<V>> entry
                : map.entrySet()) {
            if (entry.getValue().size() != 1) {
                throw new IllegalState("More than one value for key '"
                                           + entry.getKey() + "' found");
            }
            result.put(entry.getKey(), entry.getValue().get(0));
        }
        return result;
    }
}
