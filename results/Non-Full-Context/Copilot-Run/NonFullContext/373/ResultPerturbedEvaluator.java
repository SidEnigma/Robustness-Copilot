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
 public String create(List<String> sortedIncludedHeaders){
        StringBuilder canonicalRequest = new StringBuilder();
        canonicalRequest.append(method).append(NEW_LINE);
        canonicalRequest.append(canonicalUri()).append(NEW_LINE);
        canonicalRequest.append(canonicalQueryString()).append(NEW_LINE);
        canonicalRequest.append(canonicalHeaders(sortedIncludedHeaders)).append(NEW_LINE);
        canonicalRequest.append(canonicalRequestBody());
        return canonicalRequest.toString();
    }
    
        private String canonicalUri() {
            try {
                return new URI(uri).getPath();
            } catch (URISyntaxException e) {
                throw new InvalidRequestException("Invalid URI: " + uri, e);
            }
        }
    
        private String canonicalQueryString() {
            if (queryString == null) {
                return "";
            }
            String[] queryParams = queryString.split("&");
            Arrays.sort(queryParams);
            StringBuilder canonicalQueryString = new StringBuilder();
            for (String queryParam : queryParams) {
                canonicalQueryString.append(queryParam).append("&");
            }
            return canonicalQueryString.substring(0, canonicalQueryString.length() - 1);
        }
    
        private String canonicalHeaders(List<String> sortedIncludedHeaders) {
            StringBuilder canonicalHeaders = new StringBuilder();
            for (String header : sortedIncludedHeaders) {
                canonicalHeaders.append(header).append(":").append(getHeaderValue(header)).append(NEW_LINE);
            }
            return canonicalHeaders.substring(0, canonicalHeaders.length() - 1);
        }
    
        private String canonicalRequestBody() {
            if (requestBody == null) {
                return "";
            }
            return requestBody;
        }
    
        private String getHeaderValue(String header) {
            for (Header h : headers) {
                if (h.getName().equals(header)) {
                    return h.getValue();
                }
            }
            return "";
        }       
 }

 

}