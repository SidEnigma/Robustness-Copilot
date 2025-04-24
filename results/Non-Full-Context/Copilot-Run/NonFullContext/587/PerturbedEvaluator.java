/*
  * Copyright 2010 ZXing authors
  *
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
  */
 
 package com.google.zxing.aztec.detector;
 
 import com.google.zxing.NotFoundException;
 import com.google.zxing.ResultPoint;
 import com.google.zxing.aztec.AztecDetectorResult;
 import com.google.zxing.common.BitMatrix;
 import com.google.zxing.common.GridSampler;
 import com.google.zxing.common.detector.MathUtils;
 import com.google.zxing.common.detector.WhiteRectangleDetector;
 import com.google.zxing.common.reedsolomon.GenericGF;
 import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
 import com.google.zxing.common.reedsolomon.ReedSolomonException;
 
 /**
  * Encapsulates logic that can detect an Aztec Code in an image, even if the Aztec Code
  * is rotated or skewed, or partially obscured.
  *
  * @author David Olivier
  * @author Frank Yellin
  */
 public final class Detector {
 
   private static final int[] EXPECTED_CORNER_BITS = {
       0xee0,  // 07340  XXX .XX X.. ...
       0x1dc,  // 00734  ... XXX .XX X..
       0x83b,  // 04073  X.. ... XXX .XX
       0x707,  // 03407 .XX X.. ... XXX
   };
 
   private final BitMatrix image;
 
   private boolean compact;
   private int nbLayers;
   private int nbDataBlocks;
   private int nbCenterLayers;
   private int shift;
 
   public Detector(BitMatrix image) {
     this.image = image;
   }
 
   public AztecDetectorResult detect() throws NotFoundException {
     return detect(false);
   }
 
   /**
    * Detects an Aztec Code in an image.
    *
    * @param isMirror if true, image is a mirror-image of original
    * @return {@link AztecDetectorResult} encapsulating results of detecting an Aztec Code
    * @throws NotFoundException if no Aztec Code can be found
    */
    public AztecDetectorResult detect(boolean isMirror) throws NotFoundException {
 
     // 1. Get the center of the aztec matrix
     Point pCenter = getMatrixCenter();
 
     // 2. Get the center points of the four diagonal points just outside the bull's eye
     //  [topRight, bottomRight, bottomLeft, topLeft]
     ResultPoint[] bullsEyeCorners = getBullsEyeCorners(pCenter);
 
     if (isMirror) {
       ResultPoint temp = bullsEyeCorners[0];
       bullsEyeCorners[0] = bullsEyeCorners[2];
       bullsEyeCorners[2] = temp;
     }
 
     // 3. Get the size of the matrix and other parameters from the bull's eye
     extractParameters(bullsEyeCorners);
 
     // 4. Sample the grid
     BitMatrix bits = sampleGrid(image,
                                 bullsEyeCorners[shift % 4],
                                 bullsEyeCorners[(shift + 1) % 4],
                                 bullsEyeCorners[(shift + 2) % 4],
                                 bullsEyeCorners[(shift + 3) % 4]);
 
     // 5. Get the corners of the matrix.
     ResultPoint[] corners = getMatrixCornerPoints(bullsEyeCorners);
 
     return new AztecDetectorResult(bits, corners, compact, nbDataBlocks, nbLayers);
   }
 
 
/** It Retrieves the number of data layers and blocks from the layer featuring the bull's eye. */
 private void extractParameters(ResultPoint[] bullsEyeCorners) throws NotFoundException{}

 

}