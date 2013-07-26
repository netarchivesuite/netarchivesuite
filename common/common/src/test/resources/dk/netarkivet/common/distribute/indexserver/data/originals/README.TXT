The lucene indices here are now in the lucene format used by Lucene 3.6, which is also readable by Lucene 4.X.

If upgrading is necessary again, use the IndexUpgrader tool bundled with Lucene:

java -cp lucene-core-$VERSION.jar org.apache.lucene.index.IndexUpgrader -verbose /path/to/index

Note of caution:
This command replaces the old index files with the new index files, so you might want to make a backup of the old index files before you do that.
