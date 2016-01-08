CREATE SEQUENCE eav_attribute_seq;

CREATE TABLE eav_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL DEFAULT NEXTVAL('eav_attribute_seq'),
	entity_id INTEGER NOT NULL,
	type_id INTEGER NOT NULL,
	val_int INTEGER NULL,
	val_datetime TIMESTAMP NULL,
	val_varchar VARCHAR(8000) NULL,
	val_text TEXT NULL
);

ALTER TABLE eav_attribute ADD CONSTRAINT eav_attribute_pkey PRIMARY KEY (tree_id, id);

CREATE INDEX eav_attribute_idx on eav_attribute(tree_id, entity_id) TABLESPACE tsindex;
