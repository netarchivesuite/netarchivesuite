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

    @BeforeTest (alwaysRun=true)
    protected void setupTest() {
        environmentManager = new TestEnvironmentManager(TESTNAME, null, 8072);
    }

    protected void shutdownPreviousTest()  throws Exception{
        addFixture("Shutting down any previously running test.");
        environmentManager.runCommand("stop_test.sh");
        addFixture("Cleaning up old test.");
        environmentManager.runCommand("cleanup_all_test.sh");
        addFixture("Preparing deploy");
        environmentManager.runCommand("prepare_test.sh deploy_config_stresstest.xml");
    }

    protected void startTestSystem() throws Exception {
        addFixture("Starting Test");
        environmentManager.runCommand("start_test.sh");
    }

    protected void shutdownTest()  throws Exception{
        addFixture("Shutting down the test.");
        environmentManager.runCommand("stop_test.sh");
        environmentManager.runCommand("cleanup_all_test.sh");
    }


    /**
     * Copying production databases to the relevant test servers.
     */
    protected void fetchProductionData() throws Exception {
        addFixture("Copying production databases to the relevant test servers.");
        environmentManager.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -rf /tmp/prod_admindb.out");
        environmentManager.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER, "rm -rf /tmp/prod_harvestdb.dump.out");
        environmentManager.runCommand(TestEnvironment.CHECKSUM_SERVER, "rm -rf /tmp/CS");
        addFixture("Copying admin db.");
        environmentManager.runCommand("scp -r /home/test/prod-backup/prod_admindb.out test@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying harvest db");
        environmentManager.runCommand("scp -r /home/test/prod-backup/prod_harvestdb.dump.out test@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying checksum db");
        environmentManager.runCommand("scp -r /home/test/prod-backup/CS test@kb-test-acs-001.kb.dk:/tmp");
    }

    protected void deployComponents() throws Exception {
        addStep("Installing components.", "");
        environmentManager.runCommand("install_test.sh");
    }

    /**
     * When nodata is true, restore schema only PLUS the ordertemplates table in the harvestdb.
     * @param nodata
     * @throws Exception
     */
    protected void replaceDatabasesWithProd(boolean nodata) throws Exception {
        String dropAdminDB = "psql -U test -c 'drop database if exists stresstest_admindb'";
        String createAdminDB = "psql -U test -c 'create database stresstest_admindb'";
        String dropHarvestDB = "psql -U test -c 'drop database if exists stresstest_harvestdb'";
        String createHarvestDB = "psql -U test -c 'create database stresstest_harvestdb'";
        addFixture("Cleaning out harvest database");
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropHarvestDB);
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createHarvestDB);
        addFixture("Cleaning out admin database");
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropAdminDB);
        environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createAdminDB);
        addFixture("Populating empty databases.");
        if (nodata) {
            addFixture("Restoring admin data schema");
            String createRelationsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -s --schema public /tmp/prod_admindb.out";
            String populateSchemaversionsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -t schemaversions -t --clean --schema public /tmp/prod_admindb.out";
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsAdminDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaversionsAdminDB);
            addFixture("Restoring harvestdb schema");
            String createRelationsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -s --schema public /tmp/prod_harvestdb.dump.out";
            String populateSchemaVersionsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t schemaversions --clean --schema public /tmp/prod_harvestdb.dump.out";
            String populateOrdertemplatesHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t ordertemplates --clean --schema public /tmp/prod_harvestdb.dump.out";
            String populateSchedulesDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t schedules --clean --schema public /tmp/prod_harvestdb.dump.out";
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaVersionsHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateOrdertemplatesHarvestDB);
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchedulesDB);
        } else {
            addFixture("Ingesting full production admindb backup.");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + TESTNAME.toLowerCase() + "_admindb  --no-owner --schema public /tmp/prod_admindb.out");
            addFixture("Ingesting full production harvestdb backup");
            environmentManager.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + TESTNAME.toLowerCase() + "_harvestdb  --no-owner --schema public /tmp/prod_harvestdb.dump.out");
        }
        addFixture("Replacing checksum database with prod data");
        environmentManager.runTestXCommand(TestEnvironment.CHECKSUM_SERVER,
                "rm -rf CS");
        environmentManager.runTestXCommand(TestEnvironment.CHECKSUM_SERVER,
                "ln -s /tmp/CS CS");
    }


    protected void enableHarvestDatabaseUpgrade() throws Exception {
        addStep("Enabling database upgrade.", "");
        environmentManager.replaceStringInFile(TestEnvironment.JOB_ADMIN_SERVER,
                "conf/settings_GUIApplication.xml",
                "<dir>harvestDatabase/fullhddb</dir>",
                "<dir>harvestDatabase/fullhddb;upgrade=true</dir>");
    }

    protected void upgradeHarvestDatabase() throws Exception {
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
                        "< /dev/null > start_harvestdatabaseUpdateApplication.log 2>&1 ");
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
        environmentManager.runCommandWithoutQuotes(envDef +
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
