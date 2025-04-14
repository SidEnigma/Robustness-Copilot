package com.twilio;
 
 import com.twilio.exception.ApiException;
 import com.twilio.exception.AuthenticationException;
 import com.twilio.exception.CertificateValidationException;
 import com.twilio.http.HttpMethod;
 import com.twilio.http.NetworkHttpClient;
 import com.twilio.http.Request;
 import com.twilio.http.Response;
 import com.twilio.http.TwilioRestClient;
 
 import java.util.Objects;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.io.File;
 
 /**
  * Singleton class to initialize Twilio environment.
  */
 public class Twilio {
 
     public static final String VERSION = "8.26.0";
     public static final String JAVA_VERSION = System.getProperty("java.version");
 
     private static String username = System.getenv("TWILIO_ACCOUNT_SID");
     private static String password = System.getenv("TWILIO_AUTH_TOKEN");
     private static String accountSid; // username used if this is null
     private static String region = System.getenv("TWILIO_REGION");
     private static String edge = System.getenv("TWILIO_EDGE");
     private static volatile TwilioRestClient restClient;
     private static volatile ExecutorService executorService;
 
     private Twilio() {
     }
 
     /*
      * Ensures that the ExecutorService is shutdown when the JVM exits.
      */
     static {
         Runtime.getRuntime().addShutdownHook(new Thread() {
             @Override
             public void run() {
                 if (executorService != null) {
                     executorService.shutdownNow();
                 }
             }
         });
     }
 
     /**
      * Initialize the Twilio environment.
      *
      * @param username account to use
      * @param password auth token for the account
      */
     public static synchronized void init(final String username, final String password) {
         Twilio.setUsername(username);
         Twilio.setPassword(password);
     }
 
     /**
      * Initialize the Twilio environment.
      *
      * @param username   account to use
      * @param password   auth token for the account
      * @param accountSid account sid to use
      */
     public static synchronized void init(final String username, final String password, final String accountSid) {
         Twilio.setUsername(username);
         Twilio.setPassword(password);
         Twilio.setAccountSid(accountSid);
     }
 
     /**
      * Set the username.
      *
      * @param username account to use
      * @throws AuthenticationException if username is null
      */
     public static synchronized void setUsername(final String username) {
         if (username == null) {
             throw new AuthenticationException("Username can not be null");
         }
 
         if (!username.equals(Twilio.username)) {
             Twilio.invalidate();
         }
 
         Twilio.username = username;
     }
 
     /**
      * Set the auth token.
      *
      * @param password auth token to use
      * @throws AuthenticationException if password is null
      */
     public static synchronized void setPassword(final String password) {
         if (password == null) {
             throw new AuthenticationException("Password can not be null");
         }
 
         if (!password.equals(Twilio.password)) {
             Twilio.invalidate();
         }
 
         Twilio.password = password;
     }
 
     /**
      * Set the account sid.
      *
      * @param accountSid account sid to use
      * @throws AuthenticationException if account sid is null
      */
     public static synchronized void setAccountSid(final String accountSid) {
         if (accountSid == null) {
             throw new AuthenticationException("AccountSid can not be null");
         }
 
         if (!accountSid.equals(Twilio.accountSid)) {
             Twilio.invalidate();
         }
 
         Twilio.accountSid = accountSid;
     }
 
     /**
      * Set the region.
      *
      * @param region region to make request
      */
     public static synchronized void setRegion(final String region) {
         if (!Objects.equals(region, Twilio.region)) {
             Twilio.invalidate();
         }
 
         Twilio.region = region;
     }
 
     /**
      * Set the edge.
      *
      * @param edge edge to make request
      */
     public static synchronized void setEdge(final String edge) {
         if (!Objects.equals(edge, Twilio.edge)) {
             Twilio.invalidate();
         }
 
         Twilio.edge = edge;
     }
 
     /**
      * Returns (and initializes if not initialized) the Twilio Rest Client.
      *
      * @return the Twilio Rest Client
      * @throws AuthenticationException if initialization required and either accountSid or authToken is null
      */
     public static TwilioRestClient getRestClient() {
         if (Twilio.restClient == null) {
             synchronized (Twilio.class) {
                 if (Twilio.restClient == null) {
                     Twilio.restClient = buildRestClient();
                 }
             }
         }
 
         return Twilio.restClient;
     }
 
     private static TwilioRestClient buildRestClient() {
         if (Twilio.username == null || Twilio.password == null) {
             throw new AuthenticationException(
                 "TwilioRestClient was used before AccountSid and AuthToken were set, please call Twilio.init()"
             );
         }
 
         TwilioRestClient.Builder builder = new TwilioRestClient.Builder(Twilio.username, Twilio.password);
 
         if (Twilio.accountSid != null) {
             builder.accountSid(Twilio.accountSid);
         }
 
         builder.region(Twilio.region);
         builder.edge(Twilio.edge);
 
         return builder.build();
     }
 
     /**
      * Use a custom rest client.
      *
      * @param restClient rest client to use
      */
     public static void setRestClient(final TwilioRestClient restClient) {
         synchronized (Twilio.class) {
             Twilio.restClient = restClient;
         }
     }
 
     /**
      * Returns the Twilio executor service.
      *
      * @return the Twilio executor service
      */
     public static ExecutorService getExecutorService() {
         if (Twilio.executorService == null) {
             synchronized (Twilio.class) {
                 if (Twilio.executorService == null) {
                     Twilio.executorService = Executors.newCachedThreadPool();
                 }
             }
         }
         return Twilio.executorService;
     }
 
     /**
      * Use a custom executor service.
      *
      * @param executorService executor service to use
      */
     public static void setExecutorService(final ExecutorService executorService) {
         synchronized (Twilio.class) {
             Twilio.executorService = executorService;
         }
     }
 
 
/** Validate that we can connect to the new SSL certificate posted on api. */
 public static void validateSslCertificate(){
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(new TLSSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new TLSSocketFactory.AllHostsValid());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
        /**
        * Invalidate the rest client.
        */
        private static void invalidate() {
            Twilio.restClient = null;
        }
    
        /**
        * Returns the Twilio account sid.
        *
        * @return the Twilio account sid
        */
        public static String getAccountSid() {
            return Twilio.accountSid;
        }
    
        /**
        * Returns the Twilio auth token.
        *
        * @return the Twilio auth token
        */
        public static String getPassword() {
            return Twilio.password;
        }
    
        /**
        * Returns the Twilio username.
        *
        * @return the Twilio username
        */
        public static String getUsername() {
            return Twilio.username;
        }
    
        /**
        * Returns the Twilio region.
        *
        * @return the Twilio region
        */
        public static String getRegion() {
            return Twilio.region;
        }
    
        /**
        * Returns the Twilio edge.
        *
        * @return the Twilio edge
        */
        public static String getEdge() {
            return Twilio.edge;
        }
    
        /**
        * Returns the Twilio rest client.
        *
        * @return the Twilio rest client
        */
        public static TwilioRestClient getRestClientInstance() {
            return Twilio.restClient;
        }
    
        /**
        * Returns the Twilio executor service.
        *
        * @return the Twilio executor service
        */
        public static ExecutorService getExecutorServiceInstance() {
            return Twilio.      
 }

 

}