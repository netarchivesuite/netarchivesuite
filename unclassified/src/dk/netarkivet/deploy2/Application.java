package dk.netarkivet.deploy2;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

public class Application {
	// the log, for logging stuff instead of displaying them directly. 
    protected final Log log = LogFactory.getLog(getClass().getName());
	
    /** the root-branch for this application in the XML tree */
	private Element applicationRoot;
	/** The specific settings for this instance, inherited and overwritten */
	private XmlStructure settings;
	/** parameters */
	public Parameters machineParameters;
	
	/** Name of this instance */
	private String name;
	/** The total name of this instance */
	private String nameWithNamePath;
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
		try {
			
			// retrieve name
			Attribute at = applicationRoot.attribute(
					Constants.APPLICATION_NAME_ATTRIBUTE);
			if(at != null) {
				// the name is actually the classpath, so the specific class is
				// set as the name. It is the last element in the classpath.
				nameWithNamePath = at.getText();
				// the classpath is is separated by '.'
				String[] stlist = nameWithNamePath.split("[.]");
				name = stlist[stlist.length -1];
			} else {
				log.debug("Physical location has no name!");
				name = "";
				nameWithNamePath = "";
			}

			// look for the optional application instance id
			Element elem = applicationRoot.element(
					Constants.APPLICATION_INSTANCE_ID_BRANCH);
			if(elem != null) {
				applicationId = elem.getText();
			} else {
				applicationId = null;
			}
		} catch(Exception e) {
			log.debug("Application variables not extractable! ");
			throw new IOFailure("Application variables not extractable! ");
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
 		settings.Display();
	}
	
	/**
	 * Uses the name and the optional applicationId to create
	 * an unique identification for this application
	 *  
	 * @return The unique identification of this application.
	 */
	public String getIdentification() {
		String res = name;
		
		// apply only applicationId if it exists and has content
		if(applicationId != null && !applicationId.isEmpty()) {
			res += "_";
			res += applicationId;
		}
		
		return res;
	}
	
	/**
	 * @return the total name with directory path
	 */
	public String getTotalName() {
		return nameWithNamePath;
	}
	
	/**
	 * Creates the settings file for this application.
	 * This is extracted from the XMLStructure and put into a specific file.
	 * The name of the settings file for this application is:
	 * "settings_" + identification + ".xml"
	 * 
	 * @param directory The directory where the settings file should be placed.
	 */
	public void createSettingsFile(File directory) {
		// make file
		File settingsFile = new File(directory, 
				"settings_" + getIdentification() + ".xml");
		try {
			// initiate writer
			PrintWriter pw = new PrintWriter(settingsFile);
			
			try {
				// Extract the XML content of the branch for this application
				pw.println(settings.GetXML());
			} finally {
				pw.close();
			}
		} catch (Exception e) {
			log.debug("Error in creating settings file for application: " + e);
			throw new IOFailure("Cannot create settings file: " + e);
		}
	}

	/**
	 * Makes the install path with linux syntax
	 * 
	 * @return The path in linux syntax
	 */
	public String installPathLinux() {
		return machineParameters.installDir.getText() + "/" +
			settings.GetSubChildValue(Constants.ENVIRONMENT_NAME_SETTING_PATH_BRANCH);
	}

	/**
	 * Makes the install path with windows syntax.
	 * 
	 * @return The path with windows syntax.
	 */
	public String installPathWindows() {
		return machineParameters.installDir.getText() + "\\" +
			settings.GetSubChildValue(Constants.ENVIRONMENT_NAME_SETTING_PATH_BRANCH);
	}
}
