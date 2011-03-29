/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */
package dk.netarkivet.common.lifecycle;

/**
 * Extends the default construction -> deconstruction object life cycle with 
 * addition steps, giving users of
 * <code>ComponentLifeCycle</code> better control over the component startup 
 * and shutdown phases.
 */
public interface ComponentLifeCycle {
    
    /**
     * Implements functionality for starting an instances of this 
     * <code>ComponentLifeCycle</code> object. 
     * This may be loading files, establish connections, initializing data,
     *  starting threads, etc.
     */
    void start();
    
    /**
     * The inverse of the <code>start()</code> method. Contains functionality 
     * for deallocation of ressources, clearing data, closing connections, 
     * stopping threads, etc
     */
    void shutdown();
}
