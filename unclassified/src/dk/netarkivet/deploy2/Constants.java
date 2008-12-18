package dk.netarkivet.deploy2;

public class Constants {

	// Setting specific
	/** Path to the Settings branch */
	static final String SETTINGS_BRANCH = "settings";
	/** Path to the common branch within the settings branch*/
	static final String COMMON_BRANCH = "common";
	/** Path to the environment name branch in the common branch */
	static final String ENVIRONMENT_NAME_BRANCH = "environmentName";
	/** The total path to the environment name from beyond the settings branch */
	static final String[] ENVIRONMENT_NAME_TOTAL_PATH_BRANCH = 
			{SETTINGS_BRANCH, COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};
	/** The path to the environment name from the settings branch */
	static final String[] ENVIRONMENT_NAME_SETTING_PATH_BRANCH = 
			{COMMON_BRANCH, ENVIRONMENT_NAME_BRANCH};
	
	// parameter specific
	/** The path to the class path branches */
	static final String CLASS_PATH_BRANCH = "deployClassPath";
	/** The path to the java option branches */
	static final String JAVA_OPTIONS_BRANCH = "deployJavaOpt";
	/** The path to the optional installation directory */
	static final String PARAMETER_INSTALL_DIR_BRANCH = "deployInstallDir";
	/** The path to the machine user name */
	static final String PARAMETER_MACHINE_USER_NAME_BRANCH = "deployMachineUserName";
	

	// traversing the XML tree
	/** The path to physical locations in from the global scope */
	static final String PHYSICAL_LOCATION_BRANCH = "thisPhysicalLocation";
	/** The path to machines from a physical location */
	static final String MACHINE_BRANCH = "deployMachine";
	/** The path to applications from a machine */
	static final String APPLICATION_BRANCH = "applicationName";

	// physical location specific
	/** The path to name in a physical location instance */
	static final String PHYSICAL_LOCATION_NAME_BRANCH = "name";
	
	// machine specific
	/** The path to name in a machine instance */
	static final String MACHINE_NAME_BRANCH = "name";
	/** The path to the operating system variable */
	static final String MACHINE_OPERATING_SYSTEM_BRANCH = "os";

	// application specific
	/** The path to name in a application instance */
	static final String APPLICATION_NAME_BRANCH = "name";
	/** The path to the instance id for the application */
	static final String APPLICATION_INSTANCE_ID_BRANCH = "applicationInstanceId";
	
	
	// name for windows platform
	/** The operating system attribute for windows*/
	static final String OPERATING_SYSTEM_WINDOWS_ATTRIBUTE = "\"windows\"";
}
