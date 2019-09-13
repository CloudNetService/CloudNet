package de.dytanic.cloudnet.examples.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.MethodHttpHandlerAdapter;

public final class ExampleHttpHandler {

    public void registerHttpHandlerExample() {
        CloudNet.getInstance().getHttpServer().registerHandler( //register a default http handler, which receives all http message on the following path
                "/helloworld",
                (path, context) -> {
                    context
                            .response()
                            .statusCode(HttpResponseCode.HTTP_OK) //sets the response status code
                            .header("Content-Type", "text/plain") //Sets the header
                            .body("Hello, world!") //Sets the response http body
                            .context() //switch to IHttpContext
                            .closeAfter(true) //is not required. It closes automatic by default
                            .cancelNext() //cancelled that no http handler will invoked after this on this path
                    ;
                })
        ;

        CloudNet.getInstance().getHttpServer().registerHandler( //Register for the http get specific an http handler
                "/your_name/{name}", //Http
                new MethodHttpHandlerAdapter() {

                    @Override
                    public void handleGet(String path, IHttpContext context) {
                        context
                                .response()
                                .statusCode(HttpResponseCode.HTTP_OK)
                                .header("Content-Type", "text/plain")
                                .body("Your name is " + context.request().pathParameters().containsKey("name")) //get the following path parameter "name"
                                .context()
                                .closeAfter(true)
                                .cancelNext()
                        ;
                    }
                }
        );

        //Removes all Http handler from this class loader
        CloudNet.getInstance().getHttpServer().removeHandler(ExampleHttpHandler.class.getClassLoader());
    }
}