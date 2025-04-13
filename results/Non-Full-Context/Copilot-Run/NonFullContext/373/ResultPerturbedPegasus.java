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
 
 
/** A string of request components is created. */
 public String create(List<String> sortedIncludedHeaders){
        StringBuilder builder = new StringBuilder();
        builder.append(method).append(NEW_LINE);
        builder.append(uri).append(NEW_LINE);
        builder.append(queryString).append(NEW_LINE);
        builder.append(requestBody).append(NEW_LINE);
        builder.append(createSortedHeaders(sortedIncludedHeaders)).append(NEW_LINE);
        return builder.toString();      
 }

 

}