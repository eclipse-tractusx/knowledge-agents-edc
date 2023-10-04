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
package org.eclipse.tractusx.agents.edc.http;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.*;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.tractusx.agents.edc.*;
import org.eclipse.tractusx.agents.edc.rdf.RDFStore;
import org.eclipse.tractusx.agents.edc.rdf.TestRdfStore;
import org.eclipse.tractusx.agents.edc.service.DataManagement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests the graph controller
 */
public class TestGraphController {
    
    ConsoleMonitor monitor=new ConsoleMonitor();
    TestConfig config=new TestConfig();
    AgentConfig agentConfig=new AgentConfig(monitor,config);
    RDFStore store = new RDFStore(agentConfig,monitor);

    DataManagement management = new MockDataManagement();


    GraphController graphController=new GraphController(monitor, store, management, agentConfig);

    AutoCloseable mocks=null;

    @BeforeEach
    public void setUp()  {
        mocks=MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(mocks!=null) {
            mocks.close();
            mocks=null;
        }
    }
    
    @Mock
    HttpServletRequest request;
 
    @Mock
    HttpServletResponse response;

    @Mock
    ServletContext context;

    /**
     * execution helper
     * @param method http method
     * @param body optional body
     * @param asset optional asset name
     * @param accepts determines return representation
     * @param params additional parameters
     * @return response body as string
     */
    protected String testExecute(String method, String contentType, String body, String asset, String accepts, List<Map.Entry<String,String>> params) throws IOException {
        Map<String,String[]> fparams=new HashMap<>();
        StringBuilder queryString=new StringBuilder();
        boolean isFirst=true;
        for(Map.Entry<String,String> param : params) {
            if(isFirst) {
                isFirst=false;
            } else {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8));
            queryString.append("=");
            queryString.append(URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
            if(fparams.containsKey(param.getKey())) {
                String[] oarray=fparams.get(param.getKey());
                String[] narray=new String[oarray.length+1];
                System.arraycopy(oarray,0,narray,0,oarray.length);
                narray[oarray.length]=param.getValue();
                fparams.put(param.getKey(),narray);
            } else {
                String[] narray=new String[] { param.getValue() };
                fparams.put(param.getKey(),narray);
            }
        }
        when(request.getQueryString()).thenReturn(queryString.toString());
        when(request.getMethod()).thenReturn(method);
        if(asset!=null) {
            fparams.put("asset",new String[] { asset });
            when(request.getParameter("asset")).thenReturn(asset);
        }
        when(request.getParameterMap()).thenReturn(fparams);
        when(request.getServletContext()).thenReturn(context);
        when(request.getHeaders("Accept")).thenReturn(Collections.enumeration(List.of(accepts)));
        if(body!=null) {
            when(request.getContentType()).thenReturn(contentType);
            when(request.getInputStream()).thenReturn(new MockServletInputStream(new ByteArrayInputStream(body.getBytes())));
        }
        ByteArrayOutputStream responseStream=new ByteArrayOutputStream();
        MockServletOutputStream mos=new MockServletOutputStream(responseStream);
        when(response.getOutputStream()).thenReturn(mos);
        if(method.equals("POST")) {
            return graphController.postAsset(body, asset, null, null, null, null, null, true, new String[0], request).getEntity().toString();
        } else {
            return graphController.deleteAsset(asset, null, request, response, null).getEntity().toString();
        }
    }

    /**
     * test import/delete workflow
     * @throws IOException in case of an error
     */
    @Test
    public void testTurtle() throws IOException {
        String result=testExecute("POST","text/turtle", TestRdfStore.getTestTurtle(),"GraphAsset?consumer=Upload","*/*",new ArrayList<>());
        assertEquals("4",result,"Correct insert result");
        result=testExecute("DELETE",null, null,"GraphAsset?consumer=Upload","*/*",new ArrayList<>());
        assertEquals("4",result,"Correct delete result");
    }

    /**
     * test import/delete workflow
     * @throws IOException in case of an error
     */
    @Test
    public void testCsv() throws IOException {
        String result=testExecute("POST","text/csv", TestRdfStore.getTestCsv(),"GraphAsset?consumer=Upload","*/*",new ArrayList<>());
        assertEquals("11",result,"Correct insert result");
        result=testExecute("DELETE",null, null,"GraphAsset?consumer=Upload","*/*",new ArrayList<>());
        assertEquals("11",result,"Correct delete result");
    }

}
