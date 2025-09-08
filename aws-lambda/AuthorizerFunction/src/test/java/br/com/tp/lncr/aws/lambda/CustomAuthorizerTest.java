package br.com.tp.lncr.aws.lambda;

import br.com.tp.lncr.aws.lambda.rules.AllowResourcesRules;
import br.com.tp.lncr.aws.lambda.utils.AuthorizatedUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;
import com.amazonaws.services.lambda.runtime.events.SimpleIAMPolicyResponse;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomAuthorizerTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizerTest.class);

    private Map<String, Object> allowResourcesRules = new HashMap<>();
    private APIGatewayV2CustomAuthorizerEvent input;
    private CustomAuthorizer customAuthorizer;
    private final Context mockContext = null;

    @BeforeEach
    public void setUp() {
        logger.info("Iniciando configuracao do teste");
        customAuthorizer = new CustomAuthorizer();
        input = new APIGatewayV2CustomAuthorizerEvent();
        allowResourcesRules = new AllowResourcesRules().read();
        logger.info("Configuracao concluida");
    }

    @ParameterizedTest(name = "Teste {index}: Escopo={0}, Método={1}, Path={2}")
    @MethodSource("provideTestArguments")
    @DisplayName("Validação individual de recursos permitidos")
    public void validateIndividualResource(String scope, String method, String path) {
        logger.info("Executando teste individual - Escopo: {}, Método: {}, Path: {}", scope, method, path);

        Boolean expectedResult = AuthorizatedUtils.isAuthorizedResult(allowResourcesRules, scope, method, path);

        logger.debug("Resultado esperado para {} {} com escopo {}: {}", method, path, scope, expectedResult);

        try {
            input = setInput(scope, method, path);
            SimpleIAMPolicyResponse result = customAuthorizer.handleRequest(input, mockContext);

            assertNotNull(result, "Resultado não deve ser nulo");

            assertEquals(expectedResult, result.getIsAuthorized(),
                    String.format("Falha na autorização para escopo: %s, método: %s, path: %s", scope, method, path));
            assertEquals(scope, result.getContext().get("scope"),
                    String.format("Escopo incorreto no contexto para escopo: %s, método: %s, path: %s", scope, method, path));
            assertEquals(method, result.getContext().get("httpMethod"),
                    String.format("Método HTTP incorreto no contexto para escopo: %s, método: %s, path: %s", scope, method, path));
            assertEquals(path, result.getContext().get("resource"),
                    String.format("Recurso incorreto no contexto para escopo: %s, método: %s, path: %s", scope, method, path));

            if (result.getIsAuthorized() == expectedResult) {
                logger.info("✓ Teste passou para {} {} com escopo {}", method, path, scope);
            } else {
                logger.warn("✗ Teste falhou para {} {} com escopo {}", method, path, scope);
            }

        } catch (Exception e) {
            logger.error("Erro durante teste para scope: {}, method: {}, path: {}", scope, method, path, e);
            throw new RuntimeException(String.format("Teste falhou devido a erro para escopo: %s, metodo: %s, path: %s - %s",
                    scope, method, path, e.getMessage()), e);
        }
    }

    @Test
    @DisplayName("Teste de resumo - Validação geral de recursos permitidos")
    public void validateAllowedsResourceSummary() {
        logger.info("Iniciando teste de resumo de validacao de recursos permitidos");

        List<String> scopes = allowResourcesRules.keySet().stream().toList();
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;

        for (String scope : scopes) {
            logger.info("Testando escopo: {}", scope);

            for (String[] route : TestListRoutes.getTestRoutes()) {
                String method = route[0];
                String path = route[1];
                totalTests++;

                Boolean expectedResult = AuthorizatedUtils.isAuthorizedResult(allowResourcesRules, scope, method, path);

                try {
                    input = setInput(scope, method, path);
                    SimpleIAMPolicyResponse result = customAuthorizer.handleRequest(input, mockContext);

                    assertNotNull(result, "Resultado não deve ser nulo");

                    if (result.getIsAuthorized() == expectedResult) {
                        passedTests++;
                        logger.debug("✓ Teste passou para {} {} com escopo {}", method, path, scope);
                    } else {
                        failedTests++;
                        logger.warn("✗ Teste falhou para {} {} com escopo {}", method, path, scope);
                    }

                } catch (Exception e) {
                    failedTests++;
                    logger.error("Erro durante teste para scope: {}, method: {}, path: {}", scope, method, path, e);
                }
            }
            logger.info("Concluído teste do escopo: {} ✓", scope);
        }

        logger.info("Resumo dos testes:");
        logger.info("Total de testes executados: {}", totalTests);
        logger.info("Testes que passaram: {}", passedTests);
        logger.info("Testes que falharam: {}", failedTests);
        logger.info("Taxa de sucesso: {}%", String.format("%.2f", (passedTests * 100.0 / totalTests)));

        assertEquals(0, failedTests,
                String.format("Alguns testes falharam. Total: %d, Passou: %d, Falhou: %d",
                        totalTests, passedTests, failedTests));
    }

    private String createValidTestToken(String scope) {
        try {
            String secretKey = System.getenv("LNCR_OAUTH_SECRET_KEY");
            if (secretKey == null || secretKey.trim().isEmpty()) {
                secretKey = "mysecretkey";
            }
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            return com.auth0.jwt.JWT.create()
                    .withSubject("lncr-token")
                    .withClaim("scope", scope)
                    .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000)) // 1 hora
                    .sign(algorithm);
        } catch (Exception e) {
            logger.error("Erro ao criar token de teste para scope: {}", scope, e);
            return null;
        }
    }

    public APIGatewayV2CustomAuthorizerEvent setInput(String scope, String method, String path) {
        String token = createValidTestToken(scope);

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Não foi possível criar token para o scope: " + scope);
        }

        input.setVersion("2.0");
        input.setType("REQUEST");
        input.setHeaders(Map.of("Authorization", "Bearer " + token));
        input.setRawPath(path);
        input.setRequestContext(new APIGatewayV2CustomAuthorizerEvent.RequestContext());
        input.getRequestContext().setHttp(new APIGatewayV2CustomAuthorizerEvent.Http());
        input.getRequestContext().getHttp().setMethod(method);

        return input;
    }

    private static Stream<Arguments> provideTestArguments() {
        Map<String, Object> rules = new AllowResourcesRules().read();
        List<String> scopes = rules.keySet().stream().toList();
        List<String[]> routes = TestListRoutes.getTestRoutes();

        logger.info("Gerando argumentos para {} escopos e {} rotas", scopes.size(), routes.size());

        return scopes.stream()
                .flatMap(scope ->
                        routes.stream()
                                .map(route -> Arguments.of(scope, route[0], route[1]))
                );
    }
}
