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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** 
 * Takes care of the lifecycling of subcomponents(children). 
 * 
 * When extending this class you must: <ol>
 * <li> Add all lifecycle subcomponents with the addChild, before the start 
 * method is called.
 * <li> Call the <code>super.start()</code> operation to start the children.
 * <li> Call the <code>super.shutdown</code> operation to  
 */
public class LifeCycleComponent implements ComponentLifeCycle {    
    private List<ComponentLifeCycle> children = new ArrayList<ComponentLifeCycle>();    

    private final Log log = LogFactory.getLog(getClass().getName());

    @Override
    public void start() {
        log.debug("Starting " + toString());
        for (ComponentLifeCycle child: children) {
            child.start();
        }
    }
    
    @Override
    public void shutdown() {
        log.debug("Shutting down " + toString());
        for (ComponentLifeCycle child: children) {
            child.shutdown();
        }
    }
    
    /**
     * Adds a child <code>ComponentLifeCycle</code>. The childs lifecycle will 
     * be managed by by the <code>LifeCycleComponent</code>.
     * @param childComponent The child to add
     */
    public void addChild(ComponentLifeCycle childComponent) {
        ArgumentNotValid.checkNotNull(childComponent, "Child can not be null");
        children.add(childComponent);        
    }
}
