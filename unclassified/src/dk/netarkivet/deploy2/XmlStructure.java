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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy2;

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
     * @param tree The root of the tree for this instance
     */
    public XmlStructure(Element tree) {
        ArgumentNotValid.checkNotNull(tree, "Element tree");
        root = tree.createCopy();
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
     * Loading the file into the document data structure
     * 
     * @param f The XML file to be loaded.
     * @return The XML file loaded into the document data structure
     * @throws IOFailure If the file was not correctly read
     */
    private Document loadDocument(File f) throws IOFailure {
        ArgumentNotValid.checkNotNull(f, "File f");
        SAXReader reader = new SAXReader();
        if (!f.canRead()) {
            log.debug("Could not read file: '" + f + "'");
            throw new IOFailure("Could not read file: '" + f + "'");
        }
        try {
            return reader.read(f);
        } catch (DocumentException e) {
            log.warn("Could not parse the file as XML: '" + f + "'", e);
            throw new IOFailure(
                    "Could not parse the file as XML: '" + f + "'", e);
        }
    }

    /**
     * for retrieving a single specific branch  
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
     * @see GetChildren() 
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
                String errMsg = "Element " + n 
                        + " is not a branch in the tree. Null returned";
                log.debug(errMsg);
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
    public String getSubChildValue(String ...name ) {
        ArgumentNotValid.checkNotNull(name, "String ...name");
        Element e = getSubChild(name);
        if(e != null ) {
            if(e.isTextOnly()) {
                return e.getText();
            } else {
                log.debug("Element is not text");
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
            log.debug("Element is not text");
            return null;
        }
    }
    
    /**
     * Retrieves the content of a the leafs deep in the tree structure.
     * It only retrieves 
     * 
     * @param path Specifies the path in the tree (e.g. in HTML: 
     * GetSubChildValue("HTML", "HEAD", "TITLE") to get the title of 
     * a HTML document)
     * @return The content of the leaf. If it is not a leaf, return null.
     * Returns 'null' if the path to the branch cannot be found.   
     */
    @SuppressWarnings("unchecked")
    public String[] getLeafValues(String ...path) {
        // check argument
        ArgumentNotValid.checkNotNull(path, "String ...path");

        Element e = root;
        // go through tree to branch before leafs.
        for(int i=0; i<path.length-1; i++) {
            String n = path[i];
            if(e != null) {
                e = e.element(n);
            } else {
                return null;
            }
        }
        // if no wanted branches, return null 
        if(e == null) {
            return null;
        }

        // get results.
        String[] res = null;
        List<Element> elemList = e.elements(path[path.length - 1]);
        // check that any leafs exist.
        if(elemList.size() < 1) {
            return null;
        }

        // extract the value of the elements.
        res = new String[elemList.size()];
        for(int i=0; i<elemList.size(); i++) {
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
            log.trace("Overwriting illegal area: " + e);
        } catch(Exception e) {
            log.warn("Error in overwritting an XML tree: " + e);
        }
    }

    /**
     * The current tree will be overwritten by the overwriter tree
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
    public void overWriting(Element current, Element overwriter) 
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
            if(curElems.size() == 0) {
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
    public void overWriteOnly(Element branch, String value, String ... path ) {
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
        ArgumentNotValid.checkNotNull(value, "String Value");
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
}
