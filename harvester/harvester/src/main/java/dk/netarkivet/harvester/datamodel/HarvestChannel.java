/* File:        $Id: HarvestChannel.java 2712 2013-06-17 14:43:52Z ngiraud $
 * Revision:    $Revision: 2712 $
 * Author:      $Author: ngiraud $
 * Date:        $Date: 2013-06-17 16:43:52 +0200 (Mon, 17 Jun 2013) $
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
package dk.netarkivet.harvester.datamodel;

import java.io.Serializable;

import javax.servlet.jsp.PageContext;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.harvesting.HarvestController;

/**
 * Harvest channels are used to dispatch harvest jobs to specific pools of crawlers. 
 * Channels can accept either only snapshot jobs or only focused jobs. Snapshot crawls all
 * use a single hard-coded channel.
 *
 * Harvest channels names must only contain alphanumeric characters, the constraint 
 * is enforced at creation time.
 *
 * {@link HarvestDefinition}s are mapped to a {@link HarvestChannel}, 
 * and {@link HarvestController}s listen to jobs sent on a specific channel.
 *
 * Harvest channels are stored in the harvest database, as well as mappings to 
 * {@link HarvestDefinition}s and {@link HarvestController}s through two association
 * tables.
 *
 * There must be exactly one channel defined as default for every type of 
 * job (snapshot and focused). This constraint will be enforced by the DAO. 
 *
 * @author ngiraud
 *
 */
@SuppressWarnings("serial")
public class HarvestChannel implements Serializable {

    /**
     * The unique {@link HarvestChannel} for snapshot harvests.
     */
    public static final HarvestChannel SNAPSHOT = new HarvestChannel();

    /**
     * Defines acceptable channel names: at least one word character (see {@link Pattern}).
     */
    public static final String ACCEPTABLE_NAME_PATTERN = "^\\w+$";

    /**
     * The unique numeric id.
     */
    private long id;

    /**
     * The unique name of the channel. Accepts only alpha numeric characters.
     * @see #ACCEPTABLE_NAME_PATTERN
     * @see #isAcceptableName(String)
     */
    private String name;

    /**
     * Comments.
     */
    private String comments;

    /**
     * Whether this channel is the default one for the given type (snapshot or focused).
     */
    private boolean isDefault;

    /**
     * Used to build the {@link #SNAPSHOT} singleton only.
     */
    private HarvestChannel() {
        this.name = "SNAPSHOT";
        this.comments = "";
        this.isDefault = true;
    }

    /**
     * Constructor from name and comments
     * @param name channel name
     * @param comments user comments
     * @param isDefault whether this channel is the default one
     * (snapshot or focused)
     * @throws ArgumentNotValid if the name is incorrect.
     */
    public HarvestChannel(
            final String name,
            final String comments,
            final boolean isDefault) {
        if (!isAcceptableName(name)) {
            throw new ArgumentNotValid("'" + name + "' does not match pattern '"
                    + ACCEPTABLE_NAME_PATTERN + "'");
        }
        this.name = name;
        this.comments = comments;
        this.isDefault = isDefault;
    }

    /**
     * Constructor from persistent storage
     * @param id the channel id
     * @param name channel name
     * @param comments user comments
     * @param isDefault whether this channel is the default one for the given type
     * (snapshot or focused)
     * @throws ArgumentNotValid if the name is incorrect.
     */
    public HarvestChannel(
            final long id,
            final String name,
            final String comments,
            final boolean isDefault) {
        if (!isAcceptableName(name)) {
            throw new ArgumentNotValid("'" + name + "' does not match pattern '"
                    + ACCEPTABLE_NAME_PATTERN + "'");
        }
        this.id = id;
        this.name = name;
        this.comments = comments;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComments() {
        return comments;
    }

    /**
     * Renders a localized description for the {@link #SNAPSHOT} singleton.
     * @param context
     * @return a localized description.
     */
    public static String getSnapshotDescription(PageContext context) {
        return I18n.getString(
                dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE,
                context.getResponse().getLocale(),
                "harvest.channel.snapshot.desc");
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public long getId() {
        return id;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isSnapshot() {
        return SNAPSHOT.equals(this);
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Returns true if the given input is an acceptable channel name.
     * @param input
     * @return
     */
    public static boolean isAcceptableName(String input) {
        return input.matches(ACCEPTABLE_NAME_PATTERN);
    }

    @Override
    public String toString() {
        return "HarvestChannel [id=" + id + ", name=" + name + ", comments="
                + comments
                + ", isSnapShot=" + (SNAPSHOT.equals(this) ? true : false) + ", isDefault="
                + isDefault + "]";
    }

}
