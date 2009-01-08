/*$Id: DeployTester.java 520 2008-10-15 17:50:35Z svc $
* $Revision: 520 $
* $Date: 2008-10-15 19:50:35 +0200 (Wed, 15 Oct 2008) $
* $Author: svc $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.deploy2;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

public class DeployTester extends TestCase {

    String oldSettingsFileName;
    ReloadSettings rs = new ReloadSettings();
	
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private PreventSystemExit pse = new PreventSystemExit();

    // define standard arguments
    private String itConfXmlName = TestInfo.IT_CONF_FILE.getPath();
    private String securityPolicyName = TestInfo.FILE_SECURITY_POLICY.getPath();
    private String testLogPropName = TestInfo.FILE_LOG_PROP.getPath();
    private String nullzipName = TestInfo.FILE_NETATCHIVE_SUITE.getPath();
    private String output_dir = TestInfo.TMPDIR.getPath();
    private String databaseName = TestInfo.FILE_DATABASE.getPath();
    
    public void setUp() {
        rs.setUp();
	pss.setUp();
	pse.setUp();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMPDIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // reset Settings to before
	pse.tearDown();
	pss.tearDown();
        rs.tearDown();
    }

    /**
     * This test invokes the deploy application and verifies that all files
     * (settings.xml, scripts, bats) created by the script matches the target
     * files stored in SVN.
     * Any change to the output files, will break this test.
     * When the test is broken: Verify that all differences reported by this
     * test are intended and correct, when all output files are verified
     * correct, replace the target files in SVN with the new set of
     * output files.
     */
    public void testDeploy() {
        String[] args = {
                    TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
                    TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
                    TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
                    TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
                    TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                    TestInfo.ARGUMENT_DATABASE_FILE + databaseName
                    };
        DeployApplication.main(args);
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.TARGETDIR,
                                              TestInfo.TMPDIR);
        if (differences.length() > 0) {
            System.out.println(differences);
        }
        assertEquals("No differences expected", 0, differences.length());

        // find all settings*.xml files
        List<File> settingsFiles = TestFileUtils.findFiles(TestInfo.TMPDIR,
                new FileFilter() {
                    Pattern settingsPattern = Pattern.compile(
                            "^settings.*\\.xml$");
                    public boolean accept(File pathname) {
                        return settingsPattern.matcher(
                                pathname.getName()).matches();
                    }
                });
        // XSD-test them
        for (File f : settingsFiles) {
            System.setProperty("dk.netarkivet.settings.file",
                    f.getAbsolutePath());
            Settings.reload();
            //XmlUtils.validateWithXSD(new File(
            //        "./lib/data-definitions/settings.xsd"));
        }
    }

    /** 
     * Test that we can deploy with a single location.
     */
    public void testDeploySingle() {
        String single_it_conf_xml_name = TestInfo.IT_CONF_SINGLE_FILE.getPath();
        
        String[] args = {
        	TestInfo.ARGUMENT_CONFIG_FILE + single_it_conf_xml_name,
        	TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
        	TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
        	TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
        	TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
                };
        DeployApplication.main(args);
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.SINGLE_TARGET_DIR,
                                              TestInfo.TMPDIR);
        if (differences.length() > 0) {
            System.out.println(differences);
        }
        assertEquals("No differences expected", 0, differences.length());

        // find all settings*.xml files
        List<File> settingsFiles = TestFileUtils.findFiles(
                TestInfo.TMPDIR,
                new FileFilter() {
                    Pattern settingsPattern = Pattern.compile(
                            "^settings.*\\.xml$");
                    public boolean accept(File pathname) {
                        return settingsPattern.matcher(
                                pathname.getName()).matches();
                    }
                 }
        );
        // XSD-test them
        for (File f : settingsFiles) {
            System.setProperty("dk.netarkivet.settings.file",
                    f.getAbsolutePath());
            Settings.reload();
            // XmlUtils.validateWithXSD(new File(
            //  "./lib/data-definitions/settings.xsd"));
        }
    }
    
    /**
     * tests if non-existing argument is given
     */
    public void testDeployArguments1() {
	String[] args = {
		"-ERROR" + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_PARSE_ARGUMENTS, pssMsg);
    }

    /**
     * tests too many arguments.
     */
    public void testDeployArguments2() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + "ERROR", 
		TestInfo.ARGUMENT_CONFIG_FILE + "ERROR", 
		TestInfo.ARGUMENT_SECURITY_FILE + "ERROR", 
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + "ERROR" 
	};
	DeployApplication.main(args);

	// get message and exit value
	String pssMsg = pss.getErr();
	int pseVal = pse.getExitValue();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_TOO_MANY_ARGUMENTS, pssMsg);
    }

    /**
     * tests not enough arguments.
     */
    public void testDeployArguments3() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
	};
	DeployApplication.main(args);

	// get message and exit value
	String pssMsg = pss.getErr();
	int pseVal = pse.getExitValue();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS, pssMsg);
    }

    /**
     * tests configuration file argument with wrong extension.
     */
    public void testDeployArgumentsExtension1() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + "config.ERROR",
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_CONFIG_EXTENSION, pssMsg);
    }

    /**
     * tests NetarchiveSuite file argument with wrong extension.
     */
    public void testDeployArgumentsExtension2() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + "null.ERROR",
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION, pssMsg);
    }

    /**
     * tests security policy file argument with wrong extension.
     */
    public void testDeployArgumentsExtension3() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + "security.ERROR",
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_SECURITY_EXTENSION, pssMsg);
    }

    /**
     * tests log property file argument with wrong extension.
     */
    public void testDeployArgumentsExtension4() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + "log.ERROR",
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION, pssMsg);
    }
    
    /**
     * tests database file argument with wrong extension.
     */
    public void testDeployArgumentsExtension5() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
		TestInfo.ARGUMENT_DATABASE_FILE + "database.ERROR"
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_DATABASE_EXTENSION, pssMsg);
    }
    
    /**
     * tests when enough arguments are given, but configuration file 
     * is missing.
     */
    public void testDeployArgumentsLack1() {
	String[] args = {
//		TestInfo.ARGUMENT_CONFIG_FILE + it_conf_xml_name,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_CONFIG_FILE_ARG, pssMsg);
    }

    /**
     * tests when enough arguments are given, but NetarchiveSuite file 
     * is missing.
     */
    public void testDeployArgumentsLack2() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
//		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG, pssMsg);
    }

    /**
     * tests when enough arguments are given, but the security file 
     * is missing.
     */
    public void testDeployArgumentsLack3() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
//		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyFile,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_SECURITY_FILE_ARG, pssMsg);
    }

    /**
     * tests when enough arguments are given, but the log property file 
     * is missing.
     */
    public void testDeployArgumentsLack4() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
//		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropFile,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG, pssMsg);
    }

    /**
     * tests when config file argument refers to non-existing file.
     */
    public void testDeployFileExist1() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + "ERROR.xml",
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND, pssMsg);
    }

    /**
     * tests when NetarchiveSuite file argument refers to non-existing file.
     */
    public void testDeployFileExist2() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + "ERROR.zip",
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND, pssMsg);
    }

    /**
     * tests when security file argument refers to non-existing file.
     */
    public void testDeployFileExist3() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + "ERROR.policy",
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND, pssMsg);
    }

    /**
     * tests when log property file argument refers to non-existing file.
     */
    public void testDeployFileExist4() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + "ERROR.prop",
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND, pssMsg);
    }

    /**
     * tests when database file argument refers to non-existing file.
     */
    public void testDeployFileExist5() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + "ERROR.jar"
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 0.", 0, pseVal);
	assertEquals("Correct error message expected.", 
		Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND, pssMsg);
    }
}
