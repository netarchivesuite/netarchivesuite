# MySQL script to change field jobs.forcemaxbytes from int to bigint, 
# and sets default to -1.
# tested on MySQL 5.0.27
# Usage: mysql databasename < convertForceMaxbytesToBigintMysql.sql

 alter table jobs change column forcemaxbytes forcemaxbytes bigint not null default -1;
