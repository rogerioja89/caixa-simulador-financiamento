package br.gov.caixa.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GlobalExceptionMapper {

    @ServerExceptionMapper
    public Response handleNotFound(SimulacaoNaoEncontradaException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("erro", e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @ServerExceptionMapper
    public Response handleConstraintViolation(ConstraintViolationException e) {
        List<String> erros = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("erros", erros))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
