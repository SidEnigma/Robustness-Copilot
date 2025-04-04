/*
  * dCache - http://www.dcache.org/
  *
  * Copyright (C) 2017 Deutsches Elektronen-Synchrotron
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package org.dcache.util;
 
 import com.google.common.collect.ImmutableMap;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.Map;
 import java.util.Optional;
 
 /**
  * Utility methods for URI objects.
  */
 public class URIs {
 
     private static final Map<String, Integer> TO_DEFAULT_PORT = ImmutableMap.<String, Integer>builder()
           .put("ftp", 21)
           .put("http", 80)
           .put("https", 443)
           .put("gsiftp", 2811)
           .put("gridftp", 2811)
           .put("ldap", 389)
           .put("ldaps", 636)
           .put("srm", 8443)
           .build();
 
     private URIs() {
         // It's a utility class!
     }
 
     /**
      * Obtain the TCP port number from the URI.  If the port number is defined in the URI then that
      * value is used.  If the URI contains no port number but a default value is known for the
      * schema, then that default value is returned; otherwise, otherwise -1 is returned.
      */
     public static int portWithDefault(URI uri) {
         return portWithDefault(uri, null, -1);
     }
 
 
/** An optional port number is obtained from the URI provided. Use the defined port number if the URI defines one. Otherwise, if known, it uses the default port number for the URI's scheme. */
 public static Optional<Integer> optionalPortWithDefault(URI uri){}

 

}