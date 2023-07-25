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
import java.io.OutputStream;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.validation.constraints.NotNull;

/**
 * Mock implementation of ServletOutputStream
 */
public class MockServletOutputStream extends ServletOutputStream {

	private final OutputStream delegate;

	public MockServletOutputStream(OutputStream stream) {
		this.delegate = stream;
	}

	public final OutputStream getdelegate() {
		return this.delegate;
	}

	@Override
	public void write(int b) throws IOException {
		this.delegate.write(b);
	}

	@Override
	public void write(@NotNull byte[] b) throws IOException {
		this.delegate.write(b);
	}

	@Override
	public void write(@NotNull byte[] b, int from, int length) throws IOException {
		this.delegate.write(b, from, length);
	}

    @Override
	public void flush() throws IOException {
		super.flush();
		this.delegate.flush();
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.delegate.close();
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		throw new UnsupportedOperationException();
	}

}