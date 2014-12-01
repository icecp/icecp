package com.intel.icecp.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.BeforeClass;
import org.junit.Test;
/**
 * Created by nmgaston on 5/27/2016.
 */
public class CommandRequestTest {
    private static String testIntString;

    @BeforeClass
    public static void beforeClass() {
        testIntString = String.valueOf(1);
    }

    @Test
    public void EqualsAndHashCodeTest() {
        URI responseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$ret");

        FakeDataStructure fake = new FakeDataStructure(0, 1.1f, testIntString, 3, 4.4);
        CommandRequest x = CommandRequest.from("FakeClass.testMethod", responseUri, fake);
        CommandRequest y = CommandRequest.from("FakeClass.testMethod", responseUri, fake);

        assertTrue(x.equals(y) && y.equals(x));
        assertTrue(x.hashCode() == y.hashCode());
    }

    @Test
    public void NotEqualsAndHashCodeTest() {
        URI xResponseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$ret");
        URI yResponseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$return");

        FakeDataStructure fake = new FakeDataStructure(0, 1.1f, testIntString, 3, 4.4);
        CommandRequest x = CommandRequest.from("FakeClass.testMethod", xResponseUri, fake);
        CommandRequest y = CommandRequest.from("FakeClass.testMethod", yResponseUri, fake);

        assertFalse(x.equals(y) && y.equals(x));
        assertFalse(x.hashCode() == y.hashCode());
    }

    @Test
    public void EqualsWithNullObjectTest() {
        URI xResponseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$ret");

        FakeDataStructure fake = new FakeDataStructure(0, 1.1f, testIntString, 3, 4.4);
        CommandRequest x = CommandRequest.from("FakeClass.testMethod", xResponseUri, fake);
        CommandRequest y = null;

        assertFalse(x.equals(y));
    }

    @Test
    public void EqualsWithDifferentClassTest() {
        URI xResponseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$ret");
        URI yResponseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$return");

        FakeDataStructure fake = new FakeDataStructure(0, 1.1f, testIntString, 3, 4.4);
        AnotherFakeDataStructure anotherFake = new AnotherFakeDataStructure(0, 1.1f, new Object(), 3, 4.4);

        CommandRequest x = CommandRequest.from("FakeClass.testMethod", xResponseUri, fake);
        CommandRequest y = CommandRequest.from("FakeClass.testMethod", yResponseUri, anotherFake);

        assertFalse(x.equals(y));
    }
    
    @Test
    public void testCreateWithURI(){
        CommandRequest request = CommandRequest.from("name", URI.create("path"), testIntString);
        
        assertEquals("name", request.name);
        assertEquals(URI.create("path"), request.responseUri);
        assertEquals("1", request.inputs[0]);
    }
    
    @Test
    public void testCreateAutoURI(){
        CommandRequest request = CommandRequest.from("name", testIntString, 1.2);
        
        assertEquals("name", request.name);
        assertNotNull(request.responseUri);
        assertEquals("1", request.inputs[0]);
        assertEquals(1.2, request.inputs[1]);
    }
}
