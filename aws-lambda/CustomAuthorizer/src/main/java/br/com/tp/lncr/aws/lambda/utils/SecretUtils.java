package br.com.tp.lncr.aws.lambda.utils;

import br.com.tp.lncr.core.commons.exceptions.OauthException;
import br.com.tp.lncr.core.commons.utils.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

public class SecretUtils {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SecretUtils.class);
    private static final String LNCR_AWS_SECRET_KEY = System.getProperty("LNCR_AWS_SECRET_KEY","LNCR_OAUTH_SECRET_KEY");
    private static final String LNCR_AWS_SECRET_NAME = System.getProperty("LNCR_AWS_SECRET_NAME","lncr-prd-sm");
    private static final String LNCR_AWS_REGION = "us-east-1";



    public static String getAwsSecretValue(){
        String localSecretKey = System.getenv(LNCR_AWS_SECRET_KEY);
        log.info("localSecretKey: {}", localSecretKey);
        try {
            if (localSecretKey == null || localSecretKey.isEmpty()) {
                Logger.info("Fetching secret key from AWS Secrets Manager");
                SecretsManagerClient client = SecretsManagerClient.builder()
                        .region(Region.of(LNCR_AWS_REGION))
                        .build();
                GetSecretValueRequest request = GetSecretValueRequest.builder()
                        .secretId(LNCR_AWS_SECRET_NAME)
                        .build();
                return mapSecretValue(client.getSecretValue(request).secretString());
            }
            Logger.info("Using local secret key from environment variable");
            return localSecretKey;
        }catch (Exception e) {
            throw new OauthException("Erro ao buscar secret key", 500);
        }
    }

    private static String mapSecretValue(String secretString) {
        try {
            log.info("secretString: {}", secretString);
            JsonNode node = new ObjectMapper().readTree(secretString);
            return node.get(LNCR_AWS_SECRET_KEY).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse secret JSON", e);

        }
    }
}
