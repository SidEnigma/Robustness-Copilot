package edu.harvard.iq.dataverse;
 
 import com.google.gson.Gson;
 import com.google.gson.GsonBuilder;
 import com.google.gson.JsonArray;
 import com.google.gson.JsonElement;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonPrimitive;
 import com.google.gson.annotations.Expose;
 import com.google.gson.annotations.SerializedName;
 import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
 import java.io.Serializable;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import javax.json.Json;
 import javax.json.JsonArrayBuilder;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.CascadeType;
 import javax.persistence.Id;
 import javax.persistence.Index;
 import javax.persistence.JoinColumn;
 import javax.persistence.JoinTable;
 import javax.persistence.ManyToMany;
 import javax.persistence.ManyToOne;
 import javax.persistence.OneToMany;
 import javax.persistence.OrderBy;
 import javax.persistence.Table;
 import javax.persistence.Transient;
 import javax.persistence.Version;
 
 import edu.harvard.iq.dataverse.datavariable.CategoryMetadata;
 import edu.harvard.iq.dataverse.datavariable.DataVariable;
 import edu.harvard.iq.dataverse.datavariable.VarGroup;
 import edu.harvard.iq.dataverse.datavariable.VariableMetadata;
 import edu.harvard.iq.dataverse.util.DateUtil;
 import edu.harvard.iq.dataverse.util.StringUtil;
 import java.util.HashSet;
 import java.util.Set;
 import javax.validation.ConstraintViolation;
 import javax.validation.Validation;
 import javax.validation.Validator;
 import javax.validation.ValidatorFactory;
 import org.hibernate.validator.constraints.NotBlank;
 import javax.validation.constraints.Pattern;
 
 
 /**
  *
  * @author skraffmiller
  */
 @Table(indexes = {@Index(columnList="datafile_id"), @Index(columnList="datasetversion_id")} )
 @Entity
 public class FileMetadata implements Serializable {
     private static final long serialVersionUID = 1L;
     private static final Logger logger = Logger.getLogger(FileMetadata.class.getCanonicalName());
 
 
     @Expose
     @Pattern(regexp="^[^:<>;#/\"\\*\\|\\?\\\\]*$", 
             message = "{filename.illegalCharacters}")
     @NotBlank(message = "{filename.blank}")
     @Column( nullable=false )
     private String label = "";
     
     
     @ValidateDataFileDirectoryName(message = "{directoryname.illegalCharacters}")
     @Expose
     @Column ( nullable=true )
 
     private String directoryLabel;
     @Expose
     @Column(columnDefinition = "TEXT")
     private String description = "";
     
     /**
      * At the FileMetadata level, "restricted" is a historical indication of the
      * data owner's intent for the file by version. Permissions are actually
      * enforced based on the "restricted" boolean at the *DataFile* level. On
      * publish, the latest intent is copied from the FileMetadata level to the
      * DataFile level.
      */
     @Expose
     private boolean restricted;
 
     @ManyToOne
     @JoinColumn(nullable=false)
     private DatasetVersion datasetVersion;
     
     @ManyToOne
     @JoinColumn(nullable=false)
     private DataFile dataFile;
 
     /**
      * There are two types of provenance types and this "free-form" type is
      * represented in the GUI as text box the user can type into. The other type
      * is based on PROV-JSON from the W3C.
      */
     @Expose
     @Column(columnDefinition = "TEXT", nullable = true, name="prov_freeform")
     private String provFreeForm;
 
     @OneToMany (mappedBy="fileMetadata", cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST})
     private Collection<VariableMetadata> variableMetadatas;
         
 
/** Creates a copy of {@code this}, with identical business logic fields. */
 public FileMetadata createCopy(){
        FileMetadata copy = new FileMetadata();
        copy.setLabel(this.getLabel());
        copy.setDescription(this.getDescription());
        copy.setRestricted(this.isRestricted());
        copy.setDatasetVersion(this.getDatasetVersion());
        copy.setDataFile(this.getDataFile());
        copy.setProvFreeForm(this.getProvFreeForm());
        copy.setVariableMetadatas(this.getVariableMetadatas());
        return copy;
         }
         
        public FileMetadata() {
            variableMetadatas = new ArrayList<>();
        }
         
        public FileMetadata(String label, String description, boolean restricted, DatasetVersion datasetVersion, DataFile dataFile) {
            this();
            this.label = label;
            this.description = description;
            this.restricted = restricted;
            this.datasetVersion = datasetVersion;
            this.dataFile = dataFile;
        }
         
        public FileMetadata(String label, String description, boolean restricted, DatasetVersion datasetVersion, DataFile dataFile, String provFreeForm) {
            this(label, description, restricted, datasetVersion, dataFile);
            this.provFreeForm = provFreeForm;
        }
         
        public FileMetadata(String label, String description, boolean restricted, DatasetVersion datasetVersion, DataFile dataFile, String provFreeForm, Collection<VariableMetadata> variableMetadatas) {
            this(label, description, restricted, datasetVersion, dataFile, provFreeForm);
            this.variableMetadatas = variableMetadatas;
        }
         
        public String getLabel() {
            return label;
        }
         
        public void setLabel(String label) {
            this.label = label;
        }
         
        public String getDescription() {
            return description;
        }
         
        public void setDescription(String description) {
            this.description = description;
        }
         
        public boolean isRestricted() {
            return restricted;
        }
         
        public void set                 
 }

 

}