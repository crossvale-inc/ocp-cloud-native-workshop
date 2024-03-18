package org.xv.workshops.quarkus.panache.entity;

import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Path("entity/prices")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class CalculatedPriceEntityResource {

    private static final Logger LOGGER = Logger.getLogger(CalculatedPriceEntityResource.class.getName());
    
    @GET
    public List<CalculatedPrice> get(@QueryParam("orderId") Long orderId) {
    	
    	if (null == orderId) {
            return CalculatedPrice.listAll(Sort.by("orderId"));
    	}
    	
        return CalculatedPrice.list("orderId", orderId);
    }

    @GET
    @Path("{id}")
    public CalculatedPrice getSingle(Long id) {
        CalculatedPrice entity = CalculatedPrice.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Price with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @POST
    @Transactional
    public Response create(CalculatedPrice price) {
        
    	if (price.id != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

    	price.persist();
        
        LOGGER.info("Price persisted: " + price.id);

        return Response.ok(price).status(201).build();
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