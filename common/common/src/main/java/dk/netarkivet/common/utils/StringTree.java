/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.List;
import java.util.Map;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;

/** An interface defining a structure with nodes, subnodes and leaves.
 * This is a recursively defined datastructure, so each instance can be a
 * tree or a leaf.
 * Each is node is named with a String, and each leaf can contain a value of
 * type T.
 * Each non-leaf tree can have any number of subnodes, each identified by
 * a String.
 */
public interface StringTree<T> {
    /**
     * Returns true if this object is a leaf, and thus if getValue is legal.
     *
     * @return True if the implementing object is a leaf, false otherwise.
     */
    boolean isLeaf();

    /**
     * Get the value of a named sub-leaf.
     *
     * @param name Name of the sub-leaf to get the value of.  These are strings,
     * and as a shorthand may specify subtrees of subtrees by separating each
     * level with '.', i.e. getSubtrees("subtree.subsubtree").
     * @return The value of the named leaf of this Tree, if it exists.
     * @throws IllegalState if this StringTree does not have exactly one
     * leaf sub-node with the given name.
     * @throws ArgumentNotValid if argument is null or empty.
     */
    T getValue(String name);

    /** Get the value of a leaf.
     *
     * @return The value of this Tree, if it is a leaf.
     * @throws IllegalState if this StringTree is a node.
     */
    T getValue();

    /**
     * Get the only subtree with the given name.
     *
     * @param name The name of the subtree. These are strings, and as a
     * shorthand may specify subtrees of subtrees by separating each level with
     * '.', i.e. getSubtrees("subtree.subsubtree").
     * @return The single subtree with the given name.
     * @throws IllegalState if this object is a leaf or if there is not
     * exactly one subtree with the given name.
     * @throws ArgumentNotValid if argument is null or empty.
     */
    StringTree<T> getSubTree(String name);

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
    List<StringTree<T>> getSubTrees(String name);

    /** Get a map of all direct children of this node.
     *
     * @return Map of children of this node, or an empty map for none.
     * @throws IllegalState if this object is a leaf.
     */
    Map<String, List<StringTree<T>>> getChildMultimap();

    /**
     * Get a map of all direct subtrees, assuming that all subtrees are uniquely
     * named.
     * 
     * @return Map of all subtrees.
     * @throws IllegalState if this object is a leaf or if there is more
     * than one subtree with the same name.
     */
    Map<String, StringTree<T>> getChildMap();

    /** Get a multimap of the names and values of all direct subtrees, assuming
     * that all subtrees are leafs.
     *
     * @return Multimap from subtree names to values of their leaves.
     * @throws IllegalState if this object is a leaf or if any of its children
     * are not leaves.
     */
    Map<String, List<T>> getLeafMultimap();

    /**
     * Get a map of the names and values of all subtrees, assuming that all
     * subtrees are leafs and are uniquely named
     *
     * @return Map from subtree names to values of their leaves.
     *
     * @throws IllegalState if this object is a leaf or if the subtrees are not
     * uniquely named, or if any of its children are not leaves.
     */
    Map<String, T> getLeafMap();
}
