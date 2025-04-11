package net.masterthought.cucumber.json;
 
 import com.fasterxml.jackson.annotation.JsonProperty;
 import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
 import org.apache.commons.lang.StringUtils;
 
 import net.masterthought.cucumber.json.deserializers.OutputsDeserializer;
 import net.masterthought.cucumber.json.support.Resultsable;
 
 public class Hook implements Resultsable {
 
     // Start: attributes from JSON file report
     private final Result result = null;
     private final Match match = null;
 
     @JsonDeserialize(using = OutputsDeserializer.class)
     @JsonProperty("output")
     private final Output[] outputs = new Output[0];
 
     // foe Ruby reports
     private final Embedding[] embeddings = new Embedding[0];
     // End: attributes from JSON file report
 
     @Override
     public Result getResult() {
         return result;
     }
 
     @Override
     public Match getMatch() {
         return match;
     }
 
     @Override
     public Output[] getOutputs() {
         return outputs;
     }
 
     public Embedding[] getEmbeddings() {
         return embeddings;
     }
 
 
/** If the hook has at least an attachment or result with an error message, it's checked. */
 public boolean hasContent(){
        if(getOutputs().length > 0){
            return true;
        }
        if(getEmbeddings().length > 0){
            return true;
        }
        if(getResult() != null && StringUtils.isNotEmpty(getResult().getErrorMessage())){
            return true;
        }
        return false;
    }
            
 }

 

}