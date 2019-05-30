package de.dytanic.cloudnet.driver.network.http;

public interface IHttpResponse extends IHttpMessage<IHttpResponse> {

  int statusCode();

  IHttpResponse statusCode(int code);

}