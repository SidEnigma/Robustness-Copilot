package net.masterthought.cucumber.reducers;
 
 import net.masterthought.cucumber.json.Element;
 import net.masterthought.cucumber.json.Feature;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
 import static com.google.common.base.Preconditions.checkArgument;
 
 /**
  * Merge list of given features. If there are couple of scenarios with the same Id then
  * only the latest will be stored into the report.
  *
  * Uses when need to generate a report with rerun results of failed tests.
  */
 final class ReportFeatureWithRetestMerger implements ReportFeatureMerger {
 
     private static final String ERROR = "You are not able to use this type of results merge. The start_timestamp field" +
             " should be part of element object. Please, update the cucumber-jvm version.";
     private static final ElementComparator ELEMENT_COMPARATOR = new ElementComparator();
 
     @Override
     public List<Feature> merge(List<Feature> features) {
         Map<String, Feature> mergedFeatures = new HashMap<>();
         for (Feature candidate : features) {
             Feature mergedFeature = mergedFeatures.get(candidate.getId());
             if (mergedFeature == null) {
                 mergedFeatures.put(candidate.getId(), candidate);
             }
             else {
                 updateElements(mergedFeatures.get(candidate.getId()), candidate.getElements());
             }
         }
         return new ArrayList<>(mergedFeatures.values());
     }
 
 
/** If an Id of an item coincides with an Id of an element from the feature object, the feature's elements will be updated. */
 void updateElements(Feature feature, Element[] elements){
        checkArgument(feature.getStart_timestamp() != null, ERROR);
        for (Element element : elements) {
            if (element.getStart_timestamp() != null) {
                if (ELEMENT_COMPARATOR.compare(feature.getStart_timestamp(), element.getStart_timestamp()) < 0) {
                    feature.setStart_timestamp(element.getStart_timestamp());
                }
            }
            if (element.getEnd_timestamp() != null) {
                if (ELEMENT_COMPARATOR.compare(feature.getEnd_timestamp(), element.getEnd_timestamp()) > 0) {
                    feature.setEnd_timestamp(element.getEnd_timestamp());
                }
            }
            if (element.getId() != null) {
                if (feature.getElements().stream().noneMatch(e -> e.getId().equals(element.getId()))) {
                    feature.getElements().add(element);
                }
            }
        }
    }       
 }

                                  
     /**
      * @return true when candidate element happened after the target element.
      */
     boolean replaceIfExists(Element target, Element candidate) {
         return candidate.getStartTime().compareTo(target.getStartTime()) >= 0;
     }
 
     /**
      * @return true when element from elements array with index=elementInd is a background.
      */
     boolean isBackground(int elementInd, Element[] elements) {
         return elementInd >= 0 &&
                 elements != null &&
                 elementInd < elements.length &&
                 elements[elementInd].isBackground();
     }
 
     /**
      * @return an index of an element which is indicated as similar by rules
      * defined in the ELEMENT_COMPARATOR. The comparator indicates that
      * an element is found in the elements list with the same Id (for scenario)
      * as target element has or it's on the same line (for background).
      */
     int find(Element[] elements, Element target) {
         for (int i = 0; i < elements.length; i++) {
             if (ELEMENT_COMPARATOR.compare(elements[i], target) == 0) {
                 return i;
             }
         }
         return -1;
     }
 
     @Override
     public boolean test(List<ReducingMethod> reducingMethods) {
         return reducingMethods != null && reducingMethods.contains(ReducingMethod.MERGE_FEATURES_WITH_RETEST);
     }
 }
