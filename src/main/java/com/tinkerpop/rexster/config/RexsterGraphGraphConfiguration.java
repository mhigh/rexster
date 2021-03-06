package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.rexster.RexsterGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;

public class RexsterGraphGraphConfiguration implements GraphConfiguration {

    public static final int DEFAULT_BUFFER_SIZE = 100;

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String rexsterGraphUriToConnectTo;
        int bufferSize;

        try {
            rexsterGraphUriToConnectTo = properties.getString(Tokens.REXSTER_GRAPH_FILE, null);
            bufferSize = properties.getInt(Tokens.REXSTER_GRAPH_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }

        RexsterGraph graph = null;
        try {
            graph = new RexsterGraph(rexsterGraphUriToConnectTo, bufferSize);
        } catch (RuntimeException rte) {
            // if the remote server is down just ignore the error for the moment.  let
            // Rexster think the graph configuration is good.  the server may be up later.

        }

        return graph;
    }
}
