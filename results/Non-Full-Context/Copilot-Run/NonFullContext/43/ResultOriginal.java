package edu.harvard.iq.dataverse.settings.source;
 
 import edu.harvard.iq.dataverse.settings.Setting;
 import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
 import org.eclipse.microprofile.config.spi.ConfigSource;
 
 import java.time.Duration;
 import java.time.Instant;
 import java.util.Map;
 import java.util.Set;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.logging.Logger;
 
 /**
  * A caching wrapper around SettingServiceBean to provide database settings to MicroProfile Config API.
  * Please be aware that this class relies on dependency injection during the application startup.
  * Values will not be available before and a severe message will be logged to allow monitoring (potential race conditions)
  * The settings will be cached for at least one minute, avoiding unnecessary database calls.
  */
 public class DbSettingConfigSource implements ConfigSource {
     
     private static final Logger logger = Logger.getLogger(DbSettingConfigSource.class.getCanonicalName());
     private static final ConcurrentHashMap<String, String> properties = new ConcurrentHashMap<>();
     private static Instant lastUpdate;
     private static SettingsServiceBean settingsSvc;
     public static final String PREFIX = "dataverse.settings.fromdb";
     
     /**
      * Let the SettingsServiceBean be injected by DbSettingConfigHelper with PostConstruct
      * @param injected
      */
     public static void injectSettingsService(SettingsServiceBean injected) {
        settingsSvc = injected;
        updateProperties();
     }
     
 
/** Retrieve settings from the database via service and update cache. */
 public static void updateProperties(){
       if (settingsSvc == null) {
          logger.severe("SettingsServiceBean not injected yet, cannot update properties");
          return;
       }
       Instant now = Instant.now();
       if (lastUpdate == null || Duration.between(lastUpdate, now).toMillis() > 60000) {
          lastUpdate = now;
          Map<String, Setting> settings = settingsSvc.getSettings();
          Set<String> keys = settings.keySet();
          for (String key : keys) {
                Setting setting = settings.get(key);
                properties.put(PREFIX + "." + key, setting.getValue());
          }
       }
    }
    
       @Override
       public String getValue(String key) {
          if (key.startsWith(PREFIX)) {
                String settingKey = key.substring(PREFIX.length() + 1);
                return properties.get(settingKey);
          } else {
                return null;
          }
       }
    
       @Override
       public Set<String> getKeys() {
          return properties.keySet();
       }
    
       @Override
       public Map<String, String> getProperties() {
          return properties;
       }
    
       @Override
       public String getName() {
          return "Database settings";
       }
    
       @Override
       public int getOrdinal() {
          return 0;
       }
    
       @Override
       public boolean exists() {
          return true;
       }
    
       @Override
       public boolean isSingleton() {
          return true;
       }
    
       @Override
       public boolean isDynamic() {
          return false;
       }
    
       @Override
       public boolean isCacheable() {
          return true;
       }
    
       @Override
       public boolean isRemovable() {
          return false;
       }
    
       @Override
       public boolean isReadOnly() {
          return true;
       }
    
       @Override
       public boolean isOnline() {
          return true;
       }    
 }

 

}