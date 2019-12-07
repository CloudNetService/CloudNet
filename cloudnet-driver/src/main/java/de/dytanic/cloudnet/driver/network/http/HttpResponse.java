package de.dytanic.cloudnet.driver.network.http;

public interface HttpResponse extends HttpMessage<HttpResponse> {

    int statusCode();

    HttpResponse statusCode(int code);

}