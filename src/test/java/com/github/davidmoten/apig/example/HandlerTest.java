package com.github.davidmoten.apig.example;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class HandlerTest {

    @Test
    public void testHourOfDay() {
        assertEquals(0, Handler.hourOfDay(0));
    }

    @Test
    public void testHourOfDay2() {
        long t = 1511751354923L;
        assertEquals(2, Handler.hourOfDay(t));
    }

    @Test
    public void testHourOfDay3() {
        long t = 1511751354923L + TimeUnit.HOURS.toMillis(13);
        assertEquals(15, Handler.hourOfDay(t));
    }
}
