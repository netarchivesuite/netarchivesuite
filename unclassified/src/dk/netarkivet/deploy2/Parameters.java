package dk.netarkivet.deploy2;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class Parameters {
	// the log, for logging stuff instead of displaying them directly. 
    protected final Log log = LogFactory.getLog(getClass().getName());

    /** The class paths */
	List<Element> classPaths;
    /** The options for java */
	List<Element> javaOptions;
    /** Install directory */
	Element installDir;
    /** The machine user name */
	Element machineUserName;
	
	/**
	 * Constructor.
	 * Retrieves the parameters from the XML tree.
	 * 
	 * @param root
	 */
	public Parameters(XmlStructure root) {
        ArgumentNotValid.checkNotNull(root,"XmlStructure root");
        
		// initialise variables
		classPaths = root.GetChildren(Constants.CLASS_PATH_BRANCH);
		javaOptions = root.GetChildren(Constants.JAVA_OPTIONS_BRANCH);
		installDir = root.GetChild(Constants.PARAMETER_INSTALL_DIR_BRANCH);
		machineUserName = root.GetChild(Constants.PARAMETER_MACHINE_USER_NAME_BRANCH);
	}
	
	/**
	 * Constructor.
	 * Inherits the parameters of the parent instance.
	 * 
	 * @param parent The parameters of the parent instance.
	 */
	public Parameters(Parameters parent) {
        ArgumentNotValid.checkNotNull(parent,"Parameter parent");
		
		// copy parent class paths
		classPaths = new ArrayList<Element>();		
		for(Element e : parent.classPaths) {
			classPaths.add(e.createCopy());
		}

		// copy parent java options
		javaOptions = new ArrayList<Element>();
		for(Element e : parent.javaOptions) {
			javaOptions.add(e.createCopy());
		}
		
		// copy parent install dir (if any)
		if(parent.installDir != null) {
			installDir = parent.installDir.createCopy();
		} else {
			installDir = null;
		}
		// copy parent install dir (if any)
		if(parent.machineUserName != null) {
			machineUserName = parent.machineUserName.createCopy();
		} else {
			machineUserName = null;
		}
	}
	
	/**
	 * Overwrites the inherited parameters, if the root has new specified
	 * 
	 * @param root The root of the current instance. 
	 */
	public void NewParameters(Element root) {
        ArgumentNotValid.checkNotNull(root,"Element root");
        
		List<Element> tmp;
		
		// check if any class paths to overwrite existing
		tmp = root.elements(Constants.CLASS_PATH_BRANCH);
		if(tmp.size() > 0) {
			classPaths = tmp;
		}
		
		// check if any java options to overwrite existing
		tmp = root.elements(Constants.JAVA_OPTIONS_BRANCH);
		if(tmp.size() > 0) {
			javaOptions = tmp;
		}
		
		// check if new install dir to overwrite existing
		tmp = root.elements(Constants.PARAMETER_INSTALL_DIR_BRANCH);
		if(tmp.size() > 0) {
			installDir = tmp.get(0);
		}
		
		// check if new install dir to overwrite existing
		tmp = root.elements(Constants.PARAMETER_MACHINE_USER_NAME_BRANCH);
		if(tmp.size() > 0) {
			machineUserName = tmp.get(0);
		}
	}

	/**
	 * Prints out the content of this instance
	 */
	public void Display() {
		// display class paths
		System.out.println("Classpaths: ");
		for(Element e : classPaths) {
			if(e.isTextOnly()) {
				System.out.println(e.getText());
			} else {
				System.out.println("ERROR!");
				return;
			}
		}

		// display java options
		System.out.println("Java options: ");
		for(Element e : javaOptions) {
			if(e.isTextOnly()) {
				System.out.println(e.getText());
			} else {
				System.out.println("ERROR!");
				return;
			}
		}
		
		// display install directory
		System.out.println("Install directory: " + installDir.getText());
		// display machine user name
		System.out.println("Machine user name: " + machineUserName.getText());
	}
	
	public String WriteJavaParameters() {
		String res = "";
		
		// apply the java options
		for(Element e : javaOptions) {
			res += e.getText();
		}
		
		// apply the classpaths
		if(classPaths.size() > 0) {
			res += " -classpath ";

			// put ':' between all, but neither before or after
			res += classPaths.get(0).getText();
			for(int i=1; i<classPaths.size(); i++) {
				res += ":";
				res += classPaths.get(i).getText();
			}			
		}
		
		return res;
	}
}
