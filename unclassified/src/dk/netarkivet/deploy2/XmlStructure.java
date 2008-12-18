package dk.netarkivet.deploy2;

import java.io.File;
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


public class XmlStructure {

	/** the log, for logging stuff instead of displaying them directly. */ 
    protected final Log log = LogFactory.getLog(getClass().getName());
    /** The root of this branch in the XML tree */
	Element root;
	
	/**
	 * Constructor.
	 * Create an instance of this data-structure from an XML file.
	 * 
	 * @param f The XML file 
	 */
	public XmlStructure(File f) {
		ArgumentNotValid.checkTrue(f.exists(), "File f : " + f.getName() + " does not exist!");
		
		// get into 'document' format
		Document doc = LoadDocument(f);
        
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
	public Element GetRoot() {
		return root;
	}
	
	/**
	 * Loading the file into the document data structure
	 * 
	 * @param f The XML file to be loaded.
	 * @return The XML file loaded into the document data structure
	 * @throws IOFailure If the file was not correctly read
	 */
	private Document LoadDocument(File f) throws IOFailure {
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
	public Element GetChild(String name) {
		ArgumentNotValid.checkNotNullOrEmpty(name,"String name");
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
	public List<Element> GetChildren(String name) {
		ArgumentNotValid.checkNotNullOrEmpty(name,"String name");
		return root.elements(name);		
	}

	/**
	 * For receiving a list of all branches.
	 * 
	 * @return The children of this XML branch
	 * @see GetChildren(String name)
	 */
	public List<Element> GetChildren() {
		return root.elements();
	}
	
	/**
	 * Retrieving the text of this branch.
	 * If the branch is a leaf, the value is returned 
	 * 
	 * @return The text value of this element 
	 */
	public String GetValue() {
		return root.getText();
	}
	
	/** 
	 * Retrieves the value of a 
	 * Simple version of GetSubChildValue()
	 * 
	 * @param name The name of the child.
	 * @return The value of the leaf or null if the branch does not exists or is a tree.
	 */
	public String GetChildValue(String name) {
        ArgumentNotValid.checkNotNullOrEmpty(name,"String ...name");
        
		Element e = root.element(name);
		
		if(e != null && e.isTextOnly()) {
			return e.getText();
		} else {
			log.warn("Element is not accessible for reading " +
					"(either not existing or not leaf)! ");
			return null;
		}
	}
	
	/**
	 * Retrieves the content of a branch deep in tree structure.
	 *  
	 * @param name Specifies the path in the tree (e.g. in HTML: 
	 * GetSubChildValue("HTML", "HEAD", "TITLE") to get the title of a HTML document)
	 * @return The content of the leaf. If it is not a leaf, the entire XML-branch is returned.
	 * Returns 'null' if the path to the branch cannot be found.   
	 */
	public String GetSubChildValue(String ...name ) {
        // if no arguments, the XML is returned
        ArgumentNotValid.checkNotNull(name,"String ...name");
        
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
		
		if(e != null && e.isTextOnly()) {
			return e.getText();
		} else {
			log.debug("Element is not text");
			return e.asXML();
		}
	}
	
	/** 
	 * Displays the XML document
	 */
	public void Display() {
		System.out.println(root.asXML());
	}
	
	/**
	 * This function initialise the process of overwriting a part of the tree.
	 * 
	 * This is used for the Settings attributes in the deploy.
	 * 
	 * @param overwriter The settings instance for the current element
	 */
	public void OverWrite(Element overwriter) {
        ArgumentNotValid.checkNotNull(overwriter,"Element overwriter");
        
		try {
			OverWriting(root, overwriter);
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
	 * @param overwriter The element to have its values overwrite the base element
	 * @throws IllegalState If a leaf in current is about to be replaced by a tree
	 */
	public void OverWriting(Element current, Element overwriter) 
					throws IllegalState {
        ArgumentNotValid.checkNotNull(current,"Element current");
        ArgumentNotValid.checkNotNull(overwriter,"Element overwriter");

		// get the attributes to be overwritten
		List<Element> attributes = overwriter.elements();
		
		for(Element e : attributes) {
			// find corresponding attribute in current element
			Element curE = current.element(e.getName());
			
			// if not existing branch, make branch
			if(curE == null) {
				// append new branch to the tree
				current.add(e.createCopy());
			} else if(e.isTextOnly()) {
				// overwrite if leaf element

				// problem if tree is overwritten by leaf
				if(!curE.isTextOnly()) {
					log.warn("Replacing a tree with a leaf");
				}
				
				// overwrite the text
				curE.setText(e.getText());
			} else if(!curE.isTextOnly()) {
				// when both branches are trees (neither are leaf)
				// overwrite subtrees
				OverWriting(curE, e);
			} else {
				log.error("Cannot replace a leaf with a tree!");
				throw new IllegalState("Tree tries to replace leaf!");
			}
		}
	}
}
