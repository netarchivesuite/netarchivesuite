/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;


/**
 * Contains test information about all harvest definition test data
 *
 */
public class TestInfo {
    protected static final Logger log = Logger.getLogger("dk.netarkivet.harvester.datamodel.TestInfo");
    public static final File TOPDATADIR = new File("./tests/dk/netarkivet/harvester/datamodel/data/");
    public static final File DATADIR = new File(TOPDATADIR, "hadebasedir/");
    public static final File TEMPDIR = new File(TOPDATADIR, "working/");
    public static final String LONG_DOMAIN_LIST = "domainlist/longdomainlist.txt";
    public static final String DOMAIN_LIST = "domainlist/domainlist.txt";
    public static final String INVALID_DOMAIN_LIST = "domainlist/invalid_domainlist.txt";
    public static final String SEEDLISTNAME = "Default-seeds";
    public static final String SEEDLISTNAME_JOB4 = "Default-seedsjob4";
    public static final String SEEDLISTNAME2 = "Default-seeds2";
    public static final String SEEDLISTNAME3 = "Default-seeds3";
    public static final String SEEDLISTPREFIX = "http://www.bt.dk";
    public static final String SEEDLISTPREFIXJOB4 = "http://www.borsen.dk";
    public static final String SEEDS1 = "www.bt.dk\nwww.bt.dk/index.html";
    public static final String SEEDS1_JOB4 = "www.borsen.dk";
    public static final String SEEDS2 = "www.bt.dk\nwww.bt.dk/index.html\nwww.bt.dk/index-uk.html";
    public static final String SEEDS3 = "www.dr.dk/sporten/index.html\n";

    public static final String DFEAULT_PW_LIST_NAME = "default_pw_list";
    public static final SeedList seedlist = new SeedList(SEEDLISTNAME, SEEDS1);

    public static final SeedList seedlistJob4 = new SeedList(SEEDLISTNAME_JOB4,
            SEEDS1_JOB4);
    public static SeedList seedlist2 = new SeedList(SEEDLISTNAME2,
            SEEDS2);
    public static SeedList seedlist3 = new SeedList(SEEDLISTNAME2,
            SEEDS3);
    public static String DEFAULTCFGNAME = "Default";
    public static String DEFAULTCFGNAMEJOB4 = "Default";
    public static String DEFAULT_SCHEDULE_NAME = "DefaultSchedule";
    public static String TEST_SCHEDULE_NAME = "TestSchedule";
    public static String HEREANDNOW_SCHEDULE_NAME = "HereAndNowSchedule";
    public static String DEFAULT_HARVEST_NAME = "DefaultHarvestDef";
    public static String DEFAULT_HARVEST_COMMENT = "DefaultHarvestDefDesc";
    public static final File BASE_DIR_ORDER_XML_TEMPLATES = new File(TEMPDIR, "order_templates");
    public static String BASE_DIR_HERITRIX_JOBS = "jobs/";
    public static String JOB_DIR_PREFIX = "job_";
    public static final String ORDER_XML_NAME = "FullSite-order";
    public static final String ORDER_XML_FILENAME2 = "OneLevel-order";
    public static final String SCHEDULE_XML_FILENAME = "schedule.xml";
    public static final String SETTINGS_XML_FILENAME = "settings.xml";
    public static final String DEFAULTDOMAINNAME = "bt.dk";
    public static final String EXISTINGDOMAINNAME = "dr.dk";
    public static final String DEFAULTNEWDOMAINNAME = "unknowndomain.dk";
    public static final String DEFAULTPREFIX = "http://www.bt.dk";
    public static final String PASSWORD_LIST = "passwords.xml";

    public static final int GUI_WEB_SERVER_PORT = 4242;
    public static final String GUI_WEB_SERVER_WEBBASE = "/test";
    public static final String GUI_WEB_SERVER_JSP_DIRECTORY = "tests/dk/netarkivet/harvester/datamodel/data/jsp";
    public static HourlyFrequency FREQUENCY = new HourlyFrequency(1);
    public static File HARVEST_DEFINITIONS_DIR = new File(TEMPDIR,
            "harvestdefinitions");
    public static File HARVEST_DEFINITION_FILE1 = new File(HARVEST_DEFINITIONS_DIR,
            "harvestdef_42.xml");
    public static final File HARVEST_DEFINITION_FILE2 = new File(HARVEST_DEFINITIONS_DIR,
            "harvestdef_43.xml");
    public static final String HARVESTDEFINITION_WEBBASE = "/HarvestDefinition";
    public static final String HARVESTDEFINITION_JSP_DIR = "webpages/HarvestDefinition";
    public static final int HARVESTDEFINITION_PORT = 4243;

    // Job test information
    public static final Long JOBID = new Long(1234);
    public static final Long HARVESTID = new Long(5678);
    public static final File ORDERXMLFILE = new File(BASE_DIR_ORDER_XML_TEMPLATES,
            "OneLevel-order.xml");
    public static File[] SETTINGSXMLFILES = {
        new File(BASE_DIR_ORDER_XML_TEMPLATES, "FullSite-order.xml"),
        new File(BASE_DIR_ORDER_XML_TEMPLATES, "Max_20_2-order.xml")
    };
    public static Document ORDERXMLDOC = null;
    public static Document[] SETTINGSXMLDOCS = null;
    public static Date START_DATE = new GregorianCalendar(105, 2, 3, 4, 5, 6).getTime();
    public static Date END_DATE = new GregorianCalendar(106, 2, 3, 4, 5, 6).getTime();
    public static Frequency DEFAULT_FREQ = new DailyFrequency(3);
    public static Schedule TESTSCHEDULE = new TimedSchedule(START_DATE, END_DATE,
            DEFAULT_FREQ, TEST_SCHEDULE_NAME, "");
    public static Schedule DEFAULTSCHEDULE = new TimedSchedule(START_DATE, END_DATE,
            DEFAULT_FREQ, DEFAULT_SCHEDULE_NAME, "");
    private static final String TESTSCHEDULE_COMMENTS = "No comment";
    public static Schedule HERE_AND_NOW_SCHEDULE = Schedule.getInstance(null,
            null, new DailyFrequency(1), HEREANDNOW_SCHEDULE_NAME, TESTSCHEDULE_COMMENTS);

    private static final Frequency TWO_HOURLY_FREQUENCY = new HourlyFrequency(2, 42);
    public static final String TESTSCHEDULE_HOURLY_NAME = "Two hours";
    public static Schedule TESTSCHEDULE_HOURLY = Schedule.getInstance(START_DATE, END_DATE,
            TWO_HOURLY_FREQUENCY, TESTSCHEDULE_HOURLY_NAME, TESTSCHEDULE_COMMENTS);
    private static final int NUM_REPEATS = 5;
    private static final Frequency THREE_DAYS_FREQUENCY = new DailyFrequency(3);
    public static final String TESTSCHEDULE_DAILY_NAME = "Three days, five times";
    public static Schedule TESTSCHEDULE_DAILY = Schedule.getInstance(START_DATE, NUM_REPEATS,
            THREE_DAYS_FREQUENCY, TESTSCHEDULE_DAILY_NAME, TESTSCHEDULE_COMMENTS);
    private static final Frequency WEEKLY_FREQUENCY = new WeeklyFrequency(1, 3, 4, 5);
    public static final String TESTSCHEDULE_WEEKLY_NAME = "Weekly";
    public static Schedule TESTSCHEDULE_WEEKLY = Schedule.getInstance(null, null,
            WEEKLY_FREQUENCY, TESTSCHEDULE_WEEKLY_NAME, TESTSCHEDULE_COMMENTS);
    private static final Frequency MONTHLY_FREQUENCY = new MonthlyFrequency(3);
    public static final String TESTSCHEDULE_MONTHLY_NAME = "Quarterly";
    public static Schedule TESTSCHEDULE_MONTHLY = Schedule.getInstance(null, NUM_REPEATS,
            MONTHLY_FREQUENCY, TESTSCHEDULE_MONTHLY_NAME, TESTSCHEDULE_COMMENTS);

    public static final String DOMAIN_NAME = "bt.dk";
    public static final String CONFIGURATION_NAME = "Deep";
    public static final String CONFIGURATION_NAMEJOB4 = "DeepBlue";
    public static final String PASSWORD_NAME = "Secret";
    public static final String PASSWORD_COMMENT = "We don't know where this came from.";
    public static final String PASSWORD_PASSWORD_DOMAIN = "Area51";
    public static final String PASSWORD_REALM = "SecretLaBOratory";
    public static final String PASSWORD_USERNAME = "Mulder";
    public static final String PASSWORD_PASSWORD = "TrustNo1";
    public static Password password = new Password(PASSWORD_NAME, PASSWORD_COMMENT,
            PASSWORD_PASSWORD_DOMAIN, PASSWORD_REALM, PASSWORD_USERNAME,
            PASSWORD_PASSWORD);

    public static final int STATUS_NEW = 0;
    public static final int STATUS_SUBMITTED = 1;
    public static final int STATUS_STARTED = 2;
    public static final int STATUS_DONE = 3;
    public static final int STATUS_FAILED = 4;
    public static final String EXISTINGSCHEDULENAME = "DefaultSchedule";
    public static final Long EXISTINGHARVESTDEFINITIONOID = new Long(42l);
    public static final String SHORTDOMAINNAME = "x.dk";
    public static final int MAX_OBJECTS_PER_DOMAIN = 33;
    public static final File NON_EXISTING_FILE = new File("/no/such/file");
    public static final File TEMP_DOMAIN_DIR = new File(TEMPDIR, "domains");
    public static final File EXISTING_DOMAIN_DIR =
            new File(new File(TEMP_DOMAIN_DIR, "dr"), "dr.dk");
    public static final File EXISTING_DOMAIN_DIR2 =
            new File(new File(TEMP_DOMAIN_DIR, "kb"), "kb.dk");
    public static String JOB_BASE_DIR = TEMPDIR + "/jobs/" + JOB_DIR_PREFIX;

    public static final int NO_OF_TESTDOMAINS = 7;

    public static File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    static final File DBFILE = new File(TOPDATADIR, "fullhddb.jar");
    public static final File NONEXISTINGDIR =
            new File(TEMPDIR, "nonexisting");

    static {
        FileUtils.createDir(new File(JOB_BASE_DIR));
    }

    /**
     * Load resources needed by unit tests
     * <p/>
     * author: SSC
     */
    public TestInfo() {
    }

    /**
     * Create configuration with the specified name and a harvest history where
     * objectCount number of objects were most recently harvested.
     *
     * @param domainName
     * @param configName
     * @param objectCount
     * @return a domainConfiguration
     */
    public static DomainConfiguration createConfig(String domainName, String configName,
                                                    long objectCount) {
        DomainDAO dao = DomainDAO.getInstance();

        DomainConfiguration cfg = dao.read(domainName).getConfiguration(configName);
        Domain d = cfg.getDomain();
        cfg.setMaxObjects(4000);
        cfg.setMaxBytes(-1);
        HarvestInfo hi = new HarvestInfo(new Long(1234), d.getName(),
                cfg.getName(), new Date(), 1L, objectCount, StopReason.DOWNLOAD_COMPLETE);

        cfg.addHarvestInfo(hi);
        dao.update(d);

        return dao.read(domainName).getConfiguration(configName);

    }

    public static void setup() {
        ORDERXMLDOC = getOrderXmlDoc(ORDERXMLFILE);
        SETTINGSXMLDOCS = getSettingsXmlDocs(SETTINGSXMLFILES);
    }

    protected static Document getOrderXmlDoc(File f) {
        SAXReader reader = new SAXReader();

        try {
            Document orderXMLdoc = reader.read(f);
            return orderXMLdoc;
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not read file:" + f + ";", e);
            throw new IOFailure("Could not read file:" + f + ";", e);
        }
    }

    private static Document[] getSettingsXmlDocs(File[] f) {
        SAXReader reader = new SAXReader();
        Document[] docs = new Document[f.length];

        try {
            for (int i = 0; i < f.length; ++i) {
                docs[i] = reader.read(f[i]);
            }

            return docs;
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not read settings file.", e);
            throw new IOFailure("Could not read settings file.", e);
        }
    }

    public static DomainConfiguration getDefaultConfig(Domain wd) {
        return getConfig(wd, DEFAULTCFGNAME);
    }

    public static DomainConfiguration getConfigurationNotDefault(Domain wd) {
        List<SeedList> seedlists = new ArrayList<SeedList>();
        seedlists.add(seedlist);
        DomainConfiguration cfg = new DomainConfiguration("NotDefault", wd,
                seedlists, new ArrayList<Password>());
        cfg.setOrderXmlName(ORDER_XML_NAME);
        cfg.setMaxObjects(12);
        cfg.setMaxRequestRate(11);
        return cfg;
    }

    public static DomainConfiguration getDRConfiguration() {
        DomainDAO dao = DomainDAO.getInstance();
        return dao.read("dr.dk").getConfiguration("fuld_dybde");
    }

    public static DomainConfiguration getNetarkivetConfiguration() {
        DomainDAO dao = DomainDAO.getInstance();
        return dao.read("netarkivet.dk").getConfiguration("fuld_dybde");
    }

    public static Domain getDefaultNewDomain() {
        Domain wd = Domain.getDefaultDomain(DEFAULTNEWDOMAINNAME);
        wd.addSeedList(seedlist);
        return wd;
    }


    public static Domain getDefaultDomain() {
        Domain wd = Domain.getDefaultDomain(DEFAULTDOMAINNAME);
        wd.addSeedList(seedlist);
        return wd;
    }

    public static Domain getDomainNotDefault() {
        Domain wd = Domain.getDefaultDomain(EXISTINGDOMAINNAME);
        wd.addSeedList(seedlist3);
        return wd;
    }

    public static Schedule getDefaultSchedule() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
        cal.setTimeInMillis(0);
        cal.set(2005, Calendar.FEBRUARY, 1, 0, 0, 0);

        Date startDate = new Date(cal.getTime().getTime());
        cal.set(2006, Calendar.FEBRUARY, 1, 0, 0, 0);

        Date endDate = new Date(cal.getTime().getTime());

        return new TimedSchedule(startDate, endDate, new DailyFrequency(100), "DefaultSchedule", "");
    }

    public static List<DomainConfiguration> getAllDefaultConfigurations() {
        DomainConfiguration cfg;
        DomainDAO dao = DomainDAO.getInstance();
        List<DomainConfiguration> configs = new ArrayList<DomainConfiguration>();

        cfg = dao.read("kb.dk").getConfiguration("Asterix_max_20_2");
        configs.add(cfg);

        cfg = dao.read("netarkivet.dk").getConfiguration("Dansk_netarkiv_fuld_dybde");
        configs.add(cfg);

        cfg = dao.read("dr.dk").getConfiguration("fuld_dybde");
        configs.add(cfg);

        cfg = dao.read("statsbiblioteket.dk").getConfiguration("fuld_dybde");
        configs.add(cfg);

        return configs;
    }


    /**
     * Creates a harvest definition where all configurations use
     * the same order.xml.
     * The expected number of objects harvested for each configuration are:
     * asterix.kb.dk: 100
     * netarkivet.dk: 1000
     * www.statsbiblioteket.dk: 2000
     * dr.dk: 4000
     *
     * @return the described definition
     */
    public static PartialHarvest getOneOrderXmlConfig() {
        List<DomainConfiguration> configs = new ArrayList<DomainConfiguration>();
        Schedule schedule = DEFAULTSCHEDULE;

        //Note: The configurations have these expectations:
        //500, 1400, 2400, 4000
        configs.add(createConfig("kb.dk", "fuld_dybde", 112));
        configs.add(createConfig("netarkivet.dk", "fuld_dybde", 1112));
        configs.add(createConfig("statsbiblioteket.dk", "fuld_dybde", 2223));
        configs.add(createConfig("dr.dk", "fuld_dybde", 4445));

        return HarvestDefinition.createPartialHarvest(configs, schedule,
                "SameOrderXml", "All configs. use the same order.xml");
    }

    /**
     * Creates a harvest definition where different order.xml files are used
     * but where the expected number of objects is identical (1000)
     * The following order.xml files are used:
     * asterix.kb.dk:Max_20_2-order.xml
     * netarkivet.dk:OneLevel-order.xml
     * www.statsbiblioteket.dk: FullSite-order.xml
     * dr.dk: FullSite-order.xml
     *
     * @return the described definition
     */
    public static PartialHarvest getMultipleOrderXmlConfig() {
        List<DomainConfiguration> configs = new ArrayList<DomainConfiguration>();
        Schedule schedule = DEFAULTSCHEDULE;
        //Expectation: 1400
        configs.add(createConfig("kb.dk", "Asterix_max_20_2", 1112));
        configs.add(createConfig("netarkivet.dk", "Engelsk_netarkiv_et_niveau", 1112));
        configs.add(createConfig("statsbiblioteket.dk", "fuld_dybde", 1112));
        configs.add(createConfig("dr.dk", "fuld_dybde", 1112));

        return HarvestDefinition.createPartialHarvest(configs, schedule,
                "DifferentOrderXml", "Different order.xml are used");
    }


    public static DomainConfiguration getConfig(Domain d, String s) {
        List<SeedList> seedlists = new ArrayList<SeedList>();
        seedlists.add(seedlist);
        DomainConfiguration cfg = new DomainConfiguration(s, d,
                seedlists, new ArrayList<Password>());
        cfg.setOrderXmlName(ORDER_XML_NAME);
        cfg.setMaxObjects(10);
        cfg.setMaxRequestRate(11);

        return cfg;
    }
}
