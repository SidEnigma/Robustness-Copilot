/**
  * Jooby https://jooby.io
  * Apache License Version 2.0 https://jooby.io/LICENSE.txt
  * Copyright 2014 Edgar Espina
  */
 package io.jooby;
 
 import com.typesafe.config.Config;
 
 import javax.annotation.Nonnull;
 import javax.annotation.Nullable;
 import javax.crypto.Mac;
 import javax.crypto.spec.SecretKeySpec;
 import java.io.UnsupportedEncodingException;
 import java.net.URLDecoder;
 import java.net.URLEncoder;
 import java.nio.charset.StandardCharsets;
 import java.time.Duration;
 import java.time.Instant;
 import java.time.ZoneId;
 import java.time.format.DateTimeFormatter;
 import java.util.Base64;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Optional;
 import java.util.concurrent.TimeUnit;
 import java.util.function.BiFunction;
 import java.util.function.Consumer;
 
 /**
  * Response cookie implementation. Response are send it back to client using
  * {@link Context#setResponseCookie(Cookie)}.
  *
  * @author edgar
  * @since 2.0.0
  */
 public class Cookie {
 
   /** Algorithm name. */
   public static final String HMAC_SHA256 = "HmacSHA256";
 
   private static final DateTimeFormatter fmt = DateTimeFormatter
       .ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
       .withZone(ZoneId.of("GMT"));
 
   /** Cookie's name. */
   private String name;
 
   /** Cookie's value. */
   private String value;
 
   /** Cookie's domain. */
   private String domain;
 
   /** Cookie's path. */
   private String path;
 
   /** HttpOnly flag. */
   private boolean httpOnly;
 
   /** True, ensure that the session cookie is only transmitted via HTTPS. */
   private boolean secure;
 
   /**
    * By default, <code>-1</code> is returned, which indicates that the cookie will persist until
    * browser shutdown. In seconds.
    */
   private long maxAge = -1;
 
   /**
    * Value for the 'SameSite' cookie attribute.
    *
    * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
    *   https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
    */
   private SameSite sameSite;
 
   /**
    * Creates a response cookie.
    *
    * @param name Cookie's name.
    * @param value Cookie's value or <code>null</code>.
    */
   public Cookie(@Nonnull String name, @Nullable String value) {
     this.name = name;
     this.value = value;
   }
 
   /**
    * Creates a response cookie without a value.
    *
    * @param name Cookie's name.
    */
   public Cookie(@Nonnull String name) {
     this(name, null);
   }
 
   private Cookie(@Nonnull Cookie cookie) {
     this.domain = cookie.domain;
     this.value = cookie.value;
     this.name = cookie.name;
     this.maxAge = cookie.maxAge;
     this.path = cookie.path;
     this.secure = cookie.secure;
     this.httpOnly = cookie.httpOnly;
     this.sameSite = cookie.sameSite;
   }
 
   /**
    * Copy all state from this cookie and creates a new cookie.
    *
    * @return New cookie.
    */
   public @Nonnull Cookie clone() {
     return new Cookie(this);
   }
 
   /**
    * Cookie's name.
    *
    * @return Cookie's name.
    */
   public @Nonnull String getName() {
     return name;
   }
 
   /**
    * Set cookie's name.
    *
    * @param name Cookie's name.
    * @return This cookie.
    */
   public @Nonnull Cookie setName(@Nonnull String name) {
     this.name = name;
     return this;
   }
 
   /**
    * Cookie's value.
    *
    * @return Cookie's value.
    */
   public @Nullable String getValue() {
     return value;
   }
 
   /**
    * Set cookie's value.
    *
    * @param value Cookie's value.
    * @return This cookie.
    */
   public @Nonnull Cookie setValue(@Nonnull String value) {
     this.value = value;
     return this;
   }
 
   /**
    * Cookie's domain.
    *
    * @return Cookie's domain.
    */
   public @Nullable String getDomain() {
     return domain;
   }
 
   /**
    * Get cookie's domain.
    *
    * @param domain Defaults cookie's domain.
    * @return Cookie's domain..
    */
   public @Nonnull String getDomain(@Nonnull String domain) {
     return this.domain == null ? domain : domain;
   }
 
   /**
    * Set cookie's domain.
    *
    * @param domain Cookie's domain.
    * @return This cookie.
    */
   public @Nonnull Cookie setDomain(@Nonnull String domain) {
     this.domain = domain;
     return this;
   }
 
   /**
    * Cookie's path.
    *
    * @return Cookie's path.
    */
   public @Nullable String getPath() {
     return path;
   }
 
   /**
    * Cookie's path.
    *
    * @param path Defaults path.
    * @return Cookie's path.
    */
   public @Nonnull String getPath(@Nonnull String path) {
     return this.path == null ? path : this.path;
   }
 
   /**
    * Set cookie's path.
    *
    * @param path Cookie's path.
    * @return This cookie.
    */
   public @Nonnull Cookie setPath(@Nonnull String path) {
     this.path = path;
     return this;
   }
 
   /**
    * Cookie's http-only flag.
    *
    * @return Htto-only flag.
    */
   public boolean isHttpOnly() {
     return httpOnly;
   }
 
   /**
    * Set cookie's http-only.
    *
    * @param httpOnly Cookie's http-only.
    * @return This cookie.
    */
   public Cookie setHttpOnly(boolean httpOnly) {
     this.httpOnly = httpOnly;
     return this;
   }
 
   /**
    * Secure cookie.
    *
    * @return Secure cookie flag.
    */
   public boolean isSecure() {
     return secure;
   }
 
   /**
    * Set cookie secure flag.
    *
    * @param secure Cookie's secure.
    * @return This cookie.
    * @throws IllegalArgumentException if {@code false} is specified and the 'SameSite'
    * attribute value requires a secure cookie.
    */
   public @Nonnull Cookie setSecure(boolean secure) {
     if (sameSite != null && sameSite.requiresSecure() && !secure) {
       throw new IllegalArgumentException("Cookies with SameSite=" + sameSite.getValue()
           + " must be flagged as Secure. Call Cookie.setSameSite(...) with an argument"
           + " allowing non-secure cookies before calling Cookie.setSecure(false).");
     }
     this.secure = secure;
     return this;
   }
 
   /**
    * Max age value:
    *
    * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
    * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
    * - <code>positive value</code>: indicates the number of seconds from current date, where browser
    *   must expires the cookie.
    *
    * @return Max age, in seconds.
    */
   public long getMaxAge() {
     return maxAge;
   }
 
   /**
    * Set max age value:
    *
    * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
    * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
    * - <code>positive value</code>: indicates the number of seconds from current date, where browser
    *   must expires the cookie.
    *
    * @param maxAge Cookie max age.
    * @return This options.
    */
   public @Nonnull Cookie setMaxAge(@Nonnull Duration maxAge) {
     return setMaxAge(maxAge.getSeconds());
   }
 
   /**
    * Set max age value:
    *
    * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
    * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
    * - <code>positive value</code>: indicates the number of seconds from current date, where browser
    *   must expires the cookie.
    *
    * @param maxAge Cookie max age, in seconds.
    * @return This options.
    */
   public @Nonnull Cookie setMaxAge(long maxAge) {
     if (maxAge >= 0) {
       this.maxAge = maxAge;
     } else {
       this.maxAge = -1;
     }
     return this;
   }
 
   /**
    * Returns the value for the 'SameSite' parameter.
    * <ul>
    *   <li>{@link SameSite#LAX} - Cookies are allowed to be sent with top-level navigations and
    *   will be sent along with GET request initiated by third party website. This is the default
    *   value in modern browsers.</li>
    *   <li>{@link SameSite#STRICT} - Cookies will only be sent in a first-party context and not be
    *   sent along with requests initiated by third party websites.</li>
    *   <li>{@link SameSite#NONE} - Cookies will be sent in all contexts, i.e sending cross-origin
    *   is allowed. Requires the {@code Secure} attribute in latest browser versions.</li>
    *   <li>{@code null} - Not specified.</li>
    * </ul>
    *
    * @return the value for 'SameSite' parameter.
    * @see #setSecure(boolean)
    * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
    *   https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
    */
   @Nullable
   public SameSite getSameSite() {
     return sameSite;
   }
 
   /**
    * Sets the value for the 'SameSite' parameter.
    * <ul>
    *   <li>{@link SameSite#LAX} - Cookies are allowed to be sent with top-level navigations and
    *   will be sent along with GET request initiated by third party website. This is the default
    *   value in modern browsers.</li>
    *   <li>{@link SameSite#STRICT} - Cookies will only be sent in a first-party context and not be
    *   sent along with requests initiated by third party websites.</li>
    *   <li>{@link SameSite#NONE} - Cookies will be sent in all contexts, i.e sending cross-origin
    *   is allowed. Requires the {@code Secure} attribute in latest browser versions.</li>
    *   <li>{@code null} - Not specified.</li>
    * </ul>
    *
    * @param sameSite the value for the 'SameSite' parameter.
    * @return this instance.
    * @throws IllegalArgumentException if a value requiring a secure cookie is specified and this
    * cookie is not flagged as secure.
    * @see #setSecure(boolean)
    * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
    *   https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
    */
   public Cookie setSameSite(@Nullable SameSite sameSite) {
     if (sameSite != null && sameSite.requiresSecure() && !isSecure()) {
       throw new IllegalArgumentException("Cookies with SameSite=" + sameSite.getValue()
           + " must be flagged as Secure. Call Cookie.setSecure(true)"
           + " before calling Cookie.setSameSite(...).");
     }
     this.sameSite = sameSite;
     return this;
   }
 
   @Override public String toString() {
     StringBuilder buff = new StringBuilder();
     buff.append(name).append("=");
     if (value != null) {
       buff.append(value);
     }
     return buff.toString();
   }
 
   /**
    * Generates a cookie string. This is the value we sent to the client as <code>Set-Cookie</code>
    * header.
    *
    * @return Cookie string.
    */
   public @Nonnull String toCookieString() {
     StringBuilder sb = new StringBuilder();
 
     // name = value
     append(sb, name);
     sb.append("=");
     if (value != null) {
       append(sb, value);
     }
 
     // Path
     if (path != null) {
       sb.append(";Path=");
       append(sb, path);
     }
 
     // Domain
     if (domain != null) {
       sb.append(";Domain=");
       append(sb, domain);
     }
 
     // SameSite
     if (sameSite != null) {
       sb.append(";SameSite=");
       append(sb, sameSite.getValue());
     }
 
     // Secure
     if (secure) {
       sb.append(";Secure");
     }
 
     // HttpOnly
     if (httpOnly) {
       sb.append(";HttpOnly");
     }
 
     // Max-Age
     if (maxAge >= 0) {
       sb.append(";Max-Age=").append(maxAge);
 
       /** Old browsers don't support Max-Age. */
       long expires;
       if (maxAge > 0) {
         expires = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge);
       } else {
         expires = 0;
       }
       sb.append(";Expires=").append(fmt.format(Instant.ofEpochMilli(expires)));
     }
 
     return sb.toString();
   }
 
   /**
    * Sign a value using a secret key. A value and secret key are required. Sign is done with
    * {@link #HMAC_SHA256}.
    * Signed value looks like:
    *
    * <pre>
    *   [signed value] '|' [raw value]
    * </pre>
    *
    * @param value A value to sign.
    * @param secret A secret key.
    * @return A signed value.
    */
   public static @Nonnull String sign(final @Nonnull String value, final @Nonnull String secret) {
     try {
       Mac mac = Mac.getInstance(HMAC_SHA256);
       mac.init(new SecretKeySpec(secret.getBytes(), HMAC_SHA256));
       byte[] bytes = mac.doFinal(value.getBytes());
       return Base64.getEncoder().withoutPadding().encodeToString(bytes) + "|" + value;
     } catch (Exception x) {
       throw SneakyThrows.propagate(x);
     }
   }
 
 
/** Unsign a value previously signed with {@link #sign(String, String)}. */

public static String unsign(@Nonnull final String value, @Nonnull final String secret) {
  try {
    int separatorIndex = value.indexOf("|");
    if (separatorIndex < 0) {
      throw new IllegalArgumentException("Invalid signed value: " + value);
    }
    String signature = value.substring(0, separatorIndex);
    String originalValue = value.substring(separatorIndex + 1);
    
    Mac mac = Mac.getInstance(HMAC_SHA256);
    mac.init(new SecretKeySpec(secret.getBytes(), HMAC_SHA256));
    byte[] expectedSignature = Base64.getDecoder().decode(signature);
    byte[] actualSignature = mac.doFinal(originalValue.getBytes());
    
    if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
      throw new IllegalArgumentException("Invalid signature for value: " + value);
    }
    
    return originalValue;
  } catch (Exception x) {
    throw SneakyThrows.propagate(x);
  }
}
}