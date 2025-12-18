package br.com.tp.lncr.aws.lambda.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenDecoderUtils - Testes de decodificação e validação de tokens JWT")
class TokenDecoderUtilsTest {

    private String secretKey;
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        secretKey = "test-secret-key-for-unit-tests-minimum-256-bits-long-secret-key-value";
        
        // Create a valid token
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        validToken = JWT.create()
                .withClaim("client_id", "test-client-id")
                .withClaim("client_secret", "test-client-secret")
                .withClaim("customerId", "customer-123")
                .withClaim("grant_type", "client_credentials")
                .withClaim("name", "Test User")
                .withClaim("scope", "read write")
                .withClaim("sub", "subject-123")
                .withClaim("exp", (System.currentTimeMillis() / 1000) + 3600) // expires in 1 hour
                .sign(algorithm);

        // Create an expired token
        expiredToken = JWT.create()
                .withClaim("client_id", "test-client-id")
                .withClaim("exp", (System.currentTimeMillis() / 1000) - 3600) // expired 1 hour ago
                .sign(algorithm);
    }

    @Test
    @DisplayName("Deve decodificar token válido com sucesso")
    void shouldDecodeValidToken() {
        // When
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // Then
        assertNotNull(decodedJWT);
        assertEquals("test-client-id", decodedJWT.getClaim("client_id").asString());
    }

    @Test
    @DisplayName("Deve lançar exceção ao decodificar token com chave secreta inválida")
    void shouldThrowExceptionWhenDecodingWithInvalidSecretKey() {
        // Given
        String wrongSecretKey = "wrong-secret-key-for-unit-tests-minimum-256-bits-long-wrong-value";

        // When & Then
        assertThrows(JWTVerificationException.class, () ->
            TokenDecoderUtils.decodeToken(validToken, wrongSecretKey)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao decodificar token nulo")
    void shouldThrowExceptionWhenTokenIsNull() {
        // When & Then
        assertThrows(Exception.class, () ->
            TokenDecoderUtils.decodeToken(null, secretKey)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao decodificar token vazio")
    void shouldThrowExceptionWhenTokenIsEmpty() {
        // When & Then
        assertThrows(Exception.class, () ->
            TokenDecoderUtils.decodeToken("", secretKey)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção ao decodificar token com formato inválido")
    void shouldThrowExceptionWhenTokenFormatIsInvalid() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThrows(JWTVerificationException.class, () ->
            TokenDecoderUtils.decodeToken(invalidToken, secretKey)
        );
    }

    @Test
    @DisplayName("Deve extrair client_id corretamente")
    void shouldExtractClientId() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String clientId = TokenDecoderUtils.getClientId(decodedJWT);

        // Then
        assertEquals("test-client-id", clientId);
    }

    @Test
    @DisplayName("Deve extrair client_secret corretamente")
    void shouldExtractClientSecret() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String clientSecret = TokenDecoderUtils.getClientSecret(decodedJWT);

        // Then
        assertEquals("test-client-secret", clientSecret);
    }

    @Test
    @DisplayName("Deve extrair customerId corretamente")
    void shouldExtractCustomerId() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String customerId = TokenDecoderUtils.getCustomerId(decodedJWT);

        // Then
        assertEquals("customer-123", customerId);
    }

    @Test
    @DisplayName("Deve extrair grant_type corretamente")
    void shouldExtractGrantType() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String grantType = TokenDecoderUtils.getGrantType(decodedJWT);

        // Then
        assertEquals("client_credentials", grantType);
    }

    @Test
    @DisplayName("Deve extrair name corretamente")
    void shouldExtractName() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String name = TokenDecoderUtils.getName(decodedJWT);

        // Then
        assertEquals("Test User", name);
    }

    @Test
    @DisplayName("Deve extrair scope corretamente")
    void shouldExtractScope() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String scope = TokenDecoderUtils.getScope(decodedJWT);

        // Then
        assertEquals("read write", scope);
    }

    @Test
    @DisplayName("Deve extrair subject corretamente")
    void shouldExtractSubject() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        String subject = TokenDecoderUtils.getSubject(decodedJWT);

        // Then
        assertEquals("subject-123", subject);
    }

    @Test
    @DisplayName("Deve retornar false para token não expirado")
    void shouldReturnFalseForNonExpiredToken() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When
        Boolean isExpired = TokenDecoderUtils.isTokenExpired(decodedJWT);

        // Then
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Deve retornar true para token expirado")
    void shouldReturnTrueForExpiredToken() {
        // Given - Decode without verification to test expiration logic
        DecodedJWT decodedJWT = JWT.decode(expiredToken);

        // When
        Boolean isExpired = TokenDecoderUtils.isTokenExpired(decodedJWT);

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Deve retornar null quando claim não existe")
    void shouldReturnNullWhenClaimDoesNotExist() {
        // Given
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String tokenWithoutClaims = JWT.create().sign(algorithm);
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(tokenWithoutClaims, secretKey);

        // When
        String nonExistentClaim = TokenDecoderUtils.getClientId(decodedJWT);

        // Then
        assertNull(nonExistentClaim);
    }

    @Test
    @DisplayName("Deve decodificar token com múltiplos claims corretamente")
    void shouldDecodeTokenWithMultipleClaims() {
        // Given
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(validToken, secretKey);

        // When & Then
        assertAll("Verificando todos os claims",
            () -> assertEquals("test-client-id", TokenDecoderUtils.getClientId(decodedJWT)),
            () -> assertEquals("test-client-secret", TokenDecoderUtils.getClientSecret(decodedJWT)),
            () -> assertEquals("customer-123", TokenDecoderUtils.getCustomerId(decodedJWT)),
            () -> assertEquals("client_credentials", TokenDecoderUtils.getGrantType(decodedJWT)),
            () -> assertEquals("Test User", TokenDecoderUtils.getName(decodedJWT)),
            () -> assertEquals("read write", TokenDecoderUtils.getScope(decodedJWT)),
            () -> assertEquals("subject-123", TokenDecoderUtils.getSubject(decodedJWT)),
            () -> assertFalse(TokenDecoderUtils.isTokenExpired(decodedJWT))
        );
    }

    @Test
    @DisplayName("Deve validar token próximo da expiração")
    void shouldValidateTokenNearExpiration() {
        // Given - token that expires in 10 seconds
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String nearExpiryToken = JWT.create()
                .withClaim("exp", (System.currentTimeMillis() / 1000) + 10)
                .sign(algorithm);
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(nearExpiryToken, secretKey);

        // When
        Boolean isExpired = TokenDecoderUtils.isTokenExpired(decodedJWT);

        // Then
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Deve validar token recém-expirado")
    void shouldValidateJustExpiredToken() {
        // Given - token that expired 1 second ago
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String justExpiredToken = JWT.create()
                .withClaim("exp", (System.currentTimeMillis() / 1000) - 1)
                .sign(algorithm);
        DecodedJWT decodedJWT = JWT.decode(justExpiredToken);

        // When
        Boolean isExpired = TokenDecoderUtils.isTokenExpired(decodedJWT);

        // Then
        assertTrue(isExpired);
    }

    @ParameterizedTest
    @ValueSource(strings = {"read", "write", "admin", "read write admin"})
    @DisplayName("Deve extrair diferentes valores de scope")
    void shouldExtractDifferentScopeValues(String scopeValue) {
        // Given
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String token = JWT.create()
                .withClaim("scope", scopeValue)
                .withClaim("exp", (System.currentTimeMillis() / 1000) + 3600)
                .sign(algorithm);
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(token, secretKey);

        // When
        String scope = TokenDecoderUtils.getScope(decodedJWT);

        // Then
        assertEquals(scopeValue, scope);
    }

    @Test
    @DisplayName("Deve decodificar token sem claims opcionais")
    void shouldDecodeTokenWithoutOptionalClaims() {
        // Given
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String minimalToken = JWT.create()
                .withClaim("exp", (System.currentTimeMillis() / 1000) + 3600)
                .sign(algorithm);

        // When
        DecodedJWT decodedJWT = TokenDecoderUtils.decodeToken(minimalToken, secretKey);

        // Then
        assertNotNull(decodedJWT);
        assertNull(TokenDecoderUtils.getClientId(decodedJWT));
        assertNull(TokenDecoderUtils.getName(decodedJWT));
        assertFalse(TokenDecoderUtils.isTokenExpired(decodedJWT));
    }
}

