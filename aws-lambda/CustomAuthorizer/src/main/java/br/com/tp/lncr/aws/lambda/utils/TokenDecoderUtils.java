package br.com.tp.lncr.aws.lambda.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

public class TokenDecoderUtils {

    private static <T> T getClaimValue(String claim, DecodedJWT decodedJWT, Class<T> tClass) {
        return decodedJWT.getClaim(claim).as(tClass);
    }

    public static DecodedJWT decodeToken(String token) {
        String oauthSecretKey = SecretUtils.getSecretValue();
        return JWT.require(Algorithm.HMAC256(oauthSecretKey)).build().verify(token);
    }


    private static Integer getClaimAsInteger(String claim, DecodedJWT decodedJWT) {
        return getClaimValue(claim, decodedJWT, Integer.class);
    }

    private static String getClaimAsString(String claim, DecodedJWT decodedJWT) {
        return getClaimValue(claim, decodedJWT, String.class);
    }

    public static String getSubject(DecodedJWT decodedJWT) {
        return getClaimAsString("sub", decodedJWT);
    }

    public static String getScope(DecodedJWT decodedJWT) {
        return getClaimAsString("scope", decodedJWT);
    }

    public static Boolean isTokenExpired(DecodedJWT decodedJWT) {
        Integer exp = getClaimAsInteger("exp", decodedJWT);
        Long nowInSeconds = System.currentTimeMillis() / 1000;
        return exp < nowInSeconds;
    }

}
