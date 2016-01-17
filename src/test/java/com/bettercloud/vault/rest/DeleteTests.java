package com.bettercloud.vault.rest;

import org.junit.Test;

public class DeleteTests {

    @Test(expected = UnsupportedOperationException.class)
    public void testDelete_Plain() throws RestException {
        new Rest().delete();
    }

}
