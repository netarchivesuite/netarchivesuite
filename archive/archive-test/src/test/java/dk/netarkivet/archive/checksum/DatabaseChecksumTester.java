package dk.netarkivet.archive.checksum;

import com.sleepycat.je.DatabaseException;

import junit.framework.TestCase;

@SuppressWarnings({ "unused"})
public class DatabaseChecksumTester extends TestCase {

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        DatabaseChecksumArchive dca = new DatabaseChecksumArchive();
    }
    
    public void testConstructor() throws DatabaseException {
        DatabaseChecksumArchive dca = new DatabaseChecksumArchive();
    }
    
    

}
