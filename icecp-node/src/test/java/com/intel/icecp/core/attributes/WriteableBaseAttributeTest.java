package com.intel.icecp.core.attributes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WriteableBaseAttributeTest {
    @Test
    public void roundTrip() throws Exception {
        WriteableBaseAttribute<String> attr = new NameAttribute();
        attr.value("abc");
        assertEquals("abc", attr.value());
    }

    @Test
    public void roundTripLong() throws Exception {
        WriteableBaseAttribute<Long> attr = new IdAttribute();
        attr.value(4L);
        assertEquals(4L, (long)attr.value());
    }
}