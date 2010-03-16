/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.deploy;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.deploy.Constants;
import dk.netarkivet.deploy.DeployApplication;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
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
    private String arcDatabaseName = TestInfo.FILE_BP_DATABASE.getPath();
    
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
        FileUtils.removeRecursively(TestInfo.TMPDIR);
        // reset Settings to before
        pse.tearDown();
        pss.tearDown();
        rs.tearDown();
    }
    
    /**
     * Ensures, that the constructor is as a utility class. 
     */
    public void testConstructors() {
        ReflectUtils.testUtilityConstructor(DeployApplication.class);
        ReflectUtils.testUtilityConstructor(Constants.class);
        ReflectUtils.testUtilityConstructor(ScriptConstants.class);
    }

    /**
     * This test invokes the deploy application and verifies that all files
     * (settings, scripts, bats) created by the script matches the target
     * files stored in SVN.
     * Any change to the output files, will break this test.
     * When the test is broken: Verify that all differences reported by this
     * test are intended and correct, when all output files are verified
     * correct, replace the target files in SVN with the new set of
     * output files.
     * 
     * This also tests the consequences of non-default jmxremote files 
     * and non-default monitor user-name and Heritrix user-name.
     */
    public void testDeploy() {
        String[] args = {
                    TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
                    TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
                    TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
                    TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
                    TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                    TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
                    TestInfo.ARGUMENT_EVALUATE + "yes"
                    };
        DeployApplication.main(args);
        
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.TARGETDIR,
                                              TestInfo.TMPDIR);
        /**/
        if(differences.length() > 0) {
            pss.tearDown();
            System.out.println("testDeploy");
            System.out.println(differences);
            pss.setUp();
        }
        /**/
        
        assertEquals("No differences expected", 0, differences.length());
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
        /**/
        if(differences.length() > 0) {
            pss.tearDown();
            System.out.println("testDeploySingle");
            System.out.println(differences);
            pss.setUp();
        }
        /**/
        
        assertEquals("No differences expected", 0, differences.length());
    }
    
    /** 
     * Test that we can deploy with both databases (harvest database and
     * bitpreservations database) defined.
     */
    public void testDeployDatabase() {
        String database_it_conf_xml_name = 
            TestInfo.IT_CONF_DATABASE_FILE.getPath();
        
        String[] args = {
        	TestInfo.ARGUMENT_CONFIG_FILE + database_it_conf_xml_name,
        	TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
        	TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
        	TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
        	TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
                TestInfo.ARGUMENT_ARCHIVE_DATABASE_FILE + arcDatabaseName
                };
        pss.tearDown();
        DeployApplication.main(args);
        pss.setUp();
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.DATABASE_TARGET_DIR,
                                              TestInfo.TMPDIR);
        /**/
        if(differences.length() > 0) {
            pss.tearDown();
            System.out.println("testDeployDatabase:");
            System.out.println(differences);
            pss.setUp();
        }
        /**/

        assertEquals("No differences expected", 0, differences.length());
    }
    
    /**
     * tests The test arguments for argument errors.
     */
    public void testDeployTest() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
                TestInfo.ARGUMENT_TEST + TestInfo.ARGUMENT_TEST_ARG
	};
        DeployApplication.main(args);
        // compare the resulting output files with the target files
        String differences =
                TestFileUtils.compareDirsText(TestInfo.TEST_TARGET_DIR,
                                              TestInfo.TMPDIR);
        /**/
        if(differences.length() > 0) {
            pss.tearDown();
            System.out.println("testDeployTest");
            System.out.println(differences);
            pss.setUp();
        }
        /**/
        
        assertEquals("No differences expected", 0, differences.length());
    }

    /**
     * tests if non-existing argument is given
     */
    public void testDeployArguments1() {
	String[] args = {
		"-FAIL" + itConfXmlName,
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
	assertTrue("The error message should start with: " 
	        + Constants.MSG_ERROR_PARSE_ARGUMENTS, 
		pssMsg.startsWith(Constants.MSG_ERROR_PARSE_ARGUMENTS));
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
                TestInfo.ARGUMENT_LOG_PROPERTY_FILE + "ERROR",
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + "ERROR" 
	};
	DeployApplication.main(args);

	// get message and exit value
	String pssMsg = pss.getErr();
	int pseVal = pse.getExitValue();

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_TOO_MANY_ARGUMENTS, 
                pssMsg.startsWith(Constants.MSG_ERROR_TOO_MANY_ARGUMENTS));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS, 
                pssMsg.startsWith(Constants.MSG_ERROR_NOT_ENOUGH_ARGUMENTS));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_CONFIG_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_CONFIG_EXTENSION));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_NETARCHIVESUITE_EXTENSION));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_SECURITY_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_SECURITY_EXTENSION));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_LOG_PROPERTY_EXTENSION));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_DATABASE_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_DATABASE_EXTENSION));
    }
    
    /**
     * tests bitpreservation database file argument with wrong extension.
     */
    public void testDeployArgumentsExtension6() {
        String[] args = {
                TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
                TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
                TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
                TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
                TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_ARCHIVE_DATABASE_FILE + "database.ERROR"
        };
        DeployApplication.main(args);

        // get message and exit value
        int pseVal = pse.getExitValue();
        String pssMsg = pss.getErr();

        assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_BPDB_EXTENSION, 
                pssMsg.startsWith(Constants.MSG_ERROR_BPDB_EXTENSION));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_CONFIG_FILE_ARG, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_CONFIG_FILE_ARG));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_ARG));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_SECURITY_FILE_ARG, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_SECURITY_FILE_ARG));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_ARG));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_CONFIG_FILE_FOUND));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_NETARCHIVESUITE_FILE_FOUND));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_SECURITY_FILE_FOUND));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_LOG_PROPERTY_FILE_FOUND));
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

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND, 
                pssMsg.startsWith(Constants.MSG_ERROR_NO_DATABASE_FILE_FOUND));
    }

    /**
     * tests The test arguments for errors.
     */
    public void testTestArgument() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
                TestInfo.ARGUMENT_TEST + "ERROR"
	};
	DeployApplication.main(args);

	// get message and exit value
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_TEST_ARGUMENTS, 
                pssMsg.startsWith(Constants.MSG_ERROR_TEST_ARGUMENTS));
    }

    /**
     * tests the test argument for too large difference between the offset 
     * and HTTP port.
     */
    public void testTestArgument1() {
	String[] args = {
		TestInfo.ARGUMENT_CONFIG_FILE + itConfXmlName,
		TestInfo.ARGUMENT_NETARCHIVE_SUITE_FILE + nullzipName,
		TestInfo.ARGUMENT_SECURITY_FILE + securityPolicyName,
		TestInfo.ARGUMENT_LOG_PROPERTY_FILE + testLogPropName,
		TestInfo.ARGUMENT_OUTPUT_DIRECTORY + output_dir,
                TestInfo.ARGUMENT_DATABASE_FILE + databaseName,
                TestInfo.ARGUMENT_TEST + "1000,2000,test,test@kb.dk"
	};
	DeployApplication.main(args);

	// get message and exit value 
	int pseVal = pse.getExitValue();
	String pssMsg = pss.getErr();

	assertEquals("Exit value asserted 1.", 1, pseVal);
        assertTrue("The error message should start with: " 
                + Constants.MSG_ERROR_TEST_OFFSET, 
                pssMsg.startsWith(Constants.MSG_ERROR_TEST_OFFSET));
    }
}
