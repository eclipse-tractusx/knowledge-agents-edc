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

import okhttp3.Request;
import okio.Buffer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

/**
 * Wraps an ok Request into a javax servlet request
 */
public class HttpServletRequestAdapter implements HttpServletRequest {

    protected final Request request;
    protected final ServletContext context;

    public HttpServletRequestAdapter(Request request, ServletContext context) {
        this.request = request;
        this.context = context;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public long getDateHeader(String name) {
        return 0;
    }

    @Override
    public String getHeader(String name) {
        return request.header(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(request.headers().values(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(request.headers().names());
    }

    @Override
    public int getIntHeader(String name) {
        try {
            return Integer.parseInt(request.header(name));
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(nfe);
        }
    }

    @Override
    public String getMethod() {
        return request.method();
    }

    @Override
    public String getPathInfo() {
        return request.url().encodedPath();
    }

    @Override
    public String getPathTranslated() {
        return request.url().encodedPath();
    }

    @Override
    public String getContextPath() {
        return request.url().encodedPath();
    }

    @Override
    public String getQueryString() {
        return request.url().query();
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return request.url().uri().toString();
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(request.url().url().toString());
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return List.of();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return context.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        try {
            return (int) request.body().contentLength();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public long getContentLengthLong() {
        try {
            return request.body().contentLength();
        } catch (IOException e) {
            return -1L;
        }
    }

    @Override
    public String getContentType() {
        var body = request.body();
        if (body != null) {
            var contentType = body.contentType();
            if (contentType != null) {
                return contentType.toString();
            }
        }
        return null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.readByteArray());
        return new ServletInputStreamDelegator(bais);
    }

    @Override
    public String getParameter(String name) {
        return request.url().queryParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(request.url().queryParameterNames());
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.url().queryParameterValues(name).toArray(new String[0]);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = new HashMap<>();
        for (String queryParameterName : request.url().queryParameterNames()) {
            parameterMap.put(queryParameterName, getParameterValues(queryParameterName));
        }
        return parameterMap;
    }

    @Override
    public String getProtocol() {
        return request.url().isHttps() ? "HTTPS" : "HTTP";
    }

    @Override
    public String getScheme() {
        return request.url().scheme();
    }

    @Override
    public String getServerName() {
        return request.url().host();
    }

    @Override
    public int getServerPort() {
        return request.url().port();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(request.body().toString()));
    }

    @Override
    public String getRemoteAddr() {
        return request.url().fragment();
    }

    @Override
    public String getRemoteHost() {
        return request.url().host();
    }

    @Override
    public void setAttribute(String name, Object o) {
        context.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        context.removeAttribute(name);
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        return request.url().port();
    }

    @Override
    public String getLocalName() {
        return "";
    }

    @Override
    public String getLocalAddr() {
        return "";
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }
}
