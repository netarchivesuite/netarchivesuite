/*
 * #%L
 * Netarchivesuite - archive
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
/**
 * This module makes it possible to setup and run a repository with replication, active bit consistency checks for
 * bit-preservation, and support for distributed batch jobs on the archive.
 * <ul>
 *     <li>The archiving component offers a secure environment for storing your harvested material. It is designed for high
 *         preservation guarantees on bit preservation.
 *     </li>
 *     <li>It allows for replication of data on different locations, and distribution of content on several servers on each
 *         location. It supports different software and hardware platforms.
 *     </li>
 *     <li>The module allows for distributed batch jobs, running the same jobs on all servers at a location in parallel,
 *         and merging the results.
 *     </li>
 *     <li>An index of data in the archive allows fast access to the harvested materials.</li>
 * </ul>
 */
package dk.netarkivet.archive;

