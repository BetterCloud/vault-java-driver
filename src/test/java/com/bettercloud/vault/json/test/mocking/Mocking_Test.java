/*******************************************************************************
 * Copyright (c) 2013, 2015 EclipseSource.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.bettercloud.vault.json.test.mocking;

import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.json.ParseException;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;


/**
 * Make sure types do not prevent mocking by final or visibility constructs.
 */
@SuppressWarnings("PMD")
public class Mocking_Test {

    @Test
    public void mockValue() {
        JsonValue jsonValue = Mockito.mock(JsonValue.class);

        assertNotNull(jsonValue);
    }

    @Test
    public void mockObject() {
        JsonObject jsonObject = Mockito.mock(JsonObject.class);

        assertNotNull(jsonObject);
    }

    @Test
    public void mockArray() {
        JsonArray jsonArray = Mockito.mock(JsonArray.class);

        assertNotNull(jsonArray);
    }

    @Test
    public void mockParseException() {
        Mockito.mock(ParseException.class);
    }

}
