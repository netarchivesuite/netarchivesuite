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

import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.surt.SurtPrefixedDecideRule;

/**
 * Extended <code>SurtPrefixedDecideRule</code> class.
 * Enable SURT seeds that allow sub-domains if the original seed URI has no path at all.
 * Can also add/remove www/www[<x>] from the SURT and/or also add the orginal SURT seed.
 *
 * @author nicl
 */
public class NASSurtPrefixedDecideRule extends SurtPrefixedDecideRule {

    /**
     * UUID.
     */
    private static final long serialVersionUID = 3334790462876505839L;

    /** Logger instance. */
    private static final Logger logger = Logger.getLogger(NASSurtPrefixedDecideRule.class.getName());

    /**
     * Enable/Disable the removing of a preceding www[<x>] in SURT host if present.
     */
    protected boolean removeW3xSubDomain = true;
    public boolean getRemoveW3xSubDomain() {
        return removeW3xSubDomain;
    }
    public void setRemoveW3xSubDomain(boolean removeW3xSubDomain) {
        this.removeW3xSubDomain = removeW3xSubDomain;
    }

    /**
     * Enable/Disable the adding of the original SURT before removing the preceding www[<x>]. 
     */
    protected boolean addBeforeRemovingW3xSubDomain = true;
    public boolean getAddBeforeRemovingW3xSubDomain() {
        return addBeforeRemovingW3xSubDomain;
    }
    public void setAddBeforeRemovingW3xSubDomain(boolean addBeforeRemovingW3xSubDomain) {
        this.addBeforeRemovingW3xSubDomain = addBeforeRemovingW3xSubDomain;
    }

    /**
     * Enable/Disable the adding of a preceding www in SURT host if none is present.
     */
    protected boolean addW3SubDomain = true;
    public boolean getAddW3SubDomain() {
        return addW3SubDomain;
    }
    public void setAddW3SubDomain(boolean addW3SubDomain) {
        this.addW3SubDomain = addW3SubDomain;
    }

    /**
     * Enable/Disable the adding of the original SURT before adding a preceding www.
     */
    protected boolean addBeforeAddingW3SubDomain = true;
    public boolean getAddBeforeAddingW3SubDomain() {
        return addBeforeAddingW3SubDomain;
    }
    public void setAddBeforeAddingW3SubDomain(boolean addBeforeAddingW3SubDomain) {
        this.addBeforeAddingW3SubDomain = addBeforeAddingW3SubDomain;
    }

    /**
     * Enable/Disable the removing of ')/' in the SURT if the original URI does not have a path at all.
     */
    protected boolean allowSubDomainsRewrite = true;
    public boolean getAllowSubDomainsRewrite() {
        return allowSubDomainsRewrite;
    }
    public void setAllowSubDomainsRewrite(boolean allowSubDomainsRewrite) {
        this.allowSubDomainsRewrite = allowSubDomainsRewrite;
    }

    @Override
    public void addedSeed(final CrawlURI curi) {
        if(getSeedsAsSurtPrefixes()) {
            addedSeedImpl(curi);
        }
    }

    /**
     * <code>addedSeed</code implementation method to facilitate unit testing. 
     * @param curi <code>CrawlURI</code> object to convert
     * @return URI converted to SURT string
     */
    protected String addedSeedImpl(final CrawlURI curi) {
        String originalUri = curi.getSourceTag();
        if (originalUri == null && allowSubDomainsRewrite) {
            logger.warning("originalUri not available");
        }
        String surt = prefixFrom(curi.getURI());
        int idx;
        int idx2;
        String scheme;
        String surtHost;
        String port;
        String path;
        String part;
        boolean bRemoveW3x;
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
                    // Look for www[<x>] in host name.
                    idx = surtHost.lastIndexOf(',');
                    if (idx != -1) {
                        idx2 = idx;
                        if (idx == surtHost.length() - 1 && idx > 0) {
                            idx = surtHost.lastIndexOf(',', idx - 1);
                        }
                    }
                    if (idx != -1 && idx < idx2) {
                        part = surtHost.substring(idx + 1, idx2);
                        if (part.startsWith("www")) {
                            bRemoveW3x = true;
                            if (part.length() > 3) {
                                try {
                                    Integer.parseInt(part.substring(3));
                                } catch (NumberFormatException e) {
                                    bRemoveW3x = false;
                                }
                            }
                        } else {
                            bRemoveW3x = false;
                        }
                        if (bRemoveW3x) {
                            if (removeW3xSubDomain) {
                                if (addBeforeRemovingW3xSubDomain) {
                                    surt = subDomainsRewrite(path, originalUri, scheme, surtHost, port, surt);
                                    surtPrefixes.add(surt);
                                }
                                surtHost = surtHost.substring(0, idx + 1);
                                surt = scheme + "://(" + surtHost + port + ")" + path;
                            }
                        } else {
                            if (addW3SubDomain) {
                                if (addBeforeAddingW3SubDomain) {
                                    surt = subDomainsRewrite(path, originalUri, scheme, surtHost, port, surt);
                                    surtPrefixes.add(surt);
                                }
                                surtHost = surtHost + "www,";
                                surt = scheme + "://(" + surtHost + port + ")" + path;
                            }
                        }
                    } else {
                        logger.warning("very strange surt host");
                    }
                    surt = subDomainsRewrite(path, originalUri, scheme, surtHost, port, surt);
                }
            }
        }
        surtPrefixes.add(surt);
        return surt;
    }

    /**
     * Method to rewrite the SURT to allow sub-domains if the original URI does not have a path at all.
     * @param path SURT path string
     * @param originalUri original URI
     * @param scheme SURT scheme string
     * @param surtHost SURT host as comma separated list of names
     * @param surt URI converted to SURT by the default Heritrix means
     * @return original or rewritten SURT, depending on the SURT and the original URI
     */
    protected String subDomainsRewrite(String path, String originalUri, String scheme, String surtHost, String port, String surt) {
        int idx;
        if (allowSubDomainsRewrite) {
            if ("/".compareTo(path) == 0) {
                if (originalUri != null) {
                    idx = originalUri.indexOf("://");
                    if (idx != -1) {
                        idx += "://".length();
                        idx = originalUri.indexOf('/', idx);
                        if (idx == -1) {
                            surt = scheme + "://(" + surtHost + port; 
                        }
                    }
                }
            }
        }
        return surt;
    }

}
