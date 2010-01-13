/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
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

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.I18n;

/**
 * Represents the various actions which can be carried out to modify
 * Global Crawler Traps.
 *
 * @author csr
 * @since Jan 13, 2010
 */

public enum TrapActionEnum {
    CREATE_OR_UPDATE {
        @Override
        public TrapAction getTrapAction() {
            return new TrapCreateOrUpdateAction();
        }},
    READ{
        @Override
        public TrapAction getTrapAction() {
           return new TrapReadAction();
        }},
    DELETE{
        @Override
        public TrapAction getTrapAction() {
            //TODO: implement method
            throw new NotImplementedException(
                    "Not yet implemented:" + ".getTrapAction()");
        }},
    ACTIVATE{
        @Override
        public TrapAction getTrapAction() {
            return new TrapActivationAction(true);
        }},
    DEACTIVATE{
        @Override
        public TrapAction getTrapAction() {
            return new TrapActivationAction(false);
        }},
    NULL_ACTION{
        @Override
        public TrapAction getTrapAction() {
            return new TrapAction() {
                @Override
                protected void doAction(PageContext context, I18n i18n) {
                    return;
                }
            };
        }}
    ;


    public abstract TrapAction getTrapAction();



}
