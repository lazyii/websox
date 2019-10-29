package org.rainday;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.streams.Pump;

public class Socks5Server extends AbstractVerticle {
    
    private static final Logger log = LoggerFactory.getLogger(Socks5Server.class);
    
    private static final Buffer clientInit = Buffer.buffer(new byte[]{5, 1, 0}); //5：socks version, 1：支持一种认证方式 0：认证方式为0x00（AUTHENTICATION REQUIRED 不需要认证）
    private static final Buffer serverReply = Buffer.buffer(new byte[]{5, 0});//响应，不需要鉴权
    private static final Buffer serverDeny = Buffer.buffer(new byte[]{5, Byte.MIN_VALUE});//拒绝代理
    
    private static final int PORT = 11080;
    
    private NetServer server;
    
    private String username = "rainday";
    private String password = "raindayy";
    private String wsUrl = "ws://127.0.0.1:8080";
    
    @Override
    public void start() {
        NetServerOptions options = new NetServerOptions();
        options.setPort(PORT);
        server = vertx.createNetServer(options);
        server.connectHandler(socket -> {
            socket.handler(buffer -> {
                log.debug("got request: " + toHex(buffer));
                //获取websocket连接
                WebSocketProvider provider = new WebSocketProvider().setVertx(vertx);
                Promise<WebSocket> webSocketPromise = provider.getConnection(username, password, wsUrl);
        
                webSocketPromise.future().setHandler(r -> {
                    if (r.succeeded()) {
                        socket.write(serverReply);
                
                        WebSocket webSocket = r.result();
                        socket.closeHandler(voad -> webSocket.close());
                        webSocket.closeHandler(voad -> socket.close());
                        Pump.pump(socket, webSocket).start();
                        Pump.pump(webSocket, socket).start();
                    } else {
                        socket.write(serverDeny);
                    }
                });
            });
        });
        server.listen(ar -> {
            if (ar.succeeded()) {
                System.out.println("server start up");
            } else {
                System.out.println("server start failed");
            }
        });
        log.debug("socks5 server started");
    }
    
    
    private String toHex(Buffer buffer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            sb.append(String.format("%02X ", buffer.getByte(i)));
        }
        return sb.toString();
    }
    
    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (server != null) {
            server.close();
            server = null;
        }
    }
}
