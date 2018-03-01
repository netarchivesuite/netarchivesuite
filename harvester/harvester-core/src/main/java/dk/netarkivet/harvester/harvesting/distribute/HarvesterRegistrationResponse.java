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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;

/**
 * Message sent by the HarvesterStatusReceiver after processing a {@link HarvesterRegistrationRequest} message.
 * It notifies crawlers whether a given harvest channel effectively matches a {@link HarvestChannel} defined in the
 * harvest database.
 */
@SuppressWarnings({"serial"})
public class HarvesterRegistrationResponse extends HarvesterMessage {

    /** The harvest channel name. */
    private final String harvestChannelName;

    /** If true, the name matches an existing {@link HarvestChannel}. */
    private final boolean isValid;

    /**
     * Whether the matching {@link HarvestChannel} handles snapshot or focused harvests. Meaningless if {@link #isValid}
     * is false.
     */
    private final boolean isSnapshot;

    /**
     * Constructor from fields.
     *
     * @param harvestChannelName the harvest channel name
     * @param isValid whether the given name denotes an existing channel
     * @param isSnapshot true if the channel accepts snapshot harvest, false for partial.
     */
    public HarvesterRegistrationResponse(final String harvestChannelName, final boolean isValid,
            final boolean isSnapshot) {
        super(HarvesterChannels.getHarvesterRegistrationResponseChannel(), Channels.getError());
        this.harvestChannelName = harvestChannelName;
        this.isValid = isValid;
        this.isSnapshot = isSnapshot;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the harvestChannelName
     */
    public final String getHarvestChannelName() {
        return harvestChannelName;
    }

    /**
     * @return the isValid
     */
    public final boolean isValid() {
        return isValid;
    }

    /**
     * @return the isSnapshot
     */
    public final boolean isSnapshot() {
        return isSnapshot;
    }

}
