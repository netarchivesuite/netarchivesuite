-- $Id: jobcategories_init_legacy.sql 1414 2010-05-31 15:52:06Z ngiraud $
-- $Revision: 1414 $
-- $Date: 2010-05-31 17:52:06 +0200 (Mon, 31 May 2010) $
-- $Author: ngiraud $

-- PostgreSQL initialization script
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

-- Initializes the harvestchannel table to the legacy values (HIGH and LOW job priorities)

--
-- How to use:
-- psql -U <user name> -W [DB name] < jobcategories_init_legacy.sql

SET search_path TO netarchivesuite;

INSERT INTO harvestchannel(name, isdefault, comments) 
    VALUES ('FOCUSED', true, 'Channel for focused harvests');