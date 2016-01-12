CREATE TABLE eav_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL generated always as identity,
	entity_id INTEGER NOT NULL,
	type_id INTEGER NOT NULL,
	val_int INTEGER,
	val_datetime TIMESTAMP,
	val_varchar VARCHAR(8000),
	val_text CLOB,
	PRIMARY KEY (tree_id, id)
);

CREATE INDEX eav_attribute_idx on eav_attribute(tree_id, entity_id);
