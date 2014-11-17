
ALTER TABLE harvestdefinitions ADD COLUMN channel_id BIGINT DEFAULT NULL;
UPDATE schemaversions SET version = 4 WHERE tablename = 'harvestdefinitions';

ALTER TABLE jobs ADD COLUMN channel VARCHAR(300) DEFAULT NULL;
ALTER TABLE jobs ADD COLUMN snapshot BOOL;
UPDATE jobs SET channel = 'LOWPRIORITY' WHERE priority=0;
UPDATE jobs SET channel = 'HIGHPRIORITY' WHERE priority=1;
UPDATE jobs SET snapshot = true WHERE priority=0;
UPDATE jobs SET snapshot = false WHERE priority=1;
ALTER TABLE jobs DROP COLUMN priority;
UPDATE schemaversions SET version = 10 WHERE tablename = 'jobs';

CREATE TABLE harvestchannel (
id BIGINT NOT NULL PRIMARY KEY,
name VARCHAR(300) NOT NULL UNIQUE,
issnapshot BOOL NOT NULL,
isdefault BOOL NOT NULL,
comments VARCHAR(30000)
);
CREATE SEQUENCE harvestchannel_id_seq OWNED BY harvestchannel.id;
ALTER TABLE harvestchannel ALTER COLUMN id SET DEFAULT NEXTVAL('harvestchannel_id_seq');
CREATE INDEX harvestchannelnameid on harvestchannel(name) TABLESPACE tsindex;
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments)
  VALUES ('LOWPRIORITY', true, true, 'Channel for snapshot harvests');
INSERT INTO harvestchannel(name, issnapshot, isdefault, comments)
  VALUES ('HIGHPRIORITY', false, true, 'Channel for selective harvests');
INSERT INTO schemaversions ( tablename, version ) VALUES ( 'harvestchannel', 1);

ALTER TABLE extendedfield ADD COLUMN maxlen INT;
ALTER TABLE extendedfield ALTER COLUMN options TYPE VARCHAR(1000);
UPDATE schemaversions SET version = 2 WHERE tablename = 'extendedfield';

ALTER TABLE extendedfieldvalue ALTER COLUMN content TYPE VARCHAR(30000),
ALTER COLUMN content SET NOT NULL;
UPDATE schemaversions SET version = 2 WHERE tablename = 'extendedfieldvalue';




