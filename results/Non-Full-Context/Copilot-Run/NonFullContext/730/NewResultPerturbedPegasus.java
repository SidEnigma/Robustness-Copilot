/*
  * C++ Community Plugin (cxx plugin)
  * Copyright (C) 2021 SonarOpenCommunity
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
 /**
  * fork of SSLR Squid Bridge: https://github.com/SonarSource/sslr-squid-bridge/tree/2.6.1
  * Copyright (C) 2010 SonarSource / mailto: sonarqube@googlegroups.com / license: LGPL v3
  */
 package org.sonar.cxx.squidbridge.annotations;
 
 import com.google.common.annotations.VisibleForTesting;
 import com.google.common.base.Predicates;
 import com.google.common.collect.Iterables;
 import com.google.common.collect.Lists;
 import com.google.common.collect.Sets;
 import java.lang.annotation.Annotation;
 import java.net.URL;
 import java.util.*;
 import org.apache.commons.lang.StringUtils;
 import org.sonar.api.server.rule.*;
 import org.sonar.api.server.rule.RulesDefinition.NewRepository;
 import org.sonar.api.server.rule.RulesDefinition.NewRule;
 import org.sonar.api.utils.AnnotationUtils;
 import org.sonar.check.Cardinality;
 import org.sonar.check.Rule;
 import org.sonar.check.RuleProperty;
 import org.sonar.cxx.squidbridge.rules.ExternalDescriptionLoader;
 
 /**
  * Utility class which helps setting up an implementation of {@link RulesDefinition} with a list of
  * rule classes annotated with {@link Rule}, {@link RuleProperty} and SQALE annotations:
  * Exactly one of:
  * <ul>
  * <li>{@link SqaleConstantRemediation}</li>
  * <li>{@link SqaleLinearRemediation}</li>
  * <li>{@link SqaleLinearWithOffsetRemediation}</li>
  * </ul>
  * Names and descriptions are also retrieved based on the legacy SonarQube conventions:
  * <ul>
  * <li>Rule names and rule property descriptions can be defined in a property file:
  * /org/sonar/l10n/[languageKey].properties</li>
  * <li>HTML rule descriptions can be defined in individual resources:
  * /org/sonar/l10n/[languageKey]/rules/[repositoryKey]/ruleKey.html</li>
  * </ul>
  *
  * @since 2.5
  */
 public class AnnotationBasedRulesDefinition {
 
   private final NewRepository repository;
   private final String languageKey;
   private final ExternalDescriptionLoader externalDescriptionLoader;
 
 
/** Adds annotated rule classes to a NewRepository instance. */

public static void load(NewRepository repository, String languageKey, Iterable<Class> ruleClasses) {
    AnnotationBasedRulesDefinition rulesDefinition = new AnnotationBasedRulesDefinition(repository, languageKey);
    rulesDefinition.loadRuleClasses(ruleClasses);
}
 

}