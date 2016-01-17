package com.bettercloud.vault.rest;

import org.junit.Test;

public class PutTests {

    @Test(expected = UnsupportedOperationException.class)
    public void testPut_Plain() throws RestException {
        new Rest().put();
    }

}
