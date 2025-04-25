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
 
 import com.day.image.Layer;
 
 import javax.imageio.IIOImage;
 import javax.imageio.ImageIO;
 import javax.imageio.ImageWriteParam;
 import javax.imageio.ImageWriter;
 import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
 import javax.imageio.stream.ImageOutputStream;
 import java.awt.image.BufferedImage;
 import java.awt.image.ColorConvertOp;
 import java.io.IOException;
 import java.io.OutputStream;
 
 /**
  * Extension for {@link Layer} with progressive JPEG support.
  */
 public class ProgressiveJpeg {
 
     private ProgressiveJpeg() {
     }
 
 
/** Writes a JPEG image using a progressive encoding. */
 public static void write(Layer layer, double quality, OutputStream out) throws IOException{
      BufferedImage image = layer.getImage();
      if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
          image = new ColorConvertOp(null).filter(image, null);
      }
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam param = new JPEGImageWriteParam(null);
      param.setProgressiveMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality((float) quality);
      writer.setOutput(ImageIO.createImageOutputStream(out));
      writer.write(null, new IIOImage(image, null, null), param);
      writer.dispose();
  }
  } 
 }

 

}