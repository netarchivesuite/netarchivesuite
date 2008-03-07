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

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import java.util.Map;
import java.util.List;

/** An interface defining a structure with nodes, subnodes and leaves.
 * Each non-leaf tree can have any number of subnodes, each identified by
 * a String.
 */
public interface StringTree<T> {
    /** Returns true if this object is a leaf of the tree, i.e. is of
     * type T rather than StringTree<T>.
     * @return True if the implementing object is a leaf, false otherwise. */
    boolean isLeaf();

    /** Get the value of a leaf.
     * @return The value of this Tree, if it is a leaf.
     * @throws ArgumentNotValid if this StringTree is a node.
     */
    T getValue();

    /** Get the value of a named sub-leaf.
     *
     * @param name Name of the sub-leaf to get the value of
     * @return The value of the name leaf of this Tree, if it exists.
     * @throws ArgumentNotValid if this StringTree does not have a leaf
     * sub-node with the given name.
     */
    T getValue(String name);

    /** Get the named subtrees.
     * @param name The name of the subtrees.
     * @return All subtrees with the given name.
     * @throws ArgumentNotValid if this object is a leaf.
     */
    List<StringTree<T>> getSubTrees(String name);

    /** Get a map of all the children of this node.
     * @return Map of children of this node.
     * @throws ArgumentNotValid if this object is a leaf.
     */
    Map<String, List<StringTree<T>>> getChildMultimap();

    /** Get the only subtree with the given name.
     * @param name The name of the subtree.
     * @return The single subtree with the given name.
     * @throws ArgumentNotValid if this object is a leaf, or there is more
     * than one subtree with the given name.
     */
    StringTree<T> getSubTree(String name);

    /** Get a map of all subtrees, assuming there is no more than one
     * subtree with a given name.
     * @return Map of all subtrees.
     * @throws ArgumentNotValid if this object is a leaf, or there is more
     * than one subtree with a given name.
     */
    Map<String, StringTree<T>> getChildMap();

    /** Get a multimap of the names and values of all subtrees, assuming
     * that all subtrees are leafs.
     * @return Multimap from subtree names to values of their leaves.
     * @throws ArgumentNotValid if this object is not a node, or if any
     * of its children are not leaves.
     */
    Map<String, List<T>> getLeafMultimap();

    /** Get a map of the names and values of all subtrees, assuming
     * that all subtrees are leafs and no two subtrees have the same name.
     * @return Map from subtree names to values of their leaves.
     * @throws ArgumentNotValid if this object is not a node, if there is
     * more than one subtree with a given name, or if any
     * of its children are not leaves.
     */
    Map<String, T> getLeafMap();
}
