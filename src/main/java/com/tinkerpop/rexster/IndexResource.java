package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.pgm.AutomaticIndex;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.json.JSONWriter;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@Path("/{graphname}/indices")
public class IndexResource extends AbstractSubResource {

    private static Logger logger = Logger.getLogger(EdgeResource.class);

    public IndexResource() {
        super(null);
    }

    public IndexResource(UriInfo ui, HttpServletRequest req, RexsterApplicationProvider rap) {
        super(rap);
        this.httpServletRequest = req;
        this.uriInfo = ui;
    }

    /**
     * GET http://host/graph/indices
     * get.getIndices();
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllIndices(@PathParam("graphname") String graphName) {
        RexsterApplicationGraph rag = this.getRexsterApplicationGraph(graphName);

        IndexableGraph idxGraph = null;
        if (rag.getGraph() instanceof IndexableGraph) {
            idxGraph = (IndexableGraph) rag.getGraph();
        }

        if (idxGraph == null) {
            JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        long counter = 0l;

        try {
            JSONArray indexArray = new JSONArray();
            for (Index index : idxGraph.getIndices()) {
                if (counter >= start && counter < end) {
                    indexArray.put(new IndexJSONObject(index));
                }
                counter++;
            }

            this.resultObject.put(Tokens.RESULTS, indexArray);
            this.resultObject.put(Tokens.TOTAL_SIZE, counter);
            this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

        } catch (JSONException ex) {
            logger.error(ex);

            JSONObject error = generateErrorObjectJsonFail(ex);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    /**
     * GET http://host/graph/indices/indexName?key=key1&value=value1
     * Index index = graph.getIndex(indexName,...);
     * index.get(key,value);
     */
    @GET
    @Path("/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getElementsFromIndex(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(graphName, indexName);

        String key = null;
        Object value = null;

        Object temp = this.getRequestObject().opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();

        temp = this.getRequestObject().opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());


        Long start = this.getStartOffset();
        Long end = this.getEndOffset();

        long counter = 0l;


        if (null != index && key != null && value != null) {
            try {
                JSONArray elementArray = new JSONArray();
                for (Element element : (Iterable<Element>) index.get(key, value)) {
                    if (counter >= start && counter < end) {
                        elementArray.put(JSONWriter.createJSONElement(element, this.getReturnKeys(), this.hasShowTypes()));
                    }
                    counter++;
                }

                this.resultObject.put(Tokens.RESULTS, elementArray);
                this.resultObject.put(Tokens.TOTAL_SIZE, counter);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            // return info about the index itself
            HashMap map = new HashMap();
            map.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            try {
                IndexJSONObject indexJSONObject = new IndexJSONObject(index);
                map.put(Tokens.RESULTS, indexJSONObject);
            } catch (JSONException jsone) {

            }

            this.resultObject = new JSONObject(map);
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * GET http://host/graph/indices/indexName/keys
     * AutomaticIndex index = (AutomaticIndex) graph.getIndex(indexName,...);
     * index.getAutoIndexKeys();
     */
    @GET
    @Path("/{indexName}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAutomaticIndexKeys(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(graphName, indexName);

        if (null != index && index.getIndexType().equals(Index.Type.AUTOMATIC)) {
            try {
                JSONArray keyArray = new JSONArray();
                if (null == ((AutomaticIndex) index).getAutoIndexKeys()) {
                    keyArray.put((String) null);
                } else {
                    for (String key : ((Set<String>) ((AutomaticIndex) index).getAutoIndexKeys())) {
                        keyArray.put(key);
                    }
                }

                this.resultObject.put(Tokens.RESULTS, keyArray);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "Only automatic indices have user provided keys";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * GET http://host/graph/indices/indexName/count?key=?&value=?
     */
    @GET
    @Path("/{indexName}/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIndexCount(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName) {
        Index index = this.getIndexFromGraph(graphName, indexName);

        String key = null;
        Object value = null;

        Object temp = this.getRequestObject().opt(Tokens.KEY);
        if (temp != null) {
            key = temp.toString();
        }

        temp = this.getRequestObject().opt(Tokens.VALUE);
        if (temp != null) {
            value = getTypedPropertyValue(temp.toString());
        }

        if (index != null && key != null && value != null) {
            try {
                long count = index.count(key, value);

                this.resultObject.put(Tokens.TOTAL_SIZE, count);
                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key and value must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    /**
     * DELETE http://host/graph/indices/indexName
     * graph.dropIndex(indexName);
     * <p/>
     * DELETE http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...)
     * index.remove(key, value, graph.getVertex(id1));
     */
    @DELETE
    @Path("/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteIndex(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(graphName, indexName);
        final IndexableGraph graph = (IndexableGraph) this.getRexsterApplicationGraph(graphName).getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String clazz = null;

        Object temp = this.getRequestObject().opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = this.getRequestObject().opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());
        temp = this.getRequestObject().opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();
        temp = this.getRequestObject().opt(Tokens.CLASS);
        if (null != temp)
            clazz = temp.toString();

        if (key == null && value == null && id == null && clazz == null) {
            graph.dropIndex(indexName);
        } else if (null != index & key != null && value != null && clazz != null && id != null) {
            try {
                if (clazz.equals(Tokens.VERTEX))
                    index.remove(key, value, graph.getVertex(id));
                else
                    index.remove(key, value, graph.getEdge(id));

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());


            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null == index) {
            String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to lookup elements in an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();

    }

    /**
     * POST http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...);
     * index.put(key,value,graph.getVertex(id1));
     * <p/>
     * POST http://host/graph/indices/indexName?class=vertex&type=automatic&keys=[name,age]
     * graph.createIndex(indexName,Vertex.class,AUTOMATIC, {name, age})
     */
    @POST
    @Path("/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response putElementInIndexOrCreateIndex(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName, MultivaluedMap<String, String> formParams) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.buildRequestObject(formParams);
        return this.putElementInIndexOrCreateIndex(graphName, indexName);

    }

    /**
     * POST http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...);
     * index.put(key,value,graph.getVertex(id1));
     * <p/>
     * POST http://host/graph/indices/indexName?class=vertex&type=automatic&keys=[name,age]
     * graph.createIndex(indexName,Vertex.class,AUTOMATIC, {name, age})
     */
    @POST
    @Path("/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putElementInIndexOrCreateIndex(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName, JSONObject json) {
        // initializes the request object with the data POSTed to the resource.  URI parameters
        // will then be ignored when the getRequestObject is called as the request object will
        // have already been established.
        this.setRequestObject(json);
        return this.putElementInIndexOrCreateIndex(graphName, indexName);
    }

    /**
     * POST http://host/graph/indices/indexName?key=key1&value=value1&class=vertex&id=id1
     * Index index = graph.getIndex(indexName,...);
     * index.put(key,value,graph.getVertex(id1));
     * <p/>
     * POST http://host/graph/indices/indexName?class=vertex&type=automatic&keys=[name,age]
     * graph.createIndex(indexName,Vertex.class,AUTOMATIC, {name, age})
     */
    @POST
    @Path("/{indexName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response putElementInIndexOrCreateIndex(@PathParam("graphname") String graphName, @PathParam("indexName") String indexName) {
        final Index index = this.getIndexFromGraph(graphName, indexName);
        final IndexableGraph graph = (IndexableGraph) this.getRexsterApplicationGraph(graphName).getGraph();

        String key = null;
        Object value = null;
        String id = null;
        String clazz = null;
        String type = null;
        Set<String> keys = null;

        Object temp = this.getRequestObject().opt(Tokens.KEY);
        if (null != temp)
            key = temp.toString();
        temp = this.getRequestObject().opt(Tokens.VALUE);
        if (null != temp)
            value = getTypedPropertyValue(temp.toString());
        temp = this.getRequestObject().opt(Tokens.ID);
        if (null != temp)
            id = temp.toString();
        temp = this.getRequestObject().opt(Tokens.CLASS);
        if (null != temp)
            clazz = temp.toString();
        temp = this.getRequestObject().opt(Tokens.TYPE);
        if (null != temp)
            type = temp.toString();
        temp = this.getRequestObject().opt(Tokens.KEYS);
        if (null != temp) {
            try {
                JSONArray ks;
                if (temp instanceof String) {
                    ks = new JSONArray();
                    ks.put(temp);
                } else {
                    ks = (JSONArray) temp;
                }

                keys = new HashSet<String>();
                for (int i = 0; i < ks.length(); i++) {
                    keys.add(ks.getString(i));
                }
            } catch (Exception e) {
                JSONObject error = generateErrorObject("Automatic index keys must be in an array: " + temp);
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
            }
        } else {
            keys = null;
        }


        if (null != index & key != null && value != null && clazz != null && id != null) {
            try {
                if (clazz.equals(Tokens.VERTEX))
                    index.put(key, value, graph.getVertex(id));
                else if (clazz.equals(Tokens.EDGE))
                    index.put(key, value, graph.getEdge(id));
                else {
                    JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }

                this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());

            } catch (JSONException ex) {
                logger.error(ex);

                JSONObject error = generateErrorObjectJsonFail(ex);
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
            }
        } else if (null != index && null != type && null != clazz) {
            String msg = "Index [" + indexName + "] on graph [" + graphName + "] already exists";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
        } else if (null == index) {


            if (null != type && null != clazz) {
                Index.Type t;
                Class c;
                if (type.equals(Index.Type.AUTOMATIC.toString().toLowerCase()))
                    t = Index.Type.AUTOMATIC;
                else if (type.equals(Index.Type.MANUAL.toString().toLowerCase()))
                    t = Index.Type.MANUAL;
                else {
                    JSONObject error = generateErrorObject("Index type must be either automatic or manual");
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }

                if (clazz.equals(Tokens.VERTEX))
                    c = Vertex.class;
                else if (clazz.equals(Tokens.EDGE))
                    c = Edge.class;
                else {
                    JSONObject error = generateErrorObject("Index class must be either vertex or edge");
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }

                Index i;
                try {
                    if (t == Index.Type.MANUAL)
                        i = graph.createManualIndex(indexName, c);
                    else
                        i = graph.createAutomaticIndex(indexName, c, keys);
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    JSONObject error = generateErrorObject(e.getMessage());
                    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
                }
                try {
                    this.resultObject.put(Tokens.QUERY_TIME, this.sh.stopWatch());
                    this.resultObject.put(Tokens.RESULTS, new IndexJSONObject(i));
                } catch (JSONException ex) {
                    logger.error(ex);

                    JSONObject error = generateErrorObjectJsonFail(ex);
                    throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
                }


            } else {
                String msg = "Could not find index [" + indexName + "] on graph [" + graphName + "]";
                logger.info(msg);

                JSONObject error = generateErrorObject(msg);
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(error).build());
            }
        } else {
            String msg = "A key, value, id, and type (vertex/edge) must be provided to add elements to an index";
            logger.info(msg);

            JSONObject error = generateErrorObject(msg);
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }

        return Response.ok(this.resultObject).build();
    }

    private Index getIndexFromGraph(String graphName, final String name) {

        IndexableGraph idxGraph = null;
        if (this.getRexsterApplicationGraph(graphName).getGraph() instanceof IndexableGraph) {
            idxGraph = (IndexableGraph) this.getRexsterApplicationGraph(graphName).getGraph();
        }

        if (idxGraph == null) {
            JSONObject error = this.generateErrorObject("The requested graph is not of type IndexableGraph.");
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build());
        }


        for (final Index index : idxGraph.getIndices()) {
            if (index.getIndexName().equals(name))
                return index;
        }

        return null;
    }

}