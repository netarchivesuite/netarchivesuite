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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dom4j.Node;
import org.dom4j.Element;
import org.dom4j.Document;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** A class that implements the StringTree<T> interface by backing it with
 * XML. The name of each XML node corresponds to the identifier of a node
 * in the tree.
 */
public class XmlTree<T> implements StringTree<T> {
    private static final Matcher legalFieldName
            = Pattern.compile("[a-zA-Z0-9.-]*").matcher("");

    interface ValueParser<T> {
        T parse(String s);
    };

    /** The element we are encapsulating */
    private final Element element;

    /** If this tree is based on a Document, this is non-null. */
    private final Document root;

    /** The parser for contents of leaf nodes */
    private final ValueParser<T> parser;

    public XmlTree(Node n, ValueParser<T> parser) {
        ArgumentNotValid.checkNotNull(n, "Node n");
        if (n.getNodeType() == Node.DOCUMENT_NODE) {
            root = (Document) n;
            element = null;
        } else if (n.getNodeType() == Node.ELEMENT_NODE) {
            element = (Element) n;
            root = null;
        } else {
            throw new ArgumentNotValid("Invalid XML node '" + n + "'");
        }

        this.parser = parser;
    }

    /** Returns a StringTree<String> view of the given node.
     *
     * @param n A part of an XML document structure
     * @return A StringTree<String> backed by the given XML part.
     */
    public static StringTree<String> getStringTree(Node n) {
        return new XmlTree<String>(n, new ValueParser<String>() {
            public String parse(String s) {
                return s.trim();
            }
        });
    }

    /**
     * Returns true if this object is a leaf of the tree, i.e. is of type T
     * rather than Tree<T>.
     *
     * @return True if the implementing object is a leaf, false otherwise.
     */
    public boolean isLeaf() {
        return element != null && elementIsLeaf(element);
    }

    /**
     * Get the value of a leaf.
     *
     * @return The value of this Tree, if it is a leaf.
     *
     * @throws ArgumentNotValid if this Tree is a node.
     */
    public T getValue() {
        if (!isLeaf()) {
            throw new ArgumentNotValid("Node is not text, but "
                                       + (element != null?
                                          element.getNodeTypeName():
                                          root.getNodeTypeName()));
        }
        return parseLeafNode(element);
    }

    /**
     * Get the value of a named sub-leaf.
     *
     * @param name Name of the sub-leaf to get the value of
     *
     * @return The value of the name leaf of this Tree, if it exists.
     *
     * @throws ArgumentNotValid if this StringTree does not have a leaf sub-node
     * with the given name.
     */
    public T getValue(String name) {
        final Element n = selectSingleNode(name);
        if (!elementIsLeaf(n)) {
            throw new ArgumentNotValid("Subtree '" + name + "' is not a leaf");
        }
        return parseLeafNode(n);
    }

    /** Select a single node from the current tree, resolving dotted paths.
     * Currently, if any part of the dotted path has multiple subtrees with
     * the given name, this method will fail.  It may later try to resolve
     * such as long as there is only one unique endpoint.
     *
     * @param name Name of a node (tree or leaf), possibly via a dotted path
     * @return A single node found via the given name from the root of this tree.
     * @throws ArgumentNotValid if the path is illegal, if there is no such
     * node, or if there is more than one candidate for the node.
     */
    private Element selectSingleNode(String name) {
        ArgumentNotValid.checkTrue(legalFieldName.reset(name).matches(),
                                   "Name must contain only alphanumeric, dash and period");
        String[] parts = name.split("\\.");
        Element e;
        int i = 0;
        if (root != null) {
            if (parts[0].equals(root.getRootElement().getName())) {
                e = root.getRootElement();
                i++;
            } else {
                throw new ArgumentNotValid("No subtree with name '" + name + "'");
            }
        } else {
            e = element;
        }
        for (; i < parts.length; i++) {
            List<Element> childList = e.elements(parts[i]);
            if (childList == null || childList.size() == 0) {
                throw new ArgumentNotValid("No subtree with name '" + name + "'");
            }
            if (childList.size() > 1) {
                throw new ArgumentNotValid("Multiple subtrees with name '"
                                           + name + "'");
            }
            e = childList.get(0);
        }
        return e;
    }

    /** Select a list of nodes from the current tree, resolving dotted paths.
     * Currently, if any part of the dotted path has multiple subtrees with
     * the given name, this method will find all that match.
     *
     * @param name Name of a node (tree or leaf), possibly via a dotted path
     * @return A list of nodes found via the given name from the root of this tree.
     * @throws ArgumentNotValid if the path is illegal
     */
    private List<Element> selectMultipleNodes(String name) {
        ArgumentNotValid.checkTrue(legalFieldName.reset(name).matches(),
                                   "Name must contain only alphanumeric, dash and period");
        String[] parts = name.split("\\.");
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
            List<Element> newElements = new ArrayList<Element>();
            final String subname = parts[i];
            for (Element n : elements) {
                List<Element> childList = n.elements(subname);
                if (childList != null) {
                    newElements.addAll(childList);
                }
            }
            elements = newElements;
        }
        return elements;
    }

    /**
     * Get the named subtree.
     *
     * @param name The name of the subtree.
     *
     * @return The single subtree with the given name.
     *
     * @throws ArgumentNotValid if this object is a leaf, or there is more than
     *                          one subtree with the given name.
     */
    public StringTree<T> getSubTree(String name) {
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        final Element n = selectSingleNode(name);
        return new XmlTree<T>(n, parser);
    }

    /**
     * Get the named subtrees.
     *
     * @param name The name of the subtrees.
     *
     * @return All subtrees with the given name.
     *
     * @throws ArgumentNotValid if this object is a leaf.
     */
    public List<StringTree<T>> getSubTrees(String name) {
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        List<Element> nodeList = selectMultipleNodes(name);
        List<StringTree<T>> resultList = new ArrayList<StringTree<T>>(nodeList.size());
        for (Element n : nodeList) {
            resultList.add(new XmlTree<T>(n, parser));
        }
        return resultList;
    }

    /**
     * Get a map of all the children of this node.
     *
     * @return Map of children of this node.
     *
     * @throws ArgumentNotValid if this object is a leaf.
     */
    public Map<String, List<StringTree<T>>> getChildMultimap() {
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        Map<String,List<StringTree<T>>> children =
                new HashMap<String,List<StringTree<T>>>();
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
     * Get a map of all subtrees, assuming there is no more than one subtree
     * with a given name.
     *
     * @return Map of all subtrees.
     *
     * @throws ArgumentNotValid if this object is a leaf, or there is more than
     *                          one subtree with a given name.
     */
    public Map<String, StringTree<T>> getChildMap() {
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        Map<String,StringTree<T>> children =
                new HashMap<String,StringTree<T>>();
        List<Element> nodeList = getChildNodes();
        if (nodeList != null) {
            for (Element n : nodeList) {
                if (children.containsKey(n.getName())) {
                    throw new ArgumentNotValid("More than one node named '"
                                               + n.getName() + "' found");
                }
                children.put(n.getName(), new XmlTree<T>(n, parser));
            }
        }
        return children;
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
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        Map<String, List<T>> children =
                new HashMap<String, List<T>>();
        List<Element> nodeList = getChildNodes();
        if (nodeList != null) {
            for (Element n : nodeList) {
                ArgumentNotValid.checkTrue(elementIsLeaf(n),
                                           "Child " + n.getName()
                                           + " is not a leaf");
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
     * subtrees are leafs and no two subtrees have the same name.
     *
     * @return Map from subtree names to values of their leaves.
     *
     * @throws ArgumentNotValid if this object is not a node, if there is more
     *                          than one subtree with a given name, or if any of
     *                          its children are not leaves.
     */
    public Map<String, T> getLeafMap() {
        ArgumentNotValid.checkTrue(!isLeaf(), "Cannot find subtrees in a leaf");
        Map<String, T> children =
                new HashMap<String, T>();
        List<Element> nodeList = getChildNodes();
        if (nodeList != null) {
            for (Element n : nodeList) {
                ArgumentNotValid.checkTrue(elementIsLeaf(n),
                                           "Child " + n.getName()
                                           + " is not a leaf");
                if (children.containsKey(n.getName())) {
                    throw new ArgumentNotValid("More than one node named '"
                                               + n.getName() + "' found");
                } else {
                    children.put(n.getName(), parseLeafNode(n));
                }
            }
        }
        return children;
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
}
