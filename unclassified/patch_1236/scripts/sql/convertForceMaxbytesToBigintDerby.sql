#!/bin/sh

java -Xmx2048m org.apache.derby.tools.ij <<EOF

connect 'jdbc:derby:fullhddb';
create temporary table forcemaxbytesvalues ( job_id bigint not null, forcemaxbytes bigint not null );

INSERT INTO forcemaxbytesvalues ( job_id, forcemaxbytes ) SELECT jobs.job_id, jobs.forcemaxbytes from 
                                   jobs;
ALTER TABLE jobs DROP COLUMN forcemaxbytes RESTRICT;
ALTER TABLE jobs ADD COLUMN forcemaxbytes bigint NOT NULL DEFAULT -1;

UPDATE TABLE jobs SET forcemaxbytes = (SELECT forcemaxbytesvalues.forcemaxbytes FROM forcemaxbytesvalues
WHERE jobs.job_id = forcemaxbytesvalues.job_id);

EOF