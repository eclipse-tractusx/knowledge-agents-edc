// Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.tractusx.agents.edc.http.transfer;

import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpParamsDecorator;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.http.HttpUtils;
import org.eclipse.tractusx.agents.edc.sparql.DataspaceServiceExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decorator to implement specifics of the http-based Agent transfer protocol.
 * In particular the translation back from transfer to matchmaking.
 * should be placed instead of {@see org.eclipse.edc.connector.dataplane.http.params.decorators.BaseSourceHttpParamsDecorator}
 */
public class AgentSourceHttpParamsDecorator implements HttpParamsDecorator {

    /**
     * static constants
     */
    public static final String ASSET_PROP_ID = "https://w3id.org/edc/v0.0.1/ns/id";
    public static final String ACCEPT_HEADER = "https://w3id.org/catenax/ontology/common#acceptsContentType";
    public static final String QUERY_PARAMS = "queryParams";
    public static final String QUERY_PARAM = "query";
    public static final String METHOD = "method";
    public static final String DEFAULT_METHOD = "GET";
    public static final String PATH_SEGMENTS = "pathSegments";
    public static final String BASE_URL = "https://w3id.org/edc/v0.0.1/ns/baseUrl";
    public static final String BODY = "body";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String SLASH = "/";

    public static final String CX_ACCEPT_PARAM = "cx_accept";

    public static final String DEFAULT_ACCEPT = "*/*";
    /**
     * regexes to extract url-encoded form and query parts
     */
    public static final String PARAM_GROUP = "param";
    public static final String VALUE_GROUP = "value";


    public static final Pattern PARAMS = Pattern.compile(String.format("(\\?|&)(?<%s>[^=&]+)=(?<%s>[^=&]*)", PARAM_GROUP, VALUE_GROUP));

    public static final String WWW_FORM_ENCODED = "application/x-www-form-urlencoded";

    public static final String SPARQL_QUERY = "application/sparql-query";

    public static final String CONTENT_TYPE_DISPOSITION = "q=[0-9]+(\\.[0-9]+)?, ";

    /**
     * service references
     */
    protected final AgentConfig config;
    protected final Monitor monitor;

    /**
     * creates a new decorator
     *
     * @param config agent configuration
     * @param monitor logging facility
     */
    public AgentSourceHttpParamsDecorator(AgentConfig config, Monitor monitor) {
        this.config = config;
        this.monitor = monitor;
    }

    /**
     * check whether this is a transfer or a source request
     *
     * @param dataflowRequest the request to check
     * @return if this is a transfer request
     */
    public static boolean isTransferRequest(DataFlowRequest dataflowRequest) {
        return false;
    }

    /**
     * parse the body or parameter string as a url-encoded form into a map
     *
     * @param body url-encoded form body
     * @return a map of parameters
     */
    public static Map<String, List<String>> parseParams(String body) {
        Map<String, List<String>> parts = new HashMap<>();
        if (body != null) {
            Matcher matcher = PARAMS.matcher(body);
            while (matcher.find()) {
                String paramName = matcher.group(PARAM_GROUP);
                List<String> partSet = parts.computeIfAbsent(paramName, k -> new ArrayList<>());
                partSet.add(matcher.group(VALUE_GROUP));
            }
        }
        return parts;
    }

    public static Map<String, List<String>> mergeParams(Map<String, List<String>> param1, Map<String, List<String>> param2) {
        param2.forEach((key, value) -> {
            if (param1.containsKey(key)) {
                param1.get(key).addAll(value);
            } else {
                param1.put(key, value);
            }
        });
        return param1;
    }

    /**
     * Implements the decoration
     *
     * @param request transfer request (contains dynamic stuff)
     * @param address target address (contains static stuff)
     * @param params translated call content (up to now)
     * @return translated call content (identical to params)
     */
    @Override
    public HttpRequestParams.Builder decorate(DataFlowRequest request, HttpDataAddress address, HttpRequestParams.Builder params) {
        String contentType = this.extractContentType(address, request);
        String body = this.extractBody(address, request);
        Map<String, List<String>> queryParams = parseParams("?" + getRequestQueryParams(address, request));

        if (isTransferRequest(request)) {
            if (!address.getStringProperty(BASE_URL).endsWith(SLASH)) {
                params.baseUrl(address.getStringProperty(BASE_URL) + SLASH);
            }
        } else {
            // we need to annotate the base url "pure" because we do not directly hit the endpoint
            params.baseUrl("https://w3id.org/catenax");
            params.header(DataspaceServiceExecutor.TARGET_URL_SYMBOL.getSymbol(), address.getStringProperty(BASE_URL));

            // there is the case where a KA-BIND protocol call is
            // one-to-one routed through the transfer plane ... in which case
            // we may get query parameters in the body
            // in this case we leave the query in the body (and rewriting the content type)
            if (contentType != null && contentType.contains(WWW_FORM_ENCODED)) {
                Map<String, List<String>> bodyParams = parseParams("&" + body);
                contentType = SPARQL_QUERY;
                List<String> queries = queryParams.getOrDefault(QUERY_PARAM, bodyParams.getOrDefault(QUERY_PARAM, List.of()));
                if (queries.size() != 1) {
                    throw new EdcException(String.format("DataFlowRequest %s: found %d queries when contentType %s is used", request.getId(), queries.size(), WWW_FORM_ENCODED));
                }
                body = HttpUtils.urlDecodeParameter(queries.get(0));
                bodyParams.remove(QUERY_PARAM);
                queryParams.remove(QUERY_PARAM);
                mergeParams(queryParams, bodyParams);
            }
            String accept = address.getStringProperty(ACCEPT_HEADER, null);
            List<String> cxAccepts = queryParams.getOrDefault(CX_ACCEPT_PARAM, List.of());
            queryParams.remove(CX_ACCEPT_PARAM);
            if (accept == null) {
                accept = cxAccepts.stream().findFirst().orElse(DEFAULT_ACCEPT);
            }
            accept = accept.replace(CONTENT_TYPE_DISPOSITION, "").replace("%2F", "/");
            params.header("Accept", accept);
        }
        Map<String, List<String>> addressParams = parseParams("?" + address.getQueryParams());
        mergeParams(queryParams, addressParams);
        String paramString = queryParams.entrySet().stream().flatMap((param) -> param.getValue().stream().map((value) -> param.getKey() + "=" + value)).collect(Collectors.joining("&"));
        params.queryParams(!paramString.isEmpty() ? paramString : null);
        params.method(this.extractMethod(address, request));
        params.path(this.extractPath(address, request));
        if (contentType != null) {
            params.contentType(contentType);
        }
        params.body(body);
        params.nonChunkedTransfer(false);
        return params;
    }

    protected @NotNull String extractMethod(HttpDataAddress address, DataFlowRequest request) {
        if (Boolean.parseBoolean(address.getProxyMethod())) {
            return Optional.ofNullable(request.getProperties().get(METHOD)).orElseThrow(() -> new EdcException(String.format("DataFlowRequest %s: 'method' property is missing", request.getId())));
        } else {
            return Optional.ofNullable(address.getMethod()).orElse(DEFAULT_METHOD);
        }
    }

    protected @Nullable String extractPath(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyPath()) ? request.getProperties().get(PATH_SEGMENTS) : address.getPath();
    }

    protected @Nullable String getRequestQueryParams(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyQueryParams()) ? request.getProperties().get(QUERY_PARAMS) : null;
    }

    /**
     * extract the content type
     *
     * @param address target address
     * @param request data flow request
     * @return the content type (which would be derived from the query language part in case the original content type is a url-encoded form)
     */
    protected @Nullable String extractContentType(HttpDataAddress address, DataFlowRequest request) {
        String contentType = Boolean.parseBoolean(address.getProxyBody()) ? request.getProperties().get(MEDIA_TYPE) : address.getContentType();
        return contentType;
    }

    protected @Nullable String extractBody(HttpDataAddress address, DataFlowRequest request) {
        return Boolean.parseBoolean(address.getProxyBody()) ? request.getProperties().get(BODY) : null;
    }
}
