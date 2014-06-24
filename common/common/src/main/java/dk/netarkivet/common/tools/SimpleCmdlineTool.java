
package dk.netarkivet.common.tools;

/**
 * A very abstracted interface for simple command line tools. Allows for setup,
 * teardown, argument checking, usage listing and (of course) running.
 *
 */
public interface SimpleCmdlineTool {

    /**
     * Check (command line) arguments.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     * @return True, if parameters are usable. False if not.
     */
    boolean checkArgs(String... args);

    /**
     * Create any resource which may requires an explicit teardown. Implement
     * teardown in the teardown method.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     */
    void setUp(String... args);

    /**
     * Teardown any resource which requires an explicit teardown. Implement
     * creation of these in the setup method. Note that not all objects may be
     * created in case of an exception during setup, so check for null!!!
     */
    void tearDown();

    /**
     * Run the tool. Any resources that can be managed without reliable teardown
     * may be created here.
     *
     * @param args
     *            Usually the command line arguments passed directly from a
     *            public static void main(String[] args) method.
     */
    void run(String... args);

    /**
     * Describes the parameters that this tool accepts.
     *
     * @return The parameter description in a String object.
     */
    String listParameters();
}
