/*
 * #%L
 * Netarchivesuite - Heritrix 3 extensions
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.SchedulingConstants;
import org.archive.modules.deciderules.surt.SurtPrefixedDecideRule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.SurtPrefixSet;
import org.junit.Assert;
import org.junit.Test;

public class NASSurtPrefixedDecideRuleTester {

    @Test
    public void test_subdomains_rewrite() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        Method method;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;
        int idx;
        int idx2;
        String scheme;
        String surtHost;
        String port;
        String path;

        decideRule = new NASSurtPrefixedDecideRule();
        Assert.assertEquals(true, decideRule.removeW3xSubDomain);
        Assert.assertEquals(true, decideRule.addBeforeRemovingW3xSubDomain);
        Assert.assertEquals(true, decideRule.addW3SubDomain);
        Assert.assertEquals(true, decideRule.addBeforeAddingW3SubDomain);
        Assert.assertEquals(true, decideRule.allowSubDomainsRewrite);
        Assert.assertEquals(decideRule.removeW3xSubDomain, decideRule.getRemoveW3xSubDomain());
        Assert.assertEquals(decideRule.addBeforeRemovingW3xSubDomain, decideRule.getAddBeforeRemovingW3xSubDomain());
        Assert.assertEquals(decideRule.addW3SubDomain, decideRule.getAddW3SubDomain());
        Assert.assertEquals(decideRule.addBeforeAddingW3SubDomain, decideRule.getAddBeforeAddingW3SubDomain());
        Assert.assertEquals(decideRule.allowSubDomainsRewrite, decideRule.getAllowSubDomainsRewrite());
        decideRule.setRemoveW3xSubDomain(false);
        decideRule.setAddBeforeRemovingW3xSubDomain(false);
        decideRule.setAddW3SubDomain(false);
        decideRule.setAddBeforeAddingW3SubDomain(false);
        decideRule.setAllowSubDomainsRewrite(false);
        Assert.assertEquals(false, decideRule.removeW3xSubDomain);
        Assert.assertEquals(false, decideRule.addBeforeRemovingW3xSubDomain);
        Assert.assertEquals(false, decideRule.addW3SubDomain);
        Assert.assertEquals(false, decideRule.addBeforeAddingW3SubDomain);
        Assert.assertEquals(false, decideRule.allowSubDomainsRewrite);
        Assert.assertEquals(decideRule.removeW3xSubDomain, decideRule.getRemoveW3xSubDomain());
        Assert.assertEquals(decideRule.addBeforeRemovingW3xSubDomain, decideRule.getAddBeforeRemovingW3xSubDomain());
        Assert.assertEquals(decideRule.addW3SubDomain, decideRule.getAddW3SubDomain());
        Assert.assertEquals(decideRule.addBeforeAddingW3SubDomain, decideRule.getAddBeforeAddingW3SubDomain());
        Assert.assertEquals(decideRule.allowSubDomainsRewrite, decideRule.getAllowSubDomainsRewrite());
        decideRule.setRemoveW3xSubDomain(true);
        decideRule.setAddBeforeRemovingW3xSubDomain(true);
        decideRule.setAddW3SubDomain(true);
        decideRule.setAddBeforeAddingW3SubDomain(true);
        decideRule.setAllowSubDomainsRewrite(true);
        Assert.assertEquals(true, decideRule.removeW3xSubDomain);
        Assert.assertEquals(true, decideRule.addBeforeRemovingW3xSubDomain);
        Assert.assertEquals(true, decideRule.addW3SubDomain);
        Assert.assertEquals(true, decideRule.addBeforeAddingW3SubDomain);
        Assert.assertEquals(true, decideRule.allowSubDomainsRewrite);
        Assert.assertEquals(decideRule.removeW3xSubDomain, decideRule.getRemoveW3xSubDomain());
        Assert.assertEquals(decideRule.addBeforeRemovingW3xSubDomain, decideRule.getAddBeforeRemovingW3xSubDomain());
        Assert.assertEquals(decideRule.addW3SubDomain, decideRule.getAddW3SubDomain());
        Assert.assertEquals(decideRule.addBeforeAddingW3SubDomain, decideRule.getAddBeforeAddingW3SubDomain());
        Assert.assertEquals(decideRule.allowSubDomainsRewrite, decideRule.getAllowSubDomainsRewrite());

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,www2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,www23,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,www1,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,:8080)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,www1,:8443)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,:80)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:80)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(false);
            decideRule.setAddBeforeRemovingW3xSubDomain(false);
            decideRule.setAddW3SubDomain(false);
            decideRule.setAddBeforeAddingW3SubDomain(false);
            decideRule.setAllowSubDomainsRewrite(false);

            // Use reflection to call protected method.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);
            method = SurtPrefixedDecideRule.class.getDeclaredMethod("prefixFrom", new Class<?>[] {String.class});
            method.setAccessible(true);

            for (int i=0; i<cases.length; ++i) {
                String originalUri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(originalUri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(originalUri);
                //surt = decideRule.prefixFrom(curi.getURI());
                surt = (String)method.invoke(decideRule, new Object[] {curi.getURI()});

                if (surt != null) {
                    idx = surt.indexOf("://");
                    if (idx != -1) {
                        scheme = surt.substring(0, idx);
                        idx += "://".length();
                        idx2 = surt.indexOf(')', idx);
                        if (idx2 != -1 && surt.charAt(idx++) == '(') {
                            surtHost = surt.substring(idx, idx2);
                            path = surt.substring(idx2 + 1);
                            idx = surtHost.lastIndexOf(':');
                            if (idx != -1) {
                                port = surtHost.substring(idx);
                                surtHost = surtHost.substring(0, idx);
                            } else {
                                port = "";
                            }
                            surt = decideRule.subDomainsRewrite(path, originalUri, scheme, surtHost, port, surt);
                        }
                    }
                }
                // debug
                //System.out.println(originalUri + " -> " + surt );
                Assert.assertEquals(expectedSurt, surt);

                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(originalUri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,)/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,nyheder,)/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,www,",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,www,"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,www,"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,",
                    new String[] {
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,",
                    new String[] {
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,",
                    new String[] {
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,www2,",
                    new String[] {
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,www23,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,www1,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,",
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,:8080)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,",
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,www1,:8443",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,",
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,:80",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,",
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,",
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(false);
            decideRule.setAddBeforeRemovingW3xSubDomain(false);
            decideRule.setAddW3SubDomain(false);
            decideRule.setAddBeforeAddingW3SubDomain(false);
            decideRule.setAllowSubDomainsRewrite(true);

            // Use reflection to call protected method.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);
            method = SurtPrefixedDecideRule.class.getDeclaredMethod("prefixFrom", new Class<?>[] {String.class});
            method.setAccessible(true);

            for (int i=0; i<cases.length; ++i) {
                String originalUri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(originalUri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(originalUri);
                //surt = decideRule.prefixFrom(curi.getURI());
                surt = (String)method.invoke(decideRule, new Object[] {curi.getURI()});

                if (surt != null) {
                    idx = surt.indexOf("://");
                    if (idx != -1) {
                        scheme = surt.substring(0, idx);
                        idx += "://".length();
                        idx2 = surt.indexOf(')', idx);
                        if (idx2 != -1 && surt.charAt(idx++) == '(') {
                            surtHost = surt.substring(idx, idx2);
                            path = surt.substring(idx2 + 1);
                            idx = surtHost.lastIndexOf(':');
                            if (idx != -1) {
                                port = surtHost.substring(idx);
                                surtHost = surtHost.substring(0, idx);
                            } else {
                                port = "";
                            }
                            surt = decideRule.subDomainsRewrite(path, originalUri, scheme, surtHost, port, surt);
                        }
                    }
                }
                // debug
                //System.out.println(originalUri + " -> " + surt );
                Assert.assertEquals(expectedSurt, surt);

                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(originalUri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_removew3x() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,)/",
                    new String[] {
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,:8080)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,:8443)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,:80)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,:80)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(true);
            decideRule.setAddBeforeRemovingW3xSubDomain(false);
            decideRule.setAddW3SubDomain(false);
            decideRule.setAddBeforeAddingW3SubDomain(false);
            decideRule.setAllowSubDomainsRewrite(false);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_removew3x_addbefore() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,)/",
                    new String[] {
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,:8080)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,:8443)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,:80)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:80)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(true);
            decideRule.setAddBeforeRemovingW3xSubDomain(true);
            decideRule.setAddW3SubDomain(false);
            decideRule.setAddBeforeAddingW3SubDomain(false);
            decideRule.setAllowSubDomainsRewrite(false);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_addw3() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,www,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,www2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,www23,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,www1,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,www,:8080)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,www1,:8443)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,www,:80)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:80)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(false);
            decideRule.setAddBeforeRemovingW3xSubDomain(false);
            decideRule.setAddW3SubDomain(true);
            decideRule.setAddBeforeAddingW3SubDomain(false);
            decideRule.setAllowSubDomainsRewrite(false);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_addw3_addbefore() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,www,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,)/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,)/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,)/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,)/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,www2,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,)/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,www23,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,www1,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,www,:8080)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,www1,:8443)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,www,:80)/",
                    new String[] {
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:80)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:80)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(false);
            decideRule.setAddBeforeRemovingW3xSubDomain(false);
            decideRule.setAddW3SubDomain(true);
            decideRule.setAddBeforeAddingW3SubDomain(true);
            decideRule.setAllowSubDomainsRewrite(false);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_all_true_no_aource() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,www,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,www,)/",
                    new String[] {
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,)/",
                    new String[] {
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,www,:8080)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/en/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/en/"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,:8443)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,www,:80)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,)/",
                            "http://(dk,netarkivet,www2p,)/",
                            "http://(dk,netarkivet,wwwp2,)/",
                            "http://(dk,netarkivet,www2p,www,)/",
                            "http://(dk,netarkivet,wwwp2,www,)/",
                            "http://(dk,netarkivet,www2,)/",
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,:80)/",
                            "http://(dk,tv2,:8080)/",
                            "http://(dk,tv2,:8443)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/",
                            "http://(dk,tv2,www,:80)/",
                            "http://(dk,tv2,www,:8080)/",
                            "http://(dk,tv2,www1,:8443)/"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(true);
            decideRule.setAddBeforeRemovingW3xSubDomain(true);
            decideRule.setAddW3SubDomain(true);
            decideRule.setAddBeforeAddingW3SubDomain(true);
            decideRule.setAllowSubDomainsRewrite(true);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                //curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

    @Test
    public void test_all_true_with_aource() {
        Object[][] cases;
        NASSurtPrefixedDecideRule decideRule;
        Field field;
        SurtPrefixSet surtPrefixSet;
        UURI uuri;
        CrawlURI curi;
        String surt;

        cases = new Object[][] {
                {
                    "http://sport.tv2.dk/mokeybusiness/",
                    "http://(dk,tv2,sport,www,)/mokeybusiness/",
                    new String[] {
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://nyheder.tv2.dk/business",
                    "http://(dk,tv2,nyheder,www,)/",
                    new String[] {
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/"
                    }
                },
                {
                    "http://www.tv2.dk/",
                    "http://(dk,tv2,)/",
                    new String[] {
                            "http://(dk,tv2,)/",
                            "http://(dk,tv2,nyheder,)/",
                            "http://(dk,tv2,nyheder,www,)/",
                            "http://(dk,tv2,sport,)/mokeybusiness/",
                            "http://(dk,tv2,sport,www,)/mokeybusiness/",
                            "http://(dk,tv2,www,)/"
                    }
                },
                {
                    "http://www.tv2.dk",
                    "http://(dk,tv2,",
                    new String[] {
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://tv2.dk/",
                    "http://(dk,tv2,www,)/",
                    new String[] {
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://tv2.dk",
                    "http://(dk,tv2,www,",
                    new String[] {
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www2p.netarkivet.dk",
                    "http://(dk,netarkivet,www2p,www,",
                    new String[] {
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://wwwp2.netarkivet.dk",
                    "http://(dk,netarkivet,wwwp2,www,",
                    new String[] {
                            "http://(dk,netarkivet,www2p,",
                            "http://(dk,netarkivet,wwwp2,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www2.netarkivet.dk",
                    "http://(dk,netarkivet,",
                    new String[] {
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://www23.kb.dk/sorte/diamant/",
                    "http://(dk,kb,)/sorte/diamant/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "https://www1.tv2.dk:8443/en/sti",
                    "http://(dk,tv2,:8443)/en/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "http://tv2.dk:8080/",
                    "http://(dk,tv2,www,:8080)/",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "https://www1.tv2.dk:8443",
                    "http://(dk,tv2,:8443",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                },
                {
                    "https://tv2.dk:80",
                    "http://(dk,tv2,www,:80",
                    new String[] {
                            "http://(dk,kb,)/sorte/diamant/",
                            "http://(dk,kb,www23,)/sorte/diamant/",
                            "http://(dk,netarkivet,",
                            "http://(dk,tv2,"
                    }
                }
        };

        try {
            decideRule = new NASSurtPrefixedDecideRule();
            decideRule.setRemoveW3xSubDomain(true);
            decideRule.setAddBeforeRemovingW3xSubDomain(true);
            decideRule.setAddW3SubDomain(true);
            decideRule.setAddBeforeAddingW3SubDomain(true);
            decideRule.setAllowSubDomainsRewrite(true);

            // Use reflection to read protected field.
            field = SurtPrefixedDecideRule.class.getDeclaredField("surtPrefixes");
            field.setAccessible(true);
            surtPrefixSet = (SurtPrefixSet)field.get(decideRule);

            for (int i=0; i<cases.length; ++i) {
                String uri = (String)cases[i][0];
                String expectedSurt = (String)cases[i][1];
                String[] expectedPrefixes = (String[])cases[i][2];
                Arrays.sort(expectedPrefixes);

                // Construct CrawlURI the same way TextSeedModule does.
                uuri = UURIFactory.getInstance(uri);
                curi = new CrawlURI(uuri);
                curi.setSeed(true);
                curi.setSchedulingDirective(SchedulingConstants.MEDIUM);
                curi.setSourceTag(uri);
                surt = decideRule.addedSeedImpl(curi);
                // debug
                //System.out.println(uri + " -> " + surt);
                Assert.assertEquals(expectedSurt, surt);

                String[] prefixes = new String[surtPrefixSet.size()];
                surtPrefixSet.toArray(prefixes);
                Arrays.sort(prefixes);
                Assert.assertEquals(expectedPrefixes.length, prefixes.length);
                Assert.assertArrayEquals(expectedPrefixes, prefixes);
            }

            uuri = new UURI("http://www.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(false, decideRule.accepts(curi));

            uuri = new UURI("http://www23.kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

            uuri = new UURI("http://kb.dk/sorte/diamant/lort", true, "UTF-8");
            curi = new CrawlURI(uuri);
            // debug
            //System.out.println(curi.toString() + " " + decideRule.accepts(curi));
            Assert.assertEquals(true, decideRule.accepts(curi));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (SecurityException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        } catch (URIException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }
    }

}
