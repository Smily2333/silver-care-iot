package com.silvercare.iot.tcp;

import com.silvercare.iot.config.DeviceGatewayProperties;
import com.silvercare.iot.service.DevicePacketDispatcher;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class DeviceTcpServer {

    private static final Logger log = LoggerFactory.getLogger(DeviceTcpServer.class);

    private final DeviceGatewayProperties properties;
    private final DevicePacketDispatcher dispatcher;
    private final DeviceConnectionRegistry registry;
    private final ExecutorService acceptExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService clientExecutor;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public DeviceTcpServer(DeviceGatewayProperties properties,
                           DevicePacketDispatcher dispatcher,
                           DeviceConnectionRegistry registry) {
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.registry = registry;
        this.clientExecutor = new ThreadPoolExecutor(
                properties.getMaxClientThreads(),
                properties.getMaxClientThreads(),
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.getClientQueueCapacity()),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Device TCP gateway is disabled");
            return;
        }
        running = true;
        acceptExecutor.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(properties.getPort()), properties.getAcceptBacklog());
            this.serverSocket = socket;
            log.info("Device TCP gateway listening on port {}", properties.getPort());
            while (running) {
                Socket client = socket.accept();
                try {
                    clientExecutor.submit(() -> handleClient(client));
                } catch (RejectedExecutionException ex) {
                    log.warn("Device connection rejected because gateway worker pool is full: {}",
                            client.getRemoteSocketAddress());
                    closeQuietly(client);
                }
            }
        } catch (IOException ex) {
            if (running) {
                log.error("Device TCP gateway stopped unexpectedly", ex);
            }
        }
    }

    private void handleClient(Socket socket) {
        DeviceConnection connection = new DeviceConnection(socket);
        String remote = socket.getRemoteSocketAddress().toString();
        log.info("Device connection opened: {}", remote);
        try (socket) {
            socket.setSoTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(properties.getIdleTimeoutSeconds())));
            InputStream inputStream = socket.getInputStream();
            StringBuilder buffer = new StringBuilder();
            byte[] bytes = new byte[1024];
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                connection.markSeen();
                buffer.append(new String(bytes, 0, read, StandardCharsets.US_ASCII));
                drainPackets(buffer, connection);
                if (buffer.length() > properties.getMaxFrameLengthBytes()) {
                    throw new IOException("Frame buffer exceeded max length: " + properties.getMaxFrameLengthBytes());
                }
            }
        } catch (SocketTimeoutException ex) {
            log.info("Device connection idle timeout after {} seconds: {}",
                    properties.getIdleTimeoutSeconds(), remote);
        } catch (SocketException ex) {
            log.info("Device connection closed: {}", remote);
        } catch (IOException ex) {
            log.warn("Device connection error: {}", remote, ex);
        } finally {
            if (registry.remove(connection)) {
                dispatcher.onConnectionClosed(connection);
            }
        }
    }

    private void drainPackets(StringBuilder buffer, DeviceConnection connection) throws IOException {
        while (true) {
            int start = buffer.indexOf("[");
            int end = buffer.indexOf("]", start + 1);
            if (start < 0) {
                buffer.setLength(0);
                return;
            }
            if (end < 0) {
                if (start > 0) {
                    buffer.delete(0, start);
                }
                return;
            }
            String packet = buffer.substring(start, end + 1);
            if (packet.length() > properties.getMaxFrameLengthBytes()) {
                throw new IOException("Frame exceeded max length: " + properties.getMaxFrameLengthBytes());
            }
            buffer.delete(0, end + 1);
            dispatcher.dispatch(packet, connection);
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        acceptExecutor.shutdownNow();
        clientExecutor.shutdownNow();
    }
}
