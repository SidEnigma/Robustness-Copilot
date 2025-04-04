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
 
 
/** Updates feature items with items in the @elements list if an item ID coincides with an item ID in the @feature object. */

void updateElements(Feature feature, Element[] elements) {
    for (Element element : elements) {
        for (Element existingElement : feature.getElements()) {
            if (existingElement.getId().equals(element.getId())) {
                // Update the existing element with the new element's properties
                existingElement.setName(element.getName());
                existingElement.setDescription(element.getDescription());
                existingElement.setKeyword(element.getKeyword());
                existingElement.setLine(element.getLine());
                existingElement.setSteps(element.getSteps());
                existingElement.setTags(element.getTags());
                existingElement.setType(element.getType());
                existingElement.setBefore(element.getBefore());
                existingElement.setAfter(element.getAfter());
                existingElement.setComments(element.getComments());
                existingElement.setMatch(element.getMatch());
                existingElement.setResult(element.getResult());
                existingElement.setEmbeddings(element.getEmbeddings());
                existingElement.setDuration(element.getDuration());
                existingElement.setStartTimestamp(element.getStartTimestamp());
                existingElement.setEndTimestamp(element.getEndTimestamp());
                existingElement.setRetest(element.isRetest());
                existingElement.setRetestRerun(element.isRetestRerun());
                existingElement.setRetestRerunOf(element.getRetestRerunOf());
                existingElement.setRetestRerunTimestamp(element.getRetestRerunTimestamp());
                existingElement.setRetestRerunDuration(element.getRetestRerunDuration());
                existingElement.setRetestRerunStartTimestamp(element.getRetestRerunStartTimestamp());
                existingElement.setRetestRerunEndTimestamp(element.getRetestRerunEndTimestamp());
                existingElement.setRetestRerunResult(element.getRetestRerunResult());
                existingElement.setRetestRerunErrorMessage(element.getRetestRerunErrorMessage());
                existingElement.setRetestRerunErrorStackTrace(element.getRetestRerunErrorStackTrace());
                existingElement.setRetestRerunErrorCause(element.getRetestRerunErrorCause());
                existingElement.setRetestRerunErrorCauseStackTrace(element.getRetestRerunErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorStackTrace(element.getRetestRerunErrorCauseErrorStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCause(element.getRetestRerunErrorCauseErrorCause());
                existingElement.setRetestRerunErrorCauseErrorCauseStackTrace(element.getRetestRerunErrorCauseErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCause(element.getRetestRerunErrorCauseErrorCauseErrorCause());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCause(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCause());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCause(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCause());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCause(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCause());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseStackTrace(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseStackTrace());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage(element.getRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseErrorMessage());
                existingElement.setRetestRerunErrorCauseErrorCauseErrorCauseErrorCauseErrorCauseError
 

}