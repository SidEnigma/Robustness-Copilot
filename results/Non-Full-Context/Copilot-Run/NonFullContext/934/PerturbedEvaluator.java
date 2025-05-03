package edu.harvard.iq.dataverse;
 
 import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
 import edu.harvard.iq.dataverse.dataset.DatasetUtil;
 import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
 import edu.harvard.iq.dataverse.makedatacount.DatasetExternalCitations;
 import edu.harvard.iq.dataverse.makedatacount.DatasetMetrics;
 import java.nio.file.Path;
 import java.nio.file.Paths;
 import java.sql.Timestamp;
 import java.text.SimpleDateFormat;
 import java.util.ArrayList;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Objects;
 import java.util.Set;
 import javax.persistence.CascadeType;
 import javax.persistence.Entity;
 import javax.persistence.Index;
 import javax.persistence.JoinColumn;
 import javax.persistence.ManyToOne;
 import javax.persistence.NamedQueries;
 import javax.persistence.NamedQuery;
 import javax.persistence.NamedStoredProcedureQuery;
 import javax.persistence.OneToMany;
 import javax.persistence.OneToOne;
 import javax.persistence.OrderBy;
 import javax.persistence.ParameterMode;
 import javax.persistence.StoredProcedureParameter;
 import javax.persistence.Table;
 import javax.persistence.Temporal;
 import javax.persistence.TemporalType;
 import edu.harvard.iq.dataverse.util.BundleUtil;
 import edu.harvard.iq.dataverse.util.StringUtil;
 
 /**
  *
  * @author skraffmiller
  */
 @NamedQueries({
     @NamedQuery(name = "Dataset.findIdStale",
                query = "SELECT d.id FROM Dataset d WHERE d.indexTime is NULL OR d.indexTime < d.modificationTime"),
     @NamedQuery(name = "Dataset.findIdStalePermission",
                query = "SELECT d.id FROM Dataset d WHERE d.permissionIndexTime is NULL OR d.permissionIndexTime < d.permissionModificationTime"),
     @NamedQuery(name = "Dataset.findByIdentifier",
                query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier"),
     @NamedQuery(name = "Dataset.findByIdentifierAuthorityProtocol",
                query = "SELECT d FROM Dataset d WHERE d.identifier=:identifier AND d.protocol=:protocol AND d.authority=:authority"),
     @NamedQuery(name = "Dataset.findIdentifierByOwnerId", 
                 query = "SELECT o.identifier FROM Dataset o WHERE o.owner.id=:ownerId"),
     @NamedQuery(name = "Dataset.findIdByOwnerId", 
                 query = "SELECT o.id FROM Dataset o WHERE o.owner.id=:ownerId"),
     @NamedQuery(name = "Dataset.findByOwnerId", 
                 query = "SELECT o FROM Dataset o WHERE o.owner.id=:ownerId"),
     @NamedQuery(name = "Dataset.findByCreatorId",
                 query = "SELECT o FROM Dataset o WHERE o.creator.id=:creatorId"),
     @NamedQuery(name = "Dataset.findByReleaseUserId",
                 query = "SELECT o FROM Dataset o WHERE o.releaseUser.id=:releaseUserId"),
 })
 
 /*
     Below is the database stored procedure for getting a string dataset id.
     Used when the Dataverse is (optionally) configured to use
     procedurally generated values for dataset ids, instead of the default
     random strings. 
 
     The use of a stored procedure to create an identifier is explained in the
     installation documentation (where an example script is supplied).
     The stored procedure can be implemented using other SQL flavors -
     without having to modify the application code. 
             -- L.A. 4.6.2 (modified by C.S. for version 5.4.1+)
 */ 
 @NamedStoredProcedureQuery(
         name = "Dataset.generateIdentifierFromStoredProcedure",
         procedureName = "generateIdentifierFromStoredProcedure",
         parameters = {
             @StoredProcedureParameter(mode = ParameterMode.OUT, type = String.class)
         }
 )
 @Entity
 @Table(indexes = {
     @Index(columnList = "guestbook_id"),
     @Index(columnList = "thumbnailfile_id")})
 public class Dataset extends DvObjectContainer {
 
     public static final String TARGET_URL = "/citation?persistentId=";
     private static final long serialVersionUID = 1L;
 
     @OneToMany(mappedBy = "owner", cascade = CascadeType.MERGE)
     @OrderBy("id")
     private List<DataFile> files = new ArrayList<>();
 
     @Temporal(value = TemporalType.TIMESTAMP)
     private Date lastExportTime;
 
     
     @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     @OrderBy("versionNumber DESC, minorVersionNumber DESC")
     private List<DatasetVersion> versions = new ArrayList<>();
 
     @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
     private Set<DatasetLock> datasetLocks;
     
     @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
     @JoinColumn(name = "thumbnailfile_id")
     private DataFile thumbnailFile;
     
     @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetMetrics> datasetMetrics = new ArrayList<>(); 
     
     @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetExternalCitations> datasetExternalCitations = new ArrayList<>(); 
     
     /**
      * By default, Dataverse will attempt to show unique thumbnails for datasets
      * based on images that have been uploaded to them. Setting this to true
      * will result in a generic dataset thumbnail appearing instead.
      */
     private boolean useGenericThumbnail;
 
     @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
     @JoinColumn(name = "guestbook_id", unique = false, nullable = true, insertable = true, updatable = true)
     private Guestbook guestbook;
     
     @OneToMany(mappedBy="dataset", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DatasetLinkingDataverse> datasetLinkingDataverses;
 
     public List<DatasetLinkingDataverse> getDatasetLinkingDataverses() {
         return datasetLinkingDataverses;
     }
 
     public void setDatasetLinkingDataverses(List<DatasetLinkingDataverse> datasetLinkingDataverses) {
         this.datasetLinkingDataverses = datasetLinkingDataverses;
     }
 
     private boolean fileAccessRequest;
     @OneToMany(mappedBy = "dataset", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
     private List<DataFileCategory> dataFileCategories = null;
     
     @ManyToOne
     @JoinColumn(name = "citationDateDatasetFieldType_id")
     private DatasetFieldType citationDateDatasetFieldType;
     
     public DatasetFieldType getCitationDateDatasetFieldType() {
         return citationDateDatasetFieldType;
     }
 
     public void setCitationDateDatasetFieldType(DatasetFieldType citationDateDatasetFieldType) {
         this.citationDateDatasetFieldType = citationDateDatasetFieldType;
     }    
 
     public Dataset() {
         DatasetVersion datasetVersion = new DatasetVersion();
         datasetVersion.setDataset(this);
         datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
         datasetVersion.setFileMetadatas(new ArrayList<>());
         datasetVersion.setVersionNumber((long) 1);
         datasetVersion.setMinorVersionNumber((long) 0);
         versions.add(datasetVersion);
     }
     
 
/** Determines if this dataset is locked for the specified reason. */
 public boolean isLockedFor(DatasetLock.Reason reason){}

 

}