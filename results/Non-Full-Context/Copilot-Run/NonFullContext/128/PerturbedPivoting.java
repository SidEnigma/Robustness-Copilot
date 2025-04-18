/*
  * #%L
  * ACS AEM Commons Bundle
  * %%
  * Copyright (C) 2017 Adobe
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
 package com.adobe.acs.commons.hc.impl;
 
 import com.adobe.acs.commons.email.EmailService;
 import com.adobe.acs.commons.util.ModeUtil;
 import com.adobe.acs.commons.util.RequireAem;
 import com.adobe.granite.license.ProductInfo;
 import com.adobe.granite.license.ProductInfoService;
 import org.apache.commons.lang3.ArrayUtils;
 import org.apache.commons.lang3.StringUtils;
 import org.apache.felix.scr.annotations.Activate;
 import org.apache.felix.scr.annotations.Component;
 import org.apache.felix.scr.annotations.ConfigurationPolicy;
 import org.apache.felix.scr.annotations.Properties;
 import org.apache.felix.scr.annotations.Property;
 import org.apache.felix.scr.annotations.Reference;
 import org.apache.felix.scr.annotations.Service;
 import org.apache.sling.commons.osgi.PropertiesUtil;
 import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
 import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
 import org.apache.sling.hc.api.execution.HealthCheckExecutor;
 import org.apache.sling.settings.SlingSettingsService;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Scanner;
 
 @Component(
         label = "ACS AEM Commons - Health Check Status E-mailer",
         description = "Scheduled Service that runs specified Health Checks and e-mails the results",
         configurationFactory = true,
         policy = ConfigurationPolicy.REQUIRE,
         metatype = true
 )
 @Properties({
         @Property(
                 label = "Cron expression defining when this Scheduled Service will run",
                 name = "scheduler.expression",
                 description = "Every weekday @ 8am = [ 0 0 8 ? * MON-FRI * ] Visit www.cronmaker.com to generate cron expressions.",
                 value = "0 0 8 ? * MON-FRI *"
         ),
         @Property(
                 name = "scheduler.concurrent",
                 boolValue = false,
                 propertyPrivate = true
         ),
         @Property(
                 name = "scheduler.runOn",
                 value = "LEADER",
                 propertyPrivate = true
         )
 })
 @Service(value = Runnable.class)
 public class HealthCheckStatusEmailer implements Runnable {
     private static final Logger log = LoggerFactory.getLogger(HealthCheckStatusEmailer.class);
 
     private static final int HEALTH_CHECK_STATUS_PADDING = 20;
     private static final int NUM_DASHES = 100;
 
     // Disable this feature on AEM as a Cloud Service
     @Reference(target="(distribution=classic)")
     RequireAem requireAem;
     
     private volatile Calendar nextEmailTime = Calendar.getInstance();
 
     /* OSGi Properties */
 
     private static final String DEFAULT_EMAIL_TEMPLATE_PATH = "/etc/notification/email/acs-commons/health-check-status-email.txt";
     private String emailTemplatePath = DEFAULT_EMAIL_TEMPLATE_PATH;
     @Property(label = "E-mail Template Path",
             description = "The absolute JCR path to the e-mail template",
             value = DEFAULT_EMAIL_TEMPLATE_PATH)
     public static final String PROP_TEMPLATE_PATH = "email.template.path";
 
     private static final String DEFAULT_EMAIL_SUBJECT_PREFIX = "AEM Health Check report";
     private String emailSubject = DEFAULT_EMAIL_SUBJECT_PREFIX;
     @Property(label = "E-mail Subject Prefix",
             description = "The e-mail subject prefix. E-mail subject format is: <E-mail Subject Prefix> [ # Failures ] [ # Success ] [ <AEM Instance Name> ]",
             value = DEFAULT_EMAIL_SUBJECT_PREFIX)
     public static final String PROP_EMAIL_SUBJECT = "email.subject";
 
     private static final boolean DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE = true;
     private boolean sendEmailOnlyOnFailure = DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE;
     @Property(label = "Send e-mail only on failure",
             description = "If true, an e-mail is ONLY sent if at least 1 Health Check failure occurs. [ Default: true ]",
             boolValue = DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE)
     public static final String PROP_SEND_EMAIL_ONLY_ON_FAILURE = "email.send-only-on-failure";
 
     private static final String[] DEFAULT_RECIPIENT_EMAIL_ADDRESSES = new String[]{};
     private String[] recipientEmailAddresses = DEFAULT_RECIPIENT_EMAIL_ADDRESSES;
     @Property(label = "Recipient E-mail Addresses",
             description = "A list of e-mail addresses to send this e-mail to.",
             cardinality = Integer.MAX_VALUE,
             value = {})
     public static final String PROP_RECIPIENTS_EMAIL_ADDRESSES = "recipients.email-addresses";
 
     private static final String[] DEFAULT_HEALTH_CHECK_TAGS = new String[]{"system"};
     private String[] healthCheckTags = DEFAULT_HEALTH_CHECK_TAGS;
     @Property(label = "Health Check Tags",
             description = "The AEM Health Check Tag names to execute. [ Default: system ]",
             cardinality = Integer.MAX_VALUE,
             value = {"system"})
     public static final String PROP_HEALTH_CHECK_TAGS = "hc.tags";
 
     private static final int DEFAULT_HEALTH_CHECK_TIMEOUT_OVERRIDE = -1;
     private int healthCheckTimeoutOverride = DEFAULT_HEALTH_CHECK_TIMEOUT_OVERRIDE;
     @Property(label = "Health Check Timeout Override",
             description = "The AEM Health Check timeout override in milliseconds. Set < 1 to disable. [ Default: -1 ]",
             intValue = DEFAULT_HEALTH_CHECK_TIMEOUT_OVERRIDE)
     public static final String PROP_HEALTH_CHECK_TIMEOUT_OVERRIDE = "hc.timeout.override";
 
     private static final boolean DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR = true;
     private boolean healthCheckTagsOptionsOr = DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR;
     @Property(label = "'OR' Health Check Tags",
             description = "When set to true, all Health Checks that are in any of the Health Check Tags (hc.tags) are executed. If false, then the Health Check must be in ALL of the Health Check tags (hc.tags). [ Default: true ]",
             boolValue = DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR)
     public static final String PROP_HEALTH_CHECK_TAGS_OPTIONS_OR = "hc.tags.options.or";
 
     private static final String DEFAULT_FALLBACK_HOSTNAME = "Unknown AEM Instance";
     private String fallbackHostname = DEFAULT_FALLBACK_HOSTNAME;
     @Property(label = "Hostname Fallback",
             description = "The value used to identify this AEM instance if the programmatic hostname look-up fails to produce results..",
             value = DEFAULT_FALLBACK_HOSTNAME)
     public static final String PROP_FALLBACK_HOSTNAME = "hostname.fallback";
 
     private static final int DEFAULT_THROTTLE_IN_MINS = 15;
     private int throttleInMins = DEFAULT_THROTTLE_IN_MINS;
     @Property(label = "Quiet Period in Minutes",
             description = "Defines a time span that prevents this service from sending more than 1 e-mail per quiet period. This prevents e-mail spamming for frequent checks that only e-mail on failure. Default: [ 15 mins ]",
             intValue = DEFAULT_THROTTLE_IN_MINS)
     public static final String PROP_THROTTLE = "quiet.minutes";
 
     @Property(
             name = "webconsole.configurationFactory.nameHint",
             value = "Health Check Status E-mailer running every [ {scheduler.expression} ] using Health Check Tags [ {hc.tags} ] to [ {recipients.email-addresses} ]"
     )
 
     @Reference
     private ProductInfoService productInfoService;
 
     @Reference
     private SlingSettingsService slingSettingsService;
 
     @Reference
     private EmailService emailService;
 
     @Reference
     private HealthCheckExecutor healthCheckExecutor;
 
     @Override
     public final void run() {
         log.trace("Executing ACS Commons Health Check E-mailer scheduled service");
 
         final List<HealthCheckExecutionResult> success = new ArrayList<>();
         final List<HealthCheckExecutionResult> failure = new ArrayList<>();
 
         final long start = System.currentTimeMillis();
 
         final HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
         options.setForceInstantExecution(true);
         options.setCombineTagsWithOr(healthCheckTagsOptionsOr);
         if (healthCheckTimeoutOverride > 0) {
             options.setOverrideGlobalTimeout(healthCheckTimeoutOverride);
         }
         final List<HealthCheckExecutionResult> results = healthCheckExecutor.execute(options, healthCheckTags);
 
         log.debug("Obtained [ {} ] results for Health Check tags [ {} ]", results.size(), StringUtils.join(healthCheckTags, ", "));
         for (HealthCheckExecutionResult result : results) {
             if (result.getHealthCheckResult().isOk()) {
                 success.add(result);
             } else {
                 failure.add(result);
             }
         }
 
         final long timeTaken = System.currentTimeMillis() - start;
         log.info("Executed ACS Commons Health Check E-mailer scheduled service in [ {} ms ]", timeTaken);
 
         if (!sendEmailOnlyOnFailure || failure.size() > 0) {
             Calendar now = Calendar.getInstance();
             if (nextEmailTime == null || now.equals(nextEmailTime) || now.after(nextEmailTime)) {
                 sendEmail(success, failure, timeTaken);
                 now.add(Calendar.MINUTE, throttleInMins);
                 nextEmailTime = now;
             } else {
                 log.info("Did not send e-mail as it did not meet the e-mail throttle configured time of a [ {} ] minute quiet period. Next valid time to e-mail is [ {} ]", throttleInMins, nextEmailTime.getTime());
             }
         } else {
             log.debug("Declining to send e-mail notification of 100% successful Health Check execution due to configuration.");
         }
     }
 
 
/** Creates the e-mail template parameter mapping and calls the OSGi e-mail service. */
 protected final void sendEmail(final List<HealthCheckExecutionResult> success, final List<HealthCheckExecutionResult> failure, final long timeTaken){}

 

}