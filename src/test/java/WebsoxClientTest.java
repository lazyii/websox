import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rainday.Util;
import org.rainday.WebsoxServer;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Created by admin on 2019/10/29 9:35:54.
 */
@RunWith(VertxUnitRunner.class)
public class WebsoxClientTest {
    
    static Vertx vertx;
    
    @BeforeClass
    public static void before(TestContext context) {
        System.out.println("before class ");
        vertx = Vertx.vertx();
        vertx.deployVerticle(new WebsoxServer(), context.asyncAssertSuccess());
    }
    
    @AfterClass
    public static void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }
    
    @Test
    public void websoxConnectTest(TestContext context) {
        Async async = context.async();
        HttpClient wsClient = vertx.createHttpClient();
        List<String> subprotocol = Arrays.asList("loveyou3000.rainday.org");
        String wsUrl = "ws://127.0.0.1:8080";
        long minute = (System.currentTimeMillis() / 60_000) * 60_000;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(("username111" + ":" + Util.calcPass("password111", minute)).getBytes());
        
        VertxHttpHeaders headers = new VertxHttpHeaders()
                .add("Upgrade", "websocket")
                .add("Connection", "Upgrade")
                .add("Host", "vproxy-vproxy.apps.ca-central-1.starter.openshift-online.com")
                .add("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
                .add("Authorization", basicAuth);
        System.out.println("Authorization " + basicAuth);
        Promise<WebSocket> promise = Promise.promise();
        wsClient.webSocketAbs(wsUrl, headers, WebsocketVersion.V13, subprotocol, x -> {
            if (x.succeeded()) {
                context.assertEquals(true, x.succeeded());
                /*x.result().writePing(Buffer.buffer("hello")).frameHandler(pongFrame -> {
                    //pong 两帧 1：binary 2:close
                    System.out.println(pongFrame);
                    System.out.println(pongFrame.binaryData());
                });*/
                x.result().writePing(Buffer.buffer("aaaaaaaaaaaaaaaaaaaa"))/*.binaryMessageHandler(bm -> {
                    System.out.println(bm);
                    System.out.println(111);
                }).textMessageHandler(tm -> {
                    System.out.println(tm);
                    System.out.println(222);
                })*/.closeHandler(close -> {
                    System.out.println("close");
                    System.out.println(333);
                }).pongHandler(pong->{
                    System.out.println(pong);
                    System.out.println(444);
                }).frameHandler(fm->{
                    System.out.println("555***----" + fm.textData());
                    System.out.println(fm);
                    System.out.println(555);
                }).handler(h->{
                    System.out.println(h);
                    System.out.println(666);
                });
                async.complete();
            } else {
                x.cause().printStackTrace();
                context.fail(x.cause());
            }
        });
        
    }
    
    
}
