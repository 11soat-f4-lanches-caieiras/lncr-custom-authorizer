package br.com.tp.lncr.aws.lambda.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class TokenDecoderUtils {

    private static final Logger logger = LoggerFactory.getLogger(TokenDecoderUtils.class);

    private static final String LNCR_AWS_SECRET_KEY = System.getProperty("LNCR_AWS_SECRET_KEY","LNCR_OAUTH_SECRET_KEY");
    private static final String LNCR_AWS_SECRET_NAME = System.getProperty("LNCR_AWS_SECRET_NAME","lncr/oauth_secret_key");


    private static <T> T getClaimValue(String claim, DecodedJWT decodedJWT, Class<T> tClass) {
        return decodedJWT.getClaim(claim).as(tClass);
    }

    public static DecodedJWT decodeToken(String token) {
        String oauthSecretKey = getSecretValue();
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

    private static String getSecretValue(){
        String localSecretKey = System.getenv(LNCR_AWS_SECRET_KEY);
        if (localSecretKey == null || localSecretKey.isEmpty()) {
            logger.info("Fetching secret key from AWS Secrets Manager");
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of("us-east-1"))
                    .build();
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(LNCR_AWS_SECRET_NAME)
                    .build();
            return mapSecretValue(client.getSecretValue(request).secretString());
        }
        logger.info("Using local secret key from environment variable");
        return localSecretKey;

    }

    private static String mapSecretValue(String secretString) {
        try {
            JsonNode node = new ObjectMapper().readTree(secretString);
            return node.get(LNCR_AWS_SECRET_KEY).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse secret JSON", e);

        }
    }


}
