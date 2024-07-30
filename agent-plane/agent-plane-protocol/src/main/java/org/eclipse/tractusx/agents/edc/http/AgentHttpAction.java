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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.ws.rs.BadRequestException;
import org.apache.http.HttpStatus;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.system.ActionCategory;
import org.eclipse.tractusx.agents.edc.Tuple;
import org.eclipse.tractusx.agents.edc.TupleSet;
import org.slf4j.Logger;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * HttpAction is a wrapper around a request/response 
 * which may either contain a query or a predefined skill. In each case
 * the parameterization/input binding can be done either by
 * url parameters, by a binding set body or both.
 * It contains also helper code to bind parameterized queries.
 */
public class AgentHttpAction extends HttpAction {
    final String skill;
    final String graphs;
    final TupleSet tupleSet = new TupleSet();

    /**
     * regexes to deal with url parameters
     */
    public static final String URL_PARAM_REGEX = "(?<key>[^=&]+)=(?<value>[^&]+)";
    public static final Pattern URL_PARAM_PATTERN = Pattern.compile(URL_PARAM_REGEX);
    public static final String RESULTSET_CONTENT_TYPE = "application/sparql-results+json";

    /**
     * creates a new http action
     *
     * @param id call id
     * @param logger the used logging output
     * @param request servlet input
     * @param response servlet output
     * @param skill option skill reference
     */
    public AgentHttpAction(long id, Logger logger, HttpServletRequest request, HttpServletResponse response, String skill, String graphs) {
        super(id, logger, ActionCategory.ACTION, request, response);
        this.skill = skill;
        this.graphs = graphs;
        parseArgs(request, response);
        parseBody(request, response);
    }

    /**
     * parses parameters
     */
    protected void parseArgs(HttpServletRequest request, HttpServletResponse response) {
        String params = "";
        String uriParams = request.getQueryString();
        if (uriParams != null) {
            params = URLDecoder.decode(uriParams, UTF_8);
        }
        Matcher paramMatcher = URL_PARAM_PATTERN.matcher(params);
        Stack<TupleSet> ts = new Stack<>();
        ts.push(tupleSet);
        while (paramMatcher.find()) {
            String key = paramMatcher.group("key");
            String value = paramMatcher.group("value");
            while (key.startsWith("(")) {
                key = key.substring(1);
                ts.push(new TupleSet());
            }
            if (key.length() <= 0) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            String realValue = value.replace(")", "");
            if (value.length() <= 0) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            try {
                if (!"asset".equals(key) && !"query".equals(key)) {
                    ts.peek().add(key, realValue);
                }
            } catch (Exception e) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            while (value.endsWith(")")) {
                TupleSet set1 = ts.pop();
                ts.peek().merge(set1);
                value = value.substring(0, value.length() - 1);
            }
        }
    }

    /**
     * parses a given binding into a tupleset
     *
     * @param resultSet new binding spec
     * @param tuples existing bindings
     */
    public static void parseBinding(JsonNode resultSet, TupleSet tuples) throws Exception {
        ArrayNode bindings = ((ArrayNode) resultSet.get("results").get("bindings"));
        for (int count = 0; count < bindings.size(); count++) {
            TupleSet ts = new TupleSet();
            JsonNode binding = bindings.get(count);
            Iterator<String> vars = binding.fieldNames();
            while (vars.hasNext()) {
                String var = vars.next();
                JsonNode value = binding.get(var).get("value");
                ts.add(var, value.textValue());
            }
            tuples.merge(ts);
        }
    }

    /**
     * parses the body of the request as an input binding, if
     * the content type is hinting to a sparql resultset
     */
    protected void parseBody(HttpServletRequest request, HttpServletResponse response) {
        if (RESULTSET_CONTENT_TYPE.equals(request.getContentType())) {
            ObjectMapper om = new ObjectMapper();
            try {
                parseBinding(om.readTree(request.getInputStream()), tupleSet);
            } catch (Exception e) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
            }
        }
    }

    /**
     * access
     *
     * @return optional skill
     */
    public String getSkill() {
        return skill;
    }

    /**
     * access
     *
     * @return optional skill
     */
    public String getGraphs() {
        return graphs;
    }

    /**
     * access
     *
     * @return the actual input bindings
     */
    public TupleSet getInputBindings() {
        return tupleSet;
    }

    /**
     * helper method to bind a given tupleset to a parameterized query
     *
     * @param query the parameterized query
     * @param bindings the tupleset to bind
     * @return bound query
     */
    public static String bind(String query, TupleSet bindings) throws Exception {
        Pattern tuplePattern = Pattern.compile("\\([^()]*\\)");
        Pattern variablePattern = Pattern.compile("@(?<name>[a-zA-Z0-9]+)");
        Matcher tupleMatcher = tuplePattern.matcher(query);
        StringBuilder replaceQuery = new StringBuilder();
        int lastStart = 0;

        //
        // First find parameterized tuple appearances. Each tuple appearance is
        // cloned for each bound "row"
        //
        while (tupleMatcher.find()) {
            replaceQuery.append(query.substring(lastStart, tupleMatcher.start()));
            String otuple = tupleMatcher.group(0);
            Matcher variableMatcher = variablePattern.matcher(otuple);
            List<String> variables = new java.util.ArrayList<>();
            while (variableMatcher.find()) {
                variables.add(variableMatcher.group("name"));
            }
            if (variables.size() > 0) {
                boolean isFirst = true;
                Collection<Tuple> tuples = bindings.getTuples(variables.toArray(new String[0]));
                for (Tuple rtuple : tuples) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        replaceQuery.append(" ");
                    }
                    String newTuple = otuple;
                    for (String key : rtuple.getVariables()) {
                        newTuple = newTuple.replace("@" + key, rtuple.get(key));
                    }
                    replaceQuery.append(newTuple);
                }
            } else {
                replaceQuery.append(otuple);
            }
            lastStart = tupleMatcher.end();
        }
        replaceQuery.append(query.substring(lastStart));

        //
        // Replacing "global" variables appearing not in a tuple expression.
        // This cannot be done for all bindings, but only the
        // very first one
        //
        String queryString =  replaceQuery.toString();

        Matcher variableMatcher = variablePattern.matcher(queryString);
        List<String> variables = new java.util.ArrayList<>();
        while (variableMatcher.find()) {
            variables.add(variableMatcher.group("name"));
        }
        try {
            Collection<Tuple> tuples = bindings.getTuples(variables.toArray(new String[0]));
            if (tuples.size() == 0 && variables.size() > 0) {
                throw new BadRequestException(String.format("Error: Got variables %s on top-level but no bindings.", Arrays.toString(variables.toArray())));
            } else if (tuples.size() > 1) {
                System.err.println(String.format("Warning: Got %s tuples for top-level bindings of variables %s. Using only the first one.", tuples.size(), Arrays.toString(variables.toArray())));
            }
            if (tuples.size() > 0) {
                Tuple rtuple = tuples.iterator().next();
                for (String key : rtuple.getVariables()) {
                    queryString = queryString.replace("@" + key, rtuple.get(key));
                }
            }
        } catch (Exception e) {
            throw new BadRequestException(String.format("Error: Could not bind variables"), e);
        }
        return queryString;
    }
}
