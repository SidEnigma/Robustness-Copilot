/*
  * C++ Community Plugin (cxx plugin)
  * Copyright (C) 2010-2021 SonarOpenCommunity
  * http://github.com/SonarOpenCommunity/sonar-cxx
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package org.sonar.cxx.config;
 
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.IOException;
 import java.io.OutputStream;
 import java.nio.charset.Charset;
 import java.nio.charset.StandardCharsets;
 import java.util.ArrayList;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Optional;
 import javax.annotation.CheckForNull;
 import javax.annotation.Nullable;
 import org.jdom2.Attribute;
 import org.jdom2.Document;
 import org.jdom2.Element;
 import org.jdom2.Verifier;
 import org.jdom2.filter.Filters;
 import org.jdom2.output.Format;
 import org.jdom2.output.XMLOutputter;
 import org.jdom2.xpath.XPathExpression;
 import org.jdom2.xpath.XPathFactory;
 import org.sonar.api.utils.PathUtils;
 import org.sonar.api.utils.log.Logger;
 import org.sonar.api.utils.log.Loggers;
 import org.sonar.cxx.squidbridge.api.SquidConfiguration;
 
 /**
  * Database for compile options.
  *
  * To analyze source code additional information like defines and includes are needed. Only then it is possible for the
  * preprocessor and parser to generate an complete abstract syntax tree.
  *
  * The class allows to store information as key/value pairs. The information is also arranged hierarchically. If an
  * information is not found on one level, the next higher level is searched. Additional information can be e.g. on file
  * level (translation unit), global or from sonar-project.properties.
  *
  * Pre-defined hierarchy (levels): PredefinedMacros, SonarProjectProperties, Global, Files
  *
  * With {@code add} the key/value pairs are added to the database. The level parameter defines the level on which the
  * data should be inserted. For level a predefined name can be used or a new one can be defined. If level is an
  * identifier, the information is created in an element with the level-name directly under root. If level is a path, the
  * information is stored on Files level.
  *
  * With {@code get} and {@code getValues} the information is read out again afterwards. {@code get} returns the first
  * found value for key, whereby the search starts on level. {@code getValues} collects all found values over all levels.
  * It starts with the given level and further found values are added to the end of the list.
  */
 public class CxxSquidConfiguration extends SquidConfiguration {
 
   // Levels
   public static final String PREDEFINED_MACROS = "PredefinedMacros";
   public static final String SONAR_PROJECT_PROPERTIES = "SonarProjectProperties";
   public static final String GLOBAL = "Global";
   public static final String FILES = "Files";
 
   // SonarProjectProperties
   public static final String ERROR_RECOVERY_ENABLED = "ErrorRecoveryEnabled";
   public static final String CPD_IGNORE_LITERALS = "CpdIgnoreLiterals";
   public static final String CPD_IGNORE_IDENTIFIERS = "CpdIgnoreIdentifiers";
   public static final String FUNCTION_COMPLEXITY_THRESHOLD = "FunctionComplexityThreshold";
   public static final String FUNCTION_SIZE_THRESHOLD = "FunctionSizeThreshold";
   public static final String API_FILE_SUFFIXES = "ApiFileSuffixes";
   public static final String JSON_COMPILATION_DATABASE = "JsonCompilationDatabase";
 
   // Global/File Properties
   public static final String DEFINES = "Defines";
   public static final String INCLUDE_DIRECTORIES = "IncludeDirectories";
   public static final String FORCE_INCLUDES = "ForceIncludes";
 
   private static final Logger LOG = Loggers.get(CxxSquidConfiguration.class);
 
   private XPathFactory xFactory = XPathFactory.instance();
   private LinkedList<Element> parentList = new LinkedList<>();
   private Document document;
 
   private String baseDir = "";
 
   public CxxSquidConfiguration() {
     this("", Charset.defaultCharset());
   }
 
   public CxxSquidConfiguration(String baseDir) {
     this(baseDir, Charset.defaultCharset());
   }
 
   /**
    * Ctor.
    *
    * Creates the initial hierarchy for the data storage.
    */
   public CxxSquidConfiguration(String baseDir, Charset encoding) {
     super(encoding);
     this.baseDir = baseDir;
 
     var root = new Element("CompilationDatabase");
     root.setAttribute(new Attribute("version", "1.0"));
     document = new Document(root);
 
     var element = new Element(PREDEFINED_MACROS);
     root.addContent(element);
     parentList.addFirst(element);
 
     element = new Element(SONAR_PROJECT_PROPERTIES);
     root.addContent(element);
     parentList.addFirst(element);
 
     element = new Element(GLOBAL);
     root.addContent(element);
     parentList.addFirst(element);
 
     // <Files> must be first one in the list
     element = new Element(FILES);
     root.addContent(element);
     parentList.addFirst(element);
   }
 
   /**
    * Add a single key/value pair (property) to the database.
    *
    * @param level The level parameter defines the level on which the data should be inserted. For level a predefined
    * name can be used or a new one can be defined. <br>
    * - If level is an identifier, the information is created in an element with the level-name directly under root.<br>
    * - If level is a path, the information is stored on Files level. In that case the level-string is normalized and
    * converted to lower case letters to simplify the following search.
    * @param key the key to be placed into the database.
    * @param value the value corresponding to key. Several values can be assigned to one key. Internally a value-list for
    * key is created. The method can be called several times for this, but more effective is the method
    * {@code add(String, String, List<String>)}.
    */
   public void add(String level, String key, @Nullable String value) {
     if (value != null && !value.isEmpty()) {
       Element eKey = getKey(level, key);
       setValue(eKey, value);
     }
   }
 
   /**
    * Add a single key/value pair (property) to the database.
    *
    * Same as {@code add(String, String, String)} for an {@code Optional<String>}.
    *
    * @param level defines the level on which the data should be inserted
    * @param key the key to be placed into the database
    * @param value the value corresponding to key
    */
   public void add(String level, String key, Optional<String> value) {
     if (value.isPresent()) {
       Element eKey = getKey(level, key);
       setValue(eKey, value.get());
     }
   }
 
   /**
    * Add key/value pairs (properties) from an array to the database.
    *
    * Same as {@code add(String, String, String)} for an array of values.
    *
    * @param level defines the level on which the data should be inserted
    * @param key the key to be placed into the database
    * @param values the values corresponding to key
    */
   public void add(String level, String key, @Nullable String[] values) {
     if (values != null) {
       Element eKey = getKey(level, key);
       for (var value : values) {
         setValue(eKey, value);
       }
     }
   }
 
   /**
    * Add key/value pairs (properties) from a list to the database.
    *
    * Same as {@code add(String, String, String)} for a list of values.
    *
    * @param level defines the level on which the data should be inserted
    * @param key the key to be placed into the database
    * @param values the values corresponding to key
    */
   public void add(String level, String key, List<String> values) {
     if (!values.isEmpty()) {
       Element eKey = getKey(level, key);
       for (var value : values) {
         setValue(eKey, value);
       }
     }
   }
 
   /**
    * Searches for the property with the specified key.
    *
    * The first occurrence of a single value is searched for. The search is started at the specified level and if no
    * entry is found, it is continued to the higher level. The method can return {@code Optional#empty()} if the property
    * is not set.
    *
    * @param level level at which the search is started
    * @param key the property key
    * @return The value in this property list with the specified key value. Can return {@code Optional#empty()} if the
    * property is not set.
    */
   public Optional<String> get(String level, String key) {
     Element eLevel = findLevel(level, parentList.getFirst());
     do {
       if (eLevel != null) {
         Element eKey = eLevel.getChild(key);
         if (eKey != null) {
           return Optional.of(eKey.getChildText("Value"));
         }
       }
       eLevel = getParentElement(eLevel);
     } while (eLevel != null);
     return Optional.empty();
   }
 
   /**
    * Used to read multi-valued properties from one level.
    *
    * The method can return an empty list if the property is not set.
    *
    * @param level level to read
    * @param property key that is searched for
    * @return the values with the specified key value
    */
   public List<String> getLevelValues(String level, String key) {
     List<String> result = new ArrayList<>();
     Element eLevel = findLevel(level, null);
     if (eLevel != null) {
       Element eKey = eLevel.getChild(key);
       if (eKey != null) {
         for (var value : eKey.getChildren("Value")) {
           result.add(value.getText());
         }
       }
     }
 
     return result;
   }
 
   /**
    * Used to read multi-valued properties.
    *
    * Collects all found values over all levels. It starts with the given level and further found values in parent levels
    * are added to the end of the list. The method can return an empty list if the property is not set.
    *
    * @param level level at which the search is started
    * @param property key that is searched for
    * @return the values with the specified key value
    */
   public List<String> getValues(String level, String key) {
     List<String> result = new ArrayList<>();
     Element eLevel = findLevel(level, parentList.getFirst());
     do {
       if (eLevel != null) {
         Element eKey = eLevel.getChild(key);
         if (eKey != null) {
           for (var value : eKey.getChildren("Value")) {
             result.add(value.getText());
           }
         }
       }
       eLevel = getParentElement(eLevel);
     } while (eLevel != null);
     return result;
   }
 
   /**
    * Used to read multi-valued properties.
    *
    * Collects all found values over all children. Further found values in parent levels are added to the end of the
    * list. The method can return an empty list if the property is not set.
    *
    * @param level start level from which the values of all children are returned
    * @param key property key that is searched for in all children
    * @return the values with the specified key value
    */
   public List<String> getChildrenValues(String level, String key) {
     List<String> result = new ArrayList<>();
     Element eLevel = findLevel(level, parentList.getFirst());
     if (eLevel != null) {
       for (var child : eLevel.getChildren()) {
         Element eKey = child.getChild(key);
         if (eKey != null) {
           for (var value : eKey.getChildren("Value")) {
             result.add(value.getText());
           }
         }
       }
     }
     // add content of shared parents only once at the end
     eLevel = getParentElement(eLevel);
     if (eLevel != null) {
       result.addAll(getValues(eLevel.getName(), key));
     }
     return result;
   }
 
   /**
    * Effective value as boolean.
    *
    * @return {@code true} if the effective value is {@code "true"}, {@code false} for any other non empty value. If the
    * property does not have value nor default value, then {@code empty} is returned.
    */
   public Optional<Boolean> getBoolean(String level, String key) {
     return get(level, key).map(String::trim).map(Boolean::parseBoolean);
   }
 
   /**
    * Effective value as {@code int}.
    *
    * @return the value as {@code int}. If the property does not have value nor default value, then {@code empty} is
    * returned.
    * @throws IllegalStateException if value is not empty and is not a parsable integer
    */
   public Optional<Integer> getInt(String level, String key) {
     try {
       return get(level, key).map(String::trim).map(Integer::parseInt);
     } catch (NumberFormatException e) {
       throw new IllegalStateException(String.format("The property '%s' is not an int value: %s", key, e.getMessage()));
     }
   }
 
   /**
    * Effective value as {@code long}.
    *
    * @return the value as {@code long}. If the property does not have value nor default value, then {@code empty} is
    * returned.
    * @throws IllegalStateException if value is not empty and is not a parsable {@code long}
    */
   public Optional<Long> getLong(String level, String key) {
     try {
       return get(level, key).map(String::trim).map(Long::parseLong);
     } catch (NumberFormatException e) {
       throw new IllegalStateException(String.format("The property '%s' is not an long value: %s", key, e.getMessage()));
     }
   }
 
   /**
    * Effective value as {@code Float}.
    *
    * @return the value as {@code Float}. If the property does not have value nor default value, then {@code empty} is
    * returned.
    * @throws IllegalStateException if value is not empty and is not a parsable number
    */
   public Optional<Float> getFloat(String level, String key) {
     try {
       return get(level, key).map(String::trim).map(Float::valueOf);
     } catch (NumberFormatException e) {
       throw new IllegalStateException(String.format("The property '%s' is not an float value: %s", key, e.getMessage()));
     }
   }
 
   /**
    * Effective value as {@code Double}.
    *
    * @return the value as {@code Double}. If the property does not have value nor default value, then {@code empty} is
    * returned.
    * @throws IllegalStateException if value is not empty and is not a parsable number
    */
   public Optional<Double> getDouble(String level, String key) {
     try {
       return get(level, key).map(String::trim).map(Double::valueOf);
     } catch (NumberFormatException e) {
       throw new IllegalStateException(String.format("The property '%s' is not an double value: %s", key, e.getMessage()));
     }
   }
 
   /**
    * Write object to a stream: XML/UTF-8 encoded.
    *
    * @param out OutputStream to use.
    * @throws IllegalStateException in case of IOException
    */
   public void save(OutputStream out) {
     try {
       var xmlOutput = new XMLOutputter();
       xmlOutput.setFormat(Format.getPrettyFormat());
       xmlOutput.output(document, out);
     } catch (IOException e) {
       throw new IllegalStateException("Can't save XML document", e);
     }
   }
 
 
/** Returns a string representation of the object: XML/UTF-8 encoded. */

public String toString() {
  try {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    save(outputStream);
    return outputStream.toString(StandardCharsets.UTF_8.name());
  } catch (IOException e) {
    LOG.error("Error converting XML document to string", e);
    return "";
  }
}
 

}