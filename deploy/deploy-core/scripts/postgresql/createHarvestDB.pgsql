-- $Id: netarchivesuite_init.sql 1414 2010-05-31 15:52:06Z ngiraud $
-- $Revision: 1414 $
-- $Date: 2010-05-31 17:52:06 +0200 (Mon, 31 May 2010) $
-- $Author: ngiraud $

-- PostgreSQL creation scripts
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

-- Note: it is recommended by DBAs to stores indices in a separate tablespace
-- This script assumes that a tablespace 'tsindex' has been created for this
-- purpose. In order to create a tablespace, the following procedure should be
-- used (tested on Ubuntu 9.10):
--
-- identify the data directory (cf. /etc/postgresql/8.4/main//postgresql.conf)
-- execute the following commands :
--      PG_DATA=/var/lib/postgresql/8.4/main
--      sudo mkdir $PG_DATA/tsindex
--      sudo chown postgres:postgres $PG_DATA/tsindex
-- start psql : psql -U postgres -W
-- execute the query :
--      create tablespace tsindex location '/var/lib/postgresql/8.4/main/tsindex';

--
-- How to use:
-- psql -U <user name> -W [DB name] < netarchivesuite_init.sql


-- *****************************************************************************
-- Area: Basics
-- *****************************************************************************

CREATE TABLE schemaversions (
    tablename varchar(100) NOT NULL,
    version int NOT NULL
);

INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'domains', 3);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'configurations', 5);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'seedlists', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'passwords', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'ownerinfo', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'historyinfo', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'config_passwords', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'config_seedlists', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'harvestdefinitions', 4);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'partialharvests', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'fullharvests', 5);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'harvest_configs', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'schedules', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'ordertemplates', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'jobs', 9);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'job_configs', 1);
INSERT INTO schemaversions (tablename, version )
    VALUES ( 'global_crawler_trap_lists', 1);
INSERT INTO schemaversions (tablename, version )
    VALUES ( 'global_crawler_trap_expressions', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'runningjobshistory', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'runningjobsmonitor', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'frontierreportmonitor', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfieldtype', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfield', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfieldvalue', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfieldhistoryvalue', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'harvestchannel', 1);


GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE schemaversions TO netarchivesuite;

-- *****************************************************************************
-- Area: Domains
-- *****************************************************************************

CREATE TABLE domains (
    domain_id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL UNIQUE,
    comments varchar(30000),
    defaultconfig bigint NOT NULL,
    crawlertraps text,
    edition bigint NOT NULL,
    alias bigint,
    lastaliasupdate timestamp
);

CREATE SEQUENCE domains_id_seq OWNED BY domains.domain_id;
ALTER TABLE domains ALTER COLUMN domain_id SET DEFAULT NEXTVAL('domains_id_seq');

CREATE INDEX domainnameid on domains(name) TABLESPACE tsindex;
CREATE INDEX aliasindex on domains(alias) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE domains TO netarchivesuite;
GRANT USAGE ON SEQUENCE domains_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE configurations (
    config_id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL,
    comments varchar(30000),
    domain_id bigint NOT NULL,
    template_id bigint NOT NULL,
    maxobjects bigint NOT NULL DEFAULT -1,
    maxrate int,
    overridelimits bigint,
    maxbytes bigint NOT NULL DEFAULT -1
);

CREATE SEQUENCE configurations_id_seq OWNED BY configurations.config_id;
ALTER TABLE configurations ALTER COLUMN config_id SET DEFAULT NEXTVAL('configurations_id_seq');

CREATE INDEX configurationname on configurations(name) TABLESPACE tsindex;
CREATE INDEX configurationmaxbytes on configurations(maxbytes) TABLESPACE tsindex;
CREATE INDEX configdomain on configurations(domain_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE configurations TO netarchivesuite;
GRANT USAGE ON SEQUENCE configurations_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE config_passwords (
    config_id bigint NOT NULL,
    password_id int NOT NULL,
    PRIMARY KEY (config_id, password_id)
);

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE config_passwords TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE config_seedlists (
    config_id bigint NOT NULL,
    seedlist_id int NOT NULL,
    PRIMARY KEY (config_id, seedlist_id)
);

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE config_seedlists TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE seedlists (
    seedlist_id bigint NOT NULL PRIMARY KEY,
    name varchar (300) NOT NULL,
    comments varchar(30000),
    domain_id bigint NOT NULL,
    seeds text NOT NULL
);

CREATE SEQUENCE seedlists_id_seq OWNED BY seedlists.seedlist_id;
ALTER TABLE seedlists ALTER COLUMN seedlist_id SET DEFAULT NEXTVAL('seedlists_id_seq');

CREATE INDEX seedlistname on seedlists(name) TABLESPACE tsindex;
CREATE INDEX seedlistdomain on seedlists(domain_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE seedlists TO netarchivesuite;
GRANT USAGE ON SEQUENCE seedlists_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE passwords (
    password_id bigint NOT NULL PRIMARY KEY,
    name varchar (300) NOT NULL,
    comments varchar(30000),
    domain_id bigint NOT NULL,
    url varchar(300) NOT NULL,
    realm varchar(300) NOT NULL,
    username varchar(20) NOT NULL,
    password varchar(40) NOT NULL
);

CREATE SEQUENCE passwords_id_seq OWNED BY passwords.password_id;
ALTER TABLE passwords ALTER COLUMN password_id SET DEFAULT NEXTVAL('passwords_id_seq');

CREATE INDEX passwordname on passwords(name) TABLESPACE tsindex;
CREATE INDEX passworddomain on passwords(domain_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE passwords TO netarchivesuite;
GRANT USAGE ON SEQUENCE passwords_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE ownerinfo (
    ownerinfo_id bigint NOT NULL PRIMARY KEY,
    domain_id bigint NOT NULL,
    created timestamp NOT NULL,
    info varchar(1000) NOT NULL
);

CREATE SEQUENCE ownerinfo_id_seq OWNED BY ownerinfo.ownerinfo_id;
ALTER TABLE ownerinfo ALTER COLUMN ownerinfo_id SET DEFAULT NEXTVAL('ownerinfo_id_seq');

CREATE INDEX ownerinfodomain on ownerinfo(domain_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE ownerinfo TO netarchivesuite;
GRANT USAGE ON SEQUENCE ownerinfo_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE historyinfo (
    historyinfo_id bigint NOT NULL PRIMARY KEY,
    stopreason int NOT NULL,
    objectcount bigint NOT NULL,
    bytecount bigint NOT NULL,
    config_id bigint NOT NULL,
    harvest_id bigint NOT NULL,
    job_id bigint,
    harvest_time timestamp NOT NULL
);

CREATE SEQUENCE historyinfo_id_seq OWNED BY historyinfo.historyinfo_id;
ALTER TABLE historyinfo ALTER COLUMN historyinfo_id SET DEFAULT NEXTVAL('historyinfo_id_seq');

CREATE INDEX historyinfoharvest on historyinfo (harvest_id) TABLESPACE tsindex;
CREATE INDEX historyinfoconfigharvest on historyinfo (config_id,harvest_id) TABLESPACE tsindex;
CREATE INDEX historyinfojobharvest on historyinfo (job_id,harvest_id) TABLESPACE tsindex;
CREATE INDEX historyinfoharvestconfig on historyinfo (harvest_id,config_id) TABLESPACE tsindex;
CREATE INDEX historyinfoconfig on historyinfo(config_id) TABLESPACE tsindex;
CREATE INDEX historyinfojob on historyinfo(job_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE historyinfo TO netarchivesuite;
GRANT USAGE ON SEQUENCE historyinfo_id_seq TO netarchivesuite;

-- *****************************************************************************
-- Area: Harvest Definitions
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE harvestdefinitions (
     harvest_id bigint NOT NULL PRIMARY KEY,
     name varchar(300) NOT NULL UNIQUE,
     comments varchar(30000),
     numevents int NOT NULL,
     submitted timestamp NOT NULL,
     isactive bool NOT NULL,
     edition bigint NOT NULL,
     audience varchar(100)
);

CREATE INDEX harvestdefinitionssubmitdate on harvestdefinitions (submitted) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE harvestdefinitions TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE fullharvests (
     harvest_id bigint NOT NULL PRIMARY KEY,
     maxobjects bigint NOT NULL default -1,
     previoushd bigint,
     maxbytes bigint NOT NULL default -1,
     maxjobrunningtime bigint NOT NULL default 0,
     isindexready bool NOT NULL
);

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE fullharvests TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE partialharvests (
     harvest_id bigint NOT NULL PRIMARY KEY,
     schedule_id bigint NOT NULL,
     nextdate timestamp
);

CREATE INDEX partialharvestsnextdate on partialharvests (nextdate) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE partialharvests TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE harvest_configs (
     harvest_id bigint NOT NULL,
     config_id bigint NOT NULL,
     PRIMARY KEY ( harvest_id, config_id )
);

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE harvest_configs TO netarchivesuite;

-- *****************************************************************************
-- Area: Schedules
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE schedules (
    schedule_id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL UNIQUE,
    comments varchar(30000),
    startdate timestamp,
    enddate timestamp,
    maxrepeats bigint,
    timeunit int NOT NULL,
    numtimeunits bigint NOT NULL,
    anytime bool NOT NULL,
    onminute int,
    onhour int,
    ondayofweek int,
    ondayofmonth int,
    edition bigint NOT NULL
);

CREATE SEQUENCE schedules_id_seq OWNED BY schedules.schedule_id;
ALTER TABLE schedules ALTER COLUMN schedule_id SET DEFAULT NEXTVAL('schedules_id_seq');

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE schedules TO netarchivesuite;
GRANT USAGE ON SEQUENCE schedules_id_seq TO netarchivesuite;

-- *****************************************************************************
-- Area: Templates
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE ordertemplates (
    template_id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL UNIQUE,
    orderxml text NOT NULL
);

CREATE SEQUENCE ordertemplates_id_seq OWNED BY ordertemplates.template_id;
ALTER TABLE ordertemplates ALTER COLUMN template_id SET DEFAULT NEXTVAL('ordertemplates_id_seq');

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE ordertemplates TO netarchivesuite;
GRANT USAGE ON SEQUENCE ordertemplates_id_seq TO netarchivesuite;

-- *****************************************************************************
-- Area: Jobs
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE jobs (
    job_id bigint NOT NULL PRIMARY KEY,
    harvest_id bigint NOT NULL,
    status int NOT NULL,
    priority int NOT NULL,
    forcemaxbytes bigint NOT NULL default -1,
    forcemaxcount bigint,
    orderxml varchar(300) NOT NULL,
    orderxmldoc text NOT NULL,
    seedlist text NOT NULL,
    harvest_num int NOT NULL,
    harvest_errors varchar(300),
    harvest_error_details varchar(10000),
    upload_errors varchar(300),
    upload_error_details varchar(10000),
    startdate timestamp,
    enddate timestamp,
    num_configs int NOT NULL default 0,
    edition bigint NOT NULL,
    submitteddate timestamp,
    creationdate timestamp,
    resubmitted_as_job bigint,
    continuationof bigint,
    forcemaxrunningtime bigint NOT NULL DEFAULT 0,
    harvestname_prefix varchar(100)	
);


CREATE INDEX jobstatus on jobs(status) TABLESPACE tsindex;
CREATE INDEX jobharvestid on jobs(harvest_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE jobs TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE job_configs (
    job_id bigint NOT NULL,
    config_id bigint NOT NULL,
    PRIMARY KEY ( job_id, config_id )
);

CREATE INDEX jobconfigjob on job_configs(job_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE job_configs TO netarchivesuite;

-- *****************************************************************************
-- Area: Global Crawler traps
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE global_crawler_trap_lists(
    global_crawler_trap_list_id bigint NOT NULL PRIMARY KEY,
    name VARCHAR(300) NOT NULL UNIQUE,
    description VARCHAR(20000),
    isActive bool NOT NULL);

CREATE SEQUENCE global_crawler_trap_list_id_seq
OWNED BY global_crawler_trap_lists.global_crawler_trap_list_id;

ALTER TABLE global_crawler_trap_lists ALTER COLUMN global_crawler_trap_list_id
SET DEFAULT NEXTVAL('global_crawler_trap_list_id_seq');

CREATE INDEX gctlistsid on global_crawler_trap_lists(global_crawler_trap_list_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE global_crawler_trap_lists TO netarchivesuite;
GRANT USAGE ON SEQUENCE global_crawler_trap_list_id_seq TO netarchivesuite;

-- -----------------------------------------------------------------------------
CREATE TABLE global_crawler_trap_expressions(
    id bigint not null PRIMARY KEY,
    crawler_trap_list_id bigint NOT NULL,
    trap_expression VARCHAR(1000));

CREATE SEQUENCE global_crawler_trap_expressions_id_seq
OWNED BY global_crawler_trap_expressions.id;

ALTER TABLE global_crawler_trap_expressions ALTER COLUMN id
SET DEFAULT NEXTVAL('global_crawler_trap_expressions_id_seq');

CREATE INDEX gctexprid on global_crawler_trap_expressions(id) TABLESPACE tsindex;
CREATE INDEX gctexprlistid on global_crawler_trap_expressions(crawler_trap_list_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE global_crawler_trap_expressions TO netarchivesuite;
GRANT USAGE ON SEQUENCE global_crawler_trap_expressions_id_seq TO netarchivesuite;

-- *****************************************************************************
-- Area: Running Jobs Progress History
-- *****************************************************************************

-- -----------------------------------------------------------------------------

-- This table contains the archived progress information reported by the running
-- jobs, controlled by a sample rate
CREATE TABLE runningJobsHistory (
     jobId bigint NOT NULL,
     harvestName varchar(300) NOT NULL,
     hostUrl varchar(300) NOT NULL,
     progress numeric NOT NULL,
     queuedFilesCount bigint NOT NULL,
     totalQueuesCount bigint NOT NULL,
     activeQueuesCount bigint NOT NULL,
     retiredQueuesCount bigint NOT NULL,
     exhaustedQueuesCount bigint NOT NULL,
     elapsedSeconds bigint NOT NULL,
     alertsCount bigint NOT NULL,
     downloadedFilesCount bigint NOT NULL,
     currentProcessedKBPerSec integer NOT NULL,
     processedKBPerSec integer NOT NULL,
     currentProcessedDocsPerSec numeric NOT NULL,
     processedDocsPerSec numeric NOT NULL,
     activeToeCount integer NOT NULL,
     status integer NOT NULL,
     tstamp timestamp NOT NULL,
     CONSTRAINT pkRunningJobsHistory PRIMARY KEY (jobId, harvestName, elapsedSeconds, tstamp)
);

CREATE INDEX runningJobsHistoryCrawlJobId on runningJobsHistory (jobId) TABLESPACE tsindex;
CREATE INDEX runningJobsHistoryCrawlTime on runningJobsHistory (elapsedSeconds) TABLESPACE tsindex;
CREATE INDEX runningJobsHistoryHarvestName on runningJobsHistory (harvestName) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE runningJobsHistory TO netarchivesuite;

-- This table contains the latest progress information reported by the job,
-- for evey job
CREATE TABLE runningJobsMonitor (
     jobId bigint NOT NULL,
     harvestName varchar(300) NOT NULL,
     hostUrl varchar(300) NOT NULL,
     progress numeric NOT NULL,
     queuedFilesCount bigint NOT NULL,
     totalQueuesCount bigint NOT NULL,
     activeQueuesCount bigint NOT NULL,
     retiredQueuesCount bigint NOT NULL,
     exhaustedQueuesCount bigint NOT NULL,
     elapsedSeconds bigint NOT NULL,
     alertsCount bigint NOT NULL,
     downloadedFilesCount bigint NOT NULL,
     currentProcessedKBPerSec integer NOT NULL,
     processedKBPerSec integer NOT NULL,
     currentProcessedDocsPerSec numeric NOT NULL,
     processedDocsPerSec numeric NOT NULL,
     activeToeCount integer NOT NULL,
     status integer NOT NULL,
     tstamp timestamp NOT NULL,
     CONSTRAINT pkRunningJobsMonitor PRIMARY KEY (jobId, harvestName)
);

CREATE INDEX runningJobsMonitorJobId on runningJobsMonitor (jobId) TABLESPACE tsindex;
CREATE INDEX runningJobsMonitorHarvestName on runningJobsMonitor (harvestName) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE runningJobsMonitor TO netarchivesuite;

-- This table contains frontier report data
CREATE TABLE frontierReportMonitor (
     jobId bigint NOT NULL,
     filterId varchar(200) NOT NULL,
     tstamp timestamp NOT NULL,
     domainName varchar(300) NOT NULL,
     currentSize bigint NOT NULL,
     totalEnqueues bigint NOT NULL,
     sessionBalance bigint NOT NULL,
     lastCost numeric NOT NULL,
     averageCost numeric NOT NULL,
     lastDequeueTime varchar(100) NOT NULL,
     wakeTime varchar(100) NOT NULL,
     totalSpend bigint NOT NULL,
     totalBudget bigint NOT NULL,
     errorCount bigint NOT NULL,
     lastPeekUri varchar(1000) NOT NULL,
     lastQueuedUri varchar(1000) NOT NULL,
     CONSTRAINT pkFrontierReportLines UNIQUE (jobId, filterId, domainName)
);

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE frontierReportMonitor TO netarchivesuite;

create table extendedfieldtype (
    extendedfieldtype_id bigint not null primary key,
    name VARCHAR(50) not null
);

create table extendedfield (
    extendedfield_id bigint not null primary key,
    extendedfieldtype_id bigint NOT NULL,
    name VARCHAR(50) not null,
    format VARCHAR(50),
    defaultvalue VARCHAR(50),
    options VARCHAR(1000),
    datatype int not null,
    mandatory int NOT NULL,
    sequencenr int
);

create table extendedfieldvalue (
    extendedfieldvalue_id bigint not null primary key,
    extendedfield_id bigint NOT NULL,
    instance_id bigint NOT NULL,
    content VARCHAR(100) not null
);

INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 1, 'domains');
INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 2, 'harvestdefinitions');

CREATE TABLE harvestchannel (
    id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL UNIQUE,
    isdefault bool NOT NULL,
    comments varchar(30000)
);

CREATE SEQUENCE harvestchannel_id_seq OWNED BY harvestchannel.id;
ALTER TABLE harvestchannel ALTER COLUMN id SET DEFAULT NEXTVAL('harvestchannel_id_seq');

CREATE INDEX harvestchannelnameid on harvestchannel(name) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE harvestchannel TO netarchivesuite;
GRANT USAGE ON SEQUENCE harvestchannel_id_seq TO netarchivesuite;
