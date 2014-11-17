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


connect 'jdbc:derby:archivedb;create=true';

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
    values ( 'replica', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'replicafileinfo', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'file', 1);
insert into schemaversions ( tablename, version ) 
    values ( 'segment', 1);
 
    
--***************************************************************************--
-- Area: replica
-- 
--***************************************************************************--


-------------------------------------------------------------------------------
-- Name:    replica
-- Descr.:  This table contains data on individual replica, including their id, name,
-- and type, and time for the last update.
-- Expected entry count: a few

create table replica (
    replica_guid bigint not null generated always as identity primary key,
                                               -- Unique id for the table
    replica_id  varchar(300) not null unique,  -- the unique string identifier for this replica
    replica_name varchar(300) not null unique, -- Name of the replica
    replica_type varchar(50) not null,         -- Type of the replica (CHECKSUM, BITARCHIVE, NO_REPLICA_TYPE)
    filelist_updated timestamp,                -- Last time the replica performed a filelist job.
    checksum_updated timestamp                 -- Last time the replica performed a checksum job.
);

--***************************************************************************--
-- Area: replicafileinfo
-- 
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    replicafileinfo
-- Descr.:  A single entry in this table contains information about a file belonging to a specific replica: its checkum, 
-- uploadstatus (STARTED, UPLOADED, COMPLETED, FAILED), fileliststatus (missing, found), date for last filelist check, 
-- date for last checksum check 
-- Expected entry count: gigantical

create table replicafileinfo (
     replicafileinfo_guid bigint not null generated always as identity primary key,
                                       -- The unique identifier for this table.
     replica_id varchar(300) not null, -- The identifier for the replica.
     file_id bigint,                   -- The identification of the file.
     segment_id bigint,                -- The identification for the segment.
     checksum varchar(300),            -- The checksum for the file.
     upload_status int,                -- Either a string or integer representation of a ENUM (UNKNOWN, STARTED, 
                                       -- UPLOADED, FAILED, COMPLETED).
     checksum_status int,              -- Either a string or integer representation of a ENUM (UNKNOWN, CORRUPT,
                                       -- CORRUPT).
     filelist_status int,              -- Either a string or integer representation of a ENUM (NO_STATUS, 
                                       -- MISSING, OK)
     filelist_checkdatetime timestamp, -- Last time the filelist status for the file was checked 
     checksum_checkdatetime timestamp  -- Last time the checksum status for the file was checked 
     );
     
create index fileandreplica on replicafileinfo (file_id, replica_id);
create index replicaandfileliststatus on replicafileinfo (replica_id, filelist_status);
create index replicaandchecksumstatus on replicafileinfo (replica_id, checksum_status);

     
--***************************************************************************--
-- Area: file
-- 
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    file
-- Descr.:  contains information about the name of a file.
-- Expected entry count: colosal

create table file (
     file_id bigint not null generated always as identity primary key,
                            -- the id of the file and unique id for the table.
     filename varchar(300)  -- the name of the file.
); 

create index fileindex on file (filename);
--***************************************************************************--
-- Area: segment
-- 
--***************************************************************************--

-------------------------------------------------------------------------------
-- Name:    segment
-- Descr.:  contains information about replica segments.
-- Expected entry count: small
create table segment (
     segment_guid bigint not null generated always as identity primary key,
                                       -- The unique id for the table.
     replica_id bigint,                -- the id of the replica
     segment_id bigint,                -- the id of the segment 
     segment_address varchar(300),     -- the logical address of the segment
     filelist_checkdatetime timestamp, -- Last time the filelist status for the replica was checked 
     checksum_checkdatetime timestamp  -- Last time the checksum status for the replica was checked 
); 
