package dk.netarkivet.deploy2;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import dk.netarkivet.common.utils.StringUtils;

public class PhysicalLocation {
	// the log, for logging stuff instead of displaying them directly. 
    protected final Log log = LogFactory.getLog(getClass().getName());

	private Element physLocRoot;
	private XmlStructure settings;
	private Parameters machineParameters;
	private List<Machine> machines;

	private String netarchiveSuiteFileName;
	private String name;

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
			Parameters param, String netarchiveSuiteSource) {
		// make a copy of parent, don't use it directly.
		settings = new XmlStructure(parentSettings.GetRoot());
		physLocRoot = elem;
		machineParameters = new Parameters(param);
		netarchiveSuiteFileName = netarchiveSuiteSource;
		
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
		
		Display();
	}

	/**
	 * Extract the local variables from the root.
	 * 
	 * Currently, this is only the name.
	 */
	private void ExtractVariables() {
		// retrieve name
		Element elem = physLocRoot.element(Constants.PHYSICAL_LOCATION_NAME_BRANCH);
		if(elem != null) {
			name = elem.getText();
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
			String os = e.elementText("os");
			
			if(os != null && os.equalsIgnoreCase(Constants.OPERATING_SYSTEM_WINDOWS_ATTRIBUTE)) {
				machines.add(new WindowsMachine(e, settings, machineParameters, netarchiveSuiteFileName));
			} else {
				machines.add(new LinuxMachine(e, settings, machineParameters, netarchiveSuiteFileName));
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
	
	public void Write(File directory) {
		// make the script in the directory!
		MakeScripts(directory);
		
		// write all machine at this location
		for(Machine mac : machines) {
			mac.Write(directory);
		}
	}
	
	private void MakeScripts(File directory) {
		// make extension (e.g. '_kb.sh' in the script 'killall_kb.sh')
		String ext = "_" + name + ".sh";

		// make script files
		File killall = new File(directory, "killall" + ext);
		File install = new File(directory, "install" + ext);
		File startall = new File(directory, "startall" + ext);
		
		// display!
		System.out.println("killall: " + killall.getName());
		System.out.println("install: " + install.getName());
		System.out.println("startall: " + startall.getName());
		
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
					iWriter.print(mac.WriteToInstallScript());

					// write start script from machines
					sWriter.println("echo " 
                            + StringUtils.repeat("-", 44));
					sWriter.print(mac.WriteToStartScript());

					// write kill script from machines
					kWriter.println("echo " 
                            + StringUtils.repeat("-", 44));
					kWriter.print(mac.WriteToKillScript());
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
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
		}
		
	}
}
