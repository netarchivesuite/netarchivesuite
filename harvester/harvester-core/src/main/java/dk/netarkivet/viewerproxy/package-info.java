/*
 * #%L
 * Netarchivesuite - harvester
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
 * This module gives access to previously harvested material, through a proxy solution.
 * <ul>
 *  <li>The viewerproxy component supports transparent access to the harvested data, using a proxy solution, and an
 *      archive with an index over URLs stored in the archive.
 *  </li>
 *  <li>Support for browsing an entire crawl (like a snapshot or event harvest) or a single job (what one machine
 *      harvested).
 *  </li>
 *  <li>Allows for collecting unharvested URLs while browsing, for use in curation, and to include these URLs in the
 *      next crawl.
 *  </li>
 * </ul>
 */
package dk.netarkivet.viewerproxy;

