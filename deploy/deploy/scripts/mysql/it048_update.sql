-- $Id$
-- $Revision$
-- $Date$
-- $Author$

-- MySQL creation scripts
-- tested on 5.1.41-3ubuntu12.10 (Ubuntu)
--
-- How to use:
-- mysql < createfullhddb.mysql
-- mysql < it048_update.mysql

connect fullhddb;

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

INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfieldtype', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfield', 1);
INSERT INTO schemaversions ( tablename, version )
    VALUES ( 'extendedfieldvalue', 1);

INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 1, 'domains');
INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )
    VALUES ( 2, 'harvestdefinitions');
    