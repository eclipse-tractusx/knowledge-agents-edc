//
// Copyright (C) 2022-2023 Catena-X Association and others. 
// 
// This program and the accompanying materials are made available under the
// terms of the Apache License 2.0 which is available at
// http://www.apache.org/licenses/.
//  
// SPDX-FileType: SOURCE
// SPDX-FileCopyrightText: 2022-2023 Catena-X Association
// SPDX-License-Identifier: Apache-2.0
//
package org.eclipse.tractusx.agents.edc.http;

import java.io.IOException;

import javax.servlet.WriteListener;

import jakarta.servlet.ServletOutputStream;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

/**
 * An invocation handler which maps jakarta output stream
 * to a javax.servlet level
 */
public class JakartaServletOutputStreamAdapter extends javax.servlet.ServletOutputStream implements IJakartaAdapter<ServletOutputStream> {
    
    jakarta.servlet.ServletOutputStream jakartaDelegate;
    Monitor monitor;

    public JakartaServletOutputStreamAdapter(jakarta.servlet.ServletOutputStream jakartaDelegate, Monitor monitor) {
        this.jakartaDelegate=jakartaDelegate;
        this.monitor=monitor;
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
