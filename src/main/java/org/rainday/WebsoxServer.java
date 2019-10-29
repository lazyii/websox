package org.rainday;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;

import java.util.Base64;

/**
 * Created by admin on 2019/10/29 9:17:57.
 */
public class WebsoxServer extends AbstractVerticle {
    
    Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Buffer tcp = Buffer.buffer(new byte[]{5, 1, 0});// ver|cmd|rsv|atyp|dst.addr|dst.port
    private static final Buffer udp = Buffer.buffer(new byte[]{5, 3, 0});// ver|cmd|rsv|atyp|dst.addr|dst.port
    
    private static final Buffer connectResponse = Buffer.buffer(new byte[] { 5, 0, 0, 1, 0x7f, 0, 0, 1, 0x27, 0x10 });
    private static final Buffer errorResponse = Buffer.buffer(new byte[] { 5, 4, 0, 1, 0, 0, 0, 0, 0, 0 });
    
    private String username = "rainday";
    private String password = "raindayy";
    private int port = 8080;
    
    @Override
    public void start() throws Exception {
        super.start();
        username = config().getString("username", username);
        password = config().getString("password", password);
        port = Integer.valueOf(System.getProperty("server.port"));
        
        System.out.println(String.format("sout: username: %s, password: %s, port: %d", username, password, port));
        logger.info(String.format("logger: username: %s, password: %s, port: %d", username, password, port));
        
        HttpServerOptions options = new HttpServerOptions()
                .setWebsocketSubProtocols("protocol.loveyou3000.rainday.org")
                .setPort(8080);
        HttpServer websocketServer = vertx.createHttpServer(options);
        websocketServer.requestHandler(x->{
            x.response().end("<html><body> hello world </body></html>");
        });
        websocketServer.websocketHandler(serverWebSocket -> {
            System.out.println("connected ,ssl:" + serverWebSocket.isSsl());
            boolean auth = this.authorize(serverWebSocket.headers());
            if (auth) {
                serverWebSocket.accept();
                serverWebSocket.binaryMessageHandler(buffer -> {
                    //socks5 data
                    Promise<NetSocket> promise = this.socks5Handler(buffer);
                    promise.future().setHandler(r -> {
                        if (r.succeeded()) {
                            serverWebSocket.write(connectResponse);
                            
                            NetSocket netSocket = r.result();
                            serverWebSocket.closeHandler(voad -> netSocket.close());
                            netSocket.closeHandler(voad -> serverWebSocket.close());
                            Pump.pump(serverWebSocket, netSocket).start();
                            Pump.pump(netSocket, serverWebSocket).start();
                        } else {
                            //无法连接至 目标服务器
                            serverWebSocket.write(errorResponse);
                        }
                    });
                });
            } else {
                //auth failed
                serverWebSocket.reject(401);
            }
        });
        
        websocketServer.listen();
    }
    
    private boolean authorize(MultiMap headers) {
        try {
            String receive = headers.get("Authorization");
            long minute = (System.currentTimeMillis() / 60_000) * 60_000;
            String expect = "Basic " + Base64.getEncoder().encodeToString((username + ":" + Util.calcPass(password, minute)).getBytes());
            return expect.equals(receive);
        } catch (Exception e) {
            logger.error("授权失败", e);
        }
        return false;
    }
    
    private Promise<NetSocket> socks5Handler(Buffer buffer) {
        if (!buffer.getBuffer(0, tcp.length()).equals(tcp)) {
            throw new IllegalStateException("expected " + toHex(tcp) + ", got " + toHex(buffer));
        }
        int addressType = buffer.getUnsignedByte(3);
        String host;
        int port;
        /**
         *     * 0x01：IPv4
         *     * 0x03：域名
         *     * 0x04：IPv6
         */
        if (addressType == 1) {
            if (buffer.length() != 10) {
                throw new IllegalStateException("format error in client request (attribute type ipv4), got " + toHex(buffer));
            }
            host = buffer.getUnsignedByte(4) + "." +
                    buffer.getUnsignedByte(5) + "." +
                    buffer.getUnsignedByte(6) + "." +
                    buffer.getUnsignedByte(7);
            port = buffer.getUnsignedShort(8);
        } else if (addressType == 3) {
            int stringLen = buffer.getUnsignedByte(4);
            logger.debug("string len " + stringLen);
            if (buffer.length() != 7 + stringLen) {
                throw new IllegalStateException("format error in client request (attribute type domain name), got " + toHex(buffer));
            }
            host = buffer.getString(5, 5 + stringLen);
            port = buffer.getUnsignedShort(5 + stringLen);
        } else {
            throw new IllegalStateException("expected address type ip (v4) or name, got " + addressType);
        }
        logger.debug("got request: " + toHex(buffer));
        logger.debug("connect: " + host + ":" + port);
        NetClient netClient = vertx.createNetClient(new NetClientOptions().setTcpKeepAlive(true).setReusePort(true).setTcpQuickAck(true).setIdleTimeout(15));
        Promise<NetSocket> promise = Promise.promise();
        netClient.connect(port, host, promise);
        return promise;
    }
    
    private String toHex(Buffer buffer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            sb.append(String.format("%02X ", buffer.getByte(i)));
        }
        return sb.toString();
    }
    
}
