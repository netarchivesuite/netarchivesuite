
package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

public class TestInfo {
    public static final File BASEDIR =
        new File("tests/dk/netarkivet/archive/arcrepository/distribute/data");

    public static final File ORIGINALS = new File(BASEDIR, "originals");

    public static final File WORKING = new File(BASEDIR, "working");

    public static final File ARCDIR = new File(WORKING, "local_files");
    public static final File ARCFILE = new File(ARCDIR, "Upload2.ARC");


}
