/**
 * 
 */
package com.intel.icecp.rpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 *
 */
public class CommandResponseTest {

    @Test
    public void testFromError() {
        CommandResponse cs = CommandResponse.fromError(new Exception("crash"));
        assertEquals("Exception : crash", cs.out);
        assertTrue(cs.err);

        cs = CommandResponse.fromError(new NullPointerException());
        assertEquals("NullPointerException", cs.out);
        assertTrue(cs.err);

        cs = CommandResponse.fromError(new InvocationTargetException(new Exception()));
        assertEquals("Exception", cs.out);
        assertTrue(cs.err);

        cs = CommandResponse.fromError(new InvocationTargetException(new Exception("reason")));
        assertEquals("Exception : reason", cs.out);
        assertTrue(cs.err);
    }

    @Test
    public void testFromValid() {
        CommandResponse cs = CommandResponse.fromValid(2);
        assertEquals(2, cs.out);
        assertFalse(cs.err);
    }

    @Test
    public void testFromValidFuture() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.complete(3);
        CommandResponse cs = CommandResponse.fromValidFuture(future, 1, TimeUnit.MILLISECONDS);
        assertEquals(3, cs.out);
        assertFalse(cs.err);

        // force time out
        future = new CompletableFuture<>();
        cs = CommandResponse.fromValidFuture(future, 1, TimeUnit.MILLISECONDS);
        assertEquals("TimeoutException", cs.out);
        assertTrue(cs.err);

        // force exception
        future = new CompletableFuture<>();
        future.completeExceptionally(new Throwable());
        cs = CommandResponse.fromValidFuture(future, 1, TimeUnit.MILLISECONDS);
        assertEquals("ExecutionException : java.lang.Throwable", cs.out);
        assertTrue(cs.err);

        // force Interrupted
        future = new CompletableFuture<>();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        final Thread t = Thread.currentThread();
        service.schedule(new Runnable(){
            public void run(){
                t.interrupt(); 
            }
        }, 1, TimeUnit.SECONDS);
        cs = CommandResponse.fromValidFuture(future, 20, TimeUnit.SECONDS);
        assertEquals("InterruptedException", cs.out);
        assertTrue(cs.err);
    }
}
