package dk.netarkivet.wayback.batch;

import org.archive.wayback.UrlCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer;

/**
 * A factory for returning a UrlCanonicalizer.
 */
@SuppressWarnings({ "deprecation"})
public class UrlCanonicalizerFactory extends SettingsFactory<UrlCanonicalizer> {

	/** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(UrlCanonicalizerFactory.class);

    /**
     * This method returns an instance of the UrlCanonicalizer class specified
     * in the settings.xml for the dk.netarkivet.wayback module. In the event
     * that reading this file generates a SecurityException, as may occur in
     * batch operation if security does not allow System properties to be read,
     * the method will fall back on returning an instance of the class
     * dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer
     * @return a canonicalizer for urls
     */
    public static UrlCanonicalizer getDefaultUrlCanonicalizer()  {
        try {
            return SettingsFactory.getInstance(
                    WaybackSettings.URL_CANONICALIZER_CLASSNAME);
        } catch (SecurityException e) {
            logger.debug("The requested canoncializer could not be loaded. Falling back to {}",
            		NetarchiveSuiteAggressiveUrlCanonicalizer.class.toString(), e);
            return new NetarchiveSuiteAggressiveUrlCanonicalizer();
        }
    }

}
