package br.com.tp.lncr;

import br.com.tp.lncr.aws.lambda.model.RequestDTO;
import br.com.tp.lncr.aws.lambda.rules.AllowResourcesRules;
import br.com.tp.lncr.core.utils.security.AuthorizatedUtils;
import br.com.tp.lncr.aws.lambda.utils.SecretUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizer implements RequestHandler<APIGatewayV2CustomAuthorizerEvent, SimpleIAMPolicyResponse> {
    private static final Logger logger = LoggerFactory.getLogger(CustomAuthorizer.class);
    private static String secretKey;

    public CustomAuthorizer() {
        /**
         * Construtor padrão
         */
    }

    /**
     * Define a secret key manualmente (útil para testes)
     */
    public static void setSecretKey(String key) {
        secretKey = key;
    }

    private static String getSecretKey() {
        if (secretKey == null) {
            secretKey = SecretUtils.getAwsSecretValue();
        }
        return secretKey;
    }

    public SimpleIAMPolicyResponse handleRequest(final APIGatewayV2CustomAuthorizerEvent input, final Context context) {
        logger.info("Iniciando processo de autorizacao");
        printInput(input);

        Map<String, String> contextResponse = new HashMap<>();
        boolean isAuthorized = false;

        try {
            logger.info("Carregando regras de autorizacao");
            Map<String, Object> allowPathsRules = new AllowResourcesRules().read();
            logger.info("Processando requisição de autorizacao");
            RequestDTO requestDTO = new RequestDTO(input, getSecretKey());

            logger.info("Verificando autorizacao para - Método: {}, Path: {}, Escopo: {}",
                requestDTO.getHttpMethod(),
                requestDTO.getResource(),
                requestDTO.getTokenClaims().getScope());

            isAuthorized = AuthorizatedUtils.isAuthorizedResult(allowPathsRules,
                    requestDTO.getTokenClaims().getScope(),
                    requestDTO.getHttpMethod(),
                    requestDTO.getResource());

            logger.info("Resultado da autorizacao: {}", isAuthorized ? "AUTORIZADO" : "NÃO AUTORIZADO");

            contextResponse.put("resource", requestDTO.getResource());
            contextResponse.put("httpMethod", requestDTO.getHttpMethod());
            contextResponse.put("scope", requestDTO.getTokenClaims().getScope());
            SimpleIAMPolicyResponse response = new SimpleIAMPolicyResponse(isAuthorized, contextResponse);
            logger.info("Resposta de autorizacao gerada: {}", response);
            return response;

        } catch (Exception e) {
            logger.error("Erro durante o processo de autorizacao", e);
            SimpleIAMPolicyResponse response = new SimpleIAMPolicyResponse(isAuthorized, contextResponse);
            logger.info("Resposta de autorizacao gerada: {}", response);
            return response;
        }
    }

    public static void printInput(APIGatewayV2CustomAuthorizerEvent input) {
        logger.info("=== APIGatewayV2CustomAuthorizerEvent Attributes ===");

        if (input == null) {
            logger.info("Input event is null");
            return;
        }

        printBasicRequestInfo(input);
        logListOrNone("Cookies", input.getCookies());
        logMapOrNone("Headers", input.getHeaders());
        logMapOrNone("Query String Parameters", input.getQueryStringParameters());
        logMapOrNone("Path Parameters", input.getPathParameters());
        logMapOrNone("Stage Variables", input.getStageVariables());
        printRequestContext(input);
        logListOrNone("Identity Source", input.getIdentitySource());

        logger.info("=== End of APIGatewayV2CustomAuthorizerEvent Attributes ===");
    }

    private static void printBasicRequestInfo(APIGatewayV2CustomAuthorizerEvent input) {
        logger.info("Type: {}", input.getType());
        logger.info("Version: {}", input.getVersion());
        logger.info("Route Key: {}", input.getRouteKey());
        logger.info("Raw Path: {}", input.getRawPath());
        logger.info("Raw Query String: {}", input.getRawQueryString());
    }

    private static void logListOrNone(String label, java.util.List<?> list) {
        if (list != null && !list.isEmpty()) {
            logger.info("{}: {}", label, list);
        } else {
            logger.info("{}: none", label);
        }
    }

    private static void logMapOrNone(String label, Map<String, ?> map) {
        if (map != null && !map.isEmpty()) {
            logger.info("{}:", label);
            map.forEach((key, value) -> logger.info("  {}: {}", key, value));
        } else {
            logger.info("{}: none", label);
        }
    }

    private static void printRequestContext(APIGatewayV2CustomAuthorizerEvent input) {
        APIGatewayV2CustomAuthorizerEvent.RequestContext context = input.getRequestContext();
        if (context == null) {
            logger.info("Request Context: null");
            return;
        }

        logger.info("Request Context:");
        logger.info("  Account ID: {}", context.getAccountId());
        logger.info("  API ID: {}", context.getApiId());
        logger.info("  Domain Name: {}", context.getDomainName());
        logger.info("  Domain Prefix: {}", context.getDomainPrefix());
        logger.info("  Request ID: {}", context.getRequestId());
        logger.info("  Route Key: {}", context.getRouteKey());
        logger.info("  Stage: {}", context.getStage());

        String httpContext = context.getHttp() != null ? context.getHttp().toString() : "null";
        logger.info("  HTTP Context: {}", httpContext);
    }

}
