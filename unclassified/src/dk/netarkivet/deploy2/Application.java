package dk.netarkivet.deploy2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class Application {
	// the log, for logging stuff instead of displaying them directly. 
    protected final Log log = LogFactory.getLog(getClass().getName());
	
    /** the root-branch for this application in the XML tree */
	private Element applicationRoot;
	/** The specific settings for this instance, inherited and overwritten */
	private XmlStructure settings;
	/** parameters */
	private Parameters machineParameters;
	
	/** Name of this instance */
	private String name;
	/** application instance id 
	 * (optional, used when two application has same name)
	 * */
	private String applicationId;

	/**
	 * A application is the program to be run on a machine.
	 * 
	 * @param e The root of this instance in the XML document
	 * @param parentSettings The setting inherited by the parent. 
	 * @param param The machine parameters inherited by the parent. 
	 */
	public Application(Element e, XmlStructure parentSettings, 
			Parameters param) {
        ArgumentNotValid.checkNotNull(e,"Element e");
        ArgumentNotValid.checkNotNull(parentSettings,"XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param,"Parameters param");
        
		settings = new XmlStructure(parentSettings.GetRoot());
		applicationRoot = e;
		machineParameters = new Parameters(param);
		
		// retrieve the specific settings for this instance 
		Element tmpSet = applicationRoot.element(Constants.SETTINGS_BRANCH);

		// Generate the specific settings by combining the general settings 
		// and the specific, (only if this instance has specific settings)
		if(tmpSet != null) {
			settings.OverWrite(tmpSet);	
		}
		
		// check if new machine parameters
		machineParameters.NewParameters(applicationRoot);

		// Retrieve the variables for this instance.
		ExtractVariables();
		
	}
	
	/**
	 * Extract the local variables from the root.
	 * 
	 * Currently, this is the name and the optional applicationId.
	 */
	private void ExtractVariables() {
		Element elem = null;

		// retrieve name
		elem = applicationRoot.element(Constants.APPLICATION_NAME_BRANCH);
		if(elem != null) {
			name = elem.getText();
		} else {
			log.debug("Physical location has no name!");
			name = "";
		}

		// look for the optional application instance id
		elem = applicationRoot.element(Constants.APPLICATION_INSTANCE_ID_BRANCH);
		if(elem != null) {
			applicationId = elem.getText();
		} else {
			applicationId= null;
		}
	}

	/**
	 * Display the all variables for this class.
	 */
	public void Display() {
		System.out.println("APPLICATION");
		System.out.println("Name: " + name);

		if(applicationId != null) {
			System.out.println("Application instance id: " + applicationId);
		}

		machineParameters.Display();
// 		settings.Display();
		System.out.println();
	}
	
	public String Write() {
		// ?? WRITE WHAT ??
		String res = "";
		
		res += "java ";
		res += machineParameters.WriteJavaParameters();
		
//		System.out.println(res);
		return "";
	}
}
