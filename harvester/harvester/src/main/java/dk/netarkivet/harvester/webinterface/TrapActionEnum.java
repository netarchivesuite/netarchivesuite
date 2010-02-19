/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.harvester.webinterface;

import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.utils.I18n;

/**
 * Represents the various actions which can be carried out to modify
 * Global Crawler Traps.
 *
 */

public enum TrapActionEnum {

    /**
     * Corresponds to uploading of a global crawler trap list, either as a new
     * list or as an update to an existing list.
     */
    CREATE_OR_UPDATE {
        @Override
        public TrapAction getTrapAction() {
            return new TrapCreateOrUpdateAction();
        }},
    /**
     * Action to download an existing list to browser or file.
     */
    READ{
        @Override
        public TrapAction getTrapAction() {
           return new TrapReadAction();
        }},
    /**
     * Action to delete an existing list.
     */
    DELETE{
        @Override
        public TrapAction getTrapAction() {
            return new TrapDeleteAction();           
        }},
    /**
     * Change an existing list from inactive to active.
     */
    ACTIVATE{
        @Override
        public TrapAction getTrapAction() {
            return new TrapActivationAction(true);
        }},
    /**
     * Change an existing list from active to inactive.
     */
    DEACTIVATE{
        @Override
        public TrapAction getTrapAction() {
            return new TrapActivationAction(false);
        }},
    /**
     * Do nothing. The existence of a null action is an architectural
     * convenience.
     */
    NULL_ACTION{
        @Override
        public TrapAction getTrapAction() {
            /**
             * The null action is sufficiently trivial that we can implement it
             * inline rather than in a separate class.
             */
            return new TrapAction() {
                @Override
                protected void doAction(PageContext context, I18n i18n) {
                    return;
                }
            };
        }}
    ;

    /**
     * Get the concrete TrapAction which can process this request.
     * @return the correct TrapAction for this request type.
     */
    public abstract TrapAction getTrapAction();



}
