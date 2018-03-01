/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Takes care of the lifecycling of subcomponents(children).
 * <p>
 * When extending this class you must:
 * <ol>
 * <li>Add all lifecycle subcomponents with the addChild, before the start method is called.
 * <li>Call the <code>super.start()</code> operation to start the children.
 * <li>Call the <code>super.shutdown</code> operation to
 */
public class LifeCycleComponent implements ComponentLifeCycle {

    /** The instance logger. */
    private static final Logger log = LoggerFactory.getLogger(LifeCycleComponent.class);

    /** The child-components of this lifecycle. */
    private List<ComponentLifeCycle> children = new ArrayList<ComponentLifeCycle>();

    @Override
    public void start() {
        log.debug("Starting {}", toString());
        for (ComponentLifeCycle child : children) {
            child.start();
        }
    }

    @Override
    public void shutdown() {
        log.debug("Shutting down {}", toString());
        for (ComponentLifeCycle child : children) {
            child.shutdown();
        }
    }

    /**
     * Adds a child <code>ComponentLifeCycle</code>. The childs lifecycle will be managed by by the
     * <code>LifeCycleComponent</code>.
     *
     * @param childComponent The child to add
     */
    public void addChild(ComponentLifeCycle childComponent) {
        ArgumentNotValid.checkNotNull(childComponent, "Child can not be null");
        children.add(childComponent);
    }

}
