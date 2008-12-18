package dk.netarkivet.deploy2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

public class DeployConfiguration {
	
	// Configuration from XML file
	XmlStructure config;
	XmlStructure settings;
	Parameters machineParam;
	// The physical locations
	private List<PhysicalLocation> physLocs;

	private File itConfigFile;
	private File netarchiveSuiteFile;
	private File secPolicyFile;
	private File logPropFile;
	private File outputDir;

	/**
	 *  Initialise everything!
	 * 
	 * @param itConfigFileName Name of configuration file
	 * @param netarchiveSuiteFileName Name of installation file
	 * @param secPolicyFileName Name of security policy file
	 * @param logPropFileName Name of the log file
	 * @param outputDir Directory for the output
	 */
	public DeployConfiguration(String itConfigFileName, 
			String netarchiveSuiteFileName, 
			String secPolicyFileName, 
			String logPropFileName,
			String outputDirName) {
		ArgumentNotValid.checkNotNullOrEmpty(itConfigFileName, "No config file");
		ArgumentNotValid.checkNotNullOrEmpty(netarchiveSuiteFileName, "No installation file");
		ArgumentNotValid.checkNotNullOrEmpty(secPolicyFileName, "No security file");
		ArgumentNotValid.checkNotNullOrEmpty(logPropFileName, "No log file");
		
		itConfigFile = new File(itConfigFileName);
		netarchiveSuiteFile = new File(netarchiveSuiteFileName);
		secPolicyFile = new File(secPolicyFileName);
		logPropFile = new File(logPropFileName);
		
		// get configuration tree, settings and parameters
		config = new XmlStructure(itConfigFile);
		settings = new XmlStructure(config.GetChild(Constants.SETTINGS_BRANCH));
		machineParam = new Parameters(config);
		
		// if a outputDir has not been given as argument, it is the output directory
		if(outputDirName == null) {
			// Load output directory from config file
			outputDirName = "./" 
				+ config.GetSubChildValue(
						Constants.ENVIRONMENT_NAME_TOTAL_PATH_BRANCH)
				+ "/";
		}
		outputDir = new File(outputDirName);
		// make directory outputDir
		FileUtils.createDir(outputDir);

		// initialise physical location array
		physLocs = new ArrayList<PhysicalLocation>();
		
		List<Element> physList = config.GetChildren(Constants.PHYSICAL_LOCATION_BRANCH);

		// get all physical locations into the list
		for(Element elem : physList) {
			physLocs.add(new PhysicalLocation(elem, settings, machineParam, netarchiveSuiteFile.getName()));
		}
		
		Display();
	}
	
	/**
	 * Show the value of all the variables
	 */
	public void Display() {
		System.out.println("Config         : " + itConfigFile.getAbsolutePath());
		System.out.println("NetarchiveSuite: " + netarchiveSuiteFile.getAbsolutePath());
		System.out.println("Security policy: " + secPolicyFile.getAbsolutePath());
		System.out.println("Log file       : " + logPropFile.getAbsolutePath());
		System.out.println("OutputDir      : " + outputDir.getAbsolutePath());
/*		
		machineParam.Display();
		settings.Display();
		
		for(PhysicalLocation pl : physLocs) {
			pl.Display();
		}
/* */
	}
	
	public void Write() {
		// make scripts in output directory
//		List<File> killall;
		
		// ?? write what ??
		
		// write all physical locations
		for(PhysicalLocation pl : physLocs) {
			pl.Write(outputDir);
		}
	}
}
