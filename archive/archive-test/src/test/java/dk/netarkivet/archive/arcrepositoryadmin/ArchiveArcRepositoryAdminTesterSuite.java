package dk.netarkivet.archive.arcrepositoryadmin;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 *  Testsuite for the classes in the dk.netarkivet.archive.arcrepositoryadmin
 *  package.
 */
public class ArchiveArcRepositoryAdminTesterSuite
{
    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite(ArchiveArcRepositoryAdminTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(AdminDataTester.class);
        suite.addTestSuite(ChecksumStatusTester.class);
        //suite.addTestSuite(DatabaseAdminTester.class); All tests have been disabled.
        suite.addTestSuite(DBTester.class);
        suite.addTestSuite(FileListStatusTester.class);
        suite.addTestSuite(ReadOnlyAdminDataTester.class);
        suite.addTestSuite(ReplicaCacheDatabaseTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", ArchiveArcRepositoryAdminTesterSuite.class.getName()};
        TestRunner.main(args2);
    }
}
