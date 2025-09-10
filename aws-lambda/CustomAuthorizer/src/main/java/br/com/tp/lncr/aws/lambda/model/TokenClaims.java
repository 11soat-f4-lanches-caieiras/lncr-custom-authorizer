package br.com.tp.lncr.aws.lambda.model;

import br.com.tp.lncr.aws.lambda.utils.TokenDecoderUtils;

public class TokenClaims {
    public String subject;
    public String scope;

    public TokenClaims(String token){
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is null or empty");
        }

        if (TokenDecoderUtils.isTokenExpired(token)) {
            throw new IllegalArgumentException("Token is expired");
        }

        this.subject = TokenDecoderUtils.getSubject(token);
        this.scope = TokenDecoderUtils.getScope(token);

    }

    public String getSubject() {
        return subject;
    }

    public String getScope() {
        return scope;
    }



}
