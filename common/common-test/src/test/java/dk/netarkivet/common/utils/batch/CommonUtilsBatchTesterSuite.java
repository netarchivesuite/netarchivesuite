package dk.netarkivet.common.utils.batch;

import dk.netarkivet.common.utils.batch.BatchFilterTester;
import dk.netarkivet.common.utils.batch.BatchLocalFilesTester;
import dk.netarkivet.common.utils.batch.ByteClassLoaderTester;
import dk.netarkivet.common.utils.batch.FileBatchJobTester;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJobTester;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJobTester;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Suite of unittests for the classes in the 
 * dk.netarkivet.commons.utils.batch package.
 */
public class CommonUtilsBatchTesterSuite {
    public static Test suite() {
        TestSuite suite;
        suite = new TestSuite(CommonUtilsBatchTesterSuite.class.getName());

        addToSuite(suite);

        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(BatchFilterTester.class);
        suite.addTestSuite(BatchLocalFilesTester.class);
        suite.addTestSuite(ByteClassLoaderTester.class);
        suite.addTestSuite(FileRemoverTester.class);
        suite.addTestSuite(FileBatchJobTester.class);
        suite.addTestSuite(LoadableFileBatchJobTester.class);
        suite.addTestSuite(LoadableJarBatchJobTester.class);
    }

    public static void main(String args[]) {
        String args2[] = { "-noloading",
                CommonUtilsBatchTesterSuite.class.getName() };
        TestRunner.main(args2);
    }
}
