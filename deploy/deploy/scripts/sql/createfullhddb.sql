-- This script will create, but not populate, a Derby database for the
-- web archiving system.  The database will be created in the current
-- directory under the name 'fullhddb'.

-- To run this, execute java org.apache.derby.tools.ij createfullhddb.sql
-- with derbytools.jar and derby.jar in your classpath.

-- Whenever a table schema is changed, the version in the schemaversions table
-- should be upped by one and the corresponding check in the DAO should be
-- updated to enforce consistency and allow automated changes.


connect 'jdbc:derby:fullhddb;create=true';



 -- First create the metadata table that defines versions to allow updates

 create table schemaversions ( tablename varchar(100) not null, version int not null );



-- The following Derby statements were used to create the domain-related tables:

 create table domains
    (domain_id bigint not null generated always as identity primary key,
     name varchar(300) not null unique, comments varchar(30000),
     defaultconfig bigint not null, crawlertraps varchar(1000),
     edition bigint not null, alias bigint, lastaliasupdate timestamp ) ;
-- Version 2 adds the maxbytes column
 create table configurations
    (config_id bigint not null generated always as identity primary key,
     name varchar(300) not null, comments varchar(30000),
     domain_id bigint not null, template_id bigint not null, maxobjects int,
     maxrate int, overridelimits int, maxbytes bigint not null );
 create table config_passwords
    (config_id bigint not null, password_id int not null,
     primary key (config_id, password_id) );
 create table config_seedlists
    (config_id bigint not null, seedlist_id int not null,
     primary key (config_id, seedlist_id) );
 create table seedlists
    (seedlist_id bigint not null generated always as identity primary key,
     name varchar (300) not null, comments varchar(30000),
     domain_id bigint not null, seeds clob(8M) not null);
 create table passwords
    (password_id bigint not null generated always as identity primary key,
     name varchar (300) not null, comments varchar(30000),
     domain_id bigint not null, url varchar(300) not null,
     realm varchar(300) not null, username varchar(20) not null,
     password varchar(40) not null );
 create table ownerinfo
    (ownerinfo_id bigint not null generated always as identity primary key,
     domain_id bigint not null, created timestamp not null,
     info varchar(1000) not null);
 create table historyinfo
    (historyinfo_id bigint not null generated always as identity primary key,
     stopreason int not null, objectcount bigint not null,
     bytecount bigint not null, config_id bigint not null,
     harvest_id bigint not null, job_id bigint, harvest_time timestamp not null);

-- Index automatically generated when a column is unique.
-- create index domainname on domains(name);
 create index domainnameid on domains(domain_id, name);
 create index configurationname on configurations(name);
 create index configurationmaxbytes on configurations(maxbytes);
 create index configdomain on configurations(domain_id);
 create index seedlistname on seedlists(name);
 create index seedlistdomain on seedlists(domain_id);
 create index passwordname on passwords(name);
 create index passworddomain on passwords(domain_id);
 create index ownerinfodomain on ownerinfo(domain_id);

-- These indices seem to speed up showing historical info.  See bug 650.
 create index historyinfoharvest on historyinfo (harvest_id);
 create index historyinfoconfigharvest on historyinfo (config_id,harvest_id);
 create index historyinfojobharvest on historyinfo (job_id,harvest_id);
 create index historyinfoharvestconfig on historyinfo (harvest_id,config_id);

 create index historyinfoconfig on historyinfo(config_id);
 create index historyinfojob on historyinfo(job_id);

 insert into schemaversions ( tablename, version ) values ( 'domains', 2);
 insert into schemaversions ( tablename, version ) values ( 'configurations', 3);
 insert into schemaversions ( tablename, version ) values ( 'seedlists', 1);
 insert into schemaversions ( tablename, version ) values ( 'passwords', 1);
 insert into schemaversions ( tablename, version ) values ( 'ownerinfo', 1);
 insert into schemaversions ( tablename, version ) values ( 'historyinfo', 2);
 insert into schemaversions ( tablename, version ) values ( 'config_passwords', 1);
 insert into schemaversions ( tablename, version ) values ( 'config_seedlists', 1);




 -- The tables used for the HD DAO can be created in derby with these statements;

 create table harvestdefinitions
     (harvest_id bigint not null primary key, name varchar(300) not null unique,
      comments varchar(30000), numevents int not null,
      submitted timestamp not null, isactive int not null,
      edition bigint not null );
 create table fullharvests
     (harvest_id bigint not null primary key, maxobjects bigint not null,
      previoushd bigint, maxbytes bigint );
 create table partialharvests
     (harvest_id bigint not null primary key, schedule_id bigint not null,
      nextdate timestamp );
 create table harvest_configs
     (harvest_id bigint not null, config_id bigint not null,
      primary key ( harvest_id, config_id ) );

 -- Index automatically created since name column is set unique
 -- create index harvestnames on harvestdefinitions(name);

 create index harvestdefinitionssubmitdate ON harvestdefinitions (submitted);
 create index partialharvestsnextdate ON partialharvests (nextdate);

 insert into schemaversions ( tablename, version ) values ( 'harvestdefinitions', 2);
 insert into schemaversions ( tablename, version ) values ( 'partialharvests', 1);
 -- Version 2 adds the maxbytes column
 insert into schemaversions ( tablename, version ) values ( 'fullharvests', 2);
 insert into schemaversions ( tablename, version ) values ( 'harvest_configs', 1);





-- The following commands will create the table required for schedules;

 create table schedules
     (schedule_id bigint not null generated always as identity primary key,
      name varchar(300) not null unique, comments varchar(30000), startdate timestamp,
      enddate timestamp, maxrepeats bigint, timeunit int not null,
      numtimeunits bigint not null, anytime int not null, onminute int,
      onhour int, ondayofweek int, ondayofmonth int, edition bigint not null );

 -- Automatically created since column is unique.
 create index schedulename on schedules(name);

 insert into schemaversions ( tablename, version ) values ( 'schedules', 1);




-- Derby statements to create the template table:

 create table ordertemplates (
   template_id bigint not null generated always as identity primary key,
   name varchar(300) not null unique, orderxml clob(64M) not null );

 -- Automatically created since column is unique.
 -- create index templatenames on ordertemplates(name);

 insert into schemaversions ( tablename, version ) values ( 'ordertemplates', 1);



 -- Derby statements to create the job-related tables:

  create table jobs
    (job_id bigint not null primary key, harvest_id bigint not null,
     status int not null, priority int not null, forcemaxbytes int not null,
     forcemaxcount bigint, orderxml varchar(300) not null, orderxmldoc clob(64M) not null,
     seedlist clob(64M) not null, harvest_num int not null,
     harvest_errors varchar(300), harvest_error_details varchar(10000),
     upload_errors varchar(300), upload_error_details varchar(10000),
     startdate timestamp, enddate timestamp, num_configs int not null,
     edition bigint not null );

  create index jobstatus on jobs(status);
  create index jobharvestid on jobs(harvest_id);

  create table job_configs
    (job_id bigint not null, config_id bigint not null,
     primary key ( job_id, config_id ) );

  create index jobconfigjob on job_configs(job_id);

 insert into schemaversions ( tablename, version ) values ( 'jobs', 3);
 insert into schemaversions ( tablename, version ) values ( 'job_configs', 1);

-- We should now be ready to migrate into this database.
