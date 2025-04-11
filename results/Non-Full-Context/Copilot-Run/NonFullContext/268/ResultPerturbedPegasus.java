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
 
 package org.apache.shenyu.plugin.base;
 
 import org.apache.commons.collections4.CollectionUtils;
 import org.apache.shenyu.common.dto.PluginData;
 import org.apache.shenyu.common.dto.RuleData;
 import org.apache.shenyu.common.dto.SelectorData;
 import org.apache.shenyu.common.enums.SelectorTypeEnum;
 import org.apache.shenyu.plugin.api.ShenyuPlugin;
 import org.apache.shenyu.plugin.api.ShenyuPluginChain;
 import org.apache.shenyu.plugin.base.cache.BaseDataCache;
 import org.apache.shenyu.plugin.base.condition.strategy.MatchStrategyFactory;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 import org.springframework.web.server.ServerWebExchange;
 import reactor.core.publisher.Mono;
 
 import java.util.Collection;
 import java.util.List;
 import java.util.Objects;
 
 /**
  * abstract shenyu plugin please extends.
  */
 public abstract class AbstractShenyuPlugin implements ShenyuPlugin {
 
     private static final Logger LOG = LoggerFactory.getLogger(AbstractShenyuPlugin.class);
 
     /**
      * this is Template Method child has Implement your own logic.
      *
      * @param exchange exchange the current server exchange {@linkplain ServerWebExchange}
      * @param chain    chain the current chain  {@linkplain ServerWebExchange}
      * @param selector selector    {@linkplain SelectorData}
      * @param rule     rule    {@linkplain RuleData}
      * @return {@code Mono<Void>} to indicate when request handling is complete
      */
     protected abstract Mono<Void> doExecute(ServerWebExchange exchange, ShenyuPluginChain chain, SelectorData selector, RuleData rule);
 
 
/** If you want to delegate to the next @code ShenyuPlugin, Process the Web request and give the @link ShenyuPluginChain. */
 public Mono<Void> execute(final ServerWebExchange exchange, final ShenyuPluginChain chain){
      final SelectorData selector = getSelector(exchange);
      final RuleData rule = getRule(exchange);
      if (Objects.isNull(selector) || Objects.isNull(rule)) {
          return chain.execute(exchange);
      }
      return doExecute(exchange, chain, selector, rule);
  }
  
      /**
        * get the selector from the cache.
        *
        * @param exchange exchange the current server exchange {@linkplain ServerWebExchange}
        * @return {@code SelectorData}
        */
      protected SelectorData getSelector(ServerWebExchange exchange) {
          final String selectorKey = getSelectorKey(exchange);
          final SelectorData selector = BaseDataCache.getSelector(selectorKey);
          if (Objects.isNull(selector)) {
              LOG.warn("can not find selector by key:{}", selectorKey);
          }
          return selector;
      }
  
      /**
        * get the rule from the cache.
        *
        * @param exchange exchange the current server exchange {@linkplain ServerWebExchange}
        * @return {@code RuleData}
        */
      protected RuleData getRule(ServerWebExchange exchange) {
          final String ruleKey = getRuleKey(exchange);
          final RuleData rule = BaseDataCache.getRule(ruleKey);
          if (Objects.isNull(rule)) {
              LOG.warn("can not find rule by key:{}", ruleKey);
          }
          return rule;
      }
  
      /**
        * get the selector key from the cache.
        *
        * @param exchange exchange the current server exchange {@linkplain ServerWebExchange}
        * @return {@code String}
        */
      protected String getSelectorKey(ServerWebExchange exchange) {
          final String selectorKey = getSelectorKey(exchange.getRequest().getPath().value());
          if (Objects.isNull(selectorKey)) {
              LOG.warn("can not find selector key by path:{}", exchange.getRequest().getPath().value  
 }

 

}