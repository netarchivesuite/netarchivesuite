-- File:        $Id$
-- Revision:    $Revision$
-- Author:      $Author$
-- Date:        $Date$
--
-- The Netarchive Suite - Software to harvest and preserve websites
-- Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
    values ( 'domains', 2);
insert into schemaversions ( tablename, version ) 
    values ( 'configurations', 3);
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
    values ( 'harvestdefinitions', 2);
insert into schemaversions ( tablename, version ) 
    values ( 'partialharvests', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'fullharvests', 2);
insert into schemaversions ( tablename, version ) 
    values ( 'harvest_configs', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'schedules', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'ordertemplates', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'jobs', 4);
insert into schemaversions ( tablename, version ) 
    values ( 'job_configs', 1);


--***************************************************************************--
-- Area: Domains
-- Contains data on individual domains, including their seeds, ways of
-- harvesting them, and history of what has previously been harvested from them
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    domains
-- Descr.:  This table contains the basic domain information.
-- Expected entry count: Approximately 600.000 entries
create table domains (
    domain_id bigint not null generated always as identity primary key,
                                       -- Unique id for the domain
    name varchar(300) not null unique, -- Name of the domain
    comments varchar(30000),           -- Comments on domain, if any 
    defaultconfig bigint not null,     -- Configuration used for snapshot
                                       --  harvests
    crawlertraps varchar(1000),        -- Regexp(s) for excluded urlâ€™s.
    edition bigint not null,           -- Marker for optimistic locking by 
                                       --  web interface
    alias bigint,                      -- Domain that this domain is an alias
                                       --  of
    lastaliasupdate timestamp          -- Last time a user has verified that
                                       --  this domain is an alias
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
     maxobjects int,                 -- Max count of objects from the domain
     maxrate int,                    -- Max connections per minute
     overridelimits int,             -- True if the configuration's limits
                                     --  should apply in snapshot harvests
     maxbytes bigint not null default -1 -- Maximum number of bytes to harvest
                                         --  with this configuration
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
--          2. Not all passwords belongs to a configuration 
--          3. Even though it is possible to make many-to-may relations there
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
--          2. Not all seedlists belongs to a configuration 
--          3. Even though it is possible to make many-to-may relations there
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
    domain_id bigint not null,  -- The domain that the seed list belong to
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
    edition bigint not null     -- Marker for optimistic locking by web 
                                --  interface
);
 
create index harvestdefinitionssubmitdate on harvestdefinitions (submitted);

-------------------------------------------------------------------------------
-- Name:    fullharvests
-- Descr.:  This table contains additional information about snapshot harvests
create table fullharvests (
    harvest_id bigint not null primary key, -- Unique id for harvest definition
    maxobjects bigint not null,             -- Count of max objects per domain
    previoushd bigint,        -- Harvest that this snapshot harvest is based on
    maxbytes bigint default -1 -- Maximum number of bytes to harvest per domain
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
--          Note that even though it is possible to make many-to-may relations
--          there are in reality a one (harvestdefinition) to many 
--          (configutrations) relation
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
     enddate timestamp,            -- Time to stop. It must be set, if 
                                   --  maxrepeats is null, in order to avoid 
                                   --  timeframe is endless
     maxrepeats bigint,            -- Count of times it can be started. It must
                                   --  be set, if enddate is null, in order to 
                                   --  avoid timeframe is endless
     timeunit int not null,        -- Time unit used for time measure between 
                                   --  repetitions. It indicates whether it is
                                   --  hours, days, weeks or moths. possible
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
    orderxml clob(64M) not null        -- The Heritrix order.xml string
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
    priority int not null,              -- Job priority here valid values are
                                        --  defined in JobPriority.java
    forcemaxbytes bigint not null default -1, -- Max byte count that overrides
                                              --  the maxbytes value in the 
                                              --  harvest definition
    forcemaxcount bigint,           -- Max object count that overwrites the
                                    --  maxcount value in the harvest
                                    --  definition
    orderxml varchar(300) not null, -- The order.xml file name that are use
                                    --  here
    orderxmldoc clob(64M) not null, -- The contents of the order.xml file in
                                    --  text form, added with specific details
                                    --  for the included configurations, such
                                    --  as crawlertraps
    seedlist clob(64M) not null,    -- The final seed list
    harvest_num int not null,       -- For repeating harvests, which run number
                                    --  of the harvest created this job 
    harvest_errors varchar(300),    -- Short description of all errors from the
                                    --  harvest, if any
    harvest_error_details varchar(10000), -- Details of all errors from the 
                                          --  harvest, if any
    upload_errors varchar(300),     -- Short description of all errors from 
                                    --  upload, if any
    upload_error_details varchar(10000), -- Details of all errors from upload,
                                         --  if any
    startdate timestamp,                 -- The time when a crawler started 
                                         --  executing this job
    enddate timestamp,                   -- The time when this job was reported 
                                         --  done or failed
    num_configs int not null default 0,  -- Number of configurations in the
                                         --  job, autocreated for optimization
                                         --  purposes
    edition bigint not null   -- Marker for optimistic locking by web interface
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
-- Expected entry count: Many
create table job_configs(
    job_id bigint not null,     -- Reference to table jobs
    config_id bigint not null,  -- Reference to table configurations
    primary key ( job_id, config_id ) 
);

create index jobconfigjob on job_configs(job_id);
