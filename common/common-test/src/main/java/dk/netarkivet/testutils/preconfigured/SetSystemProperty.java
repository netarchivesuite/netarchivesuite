
package dk.netarkivet.testutils.preconfigured;

/** This class allows setting a system property temporarily.
 * Do not attempt to set it to null, as that will break. */
public class SetSystemProperty implements TestConfigurationIF {
    private String oldValue;
    private String property;
    private String newValue;

    public SetSystemProperty(String property, String newValue) {
        this.property = property;
        this.newValue = newValue;
    }

    public void setUp() {
        oldValue = System.getProperty(property);
        System.setProperty(property, newValue);
    }

    public void tearDown() {
        if (oldValue != null) {
            System.setProperty(property, oldValue);
        } else {
            System.clearProperty(property);
        }
    }
}
