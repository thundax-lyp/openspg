/*
 * Copyright 2023 OpenSPG Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.antgroup.openspg.server.biz.common.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.sofa.common.utils.ObjectUtil;
import com.antgroup.openspg.common.constants.SpgAppConstant;
import com.antgroup.openspg.common.util.StringUtils;
import com.antgroup.openspg.common.util.enums.ModelType;
import com.antgroup.openspg.server.api.facade.dto.common.request.ConfigRequest;
import com.antgroup.openspg.server.biz.common.ConfigManager;
import com.antgroup.openspg.server.common.model.config.Config;
import com.antgroup.openspg.server.common.service.config.ConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ConfigManagerImpl implements ConfigManager {

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public Config query(String configId, String version) {
        return configRepository.query(configId, version);
    }

    @Override
    public Config getById(Long id) {
        return configRepository.getById(id);
    }

    @Override
    public Integer create(ConfigRequest request) {
        Config config = toModel(request);
        return configRepository.save(config);
    }

    @Override
    public Integer update(ConfigRequest request) {
        Config oldConfig = getById(request.getId());
        if (oldConfig == null) {
            return 0;
        }
        Config config = toModel(request);
        return configRepository.update(config);
    }

    @Override
    public String setApiKeyDesensitization(String configStr) {
        if (StringUtils.isBlank(configStr)) {
            return configStr;
        }

        JSONObject config = JSON.parseObject(configStr);
        if (config == null) {
            return "";
        }

        JSONObject vectorizerJson = config.getJSONObject(SpgAppConstant.VECTORIZER);
        if (vectorizerJson != null && vectorizerJson.containsKey(SpgAppConstant.API_KEY)) {
            vectorizerJson.put(SpgAppConstant.API_KEY, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
            config.put(SpgAppConstant.VECTORIZER, vectorizerJson);
        }

        JSONArray llmArray = config.getJSONArray(SpgAppConstant.LLM_SELECT);
        if (CollectionUtils.isNotEmpty(llmArray)) {
            for (int i = 0; i < llmArray.size(); i++) {
                JSONObject llm = llmArray.getJSONObject(i);
                if (llm != null) {
                    if (llm.containsKey(SpgAppConstant.API_KEY)) {
                        llm.put(SpgAppConstant.API_KEY, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
                    }

                    if (llm.containsKey(SpgAppConstant.KEY)) {
                        llm.put(SpgAppConstant.KEY, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
                    }
                }
            }
            config.put(SpgAppConstant.LLM_SELECT, llmArray);
        }

        JSONObject llm = config.getJSONObject(SpgAppConstant.LLM);
        if (llm != null) {
            if (llm.containsKey(SpgAppConstant.API_KEY)) {
                llm.put(SpgAppConstant.API_KEY, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
            }
            if (llm.containsKey(SpgAppConstant.KEY)) {
                llm.put(SpgAppConstant.KEY, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
            }
        }

        JSONObject graphStore = config.getJSONObject(SpgAppConstant.GRAPH_STORE);
        if (graphStore != null) {
            graphStore.put(SpgAppConstant.PASSWORD, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY);
            config.put(SpgAppConstant.GRAPH_STORE, graphStore);
        }
        return config.toJSONString();
    }

    @Override
    public void handleApiKey(JSONObject config, String oldConfig) {
        if (config == null || StringUtils.isBlank(oldConfig)) {
            return;
        }

        JSONObject oldConfigJson = JSON.parseObject(oldConfig);
        JSONObject vectorizer = config.getJSONObject(SpgAppConstant.VECTORIZER);
        if (vectorizer != null) {
            String apiKey = vectorizer.getString(SpgAppConstant.API_KEY);
            if (vectorizer.containsKey(SpgAppConstant.API_KEY) && StringUtils.equals(apiKey, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                String oldApiKey = oldConfigJson
                        .getJSONObject(SpgAppConstant.VECTORIZER)
                        .getString(SpgAppConstant.API_KEY);
                vectorizer.put(SpgAppConstant.API_KEY, oldApiKey);
                config.put(SpgAppConstant.VECTORIZER, vectorizer);
            }
        }

        JSONArray oldLlmArray = oldConfigJson.getJSONArray(SpgAppConstant.LLM_SELECT);
        Map<String, JSONObject> oldLlmMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(oldLlmArray)) {
            for (int i = 0; i < oldLlmArray.size(); i++) {
                JSONObject oldLlm = oldLlmArray.getJSONObject(i);
                if (oldLlm != null) {
                    oldLlmMap.put(oldLlm.getString(SpgAppConstant.LLM_ID), oldLlm);
                }
            }
        }

        JSONArray llmArray = config.getJSONArray(SpgAppConstant.LLM_SELECT);
        if (CollectionUtils.isNotEmpty(llmArray)) {
            for (int i = 0; i < llmArray.size(); i++) {
                JSONObject llm = llmArray.getJSONObject(i);
                if (llm == null) {
                    continue;
                }
                JSONObject oldLlm = oldLlmMap.get(llm.getString(SpgAppConstant.LLM_ID));
                if (null == oldLlm) {
                    JSONObject oldLlmJson = oldConfigJson.getJSONObject(SpgAppConstant.LLM);
                    if (oldLlmJson == null) {
                        if (oldConfigJson.containsKey(SpgAppConstant.CHAT_LLM)) {
                            oldLlmJson = oldConfigJson.getJSONObject(SpgAppConstant.CHAT_LLM);
                        }
                        if (oldLlmJson == null && oldConfigJson.containsKey(SpgAppConstant.OPENIE_LLM)) {
                            oldLlmJson = oldConfigJson.getJSONObject(SpgAppConstant.OPENIE_LLM);
                        }
                    }
                    backwardCompatibleLLM(oldLlmJson);
                    if (oldLlmJson != null
                            && StringUtils.equals(
                            oldLlmJson.getString(SpgAppConstant.TYPE), llm.getString(SpgAppConstant.TYPE))
                            && StringUtils.equals(
                            oldLlmJson.getString(SpgAppConstant.MODEL),
                            llm.getString(SpgAppConstant.MODEL))) {
                        oldLlm = oldLlmJson;
                    }
                }
                if (oldLlm == null) {
                    continue;
                }

                if (llm.containsKey(SpgAppConstant.API_KEY)
                        && StringUtils.equals(llm.getString(SpgAppConstant.API_KEY), SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                    String llmApiKey = oldLlm.getString(SpgAppConstant.API_KEY);
                    llm.put(SpgAppConstant.API_KEY, llmApiKey);
                }

                if (llm.containsKey(SpgAppConstant.KEY)
                        && StringUtils.equals(llm.getString(SpgAppConstant.KEY), SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                    String llmApiKey = oldLlm.getString(SpgAppConstant.KEY);
                    llm.put(SpgAppConstant.KEY, llmApiKey);
                }
            }
            config.put(SpgAppConstant.LLM_SELECT, llmArray);
        }

        JSONObject llm = config.getJSONObject(SpgAppConstant.LLM);
        if (llm != null) {
            JSONObject oldLlm = oldConfigJson.getJSONObject(SpgAppConstant.LLM);
            if (oldLlm == null) {
                oldLlm = oldLlmMap.get(llm.getString(SpgAppConstant.LLM_ID));
            }
            if (oldLlm != null) {
                if (llm.containsKey(SpgAppConstant.API_KEY)
                        && StringUtils.equals(
                        llm.getString(SpgAppConstant.API_KEY), SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                    String llmApiKey = oldLlm.getString(SpgAppConstant.API_KEY);
                    llm.put(SpgAppConstant.API_KEY, llmApiKey);
                }
                if (llm.containsKey(SpgAppConstant.KEY)
                        && StringUtils.equals(
                        llm.getString(SpgAppConstant.KEY), SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                    String llmApiKey = oldLlm.getString(SpgAppConstant.KEY);
                    llm.put(SpgAppConstant.KEY, llmApiKey);
                }
            }
        }

        JSONObject graphStore = config.getJSONObject(SpgAppConstant.GRAPH_STORE);
        if (graphStore != null) {
            String password = graphStore.getString(SpgAppConstant.PASSWORD);
            if (StringUtils.equals(password, SpgAppConstant.DEFAULT_VECTORIZER_API_KEY)) {
                String oldPassword = oldConfigJson
                        .getJSONObject(SpgAppConstant.GRAPH_STORE)
                        .getString(SpgAppConstant.PASSWORD);
                graphStore.put(SpgAppConstant.PASSWORD, oldPassword);
                config.put(SpgAppConstant.GRAPH_STORE, graphStore);
            }
        }
    }

    @Override
    public void generateLLMIdCompletionLLM(JSONObject config) {
        if (config == null) {
            return;
        }

        JSONArray llmArray = config.getJSONArray(SpgAppConstant.LLM_SELECT);
        if (llmArray == null) {
            return;
        }

        for (int i = 0; i < llmArray.size(); i++) {
            JSONObject llm = llmArray.getJSONObject(i);
            if (llm != null) {
                if (StringUtils.isBlank(llm.getString("llm_id"))) {
                    String llmId = UUID.randomUUID().toString();
                    llm.put(SpgAppConstant.LLM_ID, llmId);
                }

                if (llm.getBooleanValue("default")) {
                    config.put(SpgAppConstant.LLM, JSON.parseObject(llm.toJSONString()));
                } else {
                    llm.remove(SpgAppConstant.LLM);
                }
            }
        }
        config.put(SpgAppConstant.LLM_SELECT, llmArray);
    }

    @Override
    public void backwardCompatible(JSONObject config) {
        if (config == null) {
            return;
        }
        // vectorizer 0.5 -> 0.6
        JSONObject vectorizerJson = config.getJSONObject(SpgAppConstant.VECTORIZER);
        if (vectorizerJson != null) {
            vectorizerJson.remove(SpgAppConstant.VECTORIZER);
            if (!vectorizerJson.containsKey(SpgAppConstant.TYPE)) {
                vectorizerJson.put(SpgAppConstant.TYPE, SpgAppConstant.OPENAI);
            }
            String model = vectorizerJson.getString(SpgAppConstant.MODEL);
            if (StringUtils.equals(model, "bge-m3")) {
                vectorizerJson.put(SpgAppConstant.MODEL, "BAAI/bge-m3");
            }
            config.put(SpgAppConstant.VECTORIZER, vectorizerJson);
        }

        // llm 0.5 -> 0.6
        JSONObject llmJson = config.getJSONObject(SpgAppConstant.LLM);
        if (llmJson != null) {
            backwardCompatibleLLM(llmJson);
            config.put(SpgAppConstant.LLM, llmJson);

        } else {
            if (config.containsKey(SpgAppConstant.CHAT_LLM)) {
                llmJson = JSON.parseObject(JSON.toJSONString(config.getJSONObject(SpgAppConstant.CHAT_LLM)));
            }
            if (llmJson == null && config.containsKey(SpgAppConstant.OPENIE_LLM)) {
                llmJson = JSON.parseObject(JSON.toJSONString(config.getJSONObject(SpgAppConstant.OPENIE_LLM)));
            }
            if (llmJson != null) {
                backwardCompatibleLLM(llmJson);
                config.put(SpgAppConstant.LLM, llmJson);
            }
        }

        // llm_select 0.5 -> 0.6
        JSONArray llmSelectJson = config.getJSONArray(SpgAppConstant.LLM_SELECT);
        if (CollectionUtils.isNotEmpty(llmSelectJson)) {
            for (int i = 0; i < llmSelectJson.size(); i++) {
                JSONObject llm = llmSelectJson.getJSONObject(i);
                backwardCompatibleLLM(llm);
            }
            config.put(SpgAppConstant.LLM_SELECT, llmSelectJson);

        } else if (CollectionUtils.isEmpty(llmSelectJson) && llmJson != null) {
            llmSelectJson = new JSONArray();
            JSONObject llm = JSON.parseObject(llmJson.toJSONString());
            String llmId = UUID.randomUUID().toString();
            backwardCompatibleLLM(llm);
            llm.put(SpgAppConstant.LLM_ID, llmId);
            llm.put(SpgAppConstant.DEFAULT, true);
            llm.put(SpgAppConstant.DESC, "");
            llmSelectJson.add(llm);
            config.put(SpgAppConstant.LLM_SELECT, llmSelectJson);
        }
    }

    public String getLLMIdByConfig(JSONObject config) {
        if (config == null) {
            return null;
        }

        JSONObject llmJson = config.getJSONObject(SpgAppConstant.LLM);
        return llmJson == null ? null: llmJson.getString(SpgAppConstant.LLM_ID);
    }

    public boolean isLLMChange(JSONObject oldConfig, JSONObject config) {
        if (oldConfig != null && config != null) {
            JSONObject oldLlmJson = oldConfig.getJSONObject(SpgAppConstant.LLM);
            JSONObject llmJson = config.getJSONObject(SpgAppConstant.LLM);
            if (oldLlmJson == null && llmJson == null) {
                return false;
            } else if (oldLlmJson == null) {
                return true;
            } else if (oldLlmJson.size() != llmJson.size()) {
                return true;
            } else {
                for(String key : llmJson.keySet()) {
                    if (!StringUtils.equals(key, SpgAppConstant.LLM_ID)) {
                        if (!oldLlmJson.containsKey(key)) {
                            return true;
                        }

                        Object oldVal = oldLlmJson.get(key);
                        Object newVal = llmJson.get(key);
                        if (!ObjectUtil.equals(oldVal, newVal)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private static void backwardCompatibleLLM(JSONObject llmJson) {
        if (llmJson != null) {
            String clientType = llmJson.getString(SpgAppConstant.CLIENT_TYPE);
            ModelType modelType = ModelType.getByCode(clientType);
            if (modelType == null) {
                modelType = ModelType.getByCode(llmJson.getString(SpgAppConstant.TYPE));
            }

            if (modelType != null) {
                if (!llmJson.containsKey(SpgAppConstant.LLM_ID)) {
                    String llmId = UUID.randomUUID().toString();
                    llmJson.put(SpgAppConstant.LLM_ID, llmId);
                }

                switch (modelType) {
                    case MAAS:
                        llmJson.remove("client_type");

                        if (!llmJson.containsKey("type")) {
                            llmJson.put("type", clientType);
                        }

                        if (!llmJson.containsKey("temperature")) {
                            llmJson.put("temperature", 0.7);
                        }

                        if (!llmJson.containsKey("stream")) {
                            llmJson.put("stream", "False");
                        }
                        break;
                    case OLLAMA:
                    case VLLM:
                        llmJson.remove("client_type");

                        if (!llmJson.containsKey("type")) {
                            llmJson.put("type", clientType);
                        }
                        break;
                    default:
                        log.info("not support model type: {}", clientType);
                }

            }
        }
    }

    private Config toModel(ConfigRequest request) {
        Config config = new Config();
        config.setProjectId("0");
        config.setUserNo("admin");
        config.setId(request.getId());
        config.setConfigName(request.getConfigName());
        config.setConfigId(request.getConfigId());
        config.setVersion(request.getVersion());
        config.setConfig(request.getConfig());
        return config;
    }
}
