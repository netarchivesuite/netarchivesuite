package dk.netarkivet.wayback;

import org.archive.wayback.UrlCanonicalizer;

import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:15:15 AM To
 * change this template use File | Settings | File Templates.
 */
public class UrlCanonicalizerFactory extends SettingsFactory<UrlCanonicalizer> {

    public static UrlCanonicalizer getDefaultUrlCanonicalizer() {
        return SettingsFactory.getInstance(WaybackSettings.URL_CANONICALIZER_CLASSNAME);
    }

}
