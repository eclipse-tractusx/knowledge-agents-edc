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

	public final OutputStream getDelegate() {
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