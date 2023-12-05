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

import jakarta.servlet.ServletOutputStream;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import javax.servlet.WriteListener;

/**
 * An invocation handler which maps jakarta output stream
 * to a javax.servlet level
 */
public class JakartaServletOutputStreamAdapter extends javax.servlet.ServletOutputStream implements JakartaAdapter<ServletOutputStream> {

    jakarta.servlet.ServletOutputStream jakartaDelegate;
    Monitor monitor;

    public JakartaServletOutputStreamAdapter(jakarta.servlet.ServletOutputStream jakartaDelegate, Monitor monitor) {
        this.jakartaDelegate = jakartaDelegate;
        this.monitor = monitor;
    }

    @Override
    public jakarta.servlet.ServletOutputStream getDelegate() {
        return jakartaDelegate;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public boolean isReady() {
        return jakartaDelegate.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void write(int b) throws IOException {
        jakartaDelegate.write(b);
    }

    @Override
    public void write(byte @NotNull [] b) throws IOException {
        jakartaDelegate.write(b);
    }

    @Override
    public void write(byte @NotNull [] b, int off, int len) throws IOException {
        jakartaDelegate.write(b, off, len);
    }

}
