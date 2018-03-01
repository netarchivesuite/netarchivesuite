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
package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A message to send from the IndexServer to HarvestJobManager, that the index required by harvest with a given ID is
 * ready.
 */
@SuppressWarnings({"serial"})
public class IndexReadyMessage extends HarvesterMessage {

    /** The ID for a specific harvest. */
    private Long harvestId;
    /** Is the index OK for the harvest. */
    private boolean indexOK;

    /**
     * Constructor for the IndexReadyMessage.
     *
     * @param harvestId The harvestId that requires the index.
     * @param indexIsOK is the index now OK or not
     * @param to The destination channel
     * @param replyTo The channel to reply to (not really used).
     */
    public IndexReadyMessage(Long harvestId, boolean indexIsOK, ChannelID to, ChannelID replyTo) {
        super(to, replyTo);
        ArgumentNotValid.checkNotNull(harvestId, "Long harvestId");
        this.harvestId = harvestId;
        this.indexOK = indexIsOK;
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

    /**
     * Is the index OK.
     *
     * @return true, if the index is OK, else false.
     */
    public boolean getIndexOK() {
        return indexOK;
    }
}
