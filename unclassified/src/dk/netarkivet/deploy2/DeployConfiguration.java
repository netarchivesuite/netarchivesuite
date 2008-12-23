package dk.netarkivet.deploy2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

public class DeployConfiguration {
	
	// Configuration from XML file
	/** The configuration structure (deployGlobal) */
	XmlStructure config;
	/** The settings branch of the config */
	XmlStructure settings;
	/** The parameters for running java */
	Parameters machineParam;
	/** The list of the physical locations */
	private List<PhysicalLocation> physLocs;

	/** The file containing the it-configuration. */
	private File itConfigFile;
	/** The NetarchiveSuite file (in .zip). */
	private File netarchiveSuiteFile;
	/** The security policy file. */
	private File secPolicyFile;
	/** The log property file. */
	private File logPropFile;
	/** The directory for output. */
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
		ArgumentNotValid.checkNotNullOrEmpty(
				itConfigFileName, "No config file");
		ArgumentNotValid.checkNotNullOrEmpty(
				netarchiveSuiteFileName, "No installation file");
		ArgumentNotValid.checkNotNullOrEmpty(
				secPolicyFileName, "No security file");
		ArgumentNotValid.checkNotNullOrEmpty(
				logPropFileName, "No log file");
		
		itConfigFile = new File(itConfigFileName);
		netarchiveSuiteFile = new File(netarchiveSuiteFileName);
		secPolicyFile = new File(secPolicyFileName);
		logPropFile = new File(logPropFileName);
		
		// get configuration tree, settings and parameters
		config = new XmlStructure(itConfigFile);
		settings = new XmlStructure(
				config.GetChild(Constants.SETTINGS_BRANCH));
		machineParam = new Parameters(config);
		
		// if a outputDir has not been given as argument, 
		// it is the output directory
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

		ExtractElements();
		
//		Display();
	}

	/**
	 * Extracts the physical locations and put them into the list
	 */
	private void ExtractElements() {
		// initialise physical location array
		physLocs = new ArrayList<PhysicalLocation>();

		// get the list from the XML tree
		List<Element> physList = config.GetChildren(
				Constants.PHYSICAL_LOCATION_BRANCH);

		// get all physical locations into the list
		for(Element elem : physList) {
			physLocs.add(new PhysicalLocation(elem, settings, machineParam,
					netarchiveSuiteFile.getName(), logPropFile, 
					secPolicyFile));
		}
		
	}
	
	/**
	 * Show the value of all the variables
	 */
	public void Display() {
		System.out.println("Config         : " + 
				itConfigFile.getAbsolutePath());
		System.out.println("NetarchiveSuite: " + 
				netarchiveSuiteFile.getAbsolutePath());
		System.out.println("Security policy: " + 
				secPolicyFile.getAbsolutePath());
		System.out.println("Log file       : " + 
				logPropFile.getAbsolutePath());
		System.out.println("OutputDir      : " + 
				outputDir.getAbsolutePath());
	}
	
	/**
	 * Makes every physical location create their scripts.
	 */
	public void Write() {
		// write all physical locations
		for(PhysicalLocation pl : physLocs) {
			pl.Write(outputDir);
		}
	}
}
