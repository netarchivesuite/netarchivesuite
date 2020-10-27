-- The Netarchive Suite - Software to harvest and preserve websites
-- Copyright (C) 2005 - 2018 The Royal Danish Library, 
--            the National Library of France and the Austrian National Library.

-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Lesser General Public License as
-- published by the Free Software Foundation, either version 2.1 of the
-- License, or (at your option) any later version.
 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Lesser Public License for more details.

-- You should have received a copy of the GNU General Lesser Public
-- License along with this program.  If not, see <http://www.gnu.org/licenses/lgpl-2.1.html>.

-------------------------------------------------------------------------------------------------------------

-- PostgreSQL creation scripts
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

-- Note: it is recommended by DBAs to stores indices in a separate tablespace
-- This script assumes that a tablespace 'tsindex' has been created for this
-- purpose. In order to create a tablespace, the following procedure should be
-- used (tested on Ubuntu 9.10):
--

-- Step 1. This is mandatory, but it makes it simpler to access the database
-- As root or postgres user modify pg_hba.conf so 'ident' is replaced by 'trust' for all rules. So we get a pg_hba.conf looking something like this

--    host    all   all         127.0.0.1/32          trust
--    local   all   all                               trust

-- Step 2. Create the database 'harvestdb', a user 'netarchivesuite', and a tablespace 'tsindex'

--  sudo su - postgres
--  mkdir /var/lib/pgsql/tsindex
--  psql
--   CREATE DATABASE harvestdb WITH ENCODING 'UTF8';
--   CREATE USER netarchivesuite WITH PASSWORD 'netarchivesuite';
--   CREATE TABLESPACE tsindex OWNER netarchivesuite LOCATION '/var/lib/pgsql/tsindex';
--   \q
--  pg_ctl reload

-- Step 3. Create database tables using this script
-- psql harvestdb -U netarchivesuite < 5.2.2/scripts/sql/createHarvestDB.pgsql

-- Step 4. Add Default order xml to ordertemplates table (can only be done after installation is complete)
--  cd INSTALLDIR/scripts
--  cp HarvestTemplateApplication-template.sh HarvestTemplateApplication.sh
--  Update the line 'export INSTALLDIR=/home/test/QUICKSTART' to match your INSTALLDIR
--  bash HarvestTemplateApplication.sh  create default_orderxml order_templates_dist/default_orderxml.xml  

-- Step 5. Netarchivesuite should now be able to use postgresql as its harvestdatabase.


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
    VALUES ( 'ordertemplates', 2);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'jobs', 10);
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
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'eav_attribute', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'eav_type_attribute', 1);

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
     channel_id bigint,
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

-- ****************************** insert default schedules ************************
INSERT INTO schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition)
  VALUES (1, 'Once_a_day', 'Run once every day',    null,      null,    null,       2,        1,            true,       null,     null,   null,        null,         1);
INSERT INTO schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition)
  VALUES (2, 'Once_a_month', 'Run once every month',null,      null,    null,       4,        1,            true,       null,     null,   null,        null,         1);
INSERT INTO schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition)
  VALUES (3, 'Once_a_week', 'Run once every week',  null,      null,    null,       3,        1,            true,       null,     null,   null,        null,         1);
INSERT INTO schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition)
  VALUES (4, 'Once_an_hour', 'Run every hour',      null,      null,    null,       1,        1,            true,       null,     null,   null,        null,         1);
INSERT INTO schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition)
  VALUES (5, 'Once', 'Only run once',               null,      null,    1,          1,        1,            true,       null,     null,   null,        null,         1);
 

-- *****************************************************************************
-- Area: Templates
-- *****************************************************************************

-- -----------------------------------------------------------------------------
CREATE TABLE ordertemplates (
    template_id bigint NOT NULL PRIMARY KEY,
    name varchar(300) NOT NULL UNIQUE,
    orderxml text NOT NULL,
    isActive bool NOT NULL DEFAULT TRUE
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
    channel varchar(300) NOT NULL,
    snapshot bool NOT NULL,
    forcemaxbytes bigint NOT NULL default -1,
    forcemaxcount bigint,
    forcemaxrunningtime bigint NOT NULL DEFAULT 0,
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
    submitteddate timestamp,
    creationdate timestamp,
    resubmitted_as_job bigint, 
    num_configs int NOT NULL default 0,
    edition bigint NOT NULL,
    continuationof bigint,
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
    issnapshot boolean NOT NULL,
    isdefault bool NOT NULL,
    comments varchar(30000)
);


CREATE SEQUENCE harvestchannel_id_seq OWNED BY harvestchannel.id;
ALTER TABLE harvestchannel ALTER COLUMN id SET DEFAULT NEXTVAL('harvestchannel_id_seq');

CREATE INDEX harvestchannelnameid on harvestchannel(name) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE harvestchannel TO netarchivesuite;
GRANT USAGE ON SEQUENCE harvestchannel_id_seq TO netarchivesuite;

-- insert harvestchannel used by the quickstart system 
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments) 
    VALUES ('FOCUSED', false, true, 'Channel for focused harvests');
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments) 
    VALUES ('SNAPSHOT', true, true, 'Channel for snapshot harvests');

CREATE TABLE eav_type_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL,
	name VARCHAR(96) NOT NULL,
	class_namespace VARCHAR(96) NOT NULL,
	class_name VARCHAR(96) NOT NULL,
	datatype INTEGER NOT NULL,
	viewtype INTEGER NOT NULL,
	def_int INTEGER NULL,
	def_datetime TIMESTAMP NULL,
	def_varchar VARCHAR(8000) NULL,
	def_text TEXT NULL
);

ALTER TABLE eav_type_attribute ADD CONSTRAINT eav_type_attribute_pkey PRIMARY KEY (tree_id, id);

CREATE UNIQUE INDEX eav_type_attribute_idx on eav_type_attribute(tree_id, id) TABLESPACE tsindex;

CREATE SEQUENCE eav_attribute_seq;

CREATE TABLE eav_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL DEFAULT NEXTVAL('eav_attribute_seq'),
	entity_id INTEGER NOT NULL,
	type_id INTEGER NOT NULL,
	val_int INTEGER NULL,
	val_datetime TIMESTAMP NULL,
	val_varchar VARCHAR(8000) NULL,
	val_text TEXT NULL
);

ALTER TABLE eav_attribute ADD CONSTRAINT eav_attribute_pkey PRIMARY KEY (tree_id, id);

CREATE INDEX eav_attribute_idx on eav_attribute(tree_id, entity_id) TABLESPACE tsindex;

GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE eav_attribute TO netarchivesuite;
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE eav_type_attribute TO netarchivesuite;
GRANT USAGE ON SEQUENCE eav_attribute_seq TO netarchivesuite;

--
-- INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
-- VALUES(1, 1, 'MAX_HOPS', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 1, 20, null, null, null);
--
-- INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
-- VALUES(1, 2, 'HONOR_ROBOTS_DOT_TXT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 6, 0, null, null, null);
--
-- INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
-- VALUES(1, 3, 'EXTRACT_JAVASCRIPT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 5, 1, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 1, 'MAX_HOPS', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 1, 20, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 2, 'HONOR_ROBOTS_DOT_TXT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 6, 0, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 3, 'EXTRACT_JAVASCRIPT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 5, 1, null, null, null);



