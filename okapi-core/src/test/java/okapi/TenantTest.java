/*
 * Copyright (c) 2015-2016, Index Data
 * All rights reserved.
 * See the file LICENSE for details.
 */
package okapi;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TenantTest {
  Vertx vertx;
  String doc1, doc2;
  String location;
  Async async;
  private HttpClient httpClient;
  private static String LS = System.lineSeparator();
  
  public TenantTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp(TestContext context) {
    vertx = Vertx.vertx();
    
    DeploymentOptions opt = new DeploymentOptions()
      .setConfig(new JsonObject().put("storage", "inmemory"));
    vertx.deployVerticle(MainVerticle.class.getName(),
            opt, context.asyncAssertSuccess());
    this.httpClient = vertx.createHttpClient();
  }
  
  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }
  
  private int port = Integer.parseInt(System.getProperty("port", "9130"));

  @Test
  public void test1(TestContext context) {
    this.async = context.async();
    healthCheck(context);
  }

  public void healthCheck(TestContext context) {
    httpClient.get(port, "localhost", "/_/health", response -> {
      context.assertEquals(200, response.statusCode());
      response.handler(body -> {
      });
      response.endHandler(x -> {
        listNone(context);
      });
    }).end();
  }

  public void listNone(TestContext context) {
    httpClient.get(port, "localhost", "/_/tenants", response -> {
      context.assertEquals(200, response.statusCode());
      response.handler(body -> {
        context.assertEquals("[ ]", body.toString());
      });
      response.endHandler(x -> {
        post(context);
      });
    }).end();
  }

  public void post(TestContext context) {
    doc1 = "{"+LS
            + "  \"name\" : \"roskilde\","+LS
            + "  \"description\" : \"Roskilde bibliotek\""+LS
            + "}";
    doc2 = "{"+LS
            + "  \"id\" : \"roskilde\","+LS
            + "  \"name\" : \"roskilde\","+LS
            + "  \"description\" : \"Roskilde bibliotek\""+LS
            + "}";
    httpClient.post(port, "localhost", "/_/tenants", response -> {
      context.assertEquals(201, response.statusCode());
      response.endHandler(x -> {
        location = response.getHeader("Location");
        getNone(context);
      });
    }).end(doc1);
  }

  public void getNone(TestContext context) {
    httpClient.get(port, "localhost", location + "_none", response -> {
      context.assertEquals(404, response.statusCode());
      response.endHandler(x -> {
        listOne(context);
      });
    }).end();
  }

  public void listOne(TestContext context) {
    httpClient.get(port, "localhost", "/_/tenants", response -> {
      context.assertEquals(200, response.statusCode());
      response.handler(body -> {
        context.assertEquals("[ " + doc2 + " ]", body.toString());
      });
      response.endHandler(x -> {
        getIt(context);
      });
    }).end();
  }

  public void getIt(TestContext context) {
    httpClient.get(port, "localhost", location, response -> {
      context.assertEquals(200, response.statusCode());
      response.handler(body -> {
        context.assertEquals(doc2, body.toString());
      });
      response.endHandler(x -> {
        deleteIt(context);
      });
    }).end();
  }
  
  public void deleteIt(TestContext context) {
    httpClient.delete(port, "localhost", location, response -> {
      context.assertEquals(204, response.statusCode());
      response.endHandler(x -> {
        post2(context);
      });
    }).end();
  }

  public void post2(TestContext context) {
    doc1 = "{"+LS
            + "  \"id\" : \"roskildedk\","+LS
            + "  \"name\" : \"roskilde\","+LS
            + "  \"description\" : \"Roskilde bibliotek\""+LS
            + "}";
    httpClient.post(port, "localhost", "/_/tenants", response -> {
      context.assertEquals(201, response.statusCode());
      response.endHandler(x -> {
        location = response.getHeader("Location");
        System.out.println("post2: location: " + location);
        listOne2(context);
      });
    }).end(doc1);
  }

  public void listOne2(TestContext context) {
    httpClient.get(port, "localhost", "/_/tenants", response -> {
      context.assertEquals(200, response.statusCode());
      response.handler(body -> {
        context.assertEquals("[ " + doc1 + " ]", body.toString());
      });
      response.endHandler(x -> {
        reload2(context);
      });
    }).end();
  }

  public void reload2(TestContext context) {
    httpClient.get(port, "localhost", "/_/test/reloadtenant/roskildedk", response -> {
      context.assertEquals(204, response.statusCode());
      response.endHandler(x -> {
        done(context);
      });
    }).end();
  }

  public void done(TestContext context){
    async.complete();
  }
}

