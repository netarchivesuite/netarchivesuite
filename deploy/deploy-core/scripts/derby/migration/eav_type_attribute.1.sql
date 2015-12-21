CREATE TABLE eav_type_attribute (
	tree_id INTEGER NOT NULL,
	id INTEGER NOT NULL,
	name VARCHAR(96) NOT NULL,
	class_namespace VARCHAR(96) NOT NULL,
	class_name VARCHAR(96) NOT NULL,
	datatype INTEGER NOT NULL,
	viewtype INTEGER NOT NULL,
	def_int INTEGER,
	def_datetime TIMESTAMP,
	def_varchar VARCHAR(8000),
	def_text CLOB,
	PRIMARY KEY (tree_id, id)
);

CREATE UNIQUE INDEX eav_type_attribute_idx on eav_type_attribute(tree_id, id);
