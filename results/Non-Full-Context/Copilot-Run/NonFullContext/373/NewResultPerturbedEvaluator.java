package com.twilio.jwt.validation;
 
 import com.twilio.exception.InvalidRequestException;
 import org.apache.commons.codec.digest.DigestUtils;
 import org.apache.http.Header;
 import org.apache.http.message.BasicHeader;
 
 import java.io.UnsupportedEncodingException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URLEncoder;
 import java.util.*;
 import java.util.function.Function;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Creates a canonical string out of HTTP request components.
  * <p>
  * The process of generating the canonical request is described <a href="https://www.twilio
  * .com/docs/api/credentials/public-key-client-validation-getting-started#3-create-hash-of-the-canonical-request"
  * >here</a>.
  */
 class RequestCanonicalizer {
 
     private static final String NEW_LINE = "\n";
     private static final Pattern TOKEN_REPLACE_PATTERN =
         Pattern.compile(String.format("%s|\\%s|\\%s|%s", "%7E", "+", "*", "%2F"));
 
     private final String method;
     private final String uri;
     private final String queryString;
     private final String requestBody;
     private final Header[] headers;
 
     public RequestCanonicalizer(String method, String uri, String queryString, String requestBody, Header[] headers) {
         this.method = method;
         this.uri = uri;
         this.queryString = queryString;
         this.requestBody = requestBody;
         this.headers = headers;
     }
 
 
/** Starting from a HTTP request, it creates the canonical request string. */

public String create(List<String> sortedIncludedHeaders) {
    StringBuilder canonicalRequest = new StringBuilder();

    // Append the HTTP method
    canonicalRequest.append(method).append(NEW_LINE);

    // Append the URI
    try {
        URI parsedUri = new URI(uri);
        canonicalRequest.append(URLEncoder.encode(parsedUri.getPath(), "UTF-8")).append(NEW_LINE);
    } catch (URISyntaxException | UnsupportedEncodingException e) {
        throw new InvalidRequestException("Invalid URI", e);
    }

    // Append the query string
    if (queryString != null && !queryString.isEmpty()) {
        canonicalRequest.append(URLEncoder.encode(queryString, "UTF-8")).append(NEW_LINE);
    } else {
        canonicalRequest.append(NEW_LINE);
    }

    // Append the sorted included headers
    for (String headerName : sortedIncludedHeaders) {
        String headerValue = getHeaderValue(headerName);
        canonicalRequest.append(headerName).append(":").append(headerValue).append(NEW_LINE);
    }

    // Append the request body
    if (requestBody != null && !requestBody.isEmpty()) {
        canonicalRequest.append(NEW_LINE).append(requestBody);
    }

    return canonicalRequest.toString();
}
 

}