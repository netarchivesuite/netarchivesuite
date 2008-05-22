package dk.netarkivet.testutils.preconfigured;

/**
 *
 * This interface should be implemented by classes
 * that encapsulate one particular aspect to be handled
 * by setUp() and tearDown() in many unit tests.
 */
interface TestConfigurationIF {
    /**
     * Set up the test environment to handle the particular
     * aspect that we handle.
     */
    public void setUp();
    /**
     * Reverse the effect of setUp(), setting the environment
     * back to its standard state.
     */
    public void tearDown();
}
