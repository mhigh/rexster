package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.json.JSONWriter;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.ExtensionSegmentSet;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterExtension;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;

@Path("/{graphname}/vertices")
public class VertexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(VertexResource.class);

    public VertexResource() {
        super(null);
    }

    public VertexResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    /**
     * GET http://host/graph/vertices
     * graph.getVertices();
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVertices(@PathParam("graphname") String graphName) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        try {
            long counter = 0l;
            final JSONArray vertexArray = new JSONArray();
            boolean wasInSection = false;
            for (Vertex vertex : rag.getGraph().getVertices()) {
                if (counter >= start && counter < end) {
                    wasInSection = true;
                    vertexArray.put(JSONWriter.createJSONElement(vertex, this.getReturnKeys(), this.hasShowTypes()));
                } else if (wasInSection) {
                    break;
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, vertexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, vertexArray.length());
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);
            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * GET http://host/graph/vertices/id
     * graph.getVertex(id);
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSingleVertex(@PathParam("graphname") String graphName, @PathParam("id") String id) {

        Vertex vertex = this.getRexsterApplicationGraph(graphName).getGraph().getVertex(id);
        if (null != vertex) {
            try {
                this.resultObject.put(Tokens.RESULTS, JSONWriter.createJSONElement(vertex, this.getReturnKeys(), this.hasShowTypes()));
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

                JSONArray extensionsList = getExtensionHypermedia(graphName, ExtensionPoint.VERTEX);
                if (extensionsList != null) {
                    this.resultObject.put(Tokens.EXTENSIONS, extensionsList);
                }

            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else {
            String msg = "Could not find vertex [" + id + "] on graph [" + graphName + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    @HEAD
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response headVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.HEAD);
    }

    @HEAD
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response headVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.HEAD);
    }

    @HEAD
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response headVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.HEAD);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response putVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @PUT
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response putVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.PUT);
    }

    @OPTIONS
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response optionsVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response optionsVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @OPTIONS
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response optionsVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.OPTIONS);
    }

    @DELETE
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.DELETE);
    }

    @DELETE
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.DELETE);
    }

    @DELETE
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response deleteVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.DELETE);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        this.setRequestObject(json);
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @POST
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response postVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.POST);
    }

    @GET
    @Path("/{id}/{extension: (?!outE)(?!bothE)(?!inE)(?!out)(?!both)(?!in).+}")
    public Response getVertexExtension(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        return this.executeVertexExtension(graphName, id, HttpMethod.GET);
    }

    private Response executeVertexExtension(String graphName, String id, HttpMethod httpMethodRequested) {

        Vertex vertex = this.getRexsterApplicationGraph(graphName).getGraph().getVertex(id);

        ExtensionResponse extResponse;
        ExtensionMethod methodToCall;
        ExtensionSegmentSet extensionSegmentSet = parseUriForExtensionSegment(graphName, ExtensionPoint.VERTEX);

        // determine if the namespace and extension are enabled for this graph
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        if (rag.isExtensionAllowed(extensionSegmentSet)) {

            Object returnValue = null;

            // namespace was allowed so try to run the extension
            try {

                // look for the extension as loaded through serviceloader
                List<RexsterExtension> rexsterExtensions = null;
                try {
                    rexsterExtensions = findExtensionClasses(extensionSegmentSet);
                } catch (ServiceConfigurationError sce) {
                    logger.error("ServiceLoader could not find a class referenced in com.tinkerpop.rexster.extension.RexsterExtension.");
                    JSONObject error = generateErrorObject(
                            "Class specified in com.tinkerpop.rexster.extension.RexsterExtension could not be found.",
                            sce);
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                if (rexsterExtensions == null && rexsterExtensions.size() == 0) {
                    // extension was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "]");
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                // look up the method on the extension that needs to be called.
                methodToCall = findExtensionMethod(rexsterExtensions, ExtensionPoint.VERTEX, extensionSegmentSet.getExtensionMethod(), httpMethodRequested);

                if (methodToCall == null) {
                    // extension method was not found for some reason
                    logger.error("The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "].  Check com.tinkerpop.rexster.extension.RexsterExtension file in META-INF.services.");
                    JSONObject error = generateErrorObject(
                            "The [" + extensionSegmentSet + "] extension was not found for [" + graphName + "] with a HTTP method of [" + httpMethodRequested.name() + "]");
                    throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
                }

                // found the method...time to do work
                returnValue = invokeExtension(graphName, methodToCall, vertex);

            } catch (WebApplicationException wae) {
                // already logged this...just throw it  up.
                throw wae;
            } catch (Exception ex) {
                logger.error("Dynamic invocation of the [" + extensionSegmentSet + "] extension failed.", ex);
                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

            if (returnValue instanceof ExtensionResponse) {
                extResponse = (ExtensionResponse) returnValue;

                if (extResponse.isErrorResponse()) {
                    // an error was raised within the extension.  pass it back out as an error.
                    logger.warn("The [" + extensionSegmentSet + "] extension raised an error response.");
                    throw new WebApplicationException(Response.fromResponse(extResponse.getJerseyResponse()).build());
                }
            } else {
                // extension method is not returning the correct type...needs to be an ExtensionResponse
                logger.error("The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                JSONObject error = generateErrorObject(
                        "The [" + extensionSegmentSet + "] extension does not return an ExtensionResponse.");
                throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }

        } else {
            // namespace was not allowed
            logger.error("The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            JSONObject error = generateErrorObject(
                    "The [" + extensionSegmentSet + "] extension was not configured for [" + graphName + "]");
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        String mediaType = MediaType.APPLICATION_JSON;
        if (methodToCall != null) {
            mediaType = methodToCall.getExtensionDefinition().produces();
            extResponse = tryAppendRexsterAttributesIfJson(extResponse, methodToCall, mediaType);
        }

        return Response.fromResponse(extResponse.getJerseyResponse()).type(mediaType).build();
    }

    /**
     * GET http://host/graph/vertices/id/direction
     * graph.getVertex(id).get{Direction}Edges();
     */
    @GET
    @Path("/{id}/{direction}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVertexEdges(@PathParam("graphname") String graphName, @PathParam("id") String vertexId, @PathParam("direction") String direction) {
        Vertex vertex = this.getRexsterApplicationGraph(graphName).getGraph().getVertex(vertexId);

        try {
            Long start = this.getStartOffset();
            Long end = this.getEndOffset();

            long counter = 0l;
            JSONArray edgeArray = new JSONArray();

            if (null != vertex) {
                JSONObject tempRequest = this.getNonRexsterRequest();
                if (direction.equals(Tokens.OUT_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(JSONWriter.createJSONElement(edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }

                if (direction.equals(Tokens.IN_E) || direction.equals(Tokens.BOTH_E)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(JSONWriter.createJSONElement(edge, this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }

                if (direction.equals(Tokens.OUT) || direction.equals(Tokens.BOTH)) {
                    for (Edge edge : vertex.getOutEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(JSONWriter.createJSONElement(edge.getInVertex(), this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }

                if (direction.equals(Tokens.IN) || direction.equals(Tokens.BOTH)) {
                    for (Edge edge : vertex.getInEdges()) {
                        if (this.hasPropertyValues(edge, tempRequest)) {
                            if (counter >= start && counter < end) {
                                edgeArray.put(JSONWriter.createJSONElement(edge.getOutVertex(), this.getReturnKeys(), this.hasShowTypes()));
                            }
                            counter++;
                        }
                    }
                }


            } else {
                String msg = "Could not find vertex [" + vertexId + "] on graph [" + graphName + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            this.resultObject.put(Tokens.RESULTS, edgeArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * POST http://host/graph/vertices
     * graph.addVertex(null);
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postNullVertex(@PathParam("graphname") String graphName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.postVertex(graphName, null);
    }

    /**
     * POST http://host/graph/vertices
     * graph.addVertex(null);
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postNullVertex(@PathParam("graphname") String graphName, JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.postVertex(graphName, null);
    }

    /**
     * POST http://host/graph/vertices
     * graph.addVertex(null);
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response postNullVertex(@PathParam("graphname") String graphName) {
        return this.postVertex(graphName, null);
    }

    /**
     * POST http://host/graph/vertices/id
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postVertex(@PathParam("graphname") String graphName, @PathParam("id") String id, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.postVertex(graphName, id);
    }

    /**
     * POST http://host/graph/vertices/id
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postVertex(@PathParam("graphname") String graphName, @PathParam("id") String id, JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.postVertex(graphName, id);
    }

    /**
     * POST http://host/graph/vertices/id?key=value
     * Vertex v = graph.addVertex(id);
     * v.setProperty(key,value);
     */
    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response postVertex(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        try {
            rag.tryStartTransaction();
            Vertex vertex = graph.getVertex(id);

            if (null == vertex) {
                vertex = graph.addVertex(id);
            } else {
                if (!this.hasElementProperties(this.getRequestObject())) {
                    JSONObject error = generateErrorObjectJsonFail(new Exception("Vertex with id " + id + " already exists"));
                    throw new WebApplicationException(Response.status(Status.CONFLICT).entity(error).build());
                }
            }

            Iterator keys = this.getRequestObject().keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    vertex.setProperty(key, this.getTypedPropertyValue(this.getRequestObject().getString(key)));
                }
            }

            rag.tryStopTransactionSuccess();

            this.resultObject.put(Tokens.RESULTS, JSONWriter.createJSONElement(vertex, this.getReturnKeys(), this.hasShowTypes()));
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {
            rag.tryStopTransactionFailure();

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {
            rag.tryStopTransactionFailure();

            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * DELETE http://host/graph/vertices/id
     * graph.removeVertex(graph.getVertex(id));
     * <p/>
     * DELETE http://host/graph/vertices/id?key1&key2
     * Vertex v = graph.getVertex(id);
     * v.removeProperty(key1);
     * v.removeProperty(key2);
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVertex(@PathParam("graphname") String graphName, @PathParam("id") String id) {
        final RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);
        final Graph graph = rag.getGraph();

        rag.tryStartTransaction();

        try {
            final List<String> keys = this.getNonRexsterRequestKeys();
            final Vertex vertex = graph.getVertex(id);
            if (null != vertex) {
                if (keys.size() > 0) {
                    // delete vertex properites
                    for (final String key : keys) {
                        vertex.removeProperty(key);
                    }
                } else {
                    // delete vertex
                    graph.removeVertex(vertex);
                }
            } else {
                final String msg = "Could not find vertex [" + id + "] on graph [" + graphName + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Status.NOT_FOUND).entity(error).build());
            }

            rag.tryStopTransactionSuccess();
            this.resultObject.put(Tokens.QUERY_TIME, sh.stopWatch());
        } catch (JSONException ex) {

            rag.tryStopTransactionFailure();

            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        } catch (RuntimeException re) {

            rag.tryStopTransactionFailure();

            logger.error(re);

            JSONObject error = generateErrorObject(re.getMessage(), re);
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }
}
