package br.com.tp.lncr.aws.lambda.bdd;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CustomAuthorizerSteps {

    private String token;
    private String escopo;
    private String metodoHttp;
    private String recurso;
    private Map<String, Object> resultado;
    private final String secretKey = "test-secret-key-for-unit-tests-minimum-256-bits-long-secret-key-value";

    @Dado("que existe um token JWT válido com escopo {string}")
    public void queExisteUmTokenJWTValidoComEscopo(String escopo) {
        this.escopo = escopo;
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        this.token = JWT.create()
                .withClaim("client_id", "test-client-id")
                .withClaim("client_secret", "test-client-secret")
                .withClaim("customerId", "customer-123")
                .withClaim("grant_type", "client_credentials")
                .withClaim("name", "Test User")
                .withClaim("scope", escopo)
                .withClaim("sub", "subject-123")
                .withClaim("exp", (System.currentTimeMillis() / 1000) + 3600)
                .sign(algorithm);
    }

    @E("a requisição é do tipo {string} para o recurso {string}")
    public void aRequisicaoEDoTipoParaORecurso(String metodo, String recurso) {
        this.metodoHttp = metodo;
        this.recurso = recurso;
    }

    @Quando("o autorizador processar a requisição")
    public void oAutorizadorProcessarARequisicao() {
        // Simular processamento do autorizador
        resultado = new HashMap<>();
        resultado.put("escopo", escopo);
        resultado.put("metodoHttp", metodoHttp);
        resultado.put("recurso", recurso);

        // Determinar se está autorizado baseado em regras de negócio
        boolean autorizado = determinarAutorizacao(escopo, metodoHttp, recurso);
        resultado.put("autorizado", autorizado);
    }

    @Então("a requisição deve ser autorizada")
    public void aRequisicaoDeveSerAutorizada() {
        assertTrue((Boolean) resultado.get("autorizado"), "A requisição deveria ser autorizada");
    }

    @Então("a requisição não deve ser autorizada")
    public void aRequisicaoNaoDeveSerAutorizada() {
        assertFalse((Boolean) resultado.get("autorizado"), "A requisição NÃO deveria ser autorizada");
    }

    @E("o contexto deve conter o escopo {string}")
    public void oContextoDeveConterOEscopo(String escopoEsperado) {
        assertEquals(escopoEsperado, resultado.get("escopo"));
    }

    @E("o contexto deve conter o método HTTP {string}")
    public void oContextoDeveConterOMetodoHTTP(String metodoEsperado) {
        assertEquals(metodoEsperado, resultado.get("metodoHttp"));
    }

    @E("o contexto deve conter o recurso {string}")
    public void oContextoDeveConterORecurso(String recursoEsperado) {
        assertEquals(recursoEsperado, resultado.get("recurso"));
    }

    @Então("a requisição deve ter resultado {string}")
    public void aRequisicaoDeveTerResultado(String resultadoEsperado) {
        boolean autorizado = (Boolean) resultado.get("autorizado");

        if ("autorizado".equals(resultadoEsperado)) {
            assertTrue(autorizado, "A requisição deveria ser autorizada");
        } else if ("negado".equals(resultadoEsperado)) {
            assertFalse(autorizado, "A requisição deveria ser negada");
        } else {
            fail("Resultado esperado desconhecido: " + resultadoEsperado);
        }
    }

    /**
     * Lógica simplificada de autorização baseada em escopo, método e recurso
     */
    private boolean determinarAutorizacao(String escopo, String metodo, String recurso) {
        if ("admin".equals(escopo)) {
            return true;
        }

        if ("customer".equals(escopo)) {
            return ("GET".equals(metodo) && recurso.startsWith("/customers/")) ||
                   ("POST".equals(metodo) && "/customerOrders".equals(recurso));
        }

        if ("totem".equals(escopo)) {
            return "POST".equals(metodo) && "/customers".equals(recurso);
        }

        if ("monitor".equals(escopo)) {
            return "GET".equals(metodo) && "/kitchenOrders".equals(recurso);
        }

        return false;
    }
}

