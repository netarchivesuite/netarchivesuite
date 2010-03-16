/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.viewerproxy;

import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.viewerproxy.distribute.HTTPControllerServer;

/**
 * Singleton of a viewerproxy.
 * The viewerproxy consists of:
 * - A JobIndexCache, which is able to retrieve a Lucene index file for a list
 *   of jobs
 * - An ArcRepositoryClient used by ARCArchiveAccess
 * - An ARCArchiveAccess, which retrieves objects from an ARC repository
 * - A MissingURIRecorder, which records missing urls
 * - A DelegatingController, which delegates commands to change index and handle
 *   missing url collection
 * - An NotifyingURLResolver, which looks up URLs in an ARCArchiveAccess,
 *   and notifies Observers about missing URLs,
 * - An UnknownCommandResolver, which generates an error for unknown command
 *   urls and pass on non-command urls to the NotifyingURLResolver
 * - A GetDataResolver, which handles certain command urls for getting raw data
 *   and pass on the rest to the UnknownCommandResolver,
 * - A HTTPControllerServer, which delegates certain command urls to a controller
 *   and pass on the rest to the GetDataResolver,
 * - A WebProxy, which listens for http requests, and sends them to the
 *   HTTPControllerServer
 *
 */

public class ViewerProxy implements CleanupIF {
    private static ViewerProxy instance;
    private WebProxy webProxy;
    private HTTPControllerServer controllerServer;
    private UnknownCommandResolver unknownCommandResolver;
    private GetDataResolver getDataResolver;
    private NotifyingURIResolver notifyingURIResolver;
    private JobIndexCache luceneIndexCache;
    private ARCArchiveAccess arcArchiveAccess;
    private Controller controller;
    private MissingURIRecorder missingURIRecorder;
    private ViewerArcRepositoryClient arcRepositoryClient;

    /** Initiates the viewer proxy as described in class comment. */
    private ViewerProxy() {
        arcRepositoryClient = ArcRepositoryClientFactory.getViewerInstance();
        // The Lucene index covers all items, not just non-text
        luceneIndexCache = IndexClientFactory.getFullCrawllogInstance();
        arcArchiveAccess = new ARCArchiveAccess(arcRepositoryClient);
        missingURIRecorder = new MissingURIRecorder();
        controller = new DelegatingController(missingURIRecorder, luceneIndexCache,
                                    arcArchiveAccess);
        notifyingURIResolver = new NotifyingURIResolver(arcArchiveAccess,
                                                        missingURIRecorder);
        unknownCommandResolver = new UnknownCommandResolver(notifyingURIResolver);
        getDataResolver = new GetDataResolver(unknownCommandResolver,
                arcRepositoryClient);
        controllerServer = new HTTPControllerServer(controller,
                                                    getDataResolver);
        webProxy = new WebProxy(controllerServer);
    }

    /** Get singleton instance of viewerproxy. See class comment for details.
     * @return The viewerproxy instance.
     */
    public static ViewerProxy getInstance() {
        if (instance == null) {
            instance = new ViewerProxy();
        }
        return instance;
    }

    /** Shuts down webproxy and arcrepositoryclient, and resets singleton. */
    public void cleanup() {
        instance = null;
        webProxy.kill();
        arcRepositoryClient.close();
    }
}
