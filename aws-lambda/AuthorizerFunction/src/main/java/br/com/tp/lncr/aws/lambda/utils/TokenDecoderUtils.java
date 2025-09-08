package br.com.tp.lncr.aws.lambda.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

public class TokenDecoderUtils {

    private static <T> T getClaimValue(String claim, String token, Class<T> tClass) {
        String oauthSecretKey = System.getenv("LNCR_OAUTH_SECRET_KEY");
        DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC256(oauthSecretKey)).build().verify(token);
        return decodedJWT.getClaim(claim).as(tClass);
    }

    private static Integer getClaimAsInteger(String claim, String token) {
        return getClaimValue(claim, token, Integer.class);
    }

    private static String getClaimAsString(String claim, String token) {
        return getClaimValue(claim, token, String.class);
    }

    public static String getSubject(String token) {
        return getClaimAsString("sub", token);
    }

    public static String getScope(String token) {
        return getClaimAsString("scope", token);
    }

    public static Boolean isTokenExpired(String token) {
        Integer exp = getClaimAsInteger("exp", token);
        Long nowInSeconds = System.currentTimeMillis() / 1000;
        return exp < nowInSeconds;
    }


}
