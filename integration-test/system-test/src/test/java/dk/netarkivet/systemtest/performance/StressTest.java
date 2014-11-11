/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.systemtest.performance;

import static org.testng.Assert.assertTrue;

import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.BeforeTest;

import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.environment.DefaultTestEnvironment;
import dk.netarkivet.systemtest.environment.TestController;
import dk.netarkivet.systemtest.environment.TestEnvironment;

/**
 * Test specification: https://sbforge.org/display/NAS/TEST+7.
 */
@SuppressWarnings("unused")
public abstract class StressTest extends SeleniumTest {

    static TestEnvironment testEnvironment = new DefaultTestEnvironment(
            "Stresstest",
            "foo@bar.dk",
            "SystemTest",
            8073,
            TestEnvironment.JOB_ADMIN_SERVER
    );
    static TestController testController = new TestController(testEnvironment);

    public StressTest() {
        super(testController);
    }

    @BeforeTest(alwaysRun = true)
    public void setupTest() {
        //super.setupTest();
        //testController = new TestController(TESTNAME, "kb-test-adm-001.kb.dk", 8073);
    }

    protected void shutdownPreviousTest() throws Exception {
        addFixture("Shutting down any previously running test.");
        testController.runCommand("stop_test.sh");
        addFixture("Cleaning up old test.");
        testController.runCommand("cleanup_all_test.sh");
        addFixture("Preparing deploy");
        testController.runCommand("prepare_test.sh deploy_config_stresstest.xml");
    }

    protected void startTestSystem() throws Exception {
        addFixture("Starting Test");
        testController.runCommand("start_test.sh");
    }

    protected void shutdownTest() throws Exception {
        addFixture("Shutting down the test.");
        testController.runCommand("stop_test.sh");
        testController.runCommand("cleanup_all_test.sh");
    }

    protected void checkUpdateTimes() throws Exception {
        String maximumBackupDaysString = System.getProperty("systemtest.maxbackupage", "7");
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        int maximumBackupsDays = Integer.parseInt(maximumBackupDaysString);
        addStep("Checking that backups are no more than " + maximumBackupsDays + " (systemtest.maxbackupage) days old. ", "");
        Long maximumBackupPeriod = maximumBackupsDays*24*3600*1000L; //ms
        Long harvestdbAge = System.currentTimeMillis() - getFileTimestamp("/home/test/" + backupEnv +"-backup/" + backupEnv +"_harvestdb.dump.out");
        assertTrue(harvestdbAge < maximumBackupPeriod, "harvestdb backup is older than " + maximumBackupsDays + " days");
        Long admindbAge = System.currentTimeMillis() - getFileTimestamp("/home/test/" + backupEnv +"-backup/" + backupEnv +"_admindb.out");
        assertTrue(admindbAge < maximumBackupPeriod, "admindb backup is older than " + maximumBackupsDays + " days");
        Long csAge = System.currentTimeMillis() - getFileTimestamp("/home/test/" + backupEnv +"-backup/CS");
        assertTrue(csAge < maximumBackupPeriod, "CS backup is older than " + maximumBackupsDays + " days");
        Long domainListAge =   System.currentTimeMillis() - getFileTimestamp("/home/test/" + backupEnv +"-backup/domain*.txt");
        assertTrue(domainListAge < maximumBackupPeriod,
                "Domain list backup is older than " + maximumBackupsDays + " days");
    }

    private Long getFileTimestamp(String filepath) throws Exception {
        String result = testController.runCommand("stat -c %Y " + filepath);
        return  Long.parseLong(result.trim())*1000L;
    }

    /**
     * Copying production databases to the relevant test servers.
     */
    protected void fetchProductionData() throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        addFixture("Copying production databases to the relevant test servers from the directory /home/test/" + backupEnv + "-backup");
        testController.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -rf /tmp/" + backupEnv + "_admindb.out");
        testController.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER,
                "rm -rf /tmp/" + backupEnv + "_harvestdb.dump.out");
        testController.runCommand(TestEnvironment.CHECKSUM_SERVER, "rm -rf /tmp/CS");
        addFixture("Copying admin db.");
        testController.runCommand("scp -r /home/test/" + backupEnv + "-backup/" + backupEnv
                + "_admindb.out test@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying harvest db");
        testController
                .runCommand("scp -r /home/test/" + backupEnv + "-backup/" + backupEnv
                        + "_harvestdb.dump.out test@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying checksum db");
        testController.runCommand("scp -r /home/test/" + backupEnv + "-backup/CS test@kb-test-acs-001.kb.dk:/tmp");
    }

    protected void deployComponents() throws Exception {
        addStep("Installing components.", "");
        testController.runCommand("install_test.sh");
    }

    /**
     * When nodata is true, restore schema only PLUS the ordertemplates table in the harvestdb.
     *
     * @param nodata
     * @throws Exception
     */
    protected void replaceDatabasesWithProd(boolean nodata) throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        String dropAdminDB = "psql -U test -c 'drop database if exists stresstest_admindb'";
        String createAdminDB = "psql -U test -c 'create database stresstest_admindb'";
        String dropHarvestDB = "psql -U test -c 'drop database if exists stresstest_harvestdb'";
        String createHarvestDB = "psql -U test -c 'create database stresstest_harvestdb'";
        addFixture("Cleaning out harvest database");
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropHarvestDB);
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createHarvestDB);
        addFixture("Cleaning out admin database");
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropAdminDB);
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createAdminDB);
        addFixture("Populating empty databases.");
        if (nodata) {
            addFixture("Restoring admin data schema");
            String createRelationsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -s --schema public /tmp/" + backupEnv +"_admindb.out";
            String populateSchemaversionsAdminDB = "pg_restore -U test -d stresstest_admindb  --no-owner -t schemaversions -t --clean --schema public /tmp/" + backupEnv +"_admindb.out";
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsAdminDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaversionsAdminDB);
            addFixture("Restoring harvestdb schema");
            String createRelationsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -s --schema public /tmp/" + backupEnv +"_harvestdb.dump.out";
            String populateSchemaVersionsHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t schemaversions --clean --schema public /tmp/" + backupEnv +"_harvestdb.dump.out";
            String populateOrdertemplatesHarvestDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t ordertemplates --clean --schema public /tmp/" + backupEnv +"_harvestdb.dump.out";
            String populateSchedulesDB = "pg_restore -U test -d stresstest_harvestdb  --no-owner -t schedules --clean --schema public /tmp/" + backupEnv +"_harvestdb.dump.out";
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaVersionsHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateOrdertemplatesHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchedulesDB);
            addFixture("Replacing checksum database with empty data");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "mkdir CS");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "touch CS/checksum_CS.md5");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "touch CS/removed_CS.checksum");
        } else {
            addFixture("Ingesting full production admindb backup.");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + testEnvironment.getTESTX().toLowerCase()
                            + "_admindb  --no-owner --schema public /tmp/" + backupEnv + "_admindb.out");
            addFixture("Ingesting full production harvestdb backup");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U test -d " + testEnvironment.getTESTX().toLowerCase()
                            + "_harvestdb  --no-owner --schema public /tmp/" + backupEnv + "_harvestdb.dump.out");

            addFixture("Replacing checksum database with prod data");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "rm -rf CS");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "ln -s /tmp/CS CS");
        }
    }

    protected void enableHarvestDatabaseUpgrade() throws Exception {
        addStep("Enabling database upgrade.", "");
        testController.replaceStringInFile(TestEnvironment.JOB_ADMIN_SERVER, "conf/settings_GUIApplication.xml",
                "<dir>harvestDatabase/fullhddb</dir>", "<dir>harvestDatabase/fullhddb;upgrade=true</dir>");
    }

    protected void upgradeHarvestDatabase() throws Exception {
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, "export CLASSPATH="
                + "./lib/dk.netarkivet.harvester.jar:" + "./lib/dk.netarkivet.archive.jar:"
                + "./lib/dk.netarkivet.monitor.jar:$CLASSPATH;java "
                + "-Xmx1536m  -Ddk.netarkivet.settings.file=./conf/settings_GUIApplication.xml "
                + "-Dlogback.configurationFile=./conf/logback_GUIApplication.xml "
                + "dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication "
                + "< /dev/null > start_harvestdatabaseUpdateApplication.log 2>&1 ");
    }

    private void generateDatabaseSchemas() throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        testController.runCommandWithoutQuotes("rm -rf /home/test/schemas");
        testController.runCommandWithoutQuotes("mkdir /home/test/schemas");
        String envDef = "cd /home/test/schemas;" + "export LIBDIR=/home/test/release_software_dist/"
                + testController.getTESTX() + "/lib/db;"
                + "export CLASSPATH=$LIBDIR/derby.jar:$LIBDIR/derbytools-10.8.2.2.jar;";
        // Generate schema for " + backupEnv +"uction database
        testController.runCommandWithoutQuotes(envDef + "java org.apache.derby.tools.dblook "
                + "-d 'jdbc:derby:/home/test/" + backupEnv +"-backup/fullhddb;upgrade=true' "
                + "-o /home/test/schemas/" + backupEnv +"dbs_schema.txt");
        // Generate schema for test database
        testController.runCommandWithoutQuotes(envDef + "jar xvf /home/test/release_software_dist/"
                + testController.getTESTX() + "/settings/fullhddb.jar");
        testController.runCommandWithoutQuotes(envDef
                + "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o testdbs_schema.txt");
        testController.runCommandWithoutQuotes(envDef + "rm -rf fullhddb; rm -rf META-INF");
        // Generate schema for bundled database
        testController.runCommandWithoutQuotes(envDef + "jar xvf /home/test/release_software_dist/"
                + testController.getTESTX() + "/harvestdefinitionbasedir/fullhddb.jar");
        testController.runCommandWithoutQuotes(envDef
                + "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o bundleddbs_schema.txt");
        testController.runCommandWithoutQuotes(envDef + "rm -rf fullhddb; rm -rf META-INF");
    }

    private void compareDatabaseSchemas() throws Exception {
        String envDef = "cd /home/test/schemas;";
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        // Sort the schemas and remove uninteresting lines
        testController.runCommandWithoutQuotes(envDef
                + "grep -v INDEX " + backupEnv + "dbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > " + backupEnv
                + "dbs_schema.txt.sort");
        testController.runCommandWithoutQuotes(envDef
                + "grep -v INDEX testdbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > testdbs_schema.txt.sort");
        testController
                .runCommandWithoutQuotes(envDef
                        + "grep -v INDEX bundleddbs_schema.txt|grep -v CONSTRAINT|grep -v ^-- |sort > bundleddbs_schema.txt.sort");
        // Check for Differences. The only allowed differences are in the order of the table fields.
        testController.runCommandWithoutQuotes(envDef
                + "diff -b -B testdbs_schema.txt.sort " + backupEnv + "dbs_schema.txt.sort  > test-" + backupEnv
                + "-diff", new int[] {0, 1});
        testController.runCommandWithoutQuotes(envDef
                + "diff -b -B bundleddbs_schema.txt.sort testdbs_schema.txt.sort  > test-bundled-diff",
                new int[] {0, 1});
    }

    public void addStep(java.lang.String stimuli, java.lang.String expectedResult) { super.addStep(stimuli, expectedResult); }

    public void addFixture(java.lang.String setupDescription) { super.addFixture(setupDescription); }

}
