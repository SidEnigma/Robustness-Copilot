package net.masterthought.cucumber;
 
 import java.util.Arrays;
 
 import org.apache.commons.lang.ArrayUtils;
 
 /**
  * Contains historical information about all and failed features, scenarios and steps.
  *
  * @author Damian Szczepanik (damianszczepanik@github)
  */
 public class Trends {
 
     private String[] buildNumbers = new String[0];
 
     private int[] passedFeatures = new int[0];
     private int[] failedFeatures = new int[0];
     private int[] totalFeatures = new int[0];
 
     private int[] passedScenarios = new int[0];
     private int[] failedScenarios = new int[0];
     private int[] totalScenarios = new int[0];
 
     private int[] passedSteps = new int[0];
     private int[] failedSteps = new int[0];
     private int[] skippedSteps = new int[0];
     private int[] pendingSteps = new int[0];
     private int[] undefinedSteps = new int[0];
     private int[] totalSteps = new int[0];
 
     private long[] durations = new long[0];
 
     public String[] getBuildNumbers() {
         return buildNumbers;
     }
 
     public int[] getFailedFeatures() {
         return failedFeatures;
     }
 
     public int[] getPassedFeatures() {
         return passedFeatures;
     }
 
     public int[] getTotalFeatures() {
         return totalFeatures;
     }
 
     public int[] getPassedScenarios() {
         return passedScenarios;
     }
 
     public int[] getFailedScenarios() {
         return failedScenarios;
     }
 
     public int[] getTotalScenarios() {
         return totalScenarios;
     }
 
     public int[] getPassedSteps() {
         return passedSteps;
     }
 
     public int[] getFailedSteps() {
         return failedSteps;
     }
 
     public int[] getSkippedSteps() {
         return skippedSteps;
     }
 
     public int[] getPendingSteps() {
         return pendingSteps;
     }
 
     public int[] getUndefinedSteps() {
         return undefinedSteps;
     }
 
     public int[] getTotalSteps() {
         return totalSteps;
     }
 
     public long[] getDurations() {
         return durations;
     }
 
     /**
      * Adds build into the trends.
      * @param buildNumber number of the build
      * @param reportable stats for the generated report
      */
     public void addBuild(String buildNumber, Reportable reportable) {
 
         buildNumbers = (String[]) ArrayUtils.add(buildNumbers, buildNumber);
 
         passedFeatures = ArrayUtils.add(passedFeatures, reportable.getPassedFeatures());
         failedFeatures = ArrayUtils.add(failedFeatures, reportable.getFailedFeatures());
         totalFeatures = ArrayUtils.add(totalFeatures, reportable.getFeatures());
 
         passedScenarios = ArrayUtils.add(passedScenarios, reportable.getPassedScenarios());
         failedScenarios = ArrayUtils.add(failedScenarios, reportable.getFailedScenarios());
         totalScenarios = ArrayUtils.add(totalScenarios, reportable.getScenarios());
 
         passedSteps = ArrayUtils.add(passedSteps, reportable.getPassedSteps());
         failedSteps = ArrayUtils.add(failedSteps, reportable.getFailedSteps());
         skippedSteps = ArrayUtils.add(skippedSteps, reportable.getSkippedSteps());
         pendingSteps = ArrayUtils.add(pendingSteps, reportable.getPendingSteps());
         undefinedSteps = ArrayUtils.add(undefinedSteps, reportable.getUndefinedSteps());
         totalSteps = ArrayUtils.add(totalSteps, reportable.getSteps());
 
         durations = ArrayUtils.add(durations, reportable.getDuration());
 
         // this should be removed later but for now correct features and save valid data
         applyPatchForFeatures();
         if (pendingSteps.length < buildNumbers.length) {
             fillMissingSteps();
         }
         if (durations.length < buildNumbers.length) {
             fillMissingDurations();
         }
     }
 
     /**
      * Removes elements that points to the oldest items.
      * Leave trends unchanged if the limit is bigger than current trends length.
      *
      * @param limit number of elements that will be leave
      */
     public void limitItems(int limit) {
         buildNumbers = copyLastElements(buildNumbers, limit);
 
         passedFeatures = copyLastElements(passedFeatures, limit);
         failedFeatures = copyLastElements(failedFeatures, limit);
         totalFeatures = copyLastElements(totalFeatures, limit);
 
         passedScenarios = copyLastElements(passedScenarios, limit);
         failedScenarios = copyLastElements(failedScenarios, limit);
         totalScenarios = copyLastElements(totalScenarios, limit);
 
         passedSteps = copyLastElements(passedSteps, limit);
         failedSteps = copyLastElements(failedSteps, limit);
         skippedSteps = copyLastElements(skippedSteps, limit);
         pendingSteps = copyLastElements(pendingSteps, limit);
         undefinedSteps = copyLastElements(undefinedSteps, limit);
         totalSteps = copyLastElements(totalSteps, limit);
 
         durations = copyLastElements(durations, limit);
     }
 
     private static int[] copyLastElements(int[] srcArray, int copyingLimit) {
         // if there is less elements than the limit then return array unchanged
         if (srcArray.length <= copyingLimit) {
             return srcArray;
         }
 
         int[] dest = new int[copyingLimit];
         System.arraycopy(srcArray, srcArray.length - copyingLimit, dest, 0, copyingLimit);
 
         return dest;
     }
 
     private static long[] copyLastElements(long[] srcArray, int copyingLimit) {
         // if there is less elements than the limit then return array unchanged
         if (srcArray.length <= copyingLimit) {
             return srcArray;
         }
 
         long[] dest = new long[copyingLimit];
         System.arraycopy(srcArray, srcArray.length - copyingLimit, dest, 0, copyingLimit);
 
         return dest;
     }
 
     private static String[] copyLastElements(String[] srcArray, int copyingLimit) {
         // if there is less elements than the limit then return array unchanged
         if (srcArray.length <= copyingLimit) {
             return srcArray;
         }
 
         String[] dest = new String[copyingLimit];
         System.arraycopy(srcArray, srcArray.length - copyingLimit, dest, 0, copyingLimit);
 
         return dest;
     }
 
 
/** Due to the error with the old implementation where the total functionality was passed instead of failures (and vice versa), the following fix should be applied for trends generated between version 3. */
 private void applyPatchForFeatures(){
        if (totalFeatures.length > 0 && totalFeatures[0] == 0) {
            totalFeatures[0] = failedFeatures[0];
            failedFeatures[0] = 0;
        }
    }
    
        private void fillMissingSteps() {
            for (int i = pendingSteps.length; i < buildNumbers.length; i++) {
                pendingSteps = ArrayUtils.add(pendingSteps, 0);
            }
        }
    
        private void fillMissingDurations() {
            for (int i = durations.length; i < buildNumbers.length; i++) {
                durations = ArrayUtils.add(durations, 0);
            }
        }
    
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Build numbers: ");
            for (String buildNumber : buildNumbers) {
                sb.append(buildNumber).append(", ");
            }
            sb.append("\n");
            sb.append("Passed features: ");
            for (int i = 0; i < passedFeatures.length; i++) {
                sb.append(passedFeatures[i]).append(", ");
            }
            sb.append("\n");
            sb.append("Failed features: ");
            for (int i = 0; i < failedFeatures.length; i++) {
                sb.append(failedFeatures[i]).append(", ");
            }
            sb.append("\n");
            sb.append("Total features: ");
            for (int i = 0; i < totalFeatures.length; i++) {
                sb.append(totalFeatures[i]).append(", ");
            }
            sb.append("\n");
            sb.append("Passed scenarios: ");
            for (int i = 0; i < passedScenarios.length; i++) {
                sb.append(passedScenarios[i]).append(", ");
            }
            sb.append("\n");
            sb.append("Failed scenarios     
 }

 

}