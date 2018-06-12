/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Kevin Smith, Boundless, Copyright 2018
 */

package org.geowebcache.util;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;

import org.geowebcache.service.HttpErrorCodeException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockHttpServletRequest;

public class ServletUtilsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void testUsefulErrorOnNoHostHeader() {
        MockHttpServletRequest res = new MockHttpServletRequest();
        exception.expect(allOf(
                instanceOf(HttpErrorCodeException.class),
                hasProperty("errorCode", equalTo(400)),
                hasProperty("message", equalTo("HTTP Host Header missing"))));
        ServletUtils.getServletBaseURL(res, "testPrefix");
    }

}
