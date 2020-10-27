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

--INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
--VALUES(1, 1, 'MAX_HOPS', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 1, 20, null, null, null);

--INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
--VALUES(1, 2, 'HONOR_ROBOTS_DOT_TXT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 6, 0, null, null, null);

--INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
--VALUES(1, 3, 'EXTRACT_JAVASCRIPT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 5, 1, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 1, 'MAX_HOPS', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 1, 20, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 2, 'HONOR_ROBOTS_DOT_TXT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 6, 0, null, null, null);

INSERT INTO eav_type_attribute(tree_id, id, name, class_namespace, class_name, datatype, viewtype, def_int, def_datetime, def_varchar, def_text)
VALUES(2, 3, 'EXTRACT_JAVASCRIPT', 'dk.netarkivet.harvester.datamodel.eav', 'ContentAttrType_Generic', 1, 5, 1, null, null, null);