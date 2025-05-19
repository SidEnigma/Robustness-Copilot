/*
  * Copyright (c) 2014 European Bioinformatics Institute (EMBL-EBI)
  *                    John May <jwmay@users.sf.net>
  *
  * Contact: cdk-devel@lists.sourceforge.net
  *
  * This program is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as published by
  * the Free Software Foundation; either version 2.1 of the License, or (at
  * your option) any later version. All we ask is that proper credit is given
  * for our work, which includes - but is not limited to - adding the above
  * copyright notice to the beginning of your source code files, and to any
  * copyright notice that you may distribute with programs based on this work.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  * License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  */
 
 package org.openscience.cdk.renderer.generators.standard;
 
 import java.awt.Font;
 import java.awt.Shape;
 import java.awt.font.FontRenderContext;
 import java.awt.font.GlyphVector;
 import java.awt.geom.AffineTransform;
 import java.awt.geom.Point2D;
 import java.awt.geom.Rectangle2D;
 
 /**
  * Immutable outline of text. The outline is maintained as a Java 2D shape
  * instance and can be transformed. As an immutable instance, transforming the
  * outline creates a new instance.
  *
  * @author John May
  */
 final class TextOutline {
 
     public static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
     /**
      * The original text.
      */
     private final String          text;
 
     /**
      * The original glyphs.
      */
     private final GlyphVector     glyphs;
 
     /**
      * The outline of the text (untransformed).
      */
     private final Shape           outline;
 
     /**
      * Transform applied to outline.
      */
     private final AffineTransform transform;
 
     /**
      * Create an outline of text in provided font.
      *
      * @param text the text to create an outline of
      * @param font the font style, size, and shape that defines the outline
      */
     TextOutline(final String text, final Font font) {
         this(text, font.createGlyphVector(new FontRenderContext(new AffineTransform(), true, true), text));
     }
 
     /**
      * Create an outline of text and the glyphs for that text.
      *
      * @param text the text to create an outline of
      * @param glyphs the glyphs for the provided outlined
      */
     TextOutline(String text, GlyphVector glyphs) {
         this(text, glyphs, glyphs.getOutline(), new AffineTransform());
     }
 
     /**
      * Internal constructor, requires all attributes.
      *
      * @param text the text
      * @param glyphs glyphs of the text
      * @param outline the outline of the glyphs
      * @param transform the transform
      */
     private TextOutline(String text, GlyphVector glyphs, Shape outline, AffineTransform transform) {
         this.text = text;
         this.glyphs = glyphs;
         this.outline = outline;
         this.transform = transform;
     }
 
     /**
      * The text which the outline displays.
      * @return the text
      */
     String text() {
         return text;
     }
 
     /**
      * Access the transformed outline of the text.
      *
      * @return transformed outline
      */
     Shape getOutline() {
         return transform.createTransformedShape(outline);
     }
 
     /**
      * Access the transformed bounds of the outline text.
      *
      * @return transformed bounds
      */
     Rectangle2D getBounds() {
         return transformedBounds(outline);
     }
 
     /**
      * Access the transformed logical bounds of the outline text.
      *
      * @return logical bounds
      */
     Rectangle2D getLogicalBounds() {
         return transformedBounds(glyphs.getLogicalBounds());
     }
 
 
/** Access the bounds of a shape that have been transformed. */
 private Rectangle2D transformedBounds(Shape shape){}

 

}