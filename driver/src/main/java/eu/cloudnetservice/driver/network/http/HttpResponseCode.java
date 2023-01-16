/*
 * Copyright 2019-2023 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.driver.network.http;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.NonNull;

/**
 * All http response codes. See <a href="https://developer.mozilla.org/de/docs/Web/HTTP/Status">here</a> for a full list
 * with a detailed explanation of each response code.
 *
 * @since 4.0
 */
public enum HttpResponseCode {

  /**
   * The server has received the request headers and the client should proceed to send the request body (in the case of
   * a request for which a body needs to be sent; for example, a POST request). Sending a large request body to a server
   * after a request has been rejected for inappropriate headers would be inefficient. To have a server check the
   * request's headers, a client must send Expect: 100-continue as a header in its initial request and receive a 100
   * Continue status code in response before sending the body. If the client receives an error code such as 403
   * (Forbidden) or 405 (Method Not Allowed) then it shouldn't send the request's body. The response 417 Expectation
   * Failed indicates that the request should be repeated without the Expect header as it indicates that the server
   * doesn't support expectations (this is the case, for example, of HTTP/1.0 servers).
   */
  CONTINUE(100),

  /**
   * The requester has asked the server to switch protocols and the server has agreed to do so.
   */
  SWITCHING_PROTOCOLS(101),

  /**
   * A WebDAV request may contain many sub-requests involving file operations, requiring a long time to complete the
   * request. This code indicates that the server has received and is processing the request, but no response is
   * available yet. This prevents the client from timing out and assuming the request was lost.
   */
  PROCESSING(102),

  /**
   * Used to return some response headers before final HTTP message.
   */
  EARLY_HINTS(103),

  /**
   * Standard response for successful HTTP requests. The actual response will depend on the request method used. In a
   * GET request, the response will contain an entity corresponding to the requested resource. In a POST request, the
   * response will contain an entity describing or containing the result of the action.
   */
  OK(200),

  /**
   * The request has been fulfilled, resulting in the creation of a new resource.
   */
  CREATED(201),

  /**
   * The request has been accepted for processing, but the processing has not been completed. The request might or might
   * not be eventually acted upon, and may be disallowed when processing occurs.
   */
  ACCEPTED(202),

  /**
   * The server is a transforming proxy (e.g. a Web accelerator) that received a 200 OK from its origin, but is
   * returning a modified version of the origin's response.
   */
  NON_AUTHORITATIVE_INFORMATION(203),

  /**
   * The server successfully processed the request and is not returning any content.
   */
  NO_CONTENT(204),

  /**
   * The server successfully processed the request, but is not returning any content. Unlike a 204 response, this
   * response requires that the requester reset the document view.
   */
  RESET_CONTENT(205),

  /**
   * The server is delivering only part of the resource (byte serving) due to a range header sent by the client. The
   * range header is used by HTTP clients to enable resuming of interrupted downloads, or split a download into multiple
   * simultaneous streams.
   */
  PARTIAL_CONTENT(206),

  /**
   * The message body that follows is by default an XML message and can contain a number of separate response codes,
   * depending on how many sub-requests were made.
   */
  MULTI_STATUS(207),

  /**
   * The members of a DAV binding have already been enumerated in a preceding part of the (multistatus) response, and
   * are not being included again.
   */
  ALREADY_REPORTED(208),

  /**
   * The server has fulfilled a request for the resource, and the response is a representation of the result of one or
   * more instance-manipulations applied to the current instance.
   */
  IM_USED(226),

  /**
   * Indicates multiple options for the resource from which the client may choose (via agent-driven content
   * negotiation). For example, this code could be used to present multiple video format options, to list files with
   * different filename extensions, or to suggest word-sense disambiguation.
   */
  MULTIPLE_CHOICES(300),

  /**
   * This and all future requests should be directed to the given URI.
   */
  MOVED_PERMANENTLY(301),

  /**
   * Tells the client to look at (browse to) another URL. 302 has been superseded by 303 and 307. This is an example of
   * industry practice contradicting the standard. The HTTP/1.0 specification (RFC 1945) required the client to perform
   * a temporary redirect (the original describing phrase was "Moved Temporarily"), but popular browsers implemented 302
   * with the functionality of a 303 See Other. Therefore, HTTP/1.1 added status codes 303 and 307 to distinguish
   * between the two behaviours. However, some Web applications and frameworks use the 302 status code as if it were the
   * 303.
   */
  FOUND(302),

  /**
   * The response to the request can be found under another URI using the GET method. When received in response to a
   * POST (or PUT/DELETE), the client should presume that the server has received the data and should issue a new GET
   * request to the given URI.
   */
  SEE_OTHER(303),

  /**
   * Indicates that the resource has not been modified since the version specified by the request headers
   * If-Modified-Since or If-None-Match. In such case, there is no need to retransmit the resource since the client
   * still has a previously-downloaded copy.
   */
  NOT_MODIFIED(304),

  /**
   * The requested resource is available only through a proxy, the address for which is provided in the response. For
   * security reasons, many HTTP clients (such as Mozilla Firefox and Internet Explorer) do not obey this status code.
   */
  USE_PROXY(305),

  /**
   * No longer used. Originally meant "Subsequent requests should use the specified proxy."
   */
  SWITCH_PROXY(306),

  /**
   * In this case, the request should be repeated with another URI; however, future requests should still use the
   * original URI. In contrast to how 302 was historically implemented, the request method is not allowed to be changed
   * when reissuing the original request. For example, a POST request should be repeated using another POST request.
   */
  TEMPORARY_REDIRECT(307),

  /**
   * The request and all future requests should be repeated using another URI. 307 and 308 parallel the behaviors of 302
   * and 301, but do not allow the HTTP method to change. So, for example, submitting a form to a permanently redirected
   * resource may continue smoothly.
   */
  PERMANENT_REDIRECT(308),

  /**
   * The server cannot or will not process the request due to an apparent client error (e.g., malformed request syntax,
   * size too large, invalid request message framing, or deceptive request routing).
   */
  BAD_REQUEST(400),

  /**
   * Similar to 403 Forbidden, but specifically for use when authentication is required and has failed or has not yet
   * been provided. The response must include a WWW-Authenticate header field containing a challenge applicable to the
   * requested resource. See Basic access authentication and Digest access authentication. 401 semantically means
   * "unauthorised", the user does not have valid authentication credentials for the target resource.
   * <p>
   * Note: Some sites incorrectly issue HTTP 401 when an IP address is banned from the website (usually the website
   * domain) and that specific address is refused permission to access a website.
   */
  UNAUTHORIZED(401),

  /**
   * Reserved for future use. The original intention was that this code might be used as part of some form of digital
   * cash or micropayment scheme, as proposed, for example, by GNU Taler, but that has not yet happened, and this code
   * is not usually used. Google Developers API uses this status if a particular developer has exceeded the daily limit
   * on requests. Sipgate uses this code if an account does not have sufficient funds to start a call. Shopify uses this
   * code when the store has not paid their fees and is temporarily disabled. Stripe uses this code for failed payments
   * where parameters were correct, for example blocked fraudulent payments.
   */
  PAYMENT_REQUIRED(402),

  /**
   * The request contained valid data and was understood by the server, but the server is refusing action. This may be
   * due to the user not having the necessary permissions for a resource or needing an account of some sort, or
   * attempting a prohibited action (e.g. creating a duplicate record where only one is allowed). This code is also
   * typically used if the request provided authentication via the WWW-Authenticate header field, but the server did not
   * accept that authentication. The request should not be repeated.
   */
  FORBIDDEN(403),

  /**
   * The requested resource could not be found but may be available in the future. Subsequent requests by the client are
   * permissible.
   */
  NOT_FOUND(404),

  /**
   * A request method is not supported for the requested resource; for example, a GET request on a form that requires
   * data to be presented via POST, or a PUT request on a read-only resource.
   */
  METHOD_NOT_ALLOWED(405),

  /**
   * The requested resource is capable of generating only content not acceptable according to the Accept headers sent in
   * the request.
   */
  NOT_ACCEPTABLE(406),

  /**
   * The client must first authenticate itself with the proxy.
   */
  PROXY_AUTHENTICATION_REQUIRED(407),

  /**
   * The server timed out waiting for the request. According to HTTP specifications: "The client did not produce a
   * request within the time that the server was prepared to wait. The client MAY repeat the request without
   * modifications at any later time."
   */
  REQUEST_TIMEOUT(408),

  /**
   * Indicates that the request could not be processed because of conflict in the current state of the resource, such as
   * an edit conflict between multiple simultaneous updates.
   */
  CONFLICT(409),

  /**
   * Indicates that the resource requested is no longer available and will not be available again. This should be used
   * when a resource has been intentionally removed and the resource should be purged. Upon receiving a 410 status code,
   * the client should not request the resource in the future. Clients such as search engines should remove the resource
   * from their indices. Most use cases do not require clients and search engines to purge the resource, and a "404 Not
   * Found" may be used instead.
   */
  GONE(410),

  /**
   * The request did not specify the length of its content, which is required by the requested resource.
   */
  LENGTH_REQUIRED(411),

  /**
   * The server does not meet one of the preconditions that the requester put on the request header fields.
   */
  PRECONDITION_FAIL(412),

  /**
   * The request is larger than the server is willing or able to process. Previously called "Request Entity Too Large".
   */
  PAYLOAD_TOO_LARGE(413),

  /**
   * The URI provided was too long for the server to process. Often the result of too much data being encoded as a
   * query-string of a GET request, in which case it should be converted to a POST request. Called "Request-URI Too
   * Long" previously.
   */
  URI_TOO_LONG(414),

  /**
   * The request entity has a media type which the server or resource does not support. For example, the client uploads
   * an image as image/svg+xml, but the server requires that images use a different format.
   */
  UNSUPPORTED_MEDIA_TYPE(415),

  /**
   * The client has asked for a portion of the file (byte serving), but the server cannot supply that portion. For
   * example, if the client asked for a part of the file that lies beyond the end of the file. Called "Requested Range
   * Not Satisfiable" previously.
   */
  RANGE_NOT_SATISFIABLE(416),

  /**
   * The server cannot meet the requirements of the Expect request-header field.
   */
  EXPECTATION_FAILED(417),

  /**
   * This code was defined in 1998 as one of the traditional IETF April Fools' jokes, in RFC 2324, Hyper Text Coffee Pot
   * Control Protocol, and is not expected to be implemented by actual HTTP servers. The RFC specifies this code should
   * be returned by teapots requested to brew coffee. This HTTP status is used as an Easter egg in some websites,
   * including Google.com.
   */
  I_AM_A_TEAPOT(418),

  /**
   * The request was directed at a server that is not able to produce a response (for example because of connection
   * reuse).
   */
  MISDIRECTED_REQUEST(421),

  /**
   * The request was well-formed but was unable to be followed due to semantic errors.
   */
  UNPROCESSABLE_ENTITY(422),

  /**
   * The resource that is being accessed is locked.
   */
  LOCKED(423),

  /**
   * The request failed because it depended on another request and that request failed (e.g., a PROPPATCH).
   */
  FAILED_DEPENDENCY(424),

  /**
   * Indicates that the server is unwilling to risk processing a request that might be replayed.
   */
  TOO_EARLY(425),

  /**
   * The client should switch to a different protocol such as TLS/1.0, given in the Upgrade header field.
   */
  UPGRADE_REQUIRED(426),

  /**
   * The origin server requires the request to be conditional. Intended to prevent the 'lost update' problem, where a
   * client GETs a resource's state, modifies it, and PUTs it back to the server, when meanwhile a third party has
   * modified the state on the server, leading to a conflict.
   */
  PRECONDITION_REQUIRED(428),

  /**
   * The user has sent too many requests in a given amount of time. Intended for use with rate-limiting schemes.
   */
  TOO_MANY_REQUESTS(429),

  /**
   * The server is unwilling to process the request because either an individual header field, or all the header fields
   * collectively, are too large.
   */
  REQUEST_HEADER_FIELDS_TOO_LARGE(431),

  /**
   * A server operator has received a legal demand to deny access to a resource or to a set of resources that includes
   * the requested resource.[61] The code 451 was chosen as a reference to the novel Fahrenheit 451 (see the
   * Acknowledgements in the RFC).
   */
  UNAVAILABLE_FOR_LEGAL_REASONS(451),

  /**
   * A generic error message, given when an unexpected condition was encountered and no more specific message is
   * suitable.
   */
  INTERNAL_SERVER_ERROR(500),

  /**
   * The server either does not recognize the request method, or it lacks the ability to fulfil the request. Usually
   * this implies future availability (e.g., a new feature of a web-service API).
   */
  NOT_IMPLEMENTED(501),

  /**
   * The server was acting as a gateway or proxy and received an invalid response from the upstream server.
   */
  BAD_GATEWAY(502),

  /**
   * The server cannot handle the request (because it is overloaded or down for maintenance). Generally, this is a
   * temporary state.
   */
  SERVICE_UNAVAILABLE(503),

  /**
   * The server was acting as a gateway or proxy and did not receive a timely response from the upstream server.
   */
  GATEWAY_TIMEOUT(504),

  /**
   * The server does not support the HTTP protocol version used in the request.
   */
  HTTP_VERSION_NOT_SUPPORTED(505),

  /**
   * Transparent content negotiation for the request results in a circular reference.
   */
  VARIANT_ALSO_NEGOTIATES(506),

  /**
   * The server is unable to store the representation needed to complete the request.
   */
  INSUFFICIENT_STORAGE(507),

  /**
   * The server detected an infinite loop while processing the request (instead of {@link #ALREADY_REPORTED}).
   */
  LOOP_DETECTED(508),

  /**
   * Further extensions to the request are required for the server to fulfil it.
   */
  NOT_EXTENDED(510),

  /**
   * The client needs to authenticate to gain network access. Intended for use by intercepting proxies used to control
   * access to the network (e.g., "captive portals" used to require agreement to Terms of Service before granting full
   * Internet access via a Wi-Fi hotspot).
   */
  NETWORK_AUTHENTICATION_REQUIRED(511);

  private static final Map<Integer, HttpResponseCode> BY_CODE;

  static {
    // register all response codes by their numeric association
    var builder = ImmutableMap.<Integer, HttpResponseCode>builder();
    for (var status : values()) {
      builder.put(status.code(), status);
    }
    BY_CODE = builder.build();
  }

  private final int code;

  /**
   * Constructs a new http response code.
   *
   * @param code the numeric representation of the response code.
   */
  HttpResponseCode(int code) {
    this.code = code;
  }

  /**
   * Get the http response code associated from the numeric status code, falling back to OK if the code was not found.
   *
   * @param numeric the numeric version of the response code.
   * @return the response code associated with the numeric value, or OK.
   */
  public static @NonNull HttpResponseCode fromNumeric(int numeric) {
    return BY_CODE.getOrDefault(numeric, HttpResponseCode.OK);
  }

  /**
   * Get the numeric representation of the http response code.
   *
   * @return the numeric representation of the http response code.
   */
  public int code() {
    return this.code;
  }
}
