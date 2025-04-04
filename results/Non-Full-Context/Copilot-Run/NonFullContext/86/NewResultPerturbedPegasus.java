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

void updateElements(Feature feature, Element[] elements) {
    // Iterate through the elements array
    for (Element element : elements) {
        // Check if the element's Id matches with any item's Id in the feature object
        for (int i = 0; i < feature.getElements().length; i++) {
            if (feature.getElements()[i].getId().equals(element.getId())) {
                // Update the element in the feature object
                feature.getElements()[i] = element;
                break;
            }
        }
    }
}
 

}