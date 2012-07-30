-- File:        $Id: createArchiveDB.sql 1359 2010-04-15 13:04:12Z svc $
-- Revision:    $Revision: 1359 $
-- Author:      $Author: svc $
-- Date:        $Date: 2010-04-15 15:04:12 +0200 (Thu, 15 Apr 2010) $
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

-- See createArchiveDB.sql for information of the semantics of these fields.
 
----------------------------------------------
-- FIXME MISSING CONNECT INFORMATION FOR postgreSQL
----------------------------------------------

--Q1: Which difference if any are there between 'int' and 'integer' in postgreSQL?
--Q2: which indices are necessary?

--***************************************************************************--
-- Area: Basics
-- Contains metadata for the database itself.
--***************************************************************************--

CREATE TABLE schemaversions (
    tablename varchar(100) NOT NULL, -- Name of table
    version integer NOT NULL         -- Version of table 
);

INSERT INTO schemaversions ( tablename, version ) 
    VALUES ( 'replica', 1);
INSERT INTO schemaversions ( tablename, version ) 
    VALUES ( 'replicafileinfo', 1);
INSERT INTO schemaversions ( tablename, version ) 
    VALUES ( 'file', 1);
INSERT INTO schemaversions ( tablename, version ) 
    VALUES ( 'segment', 1);
 
    
--***************************************************************************--
-- Area: replica
-- 
--***************************************************************************--

CREATE TABLE replica (
    replica_guid bigint NOT NULL PRIMARY KEY,
                                               -- Unique id for the table
    replica_id  varchar(300) NOT NULL unique,  -- the unique string identifier for this replica
    replica_name varchar(300) NOT NULL unique, -- Name of the replica
    replica_type varchar(50) NOT NULL,         -- Type of the replica (CHECKSUM, BITARCHIVE, NO_REPLICA_TYPE)
    filelist_updated timestamp,                -- Last time the replica performed a filelist job.
    checksum_updated timestamp                 -- Last time the replica performed a checksum job.
);

-- Emulate "generated always as identity" in PostgreSQL
CREATE SEQUENCE replica_guid_seq OWNED BY replica.replica_guid;
ALTER TABLE replica ALTER COLUMN replica_guid SET DEFAULT NEXTVAL('replica_guid_seq');

--***************************************************************************--
-- Area: replicafileinfo
-- 
--***************************************************************************--

CREATE TABLE replicafileinfo (
     replicafileinfo_guid bigint NOT NULL PRIMARY KEY,
                                       -- The unique identifier for this table.
     replica_id varchar(300) NOT NULL, -- The identifier for the replica.
     file_id bigint,                   -- The identification of the file.
     segment_id bigint,                -- The identification for the segment.
     checksum varchar(300),            -- The checksum for the file.
     upload_status integer,                -- Either a string or integer representation of a ENUM (UNKNOWN, STARTED, 
                                       -- UPLOADED, FAILED, COMPLETED).
     checksum_status integer,              -- Either a string or integer representation of a ENUM (UNKNOWN, CORRUPT,
                                       -- CORRUPT).
     filelist_status integer,              -- Either a string or integer representation of a ENUM (NO_STATUS, 
                                       -- MISSING, OK)
     filelist_checkdatetime timestamp, -- Last time the filelist status for the file was checked 
     checksum_checkdatetime timestamp  -- Last time the checksum status for the file was checked 
     );

-- Emulate "generated always as identity" in PostgreSQL
CREATE SEQUENCE replicafileinfo_guid_seq OWNED BY replicafileinfo.replicafileinfo_guid;
ALTER TABLE replicafileinfo ALTER COLUMN replicafileinfo_guid SET DEFAULT NEXTVAL('replicafileinfo_guid_seq');

-- Create relevant indices
create index fileandreplica on replicafileinfo (file_id, replica_id);
create index replicaandfileliststatus on replicafileinfo (replica_id, filelist_status);
create index replicaandchecksumstatus on replicafileinfo (replica_id, checksum_status);

     
--***************************************************************************--
-- Area: file
-- 
--***************************************************************************--

CREATE TABLE file (
     file_id bigint NOT NULL PRIMARY KEY,
                            -- the id of the file and unique id for the table.
     filename varchar(300)  -- the name of the file.
); 

-- Emulate "generated always as identity" in PostgreSQL
CREATE SEQUENCE file_id_seq OWNED BY file.file_id;
ALTER TABLE file ALTER COLUMN file_id SET DEFAULT NEXTVAL('file_id_seq');



CREATE INDEX fileindex on file (filename);

--***************************************************************************--
-- Area: segment
-- 
--***************************************************************************--

CREATE TABLE segment (
     segment_guid bigint NOT NULL PRIMARY KEY,
                                       -- The unique id for the table.
     replica_id bigint,                -- the id of the replica
     segment_id bigint,                -- the id of the segment 
     segment_address varchar(300),     -- the logical address of the segment
     filelist_checkdatetime timestamp, -- Last time the filelist status for the replica was checked 
     checksum_checkdatetime timestamp  -- Last time the checksum status for the replica was checked 
); 

-- Emulate "generated always as identity" in PostgreSQL
CREATE SEQUENCE segment_guid_seq OWNED BY segment.segment_guid;
ALTER TABLE segment ALTER COLUMN segment_guid SET DEFAULT NEXTVAL('segment_guid_seq');

