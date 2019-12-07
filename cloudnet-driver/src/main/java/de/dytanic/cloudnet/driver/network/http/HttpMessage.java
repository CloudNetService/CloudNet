package de.dytanic.cloudnet.driver.network.http;

import java.util.Map;

public interface HttpMessage<T extends HttpMessage> {

    HttpContext context();

    String header(String name);

    int headerAsInt(String name);

    boolean headerAsBoolean(String name);

    T header(String name, String value);

    T removeHeader(String name);

    T clearHeaders();

    boolean hasHeader(String name);

    Map<String, String> headers();

    HttpVersion version();

    T version(HttpVersion version);

    byte[] body();

    String bodyAsString();

    T body(byte[] byteArray);

    T body(String text);

}