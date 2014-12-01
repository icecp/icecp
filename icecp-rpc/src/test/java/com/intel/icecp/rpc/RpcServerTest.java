package com.intel.icecp.rpc;

import com.intel.icecp.core.mock.MockChannels;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test RpcServerImpl
 *
 */
public class RpcServerTest {
    private MockChannels channels;
    private Command primitiveCommand;
    private Command javaObjectCommand;
    // String is Serializable
    private String testIntString;
    URI uri = URI.create("ndn:/intel/node/1/module/1/module-CMD/");

    @Before
    public void setUp() throws NoSuchMethodException {
        channels = new MockChannels();

        // Add a primitiveCommand to the registry
        primitiveCommand = new FakeClass().toCommand(int.class, double.class, String.class, int.class, double.class);
        javaObjectCommand = new FakeClass().toCommand();
        testIntString = String.valueOf(1);
    }

    @Test
    public void clientAndServerIntegrationTest() throws Exception {
        // Setup channel and callback
        RpcServer rpcServer = Rpc.newServer(channels, uri);
        rpcServer.registry().add(primitiveCommand);
        rpcServer.serve();

        // Setup client and make primitiveCommand request
        RpcClient rpcClient = Rpc.newClient(channels, uri);
        // We cannot use Java object currently in the RPC mechanism
        // the primitiveCommand execute() will have argument type mismatch issue
        // if passing Java object around RPC
        CommandRequest request = CommandRequest.from("FakeClass.testMethod", 0, 1.1, testIntString, 3, 4.4);

        CompletableFuture<CommandResponse> future = rpcClient.call(request);

        // Blocking call to get response
        CommandResponse commandResponse = future.get();
        assertFalse(commandResponse.err);
        assertEquals(new FakeClass().testMethod(0, 1.1, testIntString, 3, 4.4), commandResponse.out);
    }

    @Test
    public void clientAndServerIntegrationWithoutResponseChannelTest() throws Exception {
        // Setup channel and callback
        RpcServer rpcServer = Rpc.newServer(channels, uri);
        rpcServer.registry().add(primitiveCommand);
        rpcServer.serve();

        // Setup client and make primitiveCommand request
        RpcClient rpcClient = Rpc.newClient(channels, uri);
        CommandRequest request = CommandRequest.fromWithoutResponse("FakeClass.testMethod", 0, 1.1, testIntString, 3, 4.4);
        CompletableFuture<CommandResponse> future = rpcClient.call(request);

        // Blocking call to get response
        CommandResponse commandResponse = future.get();
        assertNull(commandResponse);
    }

    @Test
    public void clientAndServerIntegrationWithProvidedResponseChannelTest() throws Exception {
        URI responseUri = URI.create("ndn:/intel/node/1/module/1/module-CMD/$ret");
        // Setup channel and callback
        RpcServer rpcServer = Rpc.newServer(channels, uri);
        rpcServer.registry().add(primitiveCommand);
        rpcServer.serve();

        // Setup client and make primitiveCommand request
        RpcClient rpcClient = Rpc.newClient(channels, uri);
        CommandRequest request = CommandRequest.from("FakeClass.testMethod", responseUri, 0, 1.1, testIntString, 3, 4.4);
        CompletableFuture<CommandResponse> future = rpcClient.call(request);

        // Blocking call to get response
        CommandResponse commandResponse = future.get();
        assertFalse(commandResponse.err);
        assertEquals(new FakeClass().testMethod(0, 1.1, testIntString, 3, 4.4), commandResponse.out);
    }

    @Test
    public void clientAndServerIntegrationWithArgumentTypeMismatch() throws Exception {
        try {
            // Setup channel and callback
            RpcServer rpcServer = Rpc.newServer(channels, uri);
            rpcServer.registry().add(javaObjectCommand);
            rpcServer.serve();

            // Setup client and make primitiveCommand request
            RpcClient rpcClient = Rpc.newClient(channels, uri);
            // We cannot use Java object currently in the RPC mechanism
            // the javaObjectCommand execute() will have argument type mismatch issue
            // if passing Java object around RPC
            FakeDataStructure fake = new FakeDataStructure(0, 1.1f, testIntString, 3, 4.4);
            CommandRequest request = CommandRequest.from("FakeClass.testMethod", fake);

            // should expect Exception thrown here.....
            CompletableFuture<CommandResponse> future = rpcClient.call(request);

            // Blocking call to get response
            CommandResponse commandResponse = future.get();

            assertFalse(commandResponse.err);
            assertEquals(new FakeClass().testMethod(fake), commandResponse.out);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof RpcServiceException);
        }
    }
}

