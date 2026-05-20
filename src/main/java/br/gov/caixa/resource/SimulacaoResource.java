package br.gov.caixa.resource;

import br.gov.caixa.dto.SimulacaoRequest;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.service.SimulacaoService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/simulacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Simulações", description = "API de simulação de financiamentos")
public class SimulacaoResource {

    @Inject
    SimulacaoService service;

    @POST
    @Operation(summary = "Cria uma nova simulação de financiamento")
    @APIResponse(responseCode = "201", description = "Simulação criada com sucesso")
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos")
    public Response simular(@Valid SimulacaoRequest request) {
        SimulacaoResponse response = service.simular(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta uma simulação existente pelo ID")
    @APIResponse(responseCode = "200", description = "Simulação encontrada")
    @APIResponse(responseCode = "404", description = "Simulação não encontrada")
    public SimulacaoResponse buscarPorId(@PathParam("id") Long id) {
        return service.buscarPorId(id);
    }
}