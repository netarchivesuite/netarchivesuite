package dk.netarkivet.harvester.datamodel;

import java.util.Date;

import dk.netarkivet.common.exceptions.PermissionDenied;

/**
 * Unit-tests for the ScheduleDBDAO class.
 */
public class ScheduleDBDAOTester extends DataModelTestCase {
    private static final String THIRTY_CHAR_STRING = "123456789012345678901234567890";

    public ScheduleDBDAOTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateChecksSize() throws Exception {
        Schedule s1 = TestInfo.getDefaultSchedule();
        ScheduleDAO dao = ScheduleDAO.getInstance();
        StringBuilder build = new StringBuilder(3030);
        for (int i = 0; i < 101; i++) {
            build.append(THIRTY_CHAR_STRING);
        }
        s1.setComments(build.toString());
        try {
            dao.update(s1);
            fail("Should throw PermissionDenied on comment of length "
                    + s1.getName().length());
        } catch (PermissionDenied e) {
            // expected
        }
        build = new StringBuilder(330);
        for (int i = 0; i < 11; i++) {
            build.append(THIRTY_CHAR_STRING);
        }
        Schedule s2 = new RepeatingSchedule(new Date(), 2, new HourlyFrequency(2),
                build.toString(), "Small comment");
        try {
            dao.create(s2);
            fail("Should throw PermissionDenied on name of length "
                    + s2.getName().length());
        } catch (PermissionDenied e) {
            //Expected
        }
    }
}