package dk.netarkivet.common.webinterface;

/**
 * A site section for test use.
 */
public class TestSiteSection extends SiteSection {
    public TestSiteSection() {
        super("Test", "Test", 1, new String[][]{{"Test", "Test"}},
              "Test", "Test");
    }

    /** No initialisation necessary in this site section. */
    public void initialize() {
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
