/* dCache - http://www.dcache.org/
  *
  * Copyright (C) 2019 Deutsches Elektronen-Synchrotron
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
 package org.dcache.auth;
 
 import static org.dcache.auth.EntityDefinition.PERSON;
 
 import com.google.common.collect.ImmutableMap;
 import java.util.Collection;
 import java.util.EnumSet;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Optional;
 import java.util.stream.Collectors;
 
 /**
  * A class containing utility methods for working with LoA statements.
  */
 public class LoAs {
 
     /**
      * A set of universal equivalent LoAs.  These relationships are transitive but not symmetric.
      */
     private static final Map<LoA, LoA> GENERIC_EQUIVALENT_LOA = ImmutableMap.<LoA, LoA>builder()
 
           /* From https://www.igtf.net/ap/authn-assurance/ */
           .put(LoA.IGTF_AP_SLCS, LoA.IGTF_LOA_ASPEN)
           .put(LoA.IGTF_AP_MICS, LoA.IGTF_LOA_BIRCH)
           .put(LoA.IGTF_AP_CLASSIC, LoA.IGTF_LOA_CEDAR)
           .put(LoA.IGTF_AP_IOTA, LoA.IGTF_LOA_DOGWOOD)
 
           /* From https://wiki.refeds.org/display/ASS/REFEDS+Assurance+Framework+ver+1.0 */
           .put(LoA.REFEDS_IAP_HIGH, LoA.REFEDS_IAP_MEDIUM)
           .put(LoA.REFEDS_IAP_MEDIUM, LoA.REFEDS_IAP_LOW)
           .build();
 
     /**
      * These equivalent LoAs if the identified entity is a natural person.  This mapping contains
      * all the generic equivalent mappings.
      */
     private static final Map<LoA, LoA> PERSONAL_EQUIVALENT_LOA = ImmutableMap.<LoA, LoA>builder()
           /* From https://wiki.refeds.org/display/ASS/REFEDS+Assurance+Framework+ver+1.0 */
           .put(LoA.IGTF_LOA_ASPEN, LoA.REFEDS_IAP_LOW)
           .put(LoA.IGTF_LOA_DOGWOOD, LoA.REFEDS_IAP_LOW)
           .put(LoA.IGTF_LOA_BIRCH, LoA.REFEDS_IAP_MEDIUM)
           .put(LoA.IGTF_LOA_CEDAR, LoA.REFEDS_IAP_MEDIUM)
           .putAll(GENERIC_EQUIVALENT_LOA)
           .build();
 
     private LoAs() {
         // prevent instantiation.
     }
 
 
/** Convert a set of asserted LoAs so that it includes all equivalent LoAs. */
 public static EnumSet<LoA> withImpliedLoA(Optional<EntityDefinition> entity, Collection<LoA> asserted){
      if (entity.isPresent() && entity.get() == PERSON) {
          return asserted.stream()
                  .map(LoAs::withImpliedLoA)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toCollection(EnumSet::copyOf));
      } else {
          return EnumSet.copyOf(asserted);
      }                 
 }

 

}