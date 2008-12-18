/* $RCSfile: Machine.java,v $
 * $Date: 2008/09/01 08:01:35 $
 * $Revision: 1.24 $
 * $Author: asjo $
 */

package dk.netarkivet.deploy2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;


/**
 * Machine defines an abstract representation of a machine in the Deploy system.
 * All non-OS specific methods are implemented in MachineBase.
 *
 *
 */
public abstract class Machine {
	// the log, for logging stuff instead of displaying them directly. 
    protected final Log log = LogFactory.getLog(getClass().getName());
	
    /** The root-branch for this machine in the XML tree */
    protected Element machineRoot;
	/** The settings, inherited from parent and overwritten */
	protected XmlStructure settings;
	/** The machine parameters */
	protected Parameters machineParameters;
	/** The list of the application on this machine */
	protected List<Application> applications;
	
	/** The name of this machine */
	protected String name;
	/** The operating system on this machine: 'windows' or 'linux' */
	protected String OS;
	/** The extension on the scipt files (specified by operating system) */
	protected String scriptExtension;
	/** The name of the NetarchiveSuite.zip file */
	protected String netarchiveSuiteFileName;
	
	/** The directory for this machine*/
	File machineDirectory;
	
	/**
	 * A machine is referring to the actual computer, where the the applications
	 * are run.
	 * 
	 * @param e The root of this instance in the XML document
	 * @param parentSettings The setting inherited by the parent. 
	 * @param param The machine parameters inherited by the parent. 
	 */
	public Machine(Element e, XmlStructure parentSettings, 
			Parameters param, String netarchiveSuiteSource) {
        ArgumentNotValid.checkNotNull(e,"Element e");
        ArgumentNotValid.checkNotNull(parentSettings,"XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param,"Parameters param");
        ArgumentNotValid.checkNotNull(netarchiveSuiteSource,"Parameters param");
        
		settings = new XmlStructure(parentSettings.GetRoot());
		machineRoot = e;
		machineParameters = new Parameters(param);
		netarchiveSuiteFileName = netarchiveSuiteSource;
		
		// retrieve the specific settings for this instance 
		Element tmpSet = machineRoot.element(Constants.SETTINGS_BRANCH);

		// Generate the specific settings by combining the general settings 
		// and the specific, (only if this instance has specific settings)
		if(tmpSet != null) {
			settings.OverWrite(tmpSet);	
		}
		
		// check if new machine parameters
		machineParameters.NewParameters(machineRoot);

		// Retrieve the variables for this instance.
		ExtractVariables();
		
		// generate the machines on this instance
		ExtractApplications();
	}
	
	/**
	 * Extract the local variables from the root.
	 * 
	 * Currently, this is the name and the operating system.
	 */
	private void ExtractVariables() {
		Element elem = null;

		// retrieve name
		elem = machineRoot.element(Constants.MACHINE_NAME_BRANCH);
		if(elem != null) {
			name = elem.getText();
		} else {
			log.debug("Physical location has no name!");
			name = "";
		}

		// Operating system is defined in subclasses
/*		elem = machineRoot.element(Constants.MACHINE_OPERATING_SYSTEM_BRANCH);
		if(elem != null) {
			OS = elem.getText();
		} else {
			log.debug("Physical location has no name!");
			OS = "unix";
		}
/* */
	}

	/**
	 * Extracts the XML for the applications from the root, 
	 * creates the applications and puts them into the list  
	 */
	private void ExtractApplications() {
		applications = new ArrayList<Application>();
		
		List<Element> le = machineRoot.elements(Constants.APPLICATION_BRANCH);
		
		for(Element e : le) {
			applications.add(new Application(e, settings, machineParameters));
		}
	}

	/**
	 * Display the all variables for this class.
	 * This involves also displaying the content of the applications. 
	 */
	public void Display() {
		
		System.out.println("MACHINE");
		System.out.println("Name: " + name);
		System.out.println("Operating system: " + OS);
//		machineParameters.Display();
// 		settings.Display();
		System.out.println("Applications: " + applications.size());
		System.out.println();

//		for(Application app : applications) {
//			app.Display();
//		}
	}
	
	/**
	 * Create the directory for the specific configurations of this machine
	 * 
	 * Write something more ?
	 */
	public void Write(File parentDirectory) {
		// ?? WRITE WHAT ??
		
		// create the directory for this machine
		machineDirectory = new File(parentDirectory, name);
		FileUtils.createDir(machineDirectory);
		
		
		// write all application for this machine
		for(Application app : applications) {
			app.Write();
		}
	}
	
	/**
	 * Make the script for killing this machine.
	 * This is put into the entire killall script for the physical location.
	 * 
	 * @return The script to kill this machine
	 */
	public String WriteToKillScript() {
		String res = "";

		res += "echo KILLING MACHINE: " + MachineUserLogin() + "\n";
		// write the operating system dependent part of the kill script
		res += OSKillScript();

		return res;
	}

	/**
	 * Make the script for installing this machine.
	 * This is put into the entire install script for the physical location.
	 * 
	 * @return The script to make the installation on this machine
	 */
	public String WriteToInstallScript() {
		String res = "";
		
		res += "echo INSTALLING TO MACHINE: " + MachineUserLogin() + "\n";
		// write the operating system dependent part of the install script
		res += OSInstallScript();

		return res;
	}

	/**
	 * Make the script for starting this machine.
	 * This is put into the entire startall script for the physical location.
	 * 
	 * @return The script to start this machine
	 */
	public String WriteToStartScript() {
		String res = "";
		
		res += "echo STARTING MACHINE: " + MachineUserLogin() + "\n";
		// write the operating system dependent part of the start script
		res += OSStartScript();
		
		return res;
	}
	
	/**
	 * The string for accessing this machine through SSH.
	 * 
	 * @return The access through SSH to the machine
	 */
	protected String MachineUserLogin() {
		return machineParameters.machineUserName.getStringValue() + "@" + name;
	}
	
	/**
	 * For retrieving the environment name variable.
	 * 
	 * @return The environment name.
	 */
	protected String GetEnvironmentName() {
		return settings.GetSubChildValue(Constants.ENVIRONMENT_NAME_SETTING_PATH_BRANCH);
	}
	
	/** 
	 * The operation system specific path to the installation directory
	 *  
	 * @return Install path.
	 */
	protected abstract String GetInstallDirPath();
	
	/**
	 * The operation system specific path to the conf directory
	 * 
	 * @return Conf path.
	 */
	protected abstract String GetConfDirPath();
	
	/**
	 *  Creates the operation system specific killing script for this machine.
	 *  
	 * @return Operation system specific part of the killscript
	 */
	protected abstract String OSKillScript();
	
	/**
	 *  Creates the operation system specific installation script for this machine.
	 *  
	 * @return Operation system specific part of the installscript
	 */
	protected abstract String OSInstallScript();
	
	/**
	 *  Creates the operation system specific starting script for this machine.
	 *  
	 * @return Operation system specific part of the startscript
	 */
	protected abstract String OSStartScript();
	
}
