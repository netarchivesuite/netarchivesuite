/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.XmlUtils;

/**
 * The structure for handling the XML files.
 */
public class XmlStructure {
    /** the log, for logging stuff instead of displaying them directly.*/
    private final Log log = LogFactory.getLog(getClass().getName());
    /** The root of this branch in the XML tree.*/
    private Element root;

    /**
     * Constructor.
     * Create an instance of this data-structure from an XML file.
     * 
     * @param f The XML file 
     */
    public XmlStructure(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        ArgumentNotValid.checkTrue(f.exists(), "File f : " + f.getName() 
                + " does not exist!");
        // get into 'document' format
        Document doc = loadDocument(f);
        // get root node
        root = doc.getRootElement();
    }

    /**
     * Constructor.
     * Creating a new instance of this data-structure from 
     * the branch of another instance. 
     * 
     * @param subTreeRoot The root of the tree for this instance
     */
    public XmlStructure(Element subTreeRoot) {
        ArgumentNotValid.checkNotNull(subTreeRoot, "Element tree");
        root = subTreeRoot.createCopy();
    }

    /**
     * Function to retrieving the root of this branch in the XML tree.
     * 
     * @return The root element
     */
    public Element getRoot(){
        return root;
    }

    /**
     * Loading the file into the document data structure.
     * 
     * @param f The XML file to be loaded.
     * @return The XML file loaded into the document data structure
     * @throws IOFailure If the file was not correctly read
     */
    private Document loadDocument(File f) throws IOFailure {
        ArgumentNotValid.checkNotNull(f, "File f");
        SAXReader reader = new SAXReader();
        if (!f.canRead()) {
            String msg = "Could not read file: '" + f.getAbsolutePath() + "'";
            log.debug(msg);
            throw new IOFailure(msg);
        }
        try {
            return reader.read(f);
        } catch (DocumentException e) {
            String msg = "Could not parse file: '" + f.getAbsolutePath() 
                    + "' as XML.";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Function for retrieving a single specific branch.  
     * 
     * @param name The name of the branch
     * @return The child element of the XML tree structure
     */
    public Element getChild(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        return root.element(name);
    }

    /**
     * For receiving a list of specific branches.
     *  
     * 
     * @param name The name of the children to be found.
     * @return A list of the children with the given name.
     */
    @SuppressWarnings("unchecked")
    public List<Element> getChildren(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name, "String name");
        return root.elements(name);
    }

    /**
     * Retrieves the XML code for this entire branch.
     * 
     * @return The XML code.
     */
    public String getXML() {
        return root.asXML();
    }
    
    /**
     * For retrieving the first children along a path.
     * 
     * @param name The path to the child.
     * @return The child element, or null if no such child exists.
     */
    public Element getSubChild(String ... name) {
        // if no arguments, the XML is returned
        ArgumentNotValid.checkNotNull(name, "String ...name");
        Element e = root;
        // go through the tree to get the correct 
        for(String n : name) {
            if(e != null) {
                e = e.element(n);
            } else {
                // the element does not exist
                log.debug("Element " + n 
                        + " is not a branch in the tree. Null returned");
                return null;
            }
        }
        return e;
    }

    /**
     * Retrieves the content of a branch deep in tree structure.
     *  
     * @param name Specifies the path in the tree (e.g. in HTML: 
     * GetSubChildValue("HTML", "HEAD", "TITLE") to get the title of 
     * a HTML document)
     * @return The content of the leaf. If it is not a leaf, the entire 
     * XML-branch is returned.
     * Returns 'null' if the path to the branch cannot be found.   
     */
    public String getSubChildValue(String ...name) {
        ArgumentNotValid.checkNotNull(name, "String ...name");
        Element e = getSubChild(name);
        if(e != null) {
            if(e.isTextOnly()) {
                return e.getText();
            } else {
                log.debug("Element is not text. The entire XML-branch "
                        + "is returned.");
                return e.asXML();
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieves the content of a branch deep in tree structure.
     *  
     * @param path Specifies the path in the tree (e.g. in HTML: 
     * GetSubChildValue("HTML", "HEAD", "TITLE") to get the title of 
     * a HTML document)
     * @return The content of the leaf. If it is not a leaf, return null.
     * Returns 'null' if the path to the branch cannot be found.   
     */
    public String getLeafValue(String ...path) {
        ArgumentNotValid.checkNotNull(path, "String ...name");
        Element e = getSubChild(path);
        if(e != null && e.isTextOnly()) {
            return e.getText();
        } else {
            log.debug("Element is not text. Null returned.");
            return null;
        }
    }
    
    /**
     * Retrieves the content of a the leafs deep in the tree structure.
     * It only retrieves branches at the first path.
     * 
     * @param path Specifies the path in the tree (e.g. in HTML: 
     * GetSubChildValue("HTML", "HEAD", "TITLE") to get the title of 
     * a HTML document)
     * @return The content of the leaf. If no leafs are found then an empty
     * collection of strings are returned (new String[0]).   
     */
    public String[] getLeafValues(String ...path) {
        // check argument
        ArgumentNotValid.checkNotNull(path, "String ...path");

        // get all leafs along path
        List<Element> elemList = getAllChildrenAlongPath(root, path);
        // check that any leafs exist.
        if(elemList.isEmpty()) {
            return new String[0];
        }

        // extract the value of the elements to an array.
        String[] res = new String[elemList.size()];
        for(int i = 0; i < elemList.size(); i++) {
            res[i] = elemList.get(i).getText();
        }

        return res;
    }
    
    /**
     * This function initialise the process of overwriting a part of the tree.
     * 
     * This is used for the Settings attributes in the deploy.
     * 
     * @param overwriter The settings instance for the current element
     */
    public void overWrite(Element overwriter) {
        ArgumentNotValid.checkNotNull(overwriter, "Element overwriter");
        try {
            overWriting(root, overwriter);
        } catch (IllegalState e) {
            log.trace("Overwriting illegal area. ", e);
        }
    }

    /**
     * The current tree will be overwritten by the overwriter tree.
     * The new branches in overwriter will be added to the current tree.
     * For the leafs which are present in both overwriter and current, 
     * the value in the current-leaf will be overwritten by the overwriter-leaf.
     *   
     * The subtrees which exists in both the overwriter and the current tree,
     * this function will be run recursively on these subtrees.
     * 
     * @param current The base element
     * @param overwriter The element to have its values overwrite 
     * the base element
     * @throws IllegalState If a leaf in current is about to be replaced 
     * by a tree
     */
    @SuppressWarnings("unchecked")
    private void overWriting(Element current, Element overwriter) 
                throws IllegalState {
        ArgumentNotValid.checkNotNull(current, "Element current");
        ArgumentNotValid.checkNotNull(overwriter, "Element overwriter");
        // get the attributes to be overwritten
        List<Element> attributes = overwriter.elements();
        List<Element> addElements = new ArrayList<Element>();
        
        // add branch if it does not exists
        for(Element e : attributes) {
            // find corresponding attribute in current element
            List<Element> curElems = current.elements(e.getName());
            
            // if no such elements in current tree, add branch.
            if(curElems.isEmpty()) {
                addElements.add(e);
            } else {
                // 
                List<Element> overElems = overwriter.elements(e.getName());

                // if the lists have a 1-1 ratio, then overwrite 
                if(curElems.size() == 1 && overElems.size() == 1) {
                    // only one branch, thus overwrite
                    Element curE = curElems.get(0);
                    // if leaf overwrite value, otherwise repeat for branches. 
                    if(curE.isTextOnly()) {
                        curE.setText(e.getText());
                    } else {
                        overWriting(curE, e);
                    }
                } else {
                    // a different amount of current branches exist (not 0).
                    // Therefore remove the branches in current tree, 
                    // and add replacements.
                    for(Element curE : curElems) {
                        current.remove(curE);
                    }
                    // add only current branch, since the others will follow.
                    addElements.add(e);
                }
            }            
        }
        
        // add all the new branches to the current branch.
        for(Element e : addElements) {
            current.add(e.createCopy());
        }
    }
    
    /**
     * Overwrites the leaf at the end of the path from the branch.
     * 
     * @param branch The branch where to begin.
     * @param value The value to overwrite the leaf with.
     * @param path The path from the branch to the leaf.
     */
    public void overWriteOnly(Element branch, String value, String ... path) {
        ArgumentNotValid.checkNotNullOrEmpty(value, "String Value");
        ArgumentNotValid.checkNotNull(path, "String path");
        ArgumentNotValid.checkPositive(path.length, "Size of String path[]");
        
        // get leaf element
        Element current = branch;
        for(String s : path) {
            current = current.element(s);
            
            // Do not overwrite non-existing element.
            if(current == null) {
                return;
            }
        }
        
        // Set the new value
        current.setText(value);
    }
    
    /**
     * Specific overwrite function for overwriting a specific character in a
     * string.
     * 
     * @param branch The initial branch of the XML tree. 
     * @param position The position in the String where the character are to be 
     * changed.
     * @param value The new value of the character to change.
     * @param path The path to the leaf of the string to change.
     */
    public void overWriteOnlyInt(Element branch, int position, char value, 
            String ... path) {
        ArgumentNotValid.checkNotNull(path, "String path");
        ArgumentNotValid.checkPositive(path.length, "Size of String path[]");
        ArgumentNotValid.checkPositive(position, "int position");
        
        // get leaf element
        Element current = branch;
        for(String s : path) {
            current = current.element(s);
            
            // Do not overwrite non-existing element.
            if(current == null) {
                return;
            }
        }
        
        // Set the new value
        char[] txt = current.getText().toCharArray();
        txt[position] = value;
        String res = new String(txt);
        current.setText(res);
    }
    
    /**
     * Creates an dom4j.Element from a String.
     * This string has to be in the XML format, otherwise return null.
     *  
     * @param content The content of a String.
     * @return The Element.
     */
    public static Element makeElementFromString(String content) {
        ArgumentNotValid.checkNotNullOrEmpty(content, "String name");

        try{
            ByteArrayInputStream in = new ByteArrayInputStream(
                    content.getBytes());
            Document doc = XmlUtils.getXmlDoc(in);

            return doc.getRootElement();
        } catch (Exception e) {
            LogFactory.getLog(XmlStructure.class).warn(
                    "makeElementFromString error caugth. Null returned.", e);
            return null;
        }
    }
    
    /**
     * This function creates the XML code for the path.
     * 
     * @param content The content at the leaf of the branch.
     * @param path The path to the branch.
     * @return The XML code for the branch with content.
     */
    public static String pathAndContentToXML(String content, String ... path) {
        ArgumentNotValid.checkNotNullOrEmpty(content, "String content");
        ArgumentNotValid.checkNotNegative(path.length, 
                "Size of 'String ... path'");

        StringBuilder res = new StringBuilder();

        // write path to the leaf
        for(int i = 0; i<path.length; i++) {
            String st = path[i];
            res.append(Constants.changeToXMLBeginScope(st));
        }

        res.append(content);

        // write path back from leaf (close xml).
        for(int i = path.length-1; i >= 0; i--) {
            String st = path[i];
            res.append(Constants.changeToXMLEndScope(st));
        }

        return res.toString();
    }

    /**
     * This function recursively calls it self, and retrieves all the leaf 
     * children from all sibling branches along the path.
     * When a call to it-self is made, the first string in path is removed.
     * 
     * @param current The current element to retrieve children along the path.
     * @param path The path to the leafs.
     * @return The complete list of elements which can be found along the path.
     */
    @SuppressWarnings("unchecked")
    public static List<Element> getAllChildrenAlongPath(Element current, 
            String ... path) {
        ArgumentNotValid.checkNotNull(current, "Element current");
        ArgumentNotValid.checkNotNull(path, "String ... path");

        // make the resulting element list.
        List<Element> res = new ArrayList<Element>();

        // get value from children
        if(path.length > 1){
            // create the new path
            String[] nextPath = new String[path.length -1];
            for(int i = 1; i < path.length; i++) {
                nextPath[i-1] = path[i];
            }

            // Get the list of children at next level of the path.
            List<Element> children = current.elements(path[0]);
            for(Element el : children) {
                    // the the result of these children.
                List<Element> childRes = getAllChildrenAlongPath(el, nextPath);
                // put children result into current result. 
                for(Element cr : childRes) {
                    res.add(cr);
                }
            }
        } else if (path.length == 1) {
            // if next level is leaf (or goal of path) return them.
            return current.elements(path[0]);
        }

        return res;
    }
}
