// Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.agents.edc.http;

import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Extended Http Client Factory which has configurable timeouts
 */
public class HttpClientFactory {

    protected static Class httpDataSourceFactory;
    protected static Field sourceFactories;
    protected static Field httpClient;
    protected static Field okHttpClient;
    protected static Field connectTimeoutMillis;
    protected static Field callTimeoutMillis;
    protected static Field readTimeoutMillis;
    protected static Field writeTimeoutMillis;
    protected static Field pingIntervalMillis;

    static {
        try {
            sourceFactories = HttpClientFactory.class.getClassLoader().loadClass("org.eclipse.edc.connector.dataplane.framework.pipeline.PipelineServiceImpl").getDeclaredField("sourceFactories");
            sourceFactories.setAccessible(true);
            httpDataSourceFactory = HttpClientFactory.class.getClassLoader().loadClass("org.eclipse.edc.connector.dataplane.http.pipeline.HttpDataSourceFactory");
            httpClient = httpDataSourceFactory.getDeclaredField("httpClient");
            httpClient.setAccessible(true);
            okHttpClient = HttpClientFactory.class.getClassLoader().loadClass("org.eclipse.edc.connector.core.base.EdcHttpClientImpl").getDeclaredField("okHttpClient");
            okHttpClient.setAccessible(true);
            connectTimeoutMillis = OkHttpClient.class.getDeclaredField("connectTimeoutMillis");
            connectTimeoutMillis.setAccessible(true);
            readTimeoutMillis = OkHttpClient.class.getDeclaredField("readTimeoutMillis");
            readTimeoutMillis.setAccessible(true);
            writeTimeoutMillis = OkHttpClient.class.getDeclaredField("writeTimeoutMillis");
            writeTimeoutMillis.setAccessible(true);
            pingIntervalMillis = OkHttpClient.class.getDeclaredField("pingIntervalMillis");
            pingIntervalMillis.setAccessible(true);
            callTimeoutMillis = OkHttpClient.class.getDeclaredField("pingIntervalMillis");
            callTimeoutMillis.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            System.err.println("HttpClientFactory could not be initialised. Leaving default okhttp settings.");
        }
    }

    /**
     * Create an modified OkHttpClient instance
     *
     * @param config agent config
     * @param client parent/blueprint instance
     * @return the modified OkHttpClient
     */
    @NotNull
    public static Map.Entry<EdcHttpClient, OkHttpClient> create(EdcHttpClient edcClient, OkHttpClient client, PipelineService service, AgentConfig config) {
        Integer connectTimeout = config.getConnectTimeout();
        Integer readTimeout = config.getReadTimeout();
        Integer callTimeout = config.getCallTimeout();
        Integer writeTimeout = config.getWriteTimeout();

        if (connectTimeout != null || readTimeout != null || callTimeout != null || writeTimeout != null) {
            try {
                edcClient = ((Collection<DataSourceFactory>) sourceFactories.get(service)).stream().flatMap(factory -> {
                            if (httpDataSourceFactory.equals(factory.getClass())) {
                                try {
                                    return Optional.of((EdcHttpClient) httpClient.get(factory)).stream();
                                } catch (IllegalArgumentException | IllegalAccessException e) {
                                    System.err.println("HttpClientFactory could not reuse okhttp client.");
                                }
                            }
                            return Optional.<EdcHttpClient>empty().stream();
                        }
                ).findFirst().orElse(edcClient);
                client = (OkHttpClient) okHttpClient.get(edcClient);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                System.err.println("HttpClientFactory could not reuse okhttp client.");
            }
            if (connectTimeoutMillis != null && connectTimeout != null) {
                try {
                    connectTimeoutMillis.set(client, connectTimeout);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    System.err.println("HttpClientFactory could not set connectTimeout");
                }
            }
            if (readTimeoutMillis != null && readTimeout != null) {
                try {
                    readTimeoutMillis.set(client, readTimeout);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    System.err.println("HttpClientFactory could not set readTimeout");
                }
            }
            if (callTimeoutMillis != null && callTimeout != null) {
                try {
                    callTimeoutMillis.set(client, callTimeout);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    System.err.println("HttpClientFactory could not set callTimeout");
                }
            }
            if (writeTimeoutMillis != null && writeTimeout != null) {
                try {
                    writeTimeoutMillis.set(client, writeTimeout);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    System.err.println("HttpClientFactory could not set writeTimeout");
                }
            }
        }
        return new AbstractMap.SimpleEntry(edcClient, client);
    }

}
