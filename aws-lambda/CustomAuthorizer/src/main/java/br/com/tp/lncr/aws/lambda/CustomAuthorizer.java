package br.com.tp.lncr.aws.lambda;

import br.com.tp.lncr.aws.lambda.model.RequestDTO;
import br.com.tp.lncr.aws.lambda.rules.AllowResourcesRules;
import br.com.tp.lncr.core.commons.utils.security.AuthorizatedUtils;
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
    private static final String SECRET_KEY = SecretUtils.getAwsSecretValue();

    public SimpleIAMPolicyResponse handleRequest(final APIGatewayV2CustomAuthorizerEvent input, final Context context) {
        logger.info("Iniciando processo de autorizacao");
        logger.info("input: {}", input.toString());

        Map<String, String> contextResponse = new HashMap<>();
        Boolean isAuthorized = false;

        try {
            logger.info("Carregando regras de autorizacao");
            Map<String, Object> allowPathsRules = new AllowResourcesRules().read();
            logger.info("Processando requisição de autorizacao");
            RequestDTO requestDTO = new RequestDTO(input,SECRET_KEY);

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

}
