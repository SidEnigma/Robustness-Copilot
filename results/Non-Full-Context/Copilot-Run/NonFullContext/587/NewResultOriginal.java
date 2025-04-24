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
 
 
/** Extracts the number of data layers and data blocks from the layer around the bull's eye. */

private void extractParameters(ResultPoint[] bullsEyeCorners) throws NotFoundException {
    int dimension = 2 * nbCenterLayers;
    int[] sides = new int[]{dimension, dimension + 1, dimension + 2, dimension + 3};
    int[] transitions = new int[sides.length];
    
    for (int i = 0; i < sides.length; i++) {
        int side = sides[i];
        transitions[i] = MathUtils.round(bullsEyeCorners[i].getX());
    }
    
    int[] sidesWithTransitions = new int[sides.length];
    int[] transitionsBetweenLayers = new int[sides.length];
    
    for (int i = 0; i < sides.length; i++) {
        int side = sides[i];
        int transition = transitions[i];
        int transitionsCount = 0;
        int increment = 1;
        
        if (transition < dimension) {
            increment = -1;
        }
        
        boolean inBlack = image.get(transition, transition);
        
        for (; transition >= 0 && transition < image.getWidth(); transition += increment) {
            if (image.get(transition, side) != inBlack) {
                transitionsCount++;
                
                if (transitionsCount == 5) {
                    break;
                }
                
                inBlack = !inBlack;
            }
        }
        
        if (transition < 0 || transition >= image.getWidth()) {
            throw NotFoundException.getNotFoundInstance();
        }
        
        sidesWithTransitions[i] = transition;
        transitionsBetweenLayers[i] = transitionsCount;
    }
    
    int[] transitionsBetweenCenters = transitionsBetweenLayers.clone();
    Arrays.sort(transitionsBetweenCenters);
    int median = transitionsBetweenCenters[sides.length / 2];
    
    int shift = 0;
    
    for (int i = 0; i < sides.length; i++) {
        int transitionsCount = transitionsBetweenLayers[i];
        
        if (transitionsCount > median) {
            shift |= 1 << (sides.length - 1 - i);
        }
    }
    
    int[] correction = new int[sides.length];
    
    for (int i = 0; i < sides.length; i++) {
        int transitionsCount = transitionsBetweenLayers[i];
        int correctionValue = 0;
        
        if (transitionsCount > median) {
            correctionValue = transitionsCount - median;
        } else {
            correctionValue = median - transitionsCount;
        }
        
        correction[i] = correctionValue;
    }
    
    int[] eccBits = new int[sides.length];
    
    for (int i = 0; i < sides.length; i++) {
        int side = sides[i];
        int transitionsCount = transitionsBetweenLayers[i];
        int correctionValue = correction[i];
        int eccBitsValue = 0;
        
        if (transitionsCount > median) {
            eccBitsValue = (transitionsCount - median) / 2;
        } else {
            eccBitsValue = (median - transitionsCount) / 2;
        }
        
        eccBits[i] = side - correctionValue - eccBitsValue;
    }
    
    correctParameter(correction, eccBits);
    
    int nbCenterLayers = 0;
    
    for (int i = 0; i < sides.length; i++) {
        nbCenterLayers += transitionsBetweenCenters[i] >> 1;
    }
    
    if (nbCenterLayers < 1) {
        throw NotFoundException.getNotFoundInstance();
    }
    
    int nbLayers = 2 * nbCenterLayers;
    int nbDataBlocks = eccBits[0] + eccBits[1];
    int nbTotalBlocks = nbDataBlocks + eccBits[2] + eccBits[3];
    
    if (nbTotalBlocks < 1) {
        throw NotFoundException.getNotFoundInstance();
    }
    
    int nbDataCodewords = nbLayers * nbLayers - nbTotalBlocks;
    
    if (nbDataCodewords < 0) {
        throw NotFoundException.getNotFoundInstance();
    }
    
    int nbDataCodewordsBits = nbDataCodewords / nbDataBlocks;
    int[] nbBitsForBlocks = new int[]{nbDataCodewordsBits * eccBits[0], nbDataCodewordsBits * eccBits[1]};
    int[] nbBitsForDataBlocks = new int[]{nbDataCodewords - nbBitsForBlocks[0], nbDataCodewords - nbBitsForBlocks[1]};
    
    this.compact = nbDataCodewords % 2 != 0;
    this.nbLayers = nbLayers;
    this.nbDataBlocks = nbDataBlocks;
    this.nbCenterLayers = nbCenterLayers;
    this.shift = shift;
}
 

}