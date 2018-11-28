/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertEquals;

import org.archive.util.SurtPrefixSet;
import org.junit.Test;

/**
 * JUNIT test for the class OnNSDomainsDecideRule.
 */
public class OnNSDomainsDecideRuleTester {

    @Test
    public void testSURTprefixConversionDomains() throws Exception {

        /*
         * First testing that the original way of making URls to SURTs behaves right on domains
         */
        assertEquals("http://(dk,dr,www,",
        		SurtPrefixSet.prefixFromPlainForceHttp("www.dr.dk"));

        assertEquals("http://(dk,dr,",
        		SurtPrefixSet.prefixFromPlainForceHttp("http://dr.dk"));
        /*
         * Then testing using OnNSDomainsDecideRule - that defines the domain using DomainNameQueueAssignmentPolicy
         */

        OnNSDomainsDecideRule oddr = new OnNSDomainsDecideRule();

        assertEquals("http://(dk,dr,", oddr.prefixFrom("http://www.dr.dk"));

        assertEquals("http://(dk,dr,", oddr.prefixFrom("http://www.dr.dk/sporten/index.php"));

        assertEquals("http://(dk,dr,", oddr.prefixFrom("http://www.dr.dk/sporten/"));

        assertEquals("http://(dk,tv2,", oddr.prefixFrom("http://sporten.tv2.dk/fodbold/"));

        assertEquals("http://(uk,co,sports,", oddr.prefixFrom("http://www.sports.co.uk/index"));

    }

    @Test
    public void testSURTprefixConversionHosts() throws Exception {

        /*
         * Testing using the convertPrefixToHost used by OnHostsDecideRule
         */
        assertEquals("http://(dk,tv2,sporten,)",
                SurtPrefixSet.convertPrefixToHost(
                		SurtPrefixSet.prefixFromPlainForceHttp("http://sporten.tv2.dk/fodbold/")));

        assertEquals("http://(com,blogspot,jmvietnam07,)",
                SurtPrefixSet.convertPrefixToHost(
                		SurtPrefixSet.prefixFromPlainForceHttp("jmvietnam07.blogspot.com/")));
    }

    @Test
    public void testSURTprefixConversionPaths() throws Exception {

        /*
         * Testing using the 'original' way of transforming URLs to SURTs used by SurtPrefixesDecideRule
         */
        assertEquals("http://(com,geocities,www,)/athens/2344/",
        		SurtPrefixSet.prefixFromPlainForceHttp("http://www.geocities.com/Athens/2344/"));

        assertEquals("http://(com,geocities,www,)/athens/2344/",
        		SurtPrefixSet.prefixFromPlainForceHttp("http://www.geocities.com/Athens/2344/index.php"));
    }

    //FIXME
    public void testSURTprefixConversionNonValidDomain() throws Exception {

        assertEquals(OnNSDomainsDecideRule.NON_VALID_DOMAIN,
                SurtPrefixSet.convertPrefixToHost(
                		//FIXME
                		//SurtPrefixSet.prefixFromPlain("http:/not?valid;bla%¤/(")));
                		SurtPrefixSet.prefixFromPlainForceHttp("http:/not?valid;bla%¤/(")));

    }

}
