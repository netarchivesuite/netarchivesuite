/*
 * #%L
 * NetarchiveSuite System test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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

import org.testng.annotations.BeforeTest;

import dk.netarkivet.systemtest.SeleniumTest;
import dk.netarkivet.systemtest.environment.DefaultTestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironment;
import dk.netarkivet.systemtest.environment.TestEnvironmentController;

/**
 * Abstract superclass for the stress tests.
 */
@SuppressWarnings("unused")
public abstract class AbstractStressTest extends SeleniumTest {

    final String compressionSuffix = " ";

    final Long SECOND = 1000L;
    final Long MINUTE = 60 * SECOND;
    final Long HOUR = 60 * MINUTE;
    final Long DAY = 24 * HOUR;

    final static TestEnvironment ENV = new DefaultTestEnvironment(
            "Stresstest",
            "csr@statsbiblioteket.dk",
            "SystemTest",
            8073,
            TestEnvironment.JOB_ADMIN_SERVER,
            "deploy_conf_stress_test.xml"
    );
    static TestEnvironmentController testController = new TestEnvironmentController(ENV);

    public AbstractStressTest() {
        super(testController);
    }

    /**
     * Don't call the superclass method as the startup procedure for stresstests is complex
     * with multiple steps.
     */
    @BeforeTest(alwaysRun = true)
    @Override
    public void setupTest() {}

    protected void shutdownPreviousTest() throws Exception {
        addFixture("Cleaning up old test.");
        int[] positiveExitCodes = new int[] { 0, 2 };
        testController.runCommand(null, "cleanup_all_test.sh", 1000, "", positiveExitCodes);
        addFixture("Preparing deploy");
        testController.runCommand("prepare_test.sh deploy_config_stresstest.xml");
    }

    protected void startTestSystem() throws Exception {
        addFixture("Starting Test");
        testController.runCommand("start_test.sh");
    }

    protected void shutdownTest() throws Exception {
        addFixture("Shutting down the test.");
        testController.runCommand("cleanup_all_test.sh");
    }

    protected void checkUpdateTimes() throws Exception {

        String maximumBackupDaysString = System.getProperty("systemtest.maxbackupage", "7");
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        int maximumBackupsDays = Integer.parseInt(maximumBackupDaysString);
        addStep("Checking that backups are no more than " + maximumBackupsDays + " (systemtest.maxbackupage) days old. ",
                "");
        Long maximumBackupPeriod = maximumBackupsDays * DAY; //ms
        Long harvestdbAge = System.currentTimeMillis() - getFileTimestamp(
                "${HOME}/" + backupEnv + "-backup/" + backupEnv + "_harvestdb.dump.out");
        assertTrue(harvestdbAge < maximumBackupPeriod, "harvestdb backup is older than " + maximumBackupsDays + " days");
        Long admindbAge = System.currentTimeMillis() - getFileTimestamp(
                "${HOME}/" + backupEnv + "-backup/" + backupEnv + "_admindb.out");
        assertTrue(admindbAge < maximumBackupPeriod, "admindb backup is older than " + maximumBackupsDays + " days");
        Long csAge = System.currentTimeMillis() - getFileTimestamp(
                "${HOME}/" + backupEnv + "-backup/CS");
        assertTrue(csAge < maximumBackupPeriod, "CS backup is older than " + maximumBackupsDays + " days");
        Long domainListAge = System.currentTimeMillis() - getFileTimestamp(
                "${HOME}/" + backupEnv + "-backup/domain*.txt");
        assertTrue(domainListAge < maximumBackupPeriod, "Domain list backup is older than " + maximumBackupsDays + " days");
    }

    private Long getFileTimestamp(String filepath) throws Exception {
        String result = testController.runCommand("stat -c %Y " + filepath);
        return Long.parseLong(result.trim()) * 1000L;
    }

    /**
     * Copying production databases to the relevant test servers.
     */
    protected void fetchProductionData() throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        addFixture("Copying production databases to the relevant test servers from the directory "
                + TestEnvironment.DEPLOYMENT_HOME + "/" + backupEnv + "-backup");
        testController.runCommand(TestEnvironment.JOB_ADMIN_SERVER, "rm -rf /tmp/" + backupEnv + "_admindb.out");
        testController.runCommand(TestEnvironment.ARCHIVE_ADMIN_SERVER,
                "rm -rf /tmp/" + backupEnv + "_harvestdb.dump.out" + compressionSuffix);
        testController.runCommand(TestEnvironment.CHECKSUM_SERVER, "rm -rf /tmp/CS");
        addFixture("Copying admin db.");
        testController.runCommand("scp -r " + "${HOME}/" + backupEnv + "-backup/" + backupEnv
                + "_admindb.out " + TestEnvironment.DEPLOYMENT_USER + "@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying harvest db");
        testController.runCommand("scp -r ${HOME}/" + backupEnv + "-backup/" + backupEnv
                        + "_harvestdb.dump.out"+ compressionSuffix + TestEnvironment.DEPLOYMENT_USER + "@kb-test-adm-001.kb.dk:/tmp");
        addFixture("Copying checksum db");
        testController.runCommand("scp -r ${HOME}/" + backupEnv + "-backup/CS " + TestEnvironment.DEPLOYMENT_USER + "@kb-test-acs-001.kb.dk:/tmp");
    }

    protected void deployComponents() throws Exception {
        addStep("Installing components.", "");
        testController.runCommand("install_test.sh");
    }

    /**
     * When nodata is true, restore schema only PLUS the ordertemplates table in the harvestdb.
     */
    protected void replaceDatabasesWithProd(boolean nodata) throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        String dropAdminDB =
                "psql -U " + TestEnvironment.DEPLOYMENT_USER + " -c 'drop database if exists stresstest_admindb'";
        String createAdminDB = "psql -U " + TestEnvironment.DEPLOYMENT_USER + " -c 'create database stresstest_admindb'";
        String dropHarvestDB =
                "psql -U " + TestEnvironment.DEPLOYMENT_USER + " -c 'drop database if exists stresstest_harvestdb'";
        String createHarvestDB =
                "psql -U " + TestEnvironment.DEPLOYMENT_USER + " -c 'create database stresstest_harvestdb'";
        addFixture("Cleaning out harvest database");
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropHarvestDB);
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createHarvestDB);
        addFixture("Cleaning out admin database");
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, dropAdminDB);
        testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createAdminDB);
        addFixture("Populating empty databases.");
        if (nodata) {
            addFixture("Restoring admin data schema");
            String createRelationsAdminDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_admindb  --no-owner -s --schema public /tmp/" + backupEnv
                            + "_admindb.out";
            String populateSchemaversionsAdminDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_admindb  --no-owner -t schemaversions -t --clean --schema public /tmp/"
                            + backupEnv + "_admindb.out";
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsAdminDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaversionsAdminDB);
            addFixture("Restoring harvestdb schema");
            String createRelationsHarvestDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_harvestdb --no-privileges  --no-owner -s --schema public /tmp/" + backupEnv
                            + "_harvestdb.dump.out" + compressionSuffix;
            String populateSchemaVersionsHarvestDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_harvestdb --no-privileges  --no-owner -t schemaversions --clean --schema public /tmp/"
                            + backupEnv + "_harvestdb.dump.out" + compressionSuffix;
            String populateOrdertemplatesHarvestDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_harvestdb --no-privileges  --no-owner -t ordertemplates --clean --schema public /tmp/"
                            + backupEnv + "_harvestdb.dump.out" + compressionSuffix;
            String populateSchedulesDB =
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                            + " -d stresstest_harvestdb --no-privileges  --no-owner -t schedules --clean --schema public /tmp/"
                            + backupEnv + "_harvestdb.dump.out" + compressionSuffix;
            String harvestChannelDB =
                                "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                                        + " -d stresstest_harvestdb --no-privileges  --no-owner -t harvestchannel --clean --schema public /tmp/"
                                        + backupEnv + "_harvestdb.dump.out" + compressionSuffix;
            String harvestChannelSeqDB =
                                "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER
                                        + " -d stresstest_harvestdb --no-privileges  --no-owner -t harvestchannel_id_seq --clean --schema public /tmp/"
                                        + backupEnv + "_harvestdb.dump.out" + compressionSuffix;
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, createRelationsHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchemaVersionsHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateOrdertemplatesHarvestDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, populateSchedulesDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, harvestChannelDB);
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER, harvestChannelSeqDB);
            addFixture("Replacing checksum database with empty data");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "mkdir CS");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "touch CS/checksum_CS.md5");
            testController.runTestXCommand(TestEnvironment.CHECKSUM_SERVER, "touch CS/removed_CS.checksum");
        } else {
            addFixture("Ingesting full production admindb backup.");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER + " -d " + ENV.getTESTX().toLowerCase()
                            + "_admindb  --no-owner --schema public /tmp/" + backupEnv + "_admindb.out");
            addFixture("Ingesting full production harvestdb backup");
            testController.runTestXCommand(TestEnvironment.JOB_ADMIN_SERVER,
                    "pg_restore -U " + TestEnvironment.DEPLOYMENT_USER + " -d " + ENV.getTESTX().toLowerCase()
                            + "_harvestdb  --no-owner --no-privileges  --schema public /tmp/" + backupEnv + "_harvestdb.dump.out" + compressionSuffix);

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
                + "./lib/netarchivesuite-harvest-scheduler.jar:./lib/netarchivesuite-monitor-core.jar:$CLASSPATH;java "
                + "-Xmx1536m -Ddk.netarkivet.settings.file=./conf/settings_GUIApplication.xml "
                + "-Dlogback.configurationFile=./conf/logback_GUIApplication.xml "
                + "dk.netarkivet.harvester.tools.HarvestdatabaseUpdateApplication "
                + "< /dev/null > start_harvestdatabaseUpdateApplication.log 2>&1 ");
    }

    private void generateDatabaseSchemas() throws Exception {
        String backupEnv = System.getProperty("systemtest.backupenv", "prod");
        testController.runCommandWithoutQuotes("rm -rf " + TestEnvironment.DEPLOYMENT_HOME + "/schemas");
        testController.runCommandWithoutQuotes("mkdir " + TestEnvironment.DEPLOYMENT_HOME + "/schemas");
        String envDef = "cd " + TestEnvironment.DEPLOYMENT_HOME + "/schemas;" + "export LIBDIR="
                + TestEnvironment.DEPLOYMENT_HOME + "/release_software_dist/"
                + ENV.getTESTX() + "/lib/db;"
                + "export CLASSPATH=$LIBDIR/derby.jar:$LIBDIR/derbytools-10.8.2.2.jar;";
        // Generate schema for " + backupEnv +"uction database
        testController.runCommandWithoutQuotes(envDef + "java org.apache.derby.tools.dblook "
                + "-d 'jdbc:derby:" + TestEnvironment.DEPLOYMENT_HOME + "/" + backupEnv
                + "-backup/fullhddb;upgrade=true' "
                + "-o " + TestEnvironment.DEPLOYMENT_HOME + "/schemas/" + backupEnv + "dbs_schema.txt");
        // Generate schema for test database
        testController.runCommandWithoutQuotes(
                envDef + "jar xvf " + TestEnvironment.DEPLOYMENT_HOME + "/release_software_dist/"
                        + ENV.getTESTX() + "/settings/fullhddb.jar");
        testController.runCommandWithoutQuotes(envDef
                + "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o testdbs_schema.txt");
        testController.runCommandWithoutQuotes(envDef + "rm -rf fullhddb; rm -rf META-INF");
        // Generate schema for bundled database
        testController.runCommandWithoutQuotes(
                envDef + "jar xvf " + TestEnvironment.DEPLOYMENT_HOME + "/release_software_dist/"
                        + ENV.getTESTX() + "/harvestdefinitionbasedir/fullhddb.jar");
        testController.runCommandWithoutQuotes(envDef
                + "java org.apache.derby.tools.dblook -d 'jdbc:derby:fullhddb;upgrade=true' -o bundleddbs_schema.txt");
        testController.runCommandWithoutQuotes(envDef + "rm -rf fullhddb; rm -rf META-INF");
    }

    private void compareDatabaseSchemas() throws Exception {
        String envDef = "cd " + TestEnvironment.DEPLOYMENT_HOME + "/schemas;";
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

    public void addStep(java.lang.String stimuli, java.lang.String expectedResult) {
        super.addStep(stimuli, expectedResult);
    }

    public void addFixture(java.lang.String setupDescription) {
        super.addFixture(setupDescription);
    }

}
