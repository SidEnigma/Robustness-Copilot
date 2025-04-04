/* SPDX-License-Identifier: Apache-2.0 */
 /* Copyright Contributors to the ODPi Egeria project. */
 package org.odpi.openmetadata.accessservices.digitalservice.properties;
 
 import com.fasterxml.jackson.annotation.JsonAutoDetect;
 import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
 import com.fasterxml.jackson.annotation.JsonInclude;
 import com.fasterxml.jackson.annotation.JsonTypeInfo;
 import com.fasterxml.jackson.databind.ObjectMapper;
 
 import java.io.Serializable;
 import java.util.*;
 
 import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
 import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
 
 /**
  * DigitalService is an anchor object for tracking the lifecycle of one of an organization's digital service.
  * The digital service instance is create when the digital service is just a concept.  It is used to record
  * the role and implementation style that it has along with information about how it will operate.
  * As the digital service moved through its lifecycle from implementation to deployment to use, more
  * information is attached to the digital service instance to support the correct management and compliance
  * of the service.
  */
 
 public class DigitalService extends DigitalServiceElementHeader
 {
     private static final long    serialVersionUID = 1L;
 
     private String                            displayName            = null;
     private String                            description            = null;
     private String                            version                = null;
     /**
      * Default constructor
      */
     public DigitalService()
     {
     }
 
 
     /**
      * Copy/clone constructor
      *
      * @param template object to copy
      */
     public DigitalService(DigitalService template)
     {
 
         if (template != null)
         {
             this.displayName = template.getDisplayName();
             this.description = template.getDescription();
             this.version = template.getVersion();
         }
     }
 
 
     /**
      * Return the version number for this Asset's type.
      *
      * @return String
      */
     public String getVersion()
     {
         return version;
     }
 
 
     /**
      * Set up the version number for this Asset's type.
      *
      * @param version String
      */
     public void setVersion(String version)
     {
         this.version = version;
     }
 
 
     /**
      * Return the display name for this asset (normally a shortened for of the qualified name).
      *
      * @return string name
      */
     public String getDisplayName()
     {
         return displayName;
     }
 
 
     /**
      * Set up the display name for this asset (normally a shortened for of the qualified name).
      *
      * @param displayName string name
      */
     public void setDisplayName(String displayName)
     {
         this.displayName = displayName;
     }
 
 
     /**
      * Return the description for this asset.
      *
      * @return string description
      */
     public String getDescription()
     {
         return description;
     }
 
 
     /**
      * Set up the description for this asset.
      *
      * @param description string
      */
     public void setDescription(String description)
     {
         this.description = description;
     }
 
 
 
     /**
      * {@inheritDoc}
      *
      * JSON-style toString
      */
     @Override
     public String toString()
     {
         return "DigitalService{" +
                 "name='" + displayName + '\'' +
                 ", description='" + description + '\'' +
                 ", version='" + version + '\'' +
                 '}';
     }
 
 
 
/** Based on the content of the properties, the result of the comparison is returned */
 public boolean equals(Object objectToCompare){
        if (objectToCompare == null)
        {
            return false;
        }
    
        if (objectToCompare instanceof DigitalService)
        {
            DigitalService digitalService = (DigitalService) objectToCompare;
    
            if (this.getDisplayName() != null && digitalService.getDisplayName() != null)
            {
                if (!this.getDisplayName().equals(digitalService.getDisplayName()))
                {
                    return false;
                }
            }
            else
            {
                if (this.getDisplayName() != null || digitalService.getDisplayName() != null)
                {
                    return false;
                }
            }
    
            if (this.getDescription() != null && digitalService.getDescription() != null)
            {
                if (!this.getDescription().equals(digitalService.getDescription()))
                {
                    return false;
                }
            }
            else
            {
                if (this.getDescription() != null || digitalService.getDescription() != null)
                {
                    return false;
                }
            }
    
            if (this.getVersion() != null && digitalService.getVersion() != null)
            {
                if (!this.getVersion().equals(digitalService.getVersion()))
                {
                    return false;
                }
            }
            else
            {
                if (this.getVersion() != null || digitalService.getVersion() != null)
                {
                    return false;
                }
            }
    
            return true;
        }
    
        return false;
    }
    
    
        /**
        * {@inheritDoc}
        *
        * Hash code for this object.
        */
        @Override
        public int hashCode()
        {
            int result = 17;
    
            if (this.getDisplayName() != null)
            {
                result = result + this.getDisplayName().hashCode();
            }
    
            if (this.getDescription() != null)
            {
                result = result + this.getDescription().hashCode();     
 }

 

}