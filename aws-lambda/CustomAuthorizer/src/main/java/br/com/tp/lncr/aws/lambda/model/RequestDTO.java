package br.com.tp.lncr.aws.lambda.model;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;

import java.util.Map;

public class RequestDTO {
    private final String resource;
    private final String httpMethod;
    private final TokenClaims tokenClaims;

    public RequestDTO(APIGatewayV2CustomAuthorizerEvent input, String secretKey) {
        if (input.getHeaders() == null) {
            throw new IllegalArgumentException("Headers are missing");
        }

        String authHeader = null;
        for (Map.Entry<String, String> header : input.getHeaders().entrySet()) {
            if ("authorization".equalsIgnoreCase(header.getKey())) {
                authHeader = header.getValue();
                break;
            }
        }

        if (authHeader == null) {
            throw new IllegalArgumentException("Authorization header is missing");
        }

        this.httpMethod = input.getRequestContext().getHttp().getMethod();
        this.resource = input.getRouteKey().replace(httpMethod + " ", "");
        this.tokenClaims = new TokenClaims(authHeader.replace("Bearer ", ""), secretKey);
    }

    public String getResource() {
        return resource;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public TokenClaims getTokenClaims() {
        return tokenClaims;
    }
}
