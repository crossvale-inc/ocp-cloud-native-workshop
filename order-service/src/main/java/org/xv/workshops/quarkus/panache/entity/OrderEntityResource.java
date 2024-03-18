package org.xv.workshops.quarkus.panache.entity;

import java.util.List;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("entity/orders")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class OrderEntityResource {

    private static final Logger LOGGER = Logger.getLogger(OrderEntityResource.class.getName());

    @Inject
    @Channel("order-placed")
    private Emitter<OrderEntity> orderPlacedEmitter;
    
    @GET
    public List<OrderEntity> get() {
        return OrderEntity.listAll(Sort.by("name"));
    }

    @GET
    @Path("{id}")
    public OrderEntity getSingle(Long id) {
        OrderEntity entity = OrderEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    public Response create(OrderEntity order) {
        
    	if (order.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        doCreate(order);

        LOGGER.info("Order persisted: " + order.id);
        
        //orderPlacedEmitter.send(order);

        LOGGER.info("Order placed: " + order.id);
        
        return Response.ok(order).status(201).build();
    }
    
    @Transactional
    public void doCreate(OrderEntity order) {
    	order.persist();
    }

    @PUT
    @Path("{id}")
    @Transactional
    public OrderEntity update(Long id, OrderEntity order) {
        if (order.name == null) {
            throw new WebApplicationException("Order Name was not set on request.", 422);
        }

        OrderEntity entity = OrderEntity.findById(id);

        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }

        entity.name = order.name;

        return entity;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(Long id) {
        OrderEntity entity = OrderEntity.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Order with id of " + id + " does not exist.", 404);
        }
        entity.delete();
        return Response.status(204).build();
    }

    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception exception) {
            LOGGER.error("Failed to handle request", exception);

            int code = 500;
            if (exception instanceof WebApplicationException) {
                code = ((WebApplicationException) exception).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);

            if (exception.getMessage() != null) {
                exceptionJson.put("error", exception.getMessage());
            }

            return Response.status(code)
                    .entity(exceptionJson)
                    .build();
        }

    }
}