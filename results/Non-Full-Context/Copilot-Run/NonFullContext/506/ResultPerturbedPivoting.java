package org.dcache.services.billing.text;
 
 import static com.google.common.base.Preconditions.checkState;
 import static com.google.common.base.Predicates.in;
 import static com.google.common.collect.Multimaps.filterValues;
 
 import com.google.common.base.Function;
 import com.google.common.collect.ImmutableCollection;
 import com.google.common.collect.ImmutableMultimap;
 import com.google.common.collect.ImmutableSet;
 import com.google.common.collect.ImmutableSetMultimap;
 import com.google.common.collect.Iterables;
 import com.google.common.collect.Maps;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedHashSet;
 import java.util.Map;
 import java.util.Set;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 /**
  * Builder for creating parsers for billing file entries.
  */
 public class BillingParserBuilder {
 
     private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\$(.+?)\\$");
 
     private ImmutableSetMultimap<String, Pattern> patternsByAttribute;
     private ImmutableSetMultimap<Pattern, String> attributesByPattern;
     private Map<String, String> formats;
 
     private final Set<String> attributes = new LinkedHashSet<>();
     private boolean canOutputArray = true;
 
     public BillingParserBuilder(Map<String, String> formats) {
         this.formats = Maps.newHashMap(formats);
         attributesByPattern = toPatterns(formats);
         patternsByAttribute = attributesByPattern.inverse();
     }
 
     public BillingParserBuilder addAttribute(String attribute) {
         attributes.add(attribute);
         return this;
     }
 
     public BillingParserBuilder addAllAttributes() {
         attributes.clear();
         attributes.addAll(patternsByAttribute.keySet());
         canOutputArray = false;
         return this;
     }
 
     public BillingParserBuilder withFormat(String message, String format) {
         formats.put(message, format);
         attributesByPattern = toPatterns(formats);
         patternsByAttribute = attributesByPattern.inverse();
         return this;
     }
 
     public BillingParserBuilder withFormat(String header) {
         String[] s = header.substring(2).trim().split(" ", 2);
         return (s.length == 2) ? withFormat(s[0], s[1]) : this;
     }
 
     public Function<String, String> buildToString() {
         String attribute = Iterables.getOnlyElement(attributes);
         String groupName = toGroupName(attribute);
         ImmutableSet<Pattern> patterns = patternsByAttribute.get(attribute);
         return line -> findSingleMatch(line, patterns, groupName);
     }
 
     public Function<String, Map<String, String>> buildToMap() {
         ImmutableMultimap<Pattern, String> patterns =
               ImmutableMultimap.copyOf(filterValues(attributesByPattern, in(attributes)));
         return line -> findMatchAsMap(line, patterns);
     }
 
     public Function<String, String[]> buildToArray() {
         checkState(canOutputArray);
 
         final ImmutableMultimap<Pattern, String> patterns =
               ImmutableMultimap.copyOf(filterValues(attributesByPattern, in(this.attributes)));
         final String[] attributes = this.attributes.toArray(String[]::new);
         return line -> findMatchAsArray(line, patterns, attributes);
     }
 
     private static String findSingleMatch(String line, ImmutableSet<Pattern> patterns,
           String groupName) {
         Matcher matcher = findMatch(line, patterns);
         return matcher != null ? matcher.group(groupName) : null;
     }
 
     private static Map<String, String> findMatchAsMap(String line,
           ImmutableMultimap<Pattern, String> patterns) {
         Matcher matcher = findMatch(line, patterns.keySet());
         if (matcher == null) {
             return Collections.emptyMap();
         }
         Map<String, String> values = new HashMap<>();
         for (String attribute : patterns.get(matcher.pattern())) {
             values.put(attribute, matcher.group(toGroupName(attribute)));
         }
         return values;
     }
 
     private static String[] findMatchAsArray(String line,
           ImmutableMultimap<Pattern, String> patterns,
           String[] attributes) {
         Matcher matcher = findMatch(line, patterns.keySet());
         String[] result = new String[attributes.length];
         if (matcher != null) {
             ImmutableCollection<String> attributesInPattern = patterns.get(matcher.pattern());
             for (int i = 0; i < attributes.length; i++) {
                 String attribute = attributes[i];
                 if (attributesInPattern.contains(attribute)) {
                     result[i] = matcher.group(toGroupName(attribute));
                 }
             }
         }
         return result;
     }
 
     private static Matcher findMatch(String line, Collection<Pattern> patterns) {
         Matcher result = null;
         for (Pattern pattern : patterns) {
             Matcher matcher = pattern.matcher(line);
             if (matcher.matches()) {
                 if (result != null) {
                     throw new IllegalArgumentException("Duplicate matches for: " + line);
                 }
                 result = matcher;
             }
         }
         return result;
     }
 
     /**
      * Returns Patterns for the provided billing formats, as a Multimap mapping the Pattern to the
      * attributes contained in the pattern.
      */
     private static ImmutableSetMultimap<Pattern, String> toPatterns(Map<String, String> formats) {
         ImmutableSetMultimap.Builder<Pattern, String> builder = ImmutableSetMultimap.builder();
         for (Map.Entry<String, String> format : formats.entrySet()) {
             builder.putAll(toPattern(format.getKey(), format.getValue()),
                   toAttributes(format.getValue()));
         }
         return builder.build();
     }
 
 
/** Returns a template to match the billing format provided. */
 private static Pattern toPattern(String name, String format){
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(format);
        while (matcher.find()) {
            matcher.appendReplacement(sb, "([^\\s]+)");
        }
        matcher.appendTail(sb);
        sb.append("$");
        return Pattern.compile(sb.toString());      
 }

 

}