/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** A message to send from the IndexServer to HarvestJobManager, that the 
 * index required by harvest with a given ID is ready.
 */
public class IndexReadyMessage extends HarvesterMessage {

    /** The ID for a specific harvest. */
    private Long harvestId;
    
    /**
     * Constructor for the IndexReadyMessage.
     * @param harvestId The harvestId that requires the index.
     * @param to The destination channel
     * @param replyTo The channel to reply to (not really used).
     */
    public IndexReadyMessage(Long harvestId, ChannelID to, 
            ChannelID replyTo) {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(harvestId, "Long harvestId");
        this.harvestId = harvestId;
    }
    
    /**
     * @return the Id of the harvest that requires this index.
     */
    public Long getHarvestId() {
        return this.harvestId;
    }
    
    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }
    
    
}
