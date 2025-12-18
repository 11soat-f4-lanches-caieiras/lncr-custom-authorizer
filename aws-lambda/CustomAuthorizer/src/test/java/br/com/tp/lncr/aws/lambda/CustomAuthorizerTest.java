package br.com.tp.lncr.aws.lambda;

import br.com.tp.lncr.CustomAuthorizer;
import br.com.tp.lncr.aws.lambda.rules.AllowResourcesRules;
import br.com.tp.lncr.core.utils.security.AuthorizatedUtils;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;

class CustomAuthorizerTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizerTest.class);

    private Map<String, Object> allowResourcesRules = new HashMap<>();
    private APIGatewayV2CustomAuthorizerEvent input;
    private CustomAuthorizer customAuthorizer;
    private final Context mockContext = null;
    private final String secretKey = "test-secret-key-for-unit-tests-minimum-256-bits-long-secret-key-value";

    @BeforeEach
    void setUp() {
        logger.info("Iniciando configuracao do teste");

        CustomAuthorizer.setSecretKey(secretKey);
        customAuthorizer = new CustomAuthorizer();
        input = new APIGatewayV2CustomAuthorizerEvent();
        allowResourcesRules = new AllowResourcesRules().read();
        logger.info("Configuracao concluida");
    }

    @AfterEach
    void tearDown() {
        // Resetar a secretKey para null para testes que precisam testar o getSecretKey
        CustomAuthorizer.setSecretKey(secretKey);
    }

    @ParameterizedTest(name = "Teste {index}: Escopo={0}, Método={1}, Path={2}")
    @MethodSource("provideTestArguments")
    @DisplayName("Validação individual de recursos permitidos")
    void validateIndividualResource(String scope, String method, String path) {
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
    void validateAllowedsResourceSummary() {
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
        input.setHeaders(Map.of("authorization", "Bearer " + token));
        input.setRouteKey(path);
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

    @Test
    @DisplayName("Teste getSecretKey quando secretKey é null - deve buscar do AWS Secrets Manager")
    void testGetSecretKeyWhenNull() {
        logger.info("Testando getSecretKey quando secretKey é null");

        // Resetar para null para forçar a busca do AWS Secrets Manager
        CustomAuthorizer.setSecretKey(null);

        // Criar um novo autorizador que tentará buscar a secret
        CustomAuthorizer newAuthorizer = new CustomAuthorizer();

        // Configurar um input válido com token mock
        String mockToken = createValidTestToken("admin");
        input.setVersion("2.0");
        input.setType("REQUEST");
        input.setHeaders(Map.of("authorization", "Bearer " + mockToken));
        input.setRouteKey("/customers");
        input.setRequestContext(new APIGatewayV2CustomAuthorizerEvent.RequestContext());
        input.getRequestContext().setHttp(new APIGatewayV2CustomAuthorizerEvent.Http());
        input.getRequestContext().getHttp().setMethod("GET");

        // Como não temos acesso ao AWS em testes, isso deve gerar uma exceção
        // mas o código deve lidar com isso e retornar uma resposta não autorizada
        SimpleIAMPolicyResponse response = newAuthorizer.handleRequest(input, mockContext);

        // Verificar que a resposta não é nula (mesmo em caso de erro)
        assertNotNull(response, "Resposta não deve ser nula mesmo com erro");
        assertFalse(response.getIsAuthorized(), "Deve retornar não autorizado em caso de erro ao buscar secret");

        // Restaurar a secretKey para os próximos testes
        CustomAuthorizer.setSecretKey(secretKey);

        logger.info("Teste de getSecretKey concluído");
    }

    @Test
    @DisplayName("Teste exception handler - linhas 69-73 - quando ocorre erro no processamento")
    void testExceptionHandlerInHandleRequest() {
        logger.info("Testando exception handler quando ocorre erro no processamento");

        // Criar input inválido para forçar uma exceção (sem token)
        input.setVersion("2.0");
        input.setType("REQUEST");
        input.setHeaders(new HashMap<>()); // Headers vazios, sem token
        input.setRouteKey("/customers");
        input.setRequestContext(new APIGatewayV2CustomAuthorizerEvent.RequestContext());
        input.getRequestContext().setHttp(new APIGatewayV2CustomAuthorizerEvent.Http());
        input.getRequestContext().getHttp().setMethod("GET");

        // Executar e verificar que o erro é tratado adequadamente
        SimpleIAMPolicyResponse response = customAuthorizer.handleRequest(input, mockContext);

        // Verificar que a resposta não é nula
        assertNotNull(response, "Resposta não deve ser nula mesmo com exceção");

        // Verificar que retorna não autorizado em caso de erro
        assertFalse(response.getIsAuthorized(), "Deve retornar não autorizado em caso de exceção");

        // Verificar que o contexto está vazio ou tem valores padrão
        assertNotNull(response.getContext(), "Contexto não deve ser nulo");

        logger.info("Teste de exception handler concluído");
    }

    @Test
    @DisplayName("Teste printInput com input null - linhas 80-82")
    void testPrintInputWithNull() {
        logger.info("Testando printInput com input null");

        // Chamar printInput com null não deve lançar exceção
        assertDoesNotThrow(() -> CustomAuthorizer.printInput(null),
            "printInput deve lidar com input null sem lançar exceção");

        logger.info("Teste de printInput com null concluído");
    }

    @Test
    @DisplayName("Teste printInput com input válido completo")
    void testPrintInputWithValidInput() {
        logger.info("Testando printInput com input válido e completo");

        // Criar input com todos os campos preenchidos
        APIGatewayV2CustomAuthorizerEvent fullInput = new APIGatewayV2CustomAuthorizerEvent();
        fullInput.setVersion("2.0");
        fullInput.setType("REQUEST");
        fullInput.setRouteKey("/customers");
        fullInput.setRawPath("/customers");
        fullInput.setRawQueryString("param1=value1");

        // Adicionar cookies (para testar logListOrNone com lista não vazia - linha 106-107)
        List<String> cookies = new ArrayList<>();
        cookies.add("sessionId=abc123");
        cookies.add("token=xyz789");
        fullInput.setCookies(cookies);

        // Adicionar headers (para testar logMapOrNone com map não vazio - linhas 124-127)
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer token123");
        headers.put("content-type", "application/json");
        fullInput.setHeaders(headers);

        // Adicionar query parameters
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("param1", "value1");
        queryParams.put("param2", "value2");
        fullInput.setQueryStringParameters(queryParams);

        // Adicionar path parameters
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("id", "123");
        fullInput.setPathParameters(pathParams);

        // Adicionar stage variables
        Map<String, String> stageVars = new HashMap<>();
        stageVars.put("stage", "prod");
        fullInput.setStageVariables(stageVars);

        // Adicionar identity source
        List<String> identitySource = new ArrayList<>();
        identitySource.add("source1");
        fullInput.setIdentitySource(identitySource);

        // Adicionar request context completo
        APIGatewayV2CustomAuthorizerEvent.RequestContext context = new APIGatewayV2CustomAuthorizerEvent.RequestContext();
        context.setAccountId("123456789");
        context.setApiId("api123");
        context.setDomainName("api.example.com");
        context.setDomainPrefix("api");
        context.setRequestId("req-123");
        context.setRouteKey("/customers");
        context.setStage("prod");

        APIGatewayV2CustomAuthorizerEvent.Http http = new APIGatewayV2CustomAuthorizerEvent.Http();
        http.setMethod("GET");
        http.setPath("/customers");
        context.setHttp(http);

        fullInput.setRequestContext(context);

        // Chamar printInput não deve lançar exceção
        assertDoesNotThrow(() -> CustomAuthorizer.printInput(fullInput),
            "printInput deve processar input completo sem lançar exceção");

        logger.info("Teste de printInput com input válido concluído");
    }

    @Test
    @DisplayName("Teste printInput com cookies e headers vazios")
    void testPrintInputWithEmptyCollections() {
        logger.info("Testando printInput com coleções vazias");

        APIGatewayV2CustomAuthorizerEvent emptyInput = new APIGatewayV2CustomAuthorizerEvent();
        emptyInput.setVersion("2.0");
        emptyInput.setType("REQUEST");
        emptyInput.setRouteKey("/test");

        // Definir coleções vazias
        emptyInput.setCookies(new ArrayList<>());
        emptyInput.setHeaders(new HashMap<>());
        emptyInput.setQueryStringParameters(new HashMap<>());
        emptyInput.setPathParameters(new HashMap<>());
        emptyInput.setStageVariables(new HashMap<>());
        emptyInput.setIdentitySource(new ArrayList<>());

        // Chamar printInput não deve lançar exceção
        assertDoesNotThrow(() -> CustomAuthorizer.printInput(emptyInput),
            "printInput deve processar coleções vazias sem lançar exceção");

        logger.info("Teste de printInput com coleções vazias concluído");
    }

    @Test
    @DisplayName("Teste printRequestContext com context null - linha 138")
    void testPrintRequestContextWithNull() {
        logger.info("Testando printRequestContext com context null");

        APIGatewayV2CustomAuthorizerEvent inputWithNullContext = new APIGatewayV2CustomAuthorizerEvent();
        inputWithNullContext.setVersion("2.0");
        inputWithNullContext.setType("REQUEST");
        inputWithNullContext.setRouteKey("/test");
        inputWithNullContext.setRequestContext(null); // Context null

        // Chamar printInput (que chama printRequestContext internamente) não deve lançar exceção
        assertDoesNotThrow(() -> CustomAuthorizer.printInput(inputWithNullContext),
            "printInput deve lidar com requestContext null sem lançar exceção");

        logger.info("Teste de printRequestContext com null concluído");
    }

    @Test
    @DisplayName("Teste printRequestContext com http null")
    void testPrintRequestContextWithNullHttp() {
        logger.info("Testando printRequestContext com http null");

        APIGatewayV2CustomAuthorizerEvent inputWithNullHttp = new APIGatewayV2CustomAuthorizerEvent();
        inputWithNullHttp.setVersion("2.0");
        inputWithNullHttp.setType("REQUEST");
        inputWithNullHttp.setRouteKey("/test");

        APIGatewayV2CustomAuthorizerEvent.RequestContext context = new APIGatewayV2CustomAuthorizerEvent.RequestContext();
        context.setAccountId("123456789");
        context.setHttp(null); // Http null
        inputWithNullHttp.setRequestContext(context);

        // Chamar printInput não deve lançar exceção
        assertDoesNotThrow(() -> CustomAuthorizer.printInput(inputWithNullHttp),
            "printInput deve lidar com http null sem lançar exceção");

        logger.info("Teste de printRequestContext com http null concluído");
    }

    @Test
    @DisplayName("Teste handleRequest com token expirado - força exception")
    void testHandleRequestWithExpiredToken() {
        logger.info("Testando handleRequest com token expirado");

        // Criar token expirado
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String expiredToken = com.auth0.jwt.JWT.create()
                .withSubject("lncr-token")
                .withClaim("scope", "admin")
                .withExpiresAt(new java.util.Date(System.currentTimeMillis() - 3600000)) // Expirado há 1 hora
                .sign(algorithm);

        input.setVersion("2.0");
        input.setType("REQUEST");
        input.setHeaders(Map.of("authorization", "Bearer " + expiredToken));
        input.setRouteKey("/customers");
        input.setRequestContext(new APIGatewayV2CustomAuthorizerEvent.RequestContext());
        input.getRequestContext().setHttp(new APIGatewayV2CustomAuthorizerEvent.Http());
        input.getRequestContext().getHttp().setMethod("GET");

        // Executar - deve retornar não autorizado devido ao token expirado
        SimpleIAMPolicyResponse response = customAuthorizer.handleRequest(input, mockContext);

        assertNotNull(response, "Resposta não deve ser nula");
        assertFalse(response.getIsAuthorized(), "Deve retornar não autorizado com token expirado");

        logger.info("Teste com token expirado concluído");
    }

    @Test
    @DisplayName("Teste handleRequest com token inválido - força exception")
    void testHandleRequestWithInvalidToken() {
        logger.info("Testando handleRequest com token inválido");

        input.setVersion("2.0");
        input.setType("REQUEST");
        input.setHeaders(Map.of("authorization", "Bearer token-invalido-sem-estrutura-jwt"));
        input.setRouteKey("/customers");
        input.setRequestContext(new APIGatewayV2CustomAuthorizerEvent.RequestContext());
        input.getRequestContext().setHttp(new APIGatewayV2CustomAuthorizerEvent.Http());
        input.getRequestContext().getHttp().setMethod("GET");

        // Executar - deve retornar não autorizado devido ao token inválido
        SimpleIAMPolicyResponse response = customAuthorizer.handleRequest(input, mockContext);

        assertNotNull(response, "Resposta não deve ser nula");
        assertFalse(response.getIsAuthorized(), "Deve retornar não autorizado com token inválido");

        logger.info("Teste com token inválido concluído");
    }
}
