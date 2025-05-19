/*
  * To change this license header, choose License Headers in Project Properties.
  * To change this template file, choose Tools | Templates
  * and open the template in the editor.
  */
 
 package edu.harvard.iq.dataverse;
 
 import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import static edu.harvard.iq.dataverse.util.StringUtil.isEmpty;
 import java.net.MalformedURLException;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.net.URL;
 import java.util.Optional;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 import javax.ejb.EJB;
 
 /**
  *
  * @author skraffmiller
  */
 public class GlobalId implements java.io.Serializable {
     
     public static final String DOI_PROTOCOL = "doi";
     public static final String HDL_PROTOCOL = "hdl";
     public static final String HDL_RESOLVER_URL = "https://hdl.handle.net/";
     public static final String DOI_RESOLVER_URL = "https://doi.org/";
     
     public static Optional<GlobalId> parse(String identifierString) {
         try {
             return Optional.of(new GlobalId(identifierString));
         } catch ( IllegalArgumentException _iae) {
             return Optional.empty();
         }
     }
     
     private static final Logger logger = Logger.getLogger(GlobalId.class.getName());
     
     @EJB
     SettingsServiceBean settingsService;
 
     /**
      * 
      * @param identifier The string to be parsed
      * @throws IllegalArgumentException if the passed string cannot be parsed.
      */
     public GlobalId(String identifier) {
         // set the protocol, authority, and identifier via parsePersistentId        
         if ( ! parsePersistentId(identifier) ){
             throw new IllegalArgumentException("Failed to parse identifier: " + identifier);
         }
     }
 
     public GlobalId(String protocol, String authority, String identifier) {
         this.protocol = protocol;
         this.authority = authority;
         this.identifier = identifier;
     }
     
     public GlobalId(DvObject dvObject) {
         this.authority = dvObject.getAuthority();
         this.protocol = dvObject.getProtocol();
         this.identifier = dvObject.getIdentifier(); 
     }
         
     private String protocol;
     private String authority;
     private String identifier;
 
     /**
      * Tests whether {@code this} instance has all the data required for a 
      * global id.
      * @return {@code true} iff all the fields are non-empty; {@code false} otherwise.
      */
     public boolean isComplete() {
         return !(isEmpty(protocol)||isEmpty(authority)||isEmpty(identifier));
     }
     
     public String getProtocol() {
         return protocol;
     }
 
     public void setProtocol(String protocol) {
         this.protocol = protocol;
     }
 
     public String getAuthority() {
         return authority;
     }
 
     public void setAuthority(String authority) {
         this.authority = authority;
     }
 
     public String getIdentifier() {
         return identifier;
     }
 
     public void setIdentifier(String identifier) {
         this.identifier = identifier;
     }
     
     public String toString() {
         return asString();
     }
     
 
/** Returns {@code this}' string representation. */

public String asString() {
    StringBuilder sb = new StringBuilder();
    sb.append(protocol).append(":");
    if (!isEmpty(authority)) {
        sb.append(authority).append("/");
    }
    sb.append(identifier);
    return sb.toString();
}
 

}