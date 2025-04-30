package edu.harvard.iq.dataverse;
 
 import edu.harvard.iq.dataverse.util.MarkupChecker;
 import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
 import edu.harvard.iq.dataverse.branding.BrandingUtil;
 import edu.harvard.iq.dataverse.util.FileUtil;
 import edu.harvard.iq.dataverse.util.StringUtil;
 import edu.harvard.iq.dataverse.util.SystemConfig;
 import edu.harvard.iq.dataverse.util.DateUtil;
 import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
 import edu.harvard.iq.dataverse.workflows.WorkflowComment;
 import java.io.Serializable;
 import java.net.URL;
 import java.sql.Timestamp;
 import java.text.DateFormat;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.*;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.stream.Collectors;
 
 import javax.json.Json;
 import javax.json.JsonArray;
 import javax.json.JsonArrayBuilder;
 import javax.json.JsonObjectBuilder;
 import javax.persistence.CascadeType;
 import javax.persistence.Column;
 import javax.persistence.Entity;
 import javax.persistence.EnumType;
 import javax.persistence.Enumerated;
 import javax.persistence.GeneratedValue;
 import javax.persistence.GenerationType;
 import javax.persistence.Id;
 import javax.persistence.Index;
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import javax.persistence.OneToMany;
 import javax.persistence.OneToOne;
 import javax.persistence.OrderBy;
 import javax.persistence.Table;
 import javax.persistence.Temporal;
 import javax.persistence.TemporalType;
 import javax.persistence.Transient;
 import javax.persistence.UniqueConstraint;
 import javax.persistence.Version;
 import javax.validation.ConstraintViolation;
 import javax.validation.Validation;
 import javax.validation.Validator;
 import javax.validation.ValidatorFactory;
 import javax.validation.constraints.Size;
 import org.apache.commons.lang3.StringUtils;
 
 /**
  *
  * @author skraffmiller
  */
 @Entity
 @Table(indexes = {@Index(columnList="dataset_id")},
         uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id,versionnumber,minorversionnumber"}))
 @ValidateVersionNote(versionNote = "versionNote", versionState = "versionState")
 public class DatasetVersion implements Serializable {
 
     private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());
 
     /**
      * Convenience comparator to compare dataset versions by their version number.
      * The draft version is considered the latest.
      */
     public static final Comparator<DatasetVersion> compareByVersion = new Comparator<DatasetVersion>() {
         @Override
         public int compare(DatasetVersion o1, DatasetVersion o2) {
             if ( o1.isDraft() ) {
                 return o2.isDraft() ? 0 : 1;
             } else {
                return (int)Math.signum( (o1.getVersionNumber().equals(o2.getVersionNumber())) ?
                         o1.getMinorVersionNumber() - o2.getMinorVersionNumber()
                        : o1.getVersionNumber() - o2.getVersionNumber() );
             }
         }
     };
 
     // TODO: Determine the UI implications of various version states
     //IMPORTANT: If you add a new value to this enum, you will also have to modify the
     // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
     public enum VersionState {
         DRAFT, RELEASED, ARCHIVED, DEACCESSIONED
     };
 
     public enum License {
         NONE, CC0
     }
 
     public static final int ARCHIVE_NOTE_MAX_LENGTH = 1000;
     public static final int VERSION_NOTE_MAX_LENGTH = 1000;
     
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     
     private String UNF;
 
     @Version
     private Long version;
 
     private Long versionNumber;
     private Long minorVersionNumber;
     
     @Size(min=0, max=VERSION_NOTE_MAX_LENGTH)
     @Column(length = VERSION_NOTE_MAX_LENGTH)
     private String versionNote;
     
     /*
      * @todo versionState should never be null so when we are ready, uncomment
      * the `nullable = false` below.
      */
 //    @Column(nullable = false)
     @Enumerated(EnumType.STRING)
     private VersionState versionState;
 
     @ManyToOne
     private Dataset dataset;
 
     @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     @OrderBy("label") // this is not our preferred ordering, which is with the AlphaNumericComparator, but does allow the files to be grouped by category
     private List<FileMetadata> fileMetadatas = new ArrayList();
     
     @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval=true)
     @JoinColumn(name = "termsOfUseAndAccess_id")
     private TermsOfUseAndAccess termsOfUseAndAccess;
     
     @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetField> datasetFields = new ArrayList();
     
     @Temporal(value = TemporalType.TIMESTAMP)
     @Column( nullable=false )
     private Date createTime;
     
     @Temporal(value = TemporalType.TIMESTAMP)
     @Column( nullable=false )
     private Date lastUpdateTime;
     
     @Temporal(value = TemporalType.TIMESTAMP)
     private Date releaseTime;
     
     @Temporal(value = TemporalType.TIMESTAMP)
     private Date archiveTime;
     
     @Size(min=0, max=ARCHIVE_NOTE_MAX_LENGTH)
     @Column(length = ARCHIVE_NOTE_MAX_LENGTH)
     //@ValidateURL() - this validation rule was making a bunch of older legacy datasets invalid;
     // removed pending further investigation (v4.13)
     private String archiveNote;
     
     @Column(nullable=true, columnDefinition = "TEXT")
     private String archivalCopyLocation;
     
     
     private String deaccessionLink;
 
     @Transient
     private String contributorNames;
     
     @Transient 
     private String jsonLd;
 
     @OneToMany(mappedBy="datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetVersionUser> datasetVersionUsers;
     
     // Is this the right mapping and cascading for when the workflowcomments table is being used for objects other than DatasetVersion?
     @OneToMany(mappedBy = "datasetVersion", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<WorkflowComment> workflowComments;
 
     @Column(nullable=true)
     private String externalStatusLabel;
     
     @Transient
     private DatasetVersionDifference dvd;
     
     
     public Long getId() {
         return this.id;
     }
 
     public void setId(Long id) {
         this.id = id;
     }
 
     public String getUNF() {
         return UNF;
     }
 
     public void setUNF(String UNF) {
         this.UNF = UNF;
     }
 
     /**
      * This is JPA's optimistic locking mechanism, and has no semantic meaning in the DV object model.
      * @return the object db version
      */
     public Long getVersion() {
         return this.version;
     }
 
     public void setVersion(Long version) {
     }
     
     public List<FileMetadata> getFileMetadatas() {
         return fileMetadatas;
     }
     
     public List<FileMetadata> getFileMetadatasSorted() {
         Collections.sort(fileMetadatas, FileMetadata.compareByLabel);
         return fileMetadatas;
     }
     
     public List<FileMetadata> getFileMetadatasSortedByLabelAndFolder() {
         ArrayList<FileMetadata> fileMetadatasCopy = new ArrayList<>();
         fileMetadatasCopy.addAll(fileMetadatas);
         Collections.sort(fileMetadatasCopy, FileMetadata.compareByLabelAndFolder);
         return fileMetadatasCopy;
     }
     
     public List<FileMetadata> getFileMetadatasFolderListing(String folderName) {
         ArrayList<FileMetadata> fileMetadatasCopy = new ArrayList<>();
         HashSet<String> subFolders = new HashSet<>();
 
         for (FileMetadata fileMetadata : fileMetadatas) {
             String thisFolder = fileMetadata.getDirectoryLabel() == null ? "" : fileMetadata.getDirectoryLabel(); 
             
             if (folderName.equals(thisFolder)) {
                 fileMetadatasCopy.add(fileMetadata);
             } else if (thisFolder.startsWith(folderName)) {
                 String subFolder = "".equals(folderName) ? thisFolder : thisFolder.substring(folderName.length() + 1);
                 if (subFolder.indexOf('/') > 0) {
                     subFolder = subFolder.substring(0, subFolder.indexOf('/'));
                 }
                 
                 if (!subFolders.contains(subFolder)) {
                     fileMetadatasCopy.add(fileMetadata);
                     subFolders.add(subFolder);
                 }
                 
             }
         }
         Collections.sort(fileMetadatasCopy, FileMetadata.compareByFullPath);
                 
         return fileMetadatasCopy; 
     }
 
     public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
         this.fileMetadatas = fileMetadatas;
     }
     
     public TermsOfUseAndAccess getTermsOfUseAndAccess() {
         return termsOfUseAndAccess;
     }
 
     public void setTermsOfUseAndAccess(TermsOfUseAndAccess termsOfUseAndAccess) {
         this.termsOfUseAndAccess = termsOfUseAndAccess;
     }
 
     public List<DatasetField> getDatasetFields() {
         return datasetFields;
     }
 
     /**
      * Sets the dataset fields for this version. Also updates the fields to 
      * have @{code this} as their dataset version.
      * @param datasetFields
      */
     public void setDatasetFields(List<DatasetField> datasetFields) {
         for ( DatasetField dsf : datasetFields ) {
             dsf.setDatasetVersion(this);
         }
         this.datasetFields = datasetFields;
     }
     
 
/** When a dataset is in draft is the only time it can be reviewed. */
 public boolean isInReview(){}

 

}