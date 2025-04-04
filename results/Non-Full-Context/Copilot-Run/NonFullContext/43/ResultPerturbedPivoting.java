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
     
 
/** Retrieve database settings through the service and update cache. */
 public static void updateProperties(){
       if (settingsSvc == null) {
          logger.severe("SettingsServiceBean not injected yet, cannot update properties");
          return;
       }
       Instant now = Instant.now();
       if (lastUpdate == null || now.minus(Duration.ofMinutes(1)).isBefore(lastUpdate)) {
          lastUpdate = now;
          Set<Setting> settings = settingsSvc.getSettings();
          for (Setting setting : settings) {
                properties.put(PREFIX + "." + setting.getKey(), setting.getValue());
          }
       }
    }
    
       @Override
       public Map<String, String> getProperties() {
          return properties;
       }
    
       @Override
       public String getValue(String key) {
          return properties.get(key);
       }
    
       @Override
       public String getName() {
          return "Database settings";
       }
    
       @Override
       public String getDescription() {
          return "Settings from the database";
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
       public boolean isActive() {
          return true;
       }
    
       @Override
       public boolean isRuntime() {
          return false;
       }
    
       @Override
       public boolean isPublic() {
          return false;
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
       public boolean isCached() {
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
       public boolean      
 }

 

}