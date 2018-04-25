-- File:        $Id$
-- Revision:    $Revision$
-- Author:      $Author$
-- Date:        $Date$
--
-- The Netarchive Suite - Software to harvest and preserve websites
-- Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
--
-- This library is free software; you can redistribute it and/or
-- modify it under the terms of the GNU Lesser General Public
-- License as published by the Free Software Foundation; either
-- version 2.1 of the License, or (at your option) any later version.
--
-- This library is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
-- Lesser General Public License for more details.
--
-- You should have received a copy of the GNU Lesser General Public
-- License along with this library; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA


-- This script will create, but not populate, a Derby database for the
-- web archiving system.  The database will be created in the current
-- directory under the name 'fullhddb'.

-- To run this, execute java org.apache.derby.tools.ij < createfullhddb.sql
-- with derbytools.jar and derby.jar in your classpath.

-- Whenever a table schema is changed, the version in the schemaversions table
-- should be upped by one and the corresponding check in the DAO should be
-- updated to enforce consistency and allow automated changes.


connect 'jdbc:derby:fullhddb;create=true';

--***************************************************************************--
-- Area: Basics
-- Contains metadata for the database itself.
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    schemaversions
-- Descr.:  This table contains an overview of tables and which version they
--          belong to
-- Purpose: This table allows automatic change of tables as they evolve.
--          When starting a DAO, it must check the version of the table.
--          If it is not the newest version, then it runs the necessary SQL
--          statements to update to the newest version.
create table schemaversions (
    tablename varchar(100) not null, -- Name of table
    version int not null             -- Version of table
);

insert into schemaversions ( tablename, version )
    values ( 'domains', 3);
insert into schemaversions ( tablename, version )
    values ( 'configurations', 5);
insert into schemaversions ( tablename, version )
    values ( 'seedlists', 1);
insert into schemaversions ( tablename, version )
    values ( 'passwords', 1);
insert into schemaversions ( tablename, version )
    values ( 'ownerinfo', 1);
insert into schemaversions ( tablename, version )
    values ( 'historyinfo', 2);
insert into schemaversions ( tablename, version )
    values ( 'config_passwords', 1);
insert into schemaversions ( tablename, version )
    values ( 'config_seedlists', 1);
insert into schemaversions ( tablename, version )
    values ( 'harvestdefinitions', 3);
insert into schemaversions ( tablename, version )
    values ( 'partialharvests', 1);
insert into schemaversions ( tablename, version )
    values ( 'fullharvests', 5);
insert into schemaversions ( tablename, version )
    values ( 'harvest_configs', 1);
insert into schemaversions ( tablename, version )
    values ( 'schedules', 1);
insert into schemaversions ( tablename, version )
    values ( 'ordertemplates', 2);
insert into schemaversions ( tablename, version )
    values ( 'jobs', 9);
insert into schemaversions ( tablename, version )
    values ( 'job_configs', 1);
insert into schemaversions (tablename, version )
    values ( 'global_crawler_trap_lists', 1);
insert into schemaversions (tablename, version )
    values ( 'global_crawler_trap_expressions', 1);
insert into schemaversions ( tablename, version )
    values ( 'runningjobshistory', 2);
insert into schemaversions ( tablename, version )
    values ( 'runningjobsmonitor', 2);
insert into schemaversions ( tablename, version )
    values ( 'frontierreportmonitor', 1);

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

--***************************************************************************--
-- Area: Domains
-- Contains data on individual domains, including their seeds, ways of
-- harvesting them, and history of what has previously been harvested from them
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    domains
-- Descr.:  This table contains the basic domain information.
-- Expected entry count: Many
create table domains (
    domain_id bigint not null generated always as identity primary key,
                                       -- Unique id for the domain
    name varchar(300) not null unique, -- Name of the domain
    comments varchar(30000),           -- Comments on domain, if any
    defaultconfig bigint not null,     -- Configuration used for snapshot
                                       --  harvests
    crawlertraps clob(64M),        -- Regexp(s) for excluded urls.
    edition bigint not null,           -- Marker for optimistic locking by
                                       --  web interface
    alias bigint,                      -- Domain that this domain is an alias
                                       --  of. Null, if this domain is not an
                                       --  alias.
    lastaliasupdate timestamp          -- Last time a user has verified that
                                       --  this domain is an alias. Null, if
                                       --  this domain is not an alias.
);

create index domainnameid on domains(domain_id, name);
create index aliasindex on domains(alias);

-------------------------------------------------------------------------------
-- Name:    configurations
-- Descr.:  This table contains information about configurations. All
--          configurations from all domains are represented here.
create table configurations (
     config_id bigint not null generated always as identity primary key,
                                     -- Unique id for the configuration
     name varchar(300) not null,     -- Name of the configuration
     comments varchar(30000),        -- User-defined comments
     domain_id bigint not null,      -- Identification of domain it belongs
                                     --  to (represented in table domains)
     template_id bigint not null,    -- Reference to template order.xml
                                     --  (represented in table ordertemplates)
     maxobjects bigint not null default -1, -- Max count of objects from the domain
     maxrate int,                    -- Max connections per minute
     overridelimits int,             -- True if the configuration's limits
                                     --  should apply in snapshot harvests
     maxbytes bigint not null default -1 -- Maximum number of bytes to harvest
                                         --  with this configuration.
                                         -- -1 means no limit.
 );

 create index configurationname on configurations(name);
 create index configurationmaxbytes on configurations(maxbytes);
 create index configdomain on configurations(domain_id);

-------------------------------------------------------------------------------
-- Name:    config_passwords
-- Descr.:  This table contains relations between configurations and passwords
--          Note the following:
--          1. the domain relation in the referenced configuration must be the
--             same as the domain relation in the referenced password.
--          2. Not all passwords belong to a configuration
--          3. Even though it is possible to make many-to-many relations there
--             are in reality a one (configuration) to many (passwords)
--             relation
create table config_passwords (
    config_id bigint not null, -- Reference to table configurations
    password_id int not null,  -- Reference to table passwords
    primary key (config_id, password_id)
);

-------------------------------------------------------------------------------
-- Name:    config_seedlists
-- Descr.:  This table contains relations between configurations and seed lists
--          Note the following:
--          1. the domain relation in the referenced configuration must be the
--             same as the domain relation in the referenced seedlist.
--          2. Not all seedlists belong to a configuration
--          3. Even though it is possible to make many-to-many relations there
--             are in reality a one (configuration) to many (seedlists)
--             relation
create table config_seedlists (
    config_id bigint not null, -- Reference to table configurations
    seedlist_id int not null,  -- Reference to table seedlists
    primary key (config_id, seedlist_id)
);

-------------------------------------------------------------------------------
-- Name:    seedlists
-- Descr.:  This table contains all seed lists for all configurations.
create table seedlists (
    seedlist_id bigint not null generated always as identity primary key,
                                -- Unique id for the seed list
    name varchar(300) not null, -- Name of the seed list
    comments varchar(30000),    -- User-defined comments
    domain_id bigint not null,  -- The domain that the seed list belongs to
    seeds clob(8M) not null     -- Seed list, newline-seperated
);

create index seedlistname on seedlists(name);
create index seedlistdomain on seedlists(domain_id);

-------------------------------------------------------------------------------
-- Name:    passwords
-- Descr.:  This table contains all passwords for all configurations.
--          It currently only supports HTTP-style passwords:
--          (URL+realm+username+password).
-- Expected entry count: Few
create table passwords (
    password_id bigint not null generated always as identity primary key,
                                   -- Unique id for the password information
    name varchar(300) not null,    -- An indicative name for the password
    comments varchar(30000),       -- User-defined comments
    domain_id bigint not null,     -- The domain that the password belongs to
    url varchar(300) not null,     -- URL for entry to the protected area
    realm varchar(300) not null,   -- Name of the password realm
    username varchar(20) not null, -- User name used for login
    password varchar(40) not null  -- The actual password for login
);

create index passwordname on passwords(name);
create index passworddomain on passwords(domain_id);

-------------------------------------------------------------------------------
-- Name:    ownerinfo
-- Descr.:  This table contains the owner information on domains
create table ownerinfo (
    ownerinfo_id bigint not null generated always as identity primary key,
                                -- Unique id for owner information
    domain_id bigint not null,  -- The domain that the information relates to
    created timestamp not null, -- When was the information entry created
    info varchar(1000) not null -- The actual owner information, e.g., WHOIS
                                --  information
);

create index ownerinfodomain on ownerinfo(domain_id);

-------------------------------------------------------------------------------
-- Name:    historyinfo
-- Descr.:  This table contains historical information about domains
create table historyinfo (
    historyinfo_id bigint not null generated always as identity primary key,
                                    -- Unique id for history information
    stopreason int not null,        -- Reason for stopping is defined and
                                    --  documented in StopReason.java
    objectcount bigint not null,    -- Count of collected objects
    bytecount bigint not null,      -- Count of collected bytes
    config_id bigint not null,      -- Configuration for the harvest
    harvest_id bigint not null,     -- Identification of harvest the
                                    --  information origins from - must be the
                                    --  same as the one given via the jobs table
    job_id bigint,                  -- Identification of job the information
                                    --  origins from
    harvest_time timestamp not null -- Time when the harvest was done
);

 create index historyinfoharvest on historyinfo (harvest_id);
 create index historyinfoconfigharvest on historyinfo (config_id,harvest_id);
 create index historyinfojobharvest on historyinfo (job_id,harvest_id);
 create index historyinfoharvestconfig on historyinfo (harvest_id,config_id);
 create index historyinfoconfig on historyinfo(config_id);
 create index historyinfojob on historyinfo(job_id);

--***************************************************************************--
-- Area: Harvest Definitions
-- Contains defined harvests, both snapshot and selective harvests, including
-- when they have previously been run.
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    harvestdefinitions
-- Descr.:  This table contains harvest definitions
-- Purpose: This table is used, together with table fullharvests and
--          partialharvests, to contain all information to make Harvest
--          Definition objects. For historical reasons, the harvest_id field
--          is not autogenerated, but is created programmatically.
create table harvestdefinitions (
    harvest_id bigint not null primary key, -- Unique id for the harvest
                                            --  definition
    name varchar(300) not null unique, -- Name of the harvest definition
    comments varchar(30000),    -- User-defined comments
    numevents int not null,     -- How many times the harvest is scheduled for
    submitted timestamp not null, -- Time when harvest definition was first
                                  --  created
    isactive int not null,      -- Indicates whether the harvest is activated
                                --  and will be run at the scheduled time
    edition bigint not null,     -- Marker for optimistic locking by web
                                 --  interface
    audience varchar(100),
    channel_id BIGINT
);

create index harvestdefinitionssubmitdate on harvestdefinitions (submitted);

-------------------------------------------------------------------------------
-- Name:    fullharvests
-- Descr.:  This table contains additional information about snapshot harvests
create table fullharvests (
    harvest_id bigint not null primary key, -- Unique id for harvest definition
    maxobjects bigint not null,             -- Count of max objects per domain
    previoushd bigint,        -- Harvest that this snapshot harvest is based on
    maxbytes bigint default -1, -- Maximum number of bytes to harvest per domain
    maxjobrunningtime bigint default 0, -- maximum snapshot running time
                                       -- (0 means no limit)
    isindexready int not null default 0 -- 0 means not ready, 1 means ready
);

-------------------------------------------------------------------------------
-- Name:    partialharvests
-- Descr.:  This table contains additional information for selective/event
--          harvests
create table partialharvests (
    harvest_id bigint not null primary key, -- Unique id for the selective/
                                            --  event harvest definition
    schedule_id bigint not null, -- Schedule for the selective/event harvest
    nextdate timestamp           -- Time when the selective/event harvest is to
                                 --  run next time
);

create index partialharvestsnextdate ON partialharvests (nextdate);

-------------------------------------------------------------------------------
-- Name:    harvest_configs
-- Descr.:  This table contains relations between harvest definitions and
--          configurations.
--          Note that even though it is possible to make many-to-many relations
--          there are in reality a one (harvestdefinition) to many
--          (configurations) relation
create table harvest_configs (
    harvest_id bigint not null, -- Reference to table harvestdefinitions
    config_id bigint not null,  -- Reference to table configurations
    primary key ( harvest_id, config_id )
);

--***************************************************************************--
-- Area: Schedules
-- Contains schedules determining when selective harvests get run. Snapshot
-- harvests do not use schedules, as they are only run once each.
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    schedules
-- Descr.:  This table contains all defined schedules.
create table schedules (
     schedule_id bigint not null generated always as identity primary key,
                                        -- Unique id for the schedule
     name varchar(300) not null unique, -- Name of schedule
     comments varchar(30000),      -- User-defined comments
     startdate timestamp,          -- Time to start, if specified
     enddate timestamp,            -- Time to stop. timeframe is endless, if
                                   --  both this field and maxrepeats are null.
     maxrepeats bigint,            -- Count of times it can be started. It must
                                   --  be set, if enddate is null, in order to
                                   --  avoid timeframe is endless
     timeunit int not null,        -- Time unit used for time measure between
                                   --  repetitions. It indicates whether it is
                                   --  hours, days, weeks or months. Possible
                                   --  values are defined in TimeUnit.java
     numtimeunits bigint not null, -- Count of time unit, for time of next
                                   --  repetition
     anytime int not null,   -- True, if this is an anytime frequency, i.e.
                             --  onminute, onhour, ondayofweek and dayofmonth
                             --  has no meaning.
     onminute int,           -- Which minute is it about to run
     onhour int,             -- Which hour is it about to run
     ondayofweek int,        -- Which day of week is it about to run
     ondayofmonth int,       -- Which day of month is it about to run
     edition bigint not null -- Marker for optimistic locking by web interface
);

--***************************************************************************--
-- Area: Templates
-- Contains templates for crawler setups, currently only Heritrix order.xml
-- files are supported.
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    ordertemplates
-- Descr.:  This table contains predefined crawler setups in the form of
--          Heritrix order.xml files.
create table ordertemplates (
    template_id bigint not null generated always as identity primary key,
                                       -- Unique id for the template
    name varchar(300) not null unique, -- Name of the template
    orderxml clob(64M) not null,        -- The Heritrix order.xml string
    isActive BOOLEAN NOT NULL DEFAULT TRUE
);

--***************************************************************************--
-- Area: Jobs
-- Contains crawl jobs, both current and past.
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    jobs
-- Descr.:  This table contains most of the information about jobs.
create table jobs (
    job_id bigint not null primary key, -- Unique id for a job
    harvest_id bigint not null,         -- Reference to the harvest that
                                        --  produced the job
    status int not null,                -- Job status where valid values are
                                        --  defined in JobStatus.java
    forcemaxbytes bigint not null default -1, -- Max byte count that overrides
                                              --  the maxbytes value in the
                                              --  harvest definition
    forcemaxcount bigint,           -- Max object count that overwrites the
                                    --  maxcount value in the harvest
                                    --  definition
    forcemaxrunningtime bigint NOT NULL DEFAULT 0,
				    -- Max number of seconds that the harvester
                                    -- can work on this job
    orderxml varchar(300) not null, -- The order.xml file name that is used
                                    --  here
    orderxmldoc clob(64M) not null, -- The contents of the order.xml file in
                                    --  text form, added with specific details
                                    --  for the included configurations, such
                                    --  as crawlertraps
    seedlist clob(64M) not null,    -- The final seed list
    harvest_num int not null,       -- For repeating harvests, which run number
                                    --  of the harvest created this job
    harvest_errors varchar(300),    -- Short description of all errors from the
                                    --  harvest, if any.
    harvest_error_details varchar(10000), -- Details of all errors from the
                                          --  harvest, if any.
    upload_errors varchar(300),     -- Short description of all errors from
                                    --  upload, if any.
    upload_error_details varchar(10000), -- Details of all errors from upload,
                                         --  if any.
    startdate timestamp,                 -- The time when a crawler started
                                         --  executing this job.
    enddate timestamp,                   -- The time when this job was reported
                                         --  done or failed.
    submitteddate timestamp, 			-- The time when this job was submitted
    creationdate timestamp, 			-- The time when this job was created
    resubmitted_as_job bigint,          -- The jobId this job was resubmitted as.
                                        --  This is null, if this job has not been
                                        --  resubmitted.
    num_configs int not null default 0,  -- Number of configurations in the
                                         --  job, autocreated for optimization
                                         --  purposes
    edition bigint not null,   -- Marker for optimistic locking by web interface
    continuationof bigint,      -- if not null this job tries to continue where this job left of
                                -- using the Heritrix recoverlog
    harvestname_prefix varchar(100),
    channel varchar(300),
    snapshot BOOLEAN NOT NULL
);

create index jobstatus on jobs(status);
create index jobharvestid on jobs(harvest_id);

-------------------------------------------------------------------------------
-- Name:    job_configs
-- Descr.:  This table contains relations between jobs and configurations.
-- Purpose: The information is used to find out which configurations used to
--          create a job. The information is basis for making a list of domain
--          configurations for a job. Note that it is possible to identify the
--          domain from the configuration, so the domain is not stored here.
--          Note that even though it is possible to make many-to-many relations
--          there are in reality a one (jobs) to many
--          (configurations) relation
-- Expected entry count: Many
create table job_configs(
    job_id bigint not null,     -- Reference to table jobs
    config_id bigint not null,  -- Reference to table configurations
    primary key ( job_id, config_id )
);

create index jobconfigjob on job_configs(job_id);

-------------------------------------------------------------------------------
-- Name:   global_crawler_trap_lists
-- Descr.: Models a list of crawler traps which may be applied globally
-- Purpose:Each list is referenced by one or more crawler trap expressions
--         which are regular expressions stored in the table
--         global_crawler_trap_expressions. If the list is active, then any
--         url matching any of these expressions is added as a crawler trap
--         to all jobs scheduled with any harvest template.
create table global_crawler_trap_lists(
  global_crawler_trap_list_id int not null generated always as identity primary key,
  name varchar(300) not null unique,     -- A name by which this list is known
                                         -- e.g. "Statsbibliotekets Master List'
  description varchar(30000),            -- An optional description of the
                                         -- list
  isActive int not null                  -- boolean valued int indicating
                                         -- whether or not the list is active
                                         -- 0=inactive, 1=active
);

-------------------------------------------------------------------------------
-- Name:    global_crawler_trap_expressions
-- Descr.:  Contains the regular expressions defining url's to be ignored
-- Purpose: Specifies the actual crawler traps in each list
create table global_crawler_trap_expressions(
    crawler_trap_list_id int not null, -- references
                                                  -- global_crawler_trap_list_id
    trap_expression varchar(1000),               -- the actual regular
                                                  -- expression for the crawler
                                                  -- trap
    primary key (crawler_trap_list_id, trap_expression)
);

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
     currentProcessedKBPerSec int NOT NULL,
     processedKBPerSec int NOT NULL,
     currentProcessedDocsPerSec numeric NOT NULL,
     processedDocsPerSec numeric NOT NULL,
     activeToeCount integer NOT NULL,
     status int NOT NULL,
     tstamp timestamp NOT NULL,
     PRIMARY KEY (jobId, harvestName, elapsedSeconds, tstamp)
);

CREATE INDEX runningJobsHistoryCrawlJobId on runningJobsHistory (jobId);
CREATE INDEX runningJobsHistoryCrawlTime on runningJobsHistory (elapsedSeconds);
CREATE INDEX runningJobsHistoryHarvestName on runningJobsHistory (harvestName);

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
     PRIMARY KEY (jobId, harvestName)
);

CREATE INDEX runningJobsMonitorJobId on runningJobsMonitor (jobId);
CREATE INDEX runningJobsMonitorHarvestName on runningJobsMonitor (harvestName);

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
     UNIQUE (jobId, filterId, domainName)
);

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
    historize int,
    sequencenr int,
    maxlen int
);

create table extendedfieldvalue (
    extendedfieldvalue_id bigint not null primary key,
    content VARCHAR(30000) not null,
    extendedfield_id bigint NOT NULL,
    instance_id bigint NOT NULL
);

INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 1, 'domains');
INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 2, 'harvestdefinitions');

CREATE TABLE harvestchannel (
  id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(300) NOT NULL UNIQUE,
  issnapshot BOOLEAN NOT NULL,
  isdefault BOOLEAN NOT NULL,
  comments VARCHAR(30000)
);
-- Insert default definition for snapshot and selective harvests.
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments)
    VALUES('SNAPSHOT', true, true, 'Channel for snapshot harvests');
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments)
    VALUES('FOCUSED', false, true, 'Channel for focused harvests');
