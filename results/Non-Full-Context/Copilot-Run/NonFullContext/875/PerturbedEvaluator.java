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
 package org.sonar.cxx.checks.metrics;
 
 import com.sonar.sslr.api.AstNode;
 import com.sonar.sslr.api.Grammar;
 import org.sonar.check.Priority;
 import org.sonar.check.Rule;
 import org.sonar.check.RuleProperty;
 import org.sonar.cxx.parser.CxxGrammarImpl;
 import org.sonar.cxx.parser.CxxKeyword;
 import org.sonar.cxx.squidbridge.annotations.ActivatedByDefault;
 import org.sonar.cxx.squidbridge.annotations.SqaleConstantRemediation;
 import org.sonar.cxx.squidbridge.checks.AbstractOneStatementPerLineCheck;
 import org.sonar.cxx.tag.Tag;
 
 /**
  * TooManyStatementsPerLineCheck - Statements should be on separate lines
  */
 @Rule(
   key = "TooManyStatementsPerLine",
   name = "Statements should be on separate lines",
   tags = {Tag.BRAIN_OVERLOAD},
   priority = Priority.MAJOR)
 @ActivatedByDefault
 @SqaleConstantRemediation("5min")
 public class TooManyStatementsPerLineCheck extends AbstractOneStatementPerLineCheck<Grammar> {
 
   private static final boolean DEFAULT_EXCLUDE_CASE_BREAK = false;
   /**
    * excludeCaseBreak - Exclude 'break' statement if it is on the same line as the switch label (case: or default:)
    */
   @RuleProperty(
     key = "excludeCaseBreak",
     description = "Exclude 'break' statement if it is on the same line as the switch label (case: or default:)",
     defaultValue = "" + DEFAULT_EXCLUDE_CASE_BREAK)
   public boolean excludeCaseBreak = DEFAULT_EXCLUDE_CASE_BREAK;
 
 
/** If subsequent generated nodes are consecutive and on the same line this method excludes them */
 private static boolean isGeneratedNodeExcluded(AstNode astNode){}

 

}