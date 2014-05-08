Tests in this package involve ingest of the full production databases - harvestdb, admindb and checksumdb.
There are three levels of test
1. Schema-level sanity test: This test involves ingest only of the database schema from the prod databases plus the
minimal data (schemaversions + ordertemplates) for the system to start. Basic functionality tests are then
 1. Creation of some domains
 1. Harvest generation and job creation
 1. Upload to arcrepository + basic arcrepository functionality
1. Full-data migration + santiy test: Ingest the full production database and test that basic functionality is
working. Tests:
 1. Browse the various sections of the GUI and look for expected data.
1. Full performance test with production data:
 1. Bitpreservation tests as in TEST7
 1. Snapshot job generation as in TEST7

These tests form a dependence hierarchy - the later tests are dependent on the earlier tests.

Expected runtimes for the tests are 15 minutes / 1-2 hours / 1-2 days .

