/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.systemtest.performance;

import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentManager;

/**
 * Test specification: https://sbforge.org/display/NAS/TEST+7.
 */
public class StressTest extends ExtendedTestCase {
    public static final String TESTNAME = "Stresstest";
    /** Handles the bash command functionality in the test environment. */
    protected TestEnvironmentManager environmentManager;

    /**
     * Test specification: http://netarchive.dk/suite/It23JMXMailCheck .
     */
    @Test (groups = {"stresstest"})
    public void generalTest() throws Exception {
        addDescription("Test specification: https://sbforge.org/display/NAS/TEST+7");
    }

    @BeforeTest (alwaysRun=true)
    public void setupTest() {
        environmentManager = new TestEnvironmentManager(TESTNAME, null, 8072);
    }

    @BeforeClass
    public void setupTestEnvironment() throws Exception {
        /*shutdownPreviousTest();
        fetchProductionData();
        deployComponents(); */
        replaceDatabasesWithProd(true);
        /*enableHarvestDatabaseUpgrade();*/
        upgradeHarvestDatabase();
        /*generateDatabaseSchemas();
        compareDatabaseSchemas();*/
        startTestSystem();
    }

    private void shutdownPreviousTest()  throws Exception{
        addStep("Shutting down any previously running test.", "");
        environmentManager.runCommand("stop_test.sh");
        environmentManager.runCommand("cleanup_all_test.sh");
        environmentManager.runCommand("prepare_test.sh deploy_config_dedup_disabled.xml");
    }

    private void startTestSystem() throws Exception {
        addStep("Starting Test", "");
        environmentManager.runCommand("start_test.sh");
    }

    /**
     * Copying production databases to the relevant test servers.
     */
    private void fetchProductionData() throws Exception {
        addStep("Copying production databases to the relevant test servers.", "");
/*        environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -rf /tmp/prod_admindb.tar");
        environmentManager.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER, "rm -rf /tmp/prod_harvestdb.tar");
        environmentManager.runCommand(TestEnvironment.CHECKSUM_SERVER, "rm -rf /tmp/CS");
        environmentManager.runCommand("scp -r /home/test/prod-backup/pg_prod_harvestdb.tar test@kb-test-adm-001.kb.dk:/tmp");
        environmentManager.runCommand("scp -r /home/test/prod-backup/pg_prod_admindb.tar test@kb-test-adm-001.kb.dk:/tmp");*/
        environmentManager.runCommand("scp -r /home/test/prod-backup/CS test@kb-test-acs-001.kb.dk:/tmp");
    }

    private void deployComponents() throws Exception {
        addStep("Installing components.", "");
        environmentManager.runCommand("install_test.sh");
    }

    /**
     * When nodata is true, restore schema only PLUS the ordertemplates table in the harvestdb.
     * @param nodata
     * @throws Exception
     */
    private void replaceDatabasesWithProd(boolean nodata) throws Exception {
        addStep("Replacing default test databases with prod data", "");
        if (nodata) {
            addStep("Restoring admin data schema", "");

            String dropAdminDB = "psql -U test -c 'drop database if exists stresstest_admindb'";
            String createAdmintDB = "psql -U test -c 'create database stresstest_admindb'";
            String createRelationsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -s --schema public /tmp/pg_prod_admindb.tar";
            String populateSchemaversionsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -t schemaversions -t --clean --schema public /tmp/pg_prod_admindb.tar";

            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropAdminDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createAdmintDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsAdminDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaversionsAdminDB);

            addStep("Restoring harvestdb schema", "");

            String dropHarvestDB = "psql -U test -c 'drop database if exists stresstest_harvestdb'";
            String createHarvestDB = "psql -U test -c 'create database stresstest_harvestdb'";
            String createRelationsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -s --schema public /tmp/pg_prod_harvestdb.tar";
            String populateSchemaVersionsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t schemaversions --clean --schema public /tmp/pg_prod_harvestdb.tar";
            String populateOrdertemplatesHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t ordertemplates --clean --schema public /tmp/pg_prod_harvestdb.tar";


            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaVersionsHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateOrdertemplatesHarvestDB);

        } else {
            addStep("Ingesting prod admin data", "");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + TESTNAME.toLowerCase() + "_admindb --clean --no-owner --schema public /tmp/pg_prod_admindb.tar");
            addStep("Ingesting prod harvest data", "");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + TESTNAME.toLowerCase() + "_harvestdb --clean --no-owner --schema public /tmp/pg_prod_harvestdb.tar");
        }
        addStep("Replacing checksum database with prod data", "");
        environmentManager.runTestXCommand(TestEnvironment.CHECKSUM_SERVER,
                "rm -rf CS");
        environmentManager.runTestXCommand(TestEnvironment.CHECKSUM_SERVER,
                "ln -s /tmp/CS CS");
    }


    private void enableHarvestDatabaseUpgrade() throws Exception {
        addStep("Enabling database upgrade.", "");
        environmentManager.replaceStringInFile(TestEnvironment.JOB_ADMIN_SERVER,
                "conf/settings_GUIApplication.xml",
                "<dir>harvestDatabase/fullhddb</dir>",
                "<dir>harvestDatabase/fullhddb;upgrade=true</dir>");
    }

    private void upgradeHarvestDatabase() throws Exception {
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                "export CLASSPATH=" +
                        "./lib/dk.netarkivet.harvester.jar:" +
                        "./lib/dk.netarkivet.archive.jar:" +
                        "./lib/dk.netarkivet.monitor.jar:$CLASSPATH;java " +
                        "-Xmx1536m  -Ddk.netarkivet.settings.file=./conf/settings_GUIApplication.xml " +
                        "-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger " +
                        "-Djava.util.logging.config.file=./conf/log_GUIApplication.prop " +
                        "-Djava.security.manager -Djava.security.policy=./conf/security.policy " +
                        "dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication " +
                        "< /dev/null > start_harvestdatabaseUpdateApplication.log 2>&1 conf/kill_external_harvest_database.sh");
    }

    private void generateDatabaseSchemas() throws Exception {
        environmentManager.runCommandWithoutQuotes("rm -rf /home/test/schemas");
        environmentManager.runCommandWithoutQuotes("mkdir /home/test/schemas");
        String envDef = "cd /home/test/schemas;" +
                "export LIBDIR=/home/test/release_software_dist/" + environmentManager.getTESTX() + "/lib/db;" +
                "export CLASSPATH=$LIBDIR/derby.jar:$LIBDIR/derbytools-10.8.2.2.jar;";
        //Generate schema for production database
        environmentManager.runCommandWithoutQuotes(envDef+
                "java org.apache.derby.tools.dblook " +
                "-d 'jdbc:derby:/home/test/prod-backup/fullhddb;upgrade=true' " +
                "-o /home/test/schemas/proddbs_schema.txt");
        //Generate schema for test database
        environmentManager.runCommandWithoutQuotes(envDef+
                "jar xvf /home/test/release_software_dist/" + environmentManager.getTESTX() + "/settings/fullhddb.jar");
        environmentManager.runCommandWithoutQuotes(envDef+
                "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o testdbs_schema.txt");
        environmentManager.runCommandWithoutQuotes(envDef+
                "rm -rf fullhddb; rm -rf META-INF");
        //Generate schema for bundled database
        environmentManager.runCommandWithoutQuotes(envDef+
                "jar xvf /home/test/release_software_dist/" + environmentManager.getTESTX() +
                "/harvestdefinitionbasedir/fullhddb.jar");
        environmentManager.runCommandWithoutQuotes(envDef+
                "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o bundleddbs_schema.txt");
        environmentManager.runCommandWithoutQuotes(envDef+
                "rm -rf fullhddb; rm -rf META-INF");
    }

    private void compareDatabaseSchemas() throws Exception {
        String envDef = "cd /home/test/schemas;";

        //Sort the schemas and remove uninteresting lines
        environmentManager.runCommandWithoutQuotes(envDef+
                "grep -v INDEX proddbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > proddbs_schema.txt.sort");
        environmentManager.runCommandWithoutQuotes(envDef+
                "grep -v INDEX testdbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > testdbs_schema.txt.sort");
        environmentManager.runCommandWithoutQuotes(envDef+
                "grep -v INDEX bundleddbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > bundleddbs_schema.txt.sort");
        //Check for Differences. The only allowed differences are in the order of the table fields.
        environmentManager.runCommandWithoutQuotes(envDef+
                "diff -b -B testdbs_schema.txt.sort proddbs_schema.txt.sort  > test-prod-diff", new int[]{0,1});
        environmentManager.runCommandWithoutQuotes(envDef+
                "diff -b -B bundleddbs_schema.txt.sort testdbs_schema.txt.sort  > test-bundled-diff", new int[]{0,1});
    }


}
