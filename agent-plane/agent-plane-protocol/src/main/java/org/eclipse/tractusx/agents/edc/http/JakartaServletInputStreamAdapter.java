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

import jakarta.servlet.ServletInputStream;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import javax.servlet.ReadListener;

/**
 * An invocation handler which maps all jakarta input stream
 * to a javax.servlet level
 */
public class JakartaServletInputStreamAdapter extends javax.servlet.ServletInputStream implements JakartaAdapter<ServletInputStream> {

    jakarta.servlet.ServletInputStream jakartaDelegate;
    Monitor monitor;

    public JakartaServletInputStreamAdapter(jakarta.servlet.ServletInputStream jakartaDelegate, Monitor monitor) {
        this.jakartaDelegate = jakartaDelegate;
        this.monitor = monitor;
    }

    @Override
    public jakarta.servlet.ServletInputStream getDelegate() {
        return jakartaDelegate;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public boolean isFinished() {
        return jakartaDelegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return jakartaDelegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        //jakartaDelegate.setReadListener(readListener);
    }

    @Override
    public int read() throws IOException {
        return jakartaDelegate.read();
    }

    @Override
    public int read(byte @NotNull [] buf) throws IOException {
        return jakartaDelegate.read(buf);
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        return jakartaDelegate.read(b, off, len);
    }

}
