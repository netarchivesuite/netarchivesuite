-- $Id: netarchivesuite_clean.sql 1414 2010-05-31 15:52:06Z ngiraud $
-- $Revision: 1414 $
-- $Date: 2010-05-31 17:52:06 +0200 (Mon, 31 May 2010) $
-- $Author: ngiraud $ 
 
-- PostgreSQL removal script
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

--
-- How to use:
-- psql -U <user name> -W [DB name] < netarchivesuite_clean.sql

DROP SCHEMA netarchivesuite CASCADE;
DROP ROLE netarchivesuite;
