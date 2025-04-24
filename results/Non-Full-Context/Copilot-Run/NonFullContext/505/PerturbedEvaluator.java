/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2013 Adobe
  * %%
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  * #L%
  */
 package com.adobe.acs.commons.images.impl;
 
 import com.adobe.acs.commons.dam.RenditionPatternPicker;
 import com.adobe.acs.commons.images.ImageTransformer;
 import com.adobe.acs.commons.images.NamedImageTransformer;
 import com.adobe.acs.commons.util.PathInfoUtil;
 import com.day.cq.commons.DownloadResource;
 import com.day.cq.commons.jcr.JcrConstants;
 import com.day.cq.dam.api.Asset;
 import com.day.cq.dam.api.Rendition;
 import com.day.cq.dam.commons.util.DamUtil;
 import com.day.cq.wcm.api.NameConstants;
 import com.day.cq.wcm.api.Page;
 import com.day.cq.wcm.api.PageManager;
 import com.day.cq.wcm.foundation.Image;
 import com.day.image.Layer;
 
 import org.apache.commons.lang.ArrayUtils;
 import org.apache.commons.lang.StringUtils;
 import org.apache.felix.scr.annotations.Activate;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.Properties;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.ReferenceCardinality;
 import org.apache.felix.scr.annotations.ReferencePolicy;
 import org.apache.felix.scr.annotations.References;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.api.SlingHttpServletRequest;
 import org.apache.sling.api.SlingHttpServletResponse;
 import org.apache.sling.api.resource.Resource;
 import org.apache.sling.api.resource.ResourceResolver;
 import org.apache.sling.api.resource.ValueMap;
 import org.apache.sling.api.servlets.OptingServlet;
 import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
 import org.apache.sling.api.wrappers.ValueMapDecorator;
 import org.apache.sling.commons.mime.MimeTypeService;
 import org.apache.sling.commons.osgi.PropertiesUtil;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.imageio.ImageIO;
 import javax.jcr.RepositoryException;
 import javax.servlet.Servlet;
 import javax.servlet.ServletException;
 import javax.servlet.http.HttpServletResponse;
 
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 @SuppressWarnings("serial")
 @Component(
         label = "ACS AEM Commons - Named Transform Image Servlet",
         description = "Transform images programatically by applying a named transform to the requested Image.",
         metatype = true
 )
 @Properties({
         @Property(
                 label = "Resource Types",
                 description = "Resource Types and Node Types to bind this servlet to.",
                 name = "sling.servlet.resourceTypes",
                 value = { "nt/file", "nt/resource", "dam/Asset", "cq/Page", "cq/PageContent", "nt/unstructured",
                         "foundation/components/image", "foundation/components/parbase", "foundation/components/page" },
                 propertyPrivate = false
         ),
         @Property(
             label = "Allows Suffix Patterns",
             description = "Regex pattern to filter allowed file names. Defaults to [ "
                     + NamedTransformImageServlet.DEFAULT_FILENAME_PATTERN + " ]",
             name = NamedTransformImageServlet.NAMED_IMAGE_FILENAME_PATTERN,
             value = NamedTransformImageServlet.DEFAULT_FILENAME_PATTERN
         ),
         @Property(
                 label = "Extension",
                 description = "",
                 name = "sling.servlet.extensions",
                 value = { "transform" },
                 propertyPrivate = true
         ),
         @Property(
                 name = "sling.servlet.methods",
                 value = { "GET" },
                 propertyPrivate = true
         )
 })
 @References({
         @Reference(
                 name = "namedImageTransformers",
                 referenceInterface = NamedImageTransformer.class,
                 policy = ReferencePolicy.DYNAMIC,
                 cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
         ),
         @Reference(
                 name = "imageTransformers",
                 referenceInterface = ImageTransformer.class,
                 policy = ReferencePolicy.DYNAMIC,
                 cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
         )
 })
 @Service(Servlet.class)
 public class NamedTransformImageServlet extends SlingSafeMethodsServlet implements OptingServlet {
 
     private static final Logger log = LoggerFactory.getLogger(NamedTransformImageServlet.class);
 
     public static final String NAME_IMAGE = "image";
 
     public static final String NAMED_IMAGE_FILENAME_PATTERN = "acs.commons.namedimage.filename.pattern";
 
     public static final String DEFAULT_FILENAME_PATTERN = "(image|img)\\.(.+)";
 
     public static final String RT_LOCAL_SOCIAL_IMAGE = "social:asiFile";
 
     public static final String RT_REMOTE_SOCIAL_IMAGE = "nt:adobesocialtype";
 
     private static final ValueMap EMPTY_PARAMS = new ValueMapDecorator(new LinkedHashMap<String, Object>());
 
     private static final String MIME_TYPE_PNG = "image/png";
 
     private static final String TYPE_QUALITY = "quality";
 
     private static final String TYPE_PROGRESSIVE = "progressive";
 
     /* Asset Rendition Pattern Picker */
 
     private static final String DEFAULT_ASSET_RENDITION_PICKER_REGEX = "cq5dam\\.web\\.(.*)";
 
     @Property(label = "Asset Rendition Picker Regex",
             description = "Regex to select the Rendition to transform when directly transforming a DAM Asset."
                     + " [ Default: cq5dam.web.(.*) ]",
             value = DEFAULT_ASSET_RENDITION_PICKER_REGEX)
     private static final String PROP_ASSET_RENDITION_PICKER_REGEX = "prop.asset-rendition-picker-regex";
 
     private final transient Map<String, NamedImageTransformer> namedImageTransformers =
             new ConcurrentHashMap<String, NamedImageTransformer>();
 
     private final transient Map<String, ImageTransformer> imageTransformers = new ConcurrentHashMap<String, ImageTransformer>();
 
     @Reference
     private transient MimeTypeService mimeTypeService;
 
     private Pattern lastSuffixPattern = Pattern.compile(DEFAULT_FILENAME_PATTERN);
 
     private transient RenditionPatternPicker renditionPatternPicker =
             new RenditionPatternPicker(Pattern.compile(DEFAULT_ASSET_RENDITION_PICKER_REGEX));
 
     /**
      * Only accept requests that.
      * - Are not null
      * - Have a suffix
      * - Whose first suffix segment is a registered transform name
      * - Whose last suffix matches the image file name pattern
      *
      * @param request SlingRequest object
      * @return true if the Servlet should handle the request
      */
     @Override
     public final boolean accepts(final SlingHttpServletRequest request) {
         if (request == null) {
             return false;
         }
 
         final String suffix = request.getRequestPathInfo().getSuffix();
         if (StringUtils.isBlank(suffix)) {
             return false;
         }
 
         final String transformName = PathInfoUtil.getFirstSuffixSegment(request);
         if (!this.namedImageTransformers.keySet().contains(transformName)) {
             return false;
         }
 
         final String lastSuffix = PathInfoUtil.getLastSuffixSegment(request);
         final Matcher matcher = lastSuffixPattern.matcher(lastSuffix);
         if (!matcher.matches()) {
             return false;
         }
 
         return true;
     }
 
     @Override
     protected final void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws
             ServletException, IOException {
         // Get the transform names from the suffix
         final List<NamedImageTransformer> selectedNamedImageTransformers = getNamedImageTransformers(request);
 
         // Collect and combine the image transformers and their params
         final ValueMap imageTransformersWithParams = getImageTransformersWithParams(selectedNamedImageTransformers);
 
         final Image image = resolveImage(request);
         final String mimeType = getMimeType(request, image);
         Layer layer = getLayer(image);
         
         if (layer == null) {
             response.setStatus(HttpServletResponse.SC_NOT_FOUND);
             return;
         }
         
         // Transform the image
         layer = this.transform(layer, imageTransformersWithParams);
 
         // Get the quality
         final double quality = this.getQuality(mimeType,
                 imageTransformersWithParams.get(TYPE_QUALITY, EMPTY_PARAMS));
 
         // Check if the image is a JPEG which has to be encoded progressively
         final boolean progressiveJpeg = isProgressiveJpeg(mimeType,
                 imageTransformersWithParams.get(TYPE_PROGRESSIVE, EMPTY_PARAMS));
 
         response.setContentType(mimeType);
 
         if (progressiveJpeg) {
             ProgressiveJpeg.write(layer, quality, response.getOutputStream());
         } else {
             layer.write(mimeType, quality, response.getOutputStream());
         }
 
         response.flushBuffer();
     }
 
 
/** Creates a new ImageTransformer object and run the imageTransformer operation. */
 protected final Layer transform(Layer layer, final ValueMap imageTransformersWithParams){}

 

}