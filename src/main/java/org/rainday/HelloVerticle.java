package org.rainday;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * Created by admin on 2019/10/30 9:51:45.
 */
public class HelloVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.createHttpServer().requestHandler(req -> req.response().end("Hello World!"))
             .listen(Integer.getInteger("http.port"), System.getProperty("http.address", "0.0.0.0"));
        
    }
    
    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
    
    }
}
