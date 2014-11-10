Tests in this package involve ingest of the full production databases - harvestdb, admindb and checksumdb.
There are three levels of test

1. Schema-level sanity test: This test involves ingest only of the database schema from the prod databases plus the
minimal data (schemaversions + ordertemplates) for the system to start. Basic functionality tests are then
    1. Creation of some domains
    1. Harvest generation and job creation
    1. Upload to arcrepository + basic arcrepository functionality
1. Intermediate sanity test: Run exactly as the full performance test, but with a smaller data set. In practice use a dump
of the PLIGT databases + a list of 25000 domains.

1. Full performance test with production data. These are run as four separate test methods with a single
setup() method that reads in the production data. The actual tests are:
    1. Bitpreservation tests as in TEST7 (2 of these)
    1. Domain ingestion from a text file
    1. Snapshot job generation as in TEST7


These tests form a dependency hierarchy - the later tests are dependent on the earlier tests.

Expected runtimes for the tests are 15 minutes / 1-2 hours / 16 hours

