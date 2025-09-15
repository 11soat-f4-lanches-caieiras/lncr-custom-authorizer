package br.com.tp.lncr.aws.lambda.model;

import br.com.tp.lncr.core.commons.utils.security.TokenDecoderUtils;
import com.auth0.jwt.interfaces.DecodedJWT;

public class TokenClaims {
    public String subject;
    public String scope;

    public TokenClaims(String token){
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        DecodedJWT tokenDecoded = TokenDecoderUtils.decodeToken(token);

        if (TokenDecoderUtils.isTokenExpired(tokenDecoded)) {
            throw new IllegalArgumentException("Token is expired");
        }

        this.subject = TokenDecoderUtils.getSubject(tokenDecoded);
        this.scope = TokenDecoderUtils.getScope(tokenDecoded);

    }

    public String getSubject() {
        return subject;
    }

    public String getScope() {
        return scope;
    }



}
