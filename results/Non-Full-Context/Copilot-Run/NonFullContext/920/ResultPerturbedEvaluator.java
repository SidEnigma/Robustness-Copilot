/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.apache.shenyu.admin.listener;
 
 import com.google.gson.reflect.TypeToken;
 import org.apache.commons.collections4.CollectionUtils;
 import org.apache.shenyu.admin.service.AppAuthService;
 import org.apache.shenyu.admin.service.MetaDataService;
 import org.apache.shenyu.admin.service.PluginService;
 import org.apache.shenyu.admin.service.RuleService;
 import org.apache.shenyu.admin.service.SelectorService;
 import org.apache.shenyu.common.dto.AppAuthData;
 import org.apache.shenyu.common.dto.ConfigData;
 import org.apache.shenyu.common.dto.MetaData;
 import org.apache.shenyu.common.dto.PluginData;
 import org.apache.shenyu.common.dto.RuleData;
 import org.apache.shenyu.common.dto.SelectorData;
 import org.apache.shenyu.common.enums.ConfigGroupEnum;
 import org.apache.shenyu.common.enums.DataEventTypeEnum;
 import org.apache.shenyu.common.utils.GsonUtils;
 import org.apache.shenyu.common.utils.Md5Utils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.beans.factory.InitializingBean;
 
 import javax.annotation.Resource;
 import java.util.List;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.ConcurrentMap;
 
 
 /**
  * Abstract class for ConfigEventListener.
  * As we think that the md5 value of the in-memory data is the same as the md5 value of the database,
  * although it may be a little different, but it doesn't matter, we will have thread to periodically
  * pull the data in the database.
  *
  * @since 2.0.0
  */
 @SuppressWarnings("all")
 public abstract class AbstractDataChangedListener implements DataChangedListener, InitializingBean {
 
     /**
      * The constant CACHE.
      */
     protected static final ConcurrentMap<String, ConfigDataCache> CACHE = new ConcurrentHashMap<>();
 
     private static final Logger LOG = LoggerFactory.getLogger(AbstractDataChangedListener.class);
 
     @Resource
     private AppAuthService appAuthService;
 
     /**
      * The Plugin service.
      */
     @Resource
     private PluginService pluginService;
 
     /**
      * The Rule service.
      */
     @Resource
     private RuleService ruleService;
 
     /**
      * The Selector service.
      */
     @Resource
     private SelectorService selectorService;
 
     @Resource
     private MetaDataService metaDataService;
 
     /**
      * fetch configuration from cache.
      *
      * @param groupKey the group key
      * @return the configuration data
      */
     public ConfigData<?> fetchConfig(final ConfigGroupEnum groupKey) {
         ConfigDataCache config = CACHE.get(groupKey.name());
         switch (groupKey) {
             case APP_AUTH:
                 List<AppAuthData> appAuthList = GsonUtils.getGson().fromJson(config.getJson(), new TypeToken<List<AppAuthData>>() {
                 }.getType());
                 return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), appAuthList);
             case PLUGIN:
                 List<PluginData> pluginList = GsonUtils.getGson().fromJson(config.getJson(), new TypeToken<List<PluginData>>() {
                 }.getType());
                 return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), pluginList);
             case RULE:
                 List<RuleData> ruleList = GsonUtils.getGson().fromJson(config.getJson(), new TypeToken<List<RuleData>>() {
                 }.getType());
                 return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), ruleList);
             case SELECTOR:
                 List<SelectorData> selectorList = GsonUtils.getGson().fromJson(config.getJson(), new TypeToken<List<SelectorData>>() {
                 }.getType());
                 return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), selectorList);
             case META_DATA:
                 List<MetaData> metaList = GsonUtils.getGson().fromJson(config.getJson(), new TypeToken<List<MetaData>>() {
                 }.getType());
                 return new ConfigData<>(config.getMd5(), config.getLastModifyTime(), metaList);
             default:
                 throw new IllegalStateException("Unexpected groupKey: " + groupKey);
         }
     }
 
     @Override
     public void onAppAuthChanged(final List<AppAuthData> changed, final DataEventTypeEnum eventType) {
         if (CollectionUtils.isEmpty(changed)) {
             return;
         }
         this.updateAppAuthCache();
         this.afterAppAuthChanged(changed, eventType);
     }
 
     @Override
     public void onMetaDataChanged(final List<MetaData> changed, final DataEventTypeEnum eventType) {
         if (CollectionUtils.isEmpty(changed)) {
             return;
         }
         this.updateMetaDataCache();
         this.afterMetaDataChanged(changed, eventType);
     }
 
     /**
      * After meta data changed.
      *
      * @param changed   the changed
      * @param eventType the event type
      */
     protected void afterMetaDataChanged(final List<MetaData> changed, final DataEventTypeEnum eventType) {
     }
 
     /**
      * After app auth changed.
      *
      * @param changed   the changed
      * @param eventType the event type
      */
     protected void afterAppAuthChanged(final List<AppAuthData> changed, final DataEventTypeEnum eventType) {
     }
 
     @Override
     public void onPluginChanged(final List<PluginData> changed, final DataEventTypeEnum eventType) {
         if (CollectionUtils.isEmpty(changed)) {
             return;
         }
         this.updatePluginCache();
         this.afterPluginChanged(changed, eventType);
     }
 
     /**
      * After plugin changed.
      *
      * @param changed   the changed
      * @param eventType the event type
      */
     protected void afterPluginChanged(final List<PluginData> changed, final DataEventTypeEnum eventType) {
     }
 
     @Override
     public void onRuleChanged(final List<RuleData> changed, final DataEventTypeEnum eventType) {
         if (CollectionUtils.isEmpty(changed)) {
             return;
         }
         this.updateRuleCache();
         this.afterRuleChanged(changed, eventType);
     }
 
     /**
      * After rule changed.
      *
      * @param changed   the changed
      * @param eventType the event type
      */
     protected void afterRuleChanged(final List<RuleData> changed, final DataEventTypeEnum eventType) {
     }
 
     @Override
     public void onSelectorChanged(final List<SelectorData> changed, final DataEventTypeEnum eventType) {
         if (CollectionUtils.isEmpty(changed)) {
             return;
         }
         this.updateSelectorCache();
         this.afterSelectorChanged(changed, eventType);
     }
 
     /**
      * After selector changed.
      *
      * @param changed   the changed
      * @param eventType the event type
      */
     protected void afterSelectorChanged(final List<SelectorData> changed, final DataEventTypeEnum eventType) {
     }
 
     @Override
     public final void afterPropertiesSet() {
         updateAppAuthCache();
         updatePluginCache();
         updateRuleCache();
         updateSelectorCache();
         updateMetaDataCache();
         afterInitialize();
     }
 
     protected abstract void afterInitialize();
 
 
/** Update the local cache if the MD5 does not equal the original */
 protected void updateCache(final ConfigGroupEnum group, final List<T> data){
        ConfigDataCache config = CACHE.get(group.name());
        if (config == null) {
            config = new ConfigDataCache();
            CACHE.put(group.name(), config);
        }
        if (config.getMd5().equals(MD5Utils.getMD5(data))) {
            return;
        }
        config.setJson(GsonUtils.getGson().toJson(data));
        config.setMd5(MD5Utils.getMD5(data));
        config.setLastModifyTime(System.currentTimeMillis());
    }
    
        /**
        * Update app auth cache.
        */
        protected void updateAppAuthCache() {
            List<AppAuthData> appAuthList = appAuthService.getAllAppAuth();
            updateCache(ConfigGroupEnum.APP_AUTH, appAuthList);
        }
    
        /**
        * Update plugin cache.
        */
        protected void updatePluginCache() {
            List<PluginData> pluginList = pluginService.getAllPlugin();
            updateCache(ConfigGroupEnum.PLUGIN, pluginList);
        }
    
        /**
        * Update rule cache.
        */
        protected void updateRuleCache() {
            List<RuleData> ruleList = ruleService.getAllRule();
            updateCache(ConfigGroupEnum.RULE, ruleList);
        }
    
        /**
        * Update selector cache.
        */
        protected void updateSelectorCache() {
            List<SelectorData> selectorList = selectorService.getAllSelector();
            updateCache(ConfigGroupEnum.SELECTOR, selectorList);
        }
    
        /**
        * Update meta data cache.
        */
        protected void updateMetaDataCache() {
            List<MetaData> metaList = metaDataService.getAllMetaData();
            updateCache(ConfigGroupEnum.META_DATA, metaList);
        }
    
        /**
        * Gets app auth service.
        *
        * @return the app auth service
        */
        public AppAuthService       
 }

 

}