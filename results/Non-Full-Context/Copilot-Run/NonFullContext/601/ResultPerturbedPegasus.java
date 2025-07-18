package net.glowstone.util;
 
 import com.google.common.base.CharMatcher;
 import com.google.common.collect.ImmutableMap;
 import net.glowstone.util.library.Library;
 import net.glowstone.util.library.LibraryKey;
 import net.glowstone.util.library.LibraryManager;
 
 import java.util.Map;
 import java.util.function.Function;
 import java.util.stream.Stream;
 
 /**
  * Compatibility bundles are bundles of libraries that other servers include in their servers
  * but Glowstone does not. We will download the libraries included in the bundle specified
  * within the Glowstone config.
  */
 public enum CompatibilityBundle {
     CRAFTBUKKIT(
         Stream.of(
             new Library("org.xerial", "sqlite-jdbc", "3.21.0.1",
                 LibraryManager.HashAlgorithm.SHA1, "81a0bcda2f100dc91dc402554f60ed2f696cded5"),
             new Library("mysql", "mysql-connector-java", "5.1.46",
                 LibraryManager.HashAlgorithm.SHA1, "9a3e63b387e376364211e96827bc27db8d7a92e9"),
             new Library("org.apache.logging.log4j", "log4j-slf4j-impl", "2.8.1",
                     LibraryManager.HashAlgorithm.SHA1, "709b07000087f3a0dec1bc4b5e05281499588cae"),
             new Library("org.apache.logging.log4j", "log4j-core", "2.8.1",
                     LibraryManager.HashAlgorithm.SHA1, "4ac28ff2f1ddf05dae3043a190451e8c46b73c31"),
             new Library("org.apache.logging.log4j", "log4j-iostreams", "2.8.1",
                     LibraryManager.HashAlgorithm.SHA1, "dab5403f14a8a7c553850066f9979563d19c1632"),
             new Library("org.apache.commons", "commons-lang3", "3.5",
                 LibraryManager.HashAlgorithm.SHA1, "6c6c702c89bfff3cd9e80b04d668c5e190d588c6")
         )
             .collect(ImmutableMap.toImmutableMap(Library::getLibraryKey, Function.identity()))
     ),
     NONE(ImmutableMap.of());
 
     public final Map<LibraryKey, Library> libraries;
 
     CompatibilityBundle(Map<LibraryKey, Library> libraries) {
         this.libraries = libraries;
     }
 
 
/** The given config value is converted into the appropriate bundle. */
 public static CompatibilityBundle fromConfig(String configValue){
        return Stream.of(values())
            .filter(bundle -> bundle.name().equalsIgnoreCase(configValue))
            .findFirst()
            .orElse(NONE);      
 }

 

}