package br.com.tp.lncr.aws.lambda.model;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2CustomAuthorizerEvent;

public class RequestDTO {
    private final String resource;
    private final String httpMethod;
    private final TokenClaims tokenClaims;

    public RequestDTO(APIGatewayV2CustomAuthorizerEvent input) {
        if (input.getHeaders() == null || !input.getHeaders().containsKey("Authorization")) {
            throw new IllegalArgumentException("Authorization header is missing");
        }
        this.httpMethod = input.getRequestContext().getHttp().getMethod();
        this.resource = input.getRawPath().replace(httpMethod + " ", "");
        this.tokenClaims = new TokenClaims(input.getHeaders().get("Authorization").replace("Bearer ", ""));
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
