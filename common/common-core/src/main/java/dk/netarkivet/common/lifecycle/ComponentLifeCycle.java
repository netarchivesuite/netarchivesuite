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

/**
 * Extends the default construction -> deconstruction object life cycle with addition steps, giving users of
 * <code>ComponentLifeCycle</code> better control over the component startup and shutdown phases.
 */
public interface ComponentLifeCycle {

    /**
     * Implements functionality for starting an instances of this <code>ComponentLifeCycle</code> object. This may be
     * loading files, establish connections, initializing data, starting threads, etc.
     */
    void start();

    /**
     * The inverse of the <code>start()</code> method. Contains functionality for deallocation of ressources, clearing
     * data, closing connections, stopping threads, etc
     */
    void shutdown();

}
