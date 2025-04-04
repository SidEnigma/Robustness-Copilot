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
     
 
/** Update cache and retrieve settings from the database. */

public static void updateProperties() {
    logger.info("Updating properties from the database");
    
    // Retrieve settings from the database using the injected SettingsServiceBean
    Map<String, String> dbSettings = settingsSvc.listAll().stream()
            .collect(Collectors.toMap(Setting::getName, Setting::getValue));
    
    // Update the properties cache with the retrieved settings
    properties.clear();
    properties.putAll(dbSettings);
    
    // Update the last update timestamp
    lastUpdate = Instant.now();
    
    logger.info("Properties updated successfully");
}
 

}