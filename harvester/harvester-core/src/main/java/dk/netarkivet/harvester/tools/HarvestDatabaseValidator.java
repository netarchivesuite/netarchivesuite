package dk.netarkivet.harvester.tools;

import java.io.File;
import java.util.Date;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DBSpecifics;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDBConnection;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypeDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.RunningJobsInfoDAO;

/** 
 * Simple tool to let you verify that you can access the database correctly with 
 * settings defined by -Ddk.netarkivet.settings.file=/fullOrrelative/path/to/settings.xml
 * or the settings.
 * Sample Derby settings:
 
  <common>...
  <database>
      <url/>
      <class>dk.netarkivet.harvester.datamodel.DerbyServerSpecifics</class>
      <baseUrl>jdbc:derby</baseUrl>
      <machine>localhost</machine>
      <port>48121</port>
      <dir>harvestDatabase/fullhddb</dir>
   </database>
 * Sample Postgresql settings:
  <common>... 
  <database>
        <url/>
      <class>dk.netarkivet.harvester.datamodel.PostgreSQLSpecifics</class>
      <baseUrl>jdbc:postgresql</baseUrl>
      <machine>localhost</machine>
      <dir>test_harvestdb</dir>
      <port>5432</port>
      <username>netarchivesuite</username>
      <password>netarchivesuite</password>
  </database> 
 */
public class HarvestDatabaseValidator {

    public static final String SETTINGSFILEPATH = "dk.netarkivet.settings.file";

    /**
     * @param args The path to a settings.xml (optional). If no argument, uses the existing settings.(e.g. by explicit setting it
     *  -Ddk.netarkivet.settings.file=/fullOrrelative/path/to/settings.xml )
     */
    public static void main(String[] args) {
        System.out.println("Running program " + HarvestDatabaseValidator.class.getName() + " at " +  new Date());
        if (args.length == 1) {
            System.out.println("Using settingsfile given as argument: " + args[0]); 
            System.setProperty(SETTINGSFILEPATH, args[0]);
            File settingsfile = new File(args[0]);
            if (!settingsfile.exists()) {
                System.err.println("Aborting program. Settingsfile '" + settingsfile.getAbsolutePath() + "' does not exist or is not a file");
                System.exit(1);
            }
        } else {
            String settingsfilename = System.getProperty(SETTINGSFILEPATH);
            if (settingsfilename == null) {
                System.out.println("Program is using the default settings");
            } else {
                System.out.println("Using settingsfile '" + settingsfilename + "' defined by setting '" + SETTINGSFILEPATH + "'");
            }
        }
        describeSettings();
        boolean success = accessTest();
        System.out.println("Database accessTest was " + (success? "":"not ") + "successful");

    }
    private static boolean accessTest() {
        String driverClass = DBSpecifics.getInstance().getDriverClassName();
        if (!Settings.verifyClass(driverClass)) {
            return false;
        }
        String jdbcUrl = HarvestDBConnection.getDBUrl();
        try {
            HarvestDBConnection.get();
        } catch (Throwable e) {
            System.out.println("ERROR. Unable to connect to database with the JDBC-url '" + jdbcUrl + "'.");
            System.err.println("The cause: "+ e.getMessage());
            return false;
        }
        System.out.println("Connection to jdbcurl '" + jdbcUrl + "' was successful");
        try {
            HarvestChannelDAO.getInstance();
            HarvestDefinitionDAO.getInstance();
            JobDAO.getInstance();
            TemplateDAO.getInstance();
            ScheduleDAO.getInstance();
            DomainDAO.getInstance();
            RunningJobsInfoDAO.getInstance();
            GlobalCrawlerTrapListDAO.getInstance();
        } catch (Throwable e) {
            System.err.println(e.getMessage());
            return false;
        }
        try {
            ExtendedFieldDAO.getInstance();
            ExtendedFieldTypeDAO.getInstance();
            ExtendedFieldValueDAO.getInstance();
        } catch (Throwable e) {
            System.err.println("WARNING: One of ExtendedField* DAO classes couldn't be instantiated:");
        }

        return true;
    }

    private static void describeSettings() {
        System.out.println("The database settings including the default ones are:");
        printSettings(CommonSettings.DB_SPECIFICS_CLASS);
        printSettings(CommonSettings.DB_BASE_URL);
        printSettings(CommonSettings.DB_DIR); // if postgresql = databasename
        printSettings(CommonSettings.DB_MACHINE);
        printSettings(CommonSettings.DB_IS_DERBY_IF_CONTAINS);
        printSettings(CommonSettings.DB_CONN_VALID_CHECK_TIMEOUT);
        printSettings(CommonSettings.DB_PASSWORD);
        printSettings(CommonSettings.DB_USERNAME);
        printSettings(CommonSettings.DB_PORT);
        printSettings(CommonSettings.DB_POOL_MIN_SIZE);
        printSettings(CommonSettings.DB_POOL_MAX_SIZE);
        printSettings(CommonSettings.DB_POOL_MAX_CONNECTION_AGE);
        printSettings(CommonSettings.DB_POOL_MAX_STM);
        printSettings(CommonSettings.DB_POOL_MAX_STM_PER_CONN);
        printSettings(CommonSettings.DB_POOL_ACQ_INC);
        printSettings(CommonSettings.DB_POOL_IDLE_CONN_TEST_ON_CHECKIN);
        printSettings(CommonSettings.DB_POOL_IDLE_CONN_TEST_PERIOD);
        printSettings(CommonSettings.DB_POOL_IDLE_CONN_TEST_QUERY);
    }

    private static void printSettings(String key) {
        String result = null;
        try {
            result = Settings.get(key);
        } catch (UnknownID e) {
            // Ignore
        }
        System.out.println("Setting '" + key + "': " + ((result == null || result.isEmpty())? "Undefined" : result));
    }

}
