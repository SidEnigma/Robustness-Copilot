/*
  * C++ Community Plugin (cxx plugin)
  * Copyright (C) 2010-2021 SonarOpenCommunity
  * http://github.com/SonarOpenCommunity/sonar-cxx
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public
  * License as published by the Free Software Foundation; either
  * version 3 of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public License
  * along with this program; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
  */
 package org.sonar.cxx;
 
 import com.sonar.sslr.api.AstNode;
 import com.sonar.sslr.api.GenericTokenType;
 import com.sonar.sslr.api.Grammar;
 import static java.lang.Math.min;
 import java.util.Collection;
 import org.sonar.api.batch.fs.InputFile;
 import org.sonar.cxx.api.CxxMetric;
 import org.sonar.cxx.config.CxxSquidConfiguration;
 import org.sonar.cxx.parser.CxxGrammarImpl;
 import org.sonar.cxx.parser.CxxParser;
 import org.sonar.cxx.squidbridge.AstScanner;
 import org.sonar.cxx.squidbridge.CommentAnalyser;
 import org.sonar.cxx.squidbridge.SourceCodeBuilderVisitor;
 import org.sonar.cxx.squidbridge.SquidAstVisitor;
 import org.sonar.cxx.squidbridge.SquidAstVisitorContextImpl;
 import org.sonar.cxx.squidbridge.api.SourceClass;
 import org.sonar.cxx.squidbridge.api.SourceCode;
 import org.sonar.cxx.squidbridge.api.SourceFile;
 import org.sonar.cxx.squidbridge.api.SourceFunction;
 import org.sonar.cxx.squidbridge.api.SourceProject;
 import org.sonar.cxx.squidbridge.indexer.QueryByType;
 import org.sonar.cxx.squidbridge.metrics.CommentsVisitor;
 import org.sonar.cxx.squidbridge.metrics.ComplexityVisitor;
 import org.sonar.cxx.squidbridge.metrics.CounterVisitor;
 import org.sonar.cxx.squidbridge.metrics.LinesVisitor;
 import org.sonar.cxx.visitors.CxxCharsetAwareVisitor;
 import org.sonar.cxx.visitors.CxxCognitiveComplexityVisitor;
 import org.sonar.cxx.visitors.CxxCpdVisitor;
 import org.sonar.cxx.visitors.CxxCyclomaticComplexityVisitor;
 import org.sonar.cxx.visitors.CxxFileLinesVisitor;
 import org.sonar.cxx.visitors.CxxFileVisitor;
 import org.sonar.cxx.visitors.CxxFunctionComplexityVisitor;
 import org.sonar.cxx.visitors.CxxFunctionSizeVisitor;
 import org.sonar.cxx.visitors.CxxHighlighterVisitor;
 import org.sonar.cxx.visitors.CxxLinesOfCodeInFunctionBodyVisitor;
 import org.sonar.cxx.visitors.CxxLinesOfCodeVisitor;
 import org.sonar.cxx.visitors.CxxParseErrorLoggerVisitor;
 import org.sonar.cxx.visitors.CxxPublicApiVisitor;
 
 public final class CxxAstScanner {
 
   private CxxAstScanner() {
   }
 
 
/** The method allows you to test checks without the deployment on Sonar instance. */
 public static SourceFile scanSingleInputFile(InputFile inputFile, SquidAstVisitor<Grammar>... visitors){
    return scanSingleInputFile(inputFile, null, visitors);    
 }

 

}