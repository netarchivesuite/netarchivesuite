-- $Id$
-- $Revision$
-- $Date$
-- $Author$ 
 
-- PostgreSQL removal script
-- presupposes PostgresSQL 8.3+
-- tested on PostgreSQL 8.4.1-1 (Ubuntu 9.10)

--
-- How to use:
-- psql -U <user name> -W [DB name] < netarchivesuite_clean.sql

DROP SCHEMA netarchivesuite CASCADE;
DROP ROLE netarchivesuite;
