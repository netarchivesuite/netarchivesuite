package dk.netarkivet.deploy2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StringUtils;

public class PhysicalLocation {
	// the log, for logging stuff instead of displaying them directly. 
	protected final Log log = LogFactory.getLog(getClass().getName());

	/** The root for the branch of this element in the XML-tree. */
	private Element physLocRoot;
	/** The settings structure. */
	private XmlStructure settings;
	/** The parameters for java. */
	private Parameters machineParameters;
	/** The list of the machines. */
	private List<Machine> machines;

	/** The name of this physical location */
	private String name;

	/** The inherited name for the NetarchiveSuite file */ 
	private String netarchiveSuiteFileName;
	/** The inherited log property file */ 
	private File logPropFile;
	/** The inherited security file */
	private File securityPolicyFile;

	/**
	 * The physical locations is referring to the position in the real world
	 * where the computers are located. 
	 * One physical location can contain many machines.   
	 * 
	 * @param elem
	 * @param parentSettings
	 * @param param
	 */
	public PhysicalLocation(Element elem, XmlStructure parentSettings, 
			Parameters param, String netarchiveSuiteSource, File logProp,
			File securityPolicy) {
		// test if valid arguments
		ArgumentNotValid.checkNotNull(elem,"Element elem (physLocRoot)");
		ArgumentNotValid.checkNotNull(parentSettings,
				"XmlStructure parentSettings");
		ArgumentNotValid.checkNotNull(param,"Parameters param");
		ArgumentNotValid.checkNotNullOrEmpty(netarchiveSuiteSource,
				"String netarchiveSuite");
		ArgumentNotValid.checkNotNull(logProp,"File logProp");
		ArgumentNotValid.checkNotNull(securityPolicy,"File securityPolicy");
		
		// make a copy of parent, don't use it directly.
		settings = new XmlStructure(parentSettings.GetRoot());
		physLocRoot = elem;
		machineParameters = new Parameters(param);
		netarchiveSuiteFileName = netarchiveSuiteSource;
		logPropFile = logProp;
		securityPolicyFile = securityPolicy;

		// retrieve the specific settings for this instance 
		Element tmpSet = physLocRoot.element(Constants.SETTINGS_BRANCH);

		// Generate the specific settings by combining the general settings 
		// and the specific, (only if this instance has specific settings)
		if(tmpSet != null) {
			settings.OverWrite(tmpSet);	
		}

		// check if new machine parameters
		machineParameters.NewParameters(physLocRoot);

		// Retrieve the variables for this instance.
		ExtractVariables();

		// generate the machines on this instance
		ExtractMachines();
	}

	/**
	 * Extract the local variables from the root.
	 * 
	 * Currently, this is only the name.
	 */
	private void ExtractVariables() {
		// retrieve name
		Attribute at = physLocRoot.attribute(
				Constants.PHYSICAL_LOCATION_NAME_ATTRIBUTES);
		if(at != null) {
			name = at.getText();
		} else {
			log.debug("Physical location has no name!");
			name = "";
		}
	}

	/**
	 * Extracts the XML for machines from the root, creates the machines,
	 * and puts them into the list  
	 */
	private void ExtractMachines() {
		machines = new ArrayList<Machine>();

		List<Element> le = physLocRoot.elements(Constants.MACHINE_BRANCH);

		for(Element e : le) {
			String os = e.attributeValue(
					Constants.MACHINE_OPERATING_SYSTEM_ATTRIBUTE);

			// only a windows machine, if the 'os' attribute exists and
			// equals (not case-sensitive) 'windows'. Else linux machine
			if(os != null && os.equalsIgnoreCase(
					Constants.OPERATING_SYSTEM_WINDOWS_ATTRIBUTE)) {
				machines.add(new WindowsMachine(e, settings, machineParameters,
						netarchiveSuiteFileName, logPropFile, 
						securityPolicyFile));
			} else {
				machines.add(new LinuxMachine(e, settings, machineParameters,
						netarchiveSuiteFileName, logPropFile, 
						securityPolicyFile));
			}
		}
	}

	/**
	 * Display the all variables for this class.
	 * This involves also displaying the content of the machines. 
	 */
	public void Display() {
		System.out.println("Physical Location");
		System.out.println("Name: " + name);
		//		machineParameters.Display();
		//		settings.Display();
		System.out.println("Machines: " + machines.size());
		System.out.println();

		/*		for(Machine mac : machines) {
			mac.Display();
		}
/* */
	}

	/**
	 * Initiate the creation of global scripts and machine scripts. 
	 * 
	 * @param directory The directory where the files are to be placed.
	 */
	public void Write(File directory) {
		ArgumentNotValid.checkNotNull(directory,"File directory");
		// make the script in the directory!
		MakeScripts(directory);

		// write all machine at this location
		for(Machine mac : machines) {
			mac.Write(directory);
		}
	}

	/**
	 * Creates the following scripts for this physical location:
	 * * killall
	 * * install
	 * * startall
	 * 
	 * @param directory The directory where the scripts are to be placed.
	 */
	private void MakeScripts(File directory) {
		ArgumentNotValid.checkNotNull(directory,"File directory");
		// make extension (e.g. '_kb.sh' in the script 'killall_kb.sh')
		String ext = "_" + name + ".sh";

		// make script files
		File killall = new File(directory, "killall" + ext);
		File install = new File(directory, "install" + ext);
		File startall = new File(directory, "startall" + ext);

		try {
			PrintWriter kWriter = new PrintWriter(killall);
			PrintWriter iWriter = new PrintWriter(install);
			PrintWriter sWriter = new PrintWriter(startall);

			try {
				kWriter.println("#!/bin/bash");
				iWriter.println("#!/bin/bash");
				sWriter.println("#!/bin/bash");

				// insert machine data
				for(Machine mac : machines) {
					// write install script from machines
					iWriter.println("echo " 
							+ StringUtils.repeat("-", 44));
					iWriter.print(mac.writeToGlobalInstallScript());

					// write start script from machines
					sWriter.println("echo " 
							+ StringUtils.repeat("-", 44));
					sWriter.print(mac.writeToGlobalStartScript());

					// write kill script from machines
					kWriter.println("echo " 
							+ StringUtils.repeat("-", 44));
					kWriter.print(mac.writeToGlobalKillScript());
				}
				
			} finally {

				// close writers
				kWriter.println("echo " 
						+ StringUtils.repeat("-", 44));
				kWriter.close();

				iWriter.println("echo " 
						+ StringUtils.repeat("-", 44));
				iWriter.close();

				sWriter.println("echo " 
						+ StringUtils.repeat("-", 44));
				sWriter.close();
			}
		} catch (IOException e) {
			log.trace("Cannot create physical location scripts: " + e);
			throw new IOFailure("Problems creating the scripts for the" +
					"physical locations: " + e);
		} catch(Exception e) {
			// ERROR
			log.trace("Unknown error: " + e);
			System.out.println("Error in creating the scripts for the" +
					"physical locations: " + e);
		}
	}
}
