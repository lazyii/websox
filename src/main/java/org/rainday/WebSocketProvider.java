package org.rainday;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Created by admin on 2019/10/28 16:03:56.
 */
public class WebSocketProvider {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    List<String> subProtocolList = Arrays.asList("protocol.loveyou3000.rainday.org");
    Vertx vertx;

    public Promise<WebSocket> getConnection(String userName, String password, String wsUrl) {
        if (vertx == null) {
            throw new NullPointerException("vertx can not be null");
        }
        HttpClient wsClient = vertx.createHttpClient();
    
        long minute = (System.currentTimeMillis() / 60_000) * 60_000;
        logger.debug("minute for auth" + minute);
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((userName + ":" + Util.calcPass(password, minute)).getBytes());
        
        VertxHttpHeaders headers = new VertxHttpHeaders()
                .add("Upgrade", "websocket")
                .add("Connection","Upgrade")
                .add("Host","vproxy-vproxy.apps.ca-central-1.starter.openshift-online.com")
                .add("Sec-WebSocket-Key","dGhlIHNhbXBsZSBub25jZQ==")
                .add("Authorization", basicAuth);
        logger.debug("Authorization {}", basicAuth);
        Promise<WebSocket> promise = Promise.promise();
        wsClient.webSocketAbs(wsUrl, headers, WebsocketVersion.V13, subProtocolList, promise);
        return promise;
    }
    
    
    public Vertx getVertx() {
        return vertx;
    }
    
    public WebSocketProvider setVertx(Vertx vertx) {
        this.vertx = vertx;
        return this;
    }
}
