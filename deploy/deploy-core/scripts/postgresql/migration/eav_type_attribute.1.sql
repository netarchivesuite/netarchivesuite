CREATE TABLE eav_type_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL,
	name VARCHAR(96) NOT NULL,
	class_namespace VARCHAR(96) NOT NULL,
	class_name VARCHAR(96) NOT NULL,
	datatype INTEGER NOT NULL,
	viewtype INTEGER NOT NULL,
	def_int INTEGER NULL,
	def_datetime TIMESTAMP NULL,
	def_varchar VARCHAR(8000) NULL,
	def_text TEXT NULL
);

ALTER TABLE eav_type_attribute ADD CONSTRAINT eav_type_attribute_pkey PRIMARY KEY (tree_id, id);

CREATE UNIQUE INDEX eav_type_attribute_idx on eav_type_attribute(tree_id, id) TABLESPACE tsindex;
