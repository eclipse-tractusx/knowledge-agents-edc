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
package org.eclipse.tractusx.agents.edc.sparql;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.sparql.serializer.FormatterElement;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.syntax.*;

import java.util.Iterator;
import java.util.Stack;

/**
 * a formatter element which stratifies nested groups
 * such that SparQL endpoints have less work optimizing the
 * resulting (depp) trees.
 */
public class StratifiedFormatterElement extends FormatterElement {

    /**
     * access the context by the serializer
     */
    SerializationContext sc;
    /**
     * keep some state about the depth of the tree
     */
    Stack<Boolean> mute = new Stack<>();

    public StratifiedFormatterElement(IndentedWriter out, SerializationContext context) {
        super(out, context);
        this.sc = context;
    }

    /**
     * visit a join, only produce parenthesis if we are on the top level
     * @param el group element
     */

    @Override
    public void visit(ElementGroup el) {
        if (mute.empty() || !mute.peek()) {
            this.out.print("{");
        }
        int initialRowNumber = this.out.getRow();
        this.out.incIndent(2);
        int row1 = this.out.getRow();
        this.out.pad();
        boolean first = true;
        Element lastElt = null;

        Element subElement;
        for (Iterator<Element> var6 = el.getElements().iterator(); var6.hasNext(); lastElt = subElement) {
            subElement = var6.next();
            if (!first) {
                if (needsDotSeparator(lastElt, subElement)) {
                    this.out.print(" . ");
                }

                this.out.newline();
            }

            if (subElement instanceof ElementGroup) {
                mute.push(true);
            } else {
                mute.push(false);
            }
            subElement.visit(this);
            mute.pop();
            first = false;
        }

        this.out.decIndent(2);
        int row2 = this.out.getRow();
        if (row1 != row2) {
            this.out.newline();
        }

        if (this.out.getRow() == initialRowNumber) {
            this.out.print(" ");
        }

        if (mute.empty() || !mute.peek()) {
            this.out.print("}");
        }
    }


    /**
     * @param el1 first element
     * @param el2 second element
     * @return decide whether a dot is required between el1 and el2
     */
    private static boolean needsDotSeparator(Element el1, Element el2) {
        return needsDotSeparator(el1) && needsDotSeparator(el2);
    }

    private static boolean needsDotSeparator(Element el) {
        return el instanceof ElementTriplesBlock || el instanceof ElementPathBlock;
    }
}
