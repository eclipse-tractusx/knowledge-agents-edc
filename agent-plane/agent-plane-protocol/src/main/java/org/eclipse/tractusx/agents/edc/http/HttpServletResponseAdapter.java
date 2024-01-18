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

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * builds a ok response from an in-memory servlet response implementation
 */
public class HttpServletResponseAdapter implements HttpServletResponse {

    Response.Builder builder = new Response.Builder();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ServletOutputStream sos = new ServletOutputStreamDelegator(bos);

    String contentType;
    long contentLength;

    public HttpServletResponseAdapter(Request request) {
        builder.request(request);
        builder.protocol(Protocol.HTTP_1_1);
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {

    }

    @Override
    public void sendError(int sc) throws IOException {

    }

    @Override
    public void sendRedirect(String location) throws IOException {

    }

    @Override
    public void setDateHeader(String name, long date) {
        builder.header(name, String.valueOf(date));
    }

    @Override
    public void addDateHeader(String name, long date) {
        builder.addHeader(name, String.valueOf(date));
    }

    @Override
    public void setHeader(String name, String value) {
        builder.header(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        builder.addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        builder.header(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        builder.addHeader(name, String.valueOf(value));
    }

    @Override
    public void setStatus(int sc) {
        builder.code(sc).message(String.format("Status %d", sc));
    }

    @Override
    public void setStatus(int sc, String sm) {
        builder.code(sc).message(sm);
    }

    @Override
    public int getStatus() {
        return builder.build().code();
    }

    @Override
    public String getHeader(String name) {
        return builder.build().header(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return builder.build().headers().values(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return builder.build().headers().names();
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return sos;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(bos);
    }

    @Override
    public void setCharacterEncoding(String charset) {
    }

    @Override
    public void setContentLength(int len) {
        contentLength = len;
    }

    @Override
    public void setContentLengthLong(long len) {
        contentLength = len;
    }

    @Override
    public void setContentType(String type) {
        contentType = type;
    }

    @Override
    public void setBufferSize(int size) {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public void resetBuffer() {
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    public Response toResponse() {
        if (contentType != null) {
            ResponseBody body = ResponseBody.create(bos.toByteArray(), MediaType.parse(contentType));
            builder.body(body);
        }
        return builder.build();
    }
}
