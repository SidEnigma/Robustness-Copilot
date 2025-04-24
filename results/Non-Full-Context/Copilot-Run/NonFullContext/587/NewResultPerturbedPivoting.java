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
 
 
/** Gets the number of data layers and data blocks from the layer around the bull's eye. */

private void extractParameters(ResultPoint[] bullsEyeCorners) throws NotFoundException {
    int dimension = image.getHeight();
    ResultPoint p0 = bullsEyeCorners[0];
    ResultPoint p1 = bullsEyeCorners[1];
    ResultPoint p2 = bullsEyeCorners[2];
    ResultPoint p3 = bullsEyeCorners[3];

    int moduleSize = MathUtils.round(ResultPoint.distance(p0, p1) / 7.0f);
    if (moduleSize < 2) {
        throw NotFoundException.getNotFoundInstance();
    }

    int topRightX;
    int topRightY;
    int topLeftX;
    int topLeftY;
    int bottomLeftX;
    int bottomLeftY;
    int bottomRightX;
    int bottomRightY;

    if (Math.abs(p0.getX() - p2.getX()) < Math.abs(p0.getY() - p2.getY())) {
        topRightX = MathUtils.round(p0.getX() - (p1.getX() - p0.getX()) / 2.0f);
        topRightY = MathUtils.round(p0.getY() + (p1.getY() - p0.getY()) / 2.0f);
        topLeftX = MathUtils.round(p2.getX() + (p1.getX() - p2.getX()) / 2.0f);
        topLeftY = MathUtils.round(p2.getY() - (p2.getY() - p1.getY()) / 2.0f);
        bottomLeftX = MathUtils.round(p2.getX() - (p3.getX() - p2.getX()) / 2.0f);
        bottomLeftY = MathUtils.round(p2.getY() + (p3.getY() - p2.getY()) / 2.0f);
        bottomRightX = MathUtils.round(p0.getX() + (p3.getX() - p0.getX()) / 2.0f);
        bottomRightY = MathUtils.round(p0.getY() - (p0.getY() - p3.getY()) / 2.0f);
    } else {
        topRightX = MathUtils.round(p0.getX() + (p1.getX() - p0.getX()) / 2.0f);
        topRightY = MathUtils.round(p0.getY() + (p1.getY() - p0.getY()) / 2.0f);
        topLeftX = MathUtils.round(p2.getX() + (p1.getX() - p2.getX()) / 2.0f);
        topLeftY = MathUtils.round(p2.getY() + (p2.getY() - p1.getY()) / 2.0f);
        bottomLeftX = MathUtils.round(p2.getX() - (p3.getX() - p2.getX()) / 2.0f);
        bottomLeftY = MathUtils.round(p2.getY() - (p3.getY() - p2.getY()) / 2.0f);
        bottomRightX = MathUtils.round(p0.getX() - (p0.getX() - p3.getX()) / 2.0f);
        bottomRightY = MathUtils.round(p0.getY() - (p0.getY() - p3.getY()) / 2.0f);
    }

    int dimensionTop = MathUtils.round(ResultPoint.distance(new ResultPoint(topRightX, topRightY), new ResultPoint(topLeftX, topLeftY)) / moduleSize);
    int dimensionRight = MathUtils.round(ResultPoint.distance(new ResultPoint(topRightX, topRightY), new ResultPoint(bottomRightX, bottomRightY)) / moduleSize);
    int dimensionBottom = MathUtils.round(ResultPoint.distance(new ResultPoint(bottomLeftX, bottomLeftY), new ResultPoint(bottomRightX, bottomRightY)) / moduleSize);
    int dimensionLeft = MathUtils.round(ResultPoint.distance(new ResultPoint(topLeftX, topLeftY), new ResultPoint(bottomLeftX, bottomLeftY)) / moduleSize);

    int dimensionMin = Math.min(Math.min(dimensionTop, dimensionRight), Math.min(dimensionBottom, dimensionLeft));

    if ((dimensionTop & 0x01) == 1) {
        dimensionTop++;
    }
    if ((dimensionRight & 0x01) == 1) {
        dimensionRight++;
    }
    if ((dimensionBottom & 0x01) == 1) {
        dimensionBottom++;
    }
    if ((dimensionLeft & 0x01) == 1) {
        dimensionLeft++;
    }

    int shift = 0;
    if (dimensionTop < dimensionMin) {
        shift++;
    }
    if (dimensionRight < dimensionMin) {
        shift += 2;
    }
    if (dimensionBottom < dimensionMin) {
        shift += 3;
    }

    int[] bits = new int[4];
    bits[0] = sampleGrid(image, dimensionTop, dimensionRight, dimensionBottom, dimensionLeft, topRightX, topRightY, topLeftX, topLeftY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY);
    bits[1] = sampleGrid(image, dimensionTop, dimensionRight, dimensionBottom, dimensionLeft, topRightX, topRightY, topLeftX, topLeftY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY);
    bits[2] = sampleGrid(image, dimensionTop, dimensionRight, dimensionBottom, dimensionLeft, topRightX, topRightY, topLeftX, topLeftY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY);
    bits[3] = sampleGrid(image, dimensionTop, dimensionRight, dimensionBottom, dimensionLeft, topRightX, topRightY, topLeftX, topLeftY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY);

    this.compact = false;
    this.nbLayers = 0;
    this.nbDataBlocks = 0;
    this.nbCenterLayers = 0;

    for (int i = 0; i < 4; i++) {
        int bitsMatched = matchBitPattern(bits[i], EXPECTED_CORNER_BITS[i]);
        if (bitsMatched == -1) {
            throw NotFoundException.getNotFoundInstance();
        }
        int layer = bitsMatched >> 7;
        int dataBlocks = (bitsMatched >> 5) & 0x03;
        int centerLayers = bitsMatched & 0x1F;

        this.compact |= layer == 2;
        this.nbLayers += layer;
        this.nbDataBlocks += dataBlocks;
        this.nbCenterLayers += centerLayers;
    }

    if (this.nbLayers > 4) {
        throw NotFoundException.getNotFoundInstance();
    }
}
 

}