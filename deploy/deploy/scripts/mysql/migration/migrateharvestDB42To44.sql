
ALTER TABLE harvestdefinitions ADD COLUMN channel_id BIGINT DEFAULT NULL;
UPDATE schemaversions SET version = 4 WHERE tablename = 'harvestdefinitions';

ALTER TABLE jobs ADD COLUMN channel VARCHAR(300) DEFAULT NULL;
ALTER TABLE jobs ADD COLUMN snapshot BOOL;
UPDATE jobs SET channel = 'snapshot' WHERE priority=0;
UPDATE jobs SET channel = 'focused' WHERE priority=1;
UPDATE jobs SET snapshot = true WHERE priority=0;
UPDATE jobs SET snapshot = false WHERE priority=1;
ALTER TABLE jobs DROP COLUMN priority;
UPDATE schemaversions SET version = 10 WHERE tablename = 'jobs';

CREATE TABLE harvestchannel (
id BIGINT NOT NULL PRIMARY KEY,
name VARCHAR(300) NOT NULL UNIQUE,
issnapshot BOOL NOT NULL,
isdefault BOOL NOT NULL,
comments TEXT
);
INSERT INTO harvestchannel(id, name, issnapshot, isdefault, comments) VALUES(1, 'SNAPSHOT', true, true, 'Channel for snapshot harvests');
INSERT INTO harvestchannel(id, name, issnapshot, isdefault, comments) VALUES(2, 'FOCUSED', false, true, 'Channel for focused harvests');
INSERT INTO schemaversions(tablename, version) VALUES ('harvestchannel', 1);

ALTER TABLE extendedfield ADD COLUMN maxlen INT;
ALTER TABLE extendedfield MODIFY options TEXT;
UPDATE schemaversions SET version = 2 WHERE tablename = 'extendedfield';

ALTER TABLE extendedfieldvalue MODIFY content TEXT NOT NULL;
UPDATE schemaversions SET version = 2 WHERE tablename = 'extendedfieldvalue';




