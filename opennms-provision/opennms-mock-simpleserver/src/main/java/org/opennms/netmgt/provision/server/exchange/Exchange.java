/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 1999-2011 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *     along with OpenNMS(R).  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information contact: 
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.netmgt.provision.server.exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Exchange interface.</p>
 *
 * @author ranger
 * @version $Id: $
 */
public interface Exchange {
    /**
     * <p>sendRequest</p>
     *
     * @param out a {@link java.io.OutputStream} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public boolean sendRequest(OutputStream out) throws IOException;
    /**
     * <p>processResponse</p>
     *
     * @param in a {@link java.io.BufferedReader} object.
     * @return a boolean.
     * @throws java.io.IOException if any.
     */
    public boolean processResponse(BufferedReader in) throws IOException;
    /**
     * <p>matchResponseByString</p>
     *
     * @param input a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean matchResponseByString(String input);
}
