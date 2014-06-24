package dk.netarkivet.harvester.harvesting;

import junit.framework.TestCase;
import org.archive.util.SurtPrefixSet;
import dk.netarkivet.harvester.harvesting.OnNSDomainsDecideRule;

/**
 * JUNIT test for the class OnNSDomainsDecideRule.
 */
public class OnNSDomainsDecideRuleTester extends TestCase {
    public OnNSDomainsDecideRuleTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testSURTprefixConversionDomains() throws Exception {

        /*
         First testing that the original way of making URls to SURTs behaves right on domains
         */
        assertEquals("http://(dk,dr,www,",SurtPrefixSet.prefixFromPlain("www.dr.dk"));

        assertEquals("http://(dk,dr,",SurtPrefixSet.prefixFromPlain("http://dr.dk"));

        /*
         Then testing using OnNSDomainsDecideRule - that defines the domain
         using DomainNameQueueAssignmentPolicy
         */

        OnNSDomainsDecideRule oddr = new OnNSDomainsDecideRule("");

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk"));

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk/sporten/index.php"));

        assertEquals("http://(dk,dr,",oddr.prefixFrom("http://www.dr.dk/sporten/"));

        assertEquals("http://(dk,tv2,",oddr.prefixFrom("http://sporten.tv2.dk/fodbold/"));

        assertEquals("http://(uk,co,sports,",oddr.prefixFrom("http://www.sports.co.uk/index"));

    }

    public void testSURTprefixConversionHosts() throws Exception {

        /*
         Testing using the convertPrefixToHost used by OnHostsDecideRule
         */
        assertEquals("http://(dk,tv2,sporten,)",
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlain("http://sporten.tv2.dk/fodbold/")));

        assertEquals("http://(com,blogspot,jmvietnam07,)",
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlain("jmvietnam07.blogspot.com/")));

    }

    public void testSURTprefixConversionPaths() throws Exception {

        /*
         Testing using the 'original' way of transforming URLs to SURTs
         used by SurtPrefixesDecideRule
         */

        assertEquals("http://(com,geocities,www,)/athens/2344/",
                SurtPrefixSet.prefixFromPlain("http://www.geocities.com/Athens/2344/"));

        assertEquals("http://(com,geocities,www,)/athens/2344/",
                SurtPrefixSet.prefixFromPlain("http://www.geocities.com/Athens/2344/index.php"));

    }

    public void testSURTprefixConversionNonValidDomain() throws Exception {

        assertEquals(OnNSDomainsDecideRule.NON_VALID_DOMAIN,
                SurtPrefixSet.convertPrefixToHost(
                        SurtPrefixSet.prefixFromPlain("http:/not?valid;bla%¤/(")));

    }

}
