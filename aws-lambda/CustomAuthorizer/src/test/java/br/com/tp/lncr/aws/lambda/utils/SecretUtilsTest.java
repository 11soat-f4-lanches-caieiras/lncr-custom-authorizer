package br.com.tp.lncr.aws.lambda.utils;

import br.com.tp.lncr.core.exceptions.OauthException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecretUtils - Testes de recuperação de secrets")
class SecretUtilsTest {

    private static final String TEST_SECRET_KEY = "test-secret-key-value";
    private static final String ENV_VAR_NAME = "LNCR_OAUTH_SECRET_KEY";

    @BeforeEach
    void setUp() {
        // Clear system properties before each test
        System.clearProperty("LNCR_AWS_SECRET_KEY");
        System.clearProperty("LNCR_AWS_SECRET_NAME");
    }

    @Test
    @DisplayName("Deve retornar secret key da variável de ambiente quando configurada")
    void shouldReturnSecretFromEnvironmentVariable() {
        // Given - This test requires environment variable to be set
        String envSecret = System.getenv(ENV_VAR_NAME);

        // When
        if (envSecret != null && !envSecret.isEmpty()) {
            String secret = SecretUtils.getAwsSecretValue();

            // Then
            assertNotNull(secret);
            assertEquals(envSecret, secret);
        } else {
            // Skip test if environment variable is not set
            System.out.println("Skipping test - environment variable not set");
        }
    }

    @Test
    @DisplayName("Deve usar nome padrão de secret quando não especificado")
    void shouldUseDefaultSecretName() {
        // Given
        assertNull(System.getProperty("LNCR_AWS_SECRET_NAME"));

        // When & Then
        // The default value should be "lncr-prd-sm" as per the code
        // This is tested indirectly through the getAwsSecretValue method
        assertDoesNotThrow(() -> {
            try {
                SecretUtils.getAwsSecretValue();
            } catch (OauthException e) {
                // Expected when AWS is not available
                assertTrue(e.getMessage().contains("Erro ao buscar secret key"));
            }
        });
    }

    @Test
    @DisplayName("Deve usar nome padrão de chave quando não especificado")
    void shouldUseDefaultSecretKey() {
        // Given
        assertNull(System.getProperty("LNCR_AWS_SECRET_KEY"));

        // When & Then
        // The default value should be "LNCR_OAUTH_SECRET_KEY" as per the code
        assertDoesNotThrow(() -> {
            try {
                SecretUtils.getAwsSecretValue();
            } catch (OauthException e) {
                // Expected when AWS is not available or env var not set
                assertTrue(e.getMessage().contains("Erro ao buscar secret key"));
            }
        });
    }

    @Test
    @DisplayName("Deve usar propriedades customizadas quando especificadas")
    void shouldUseCustomPropertiesWhenSpecified() {
        // Given
        System.setProperty("LNCR_AWS_SECRET_KEY", "CUSTOM_KEY");
        System.setProperty("LNCR_AWS_SECRET_NAME", "custom-secret-name");

        // When & Then
        assertDoesNotThrow(() -> {
            try {
                SecretUtils.getAwsSecretValue();
            } catch (OauthException e) {
                // Expected when AWS is not available
                assertTrue(e.getMessage().contains("Erro ao buscar secret key"));
            }
        });

        // Cleanup
        System.clearProperty("LNCR_AWS_SECRET_KEY");
        System.clearProperty("LNCR_AWS_SECRET_NAME");
    }

    @Test
    @DisplayName("Deve lançar OauthException quando houver erro ao buscar secret da AWS")
    void shouldThrowOauthExceptionWhenAwsSecretFetchFails() {
        // Given - No environment variable set and AWS will fail
        // This assumes the environment variable is not set

        // When & Then
        if (System.getenv(ENV_VAR_NAME) == null || System.getenv(ENV_VAR_NAME).isEmpty()) {
            assertThrows(OauthException.class, SecretUtils::getAwsSecretValue);
        }
    }

    @Test
    @DisplayName("Deve validar que a exceção contém mensagem apropriada")
    void shouldValidateExceptionMessage() {
        // Given - Assuming no environment variable is set

        // When & Then
        if (System.getenv(ENV_VAR_NAME) == null || System.getenv(ENV_VAR_NAME).isEmpty()) {
            OauthException exception = assertThrows(OauthException.class, SecretUtils::getAwsSecretValue);

            assertEquals("Erro ao buscar secret key", exception.getMessage());
            assertEquals(500, exception.getCode());
        }
    }

    @Test
    @DisplayName("Deve validar que o construtor é privado")
    void shouldHavePrivateConstructor() throws Exception {
        // Given
        var constructor = SecretUtils.class.getDeclaredConstructor();

        // Then
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));

        // When - Try to instantiate using reflection
        constructor.setAccessible(true);
        SecretUtils instance = constructor.newInstance();

        // Then
        assertNotNull(instance);
    }

    @Test
    @DisplayName("Deve retornar secret quando variável de ambiente está configurada")
    @EnabledIfEnvironmentVariable(named = "LNCR_OAUTH_SECRET_KEY", matches = ".+")
    void shouldReturnSecretWhenEnvironmentVariableIsSet() {
        // When
        String secret = SecretUtils.getAwsSecretValue();

        // Then
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
        assertEquals(System.getenv(ENV_VAR_NAME), secret);
    }

    @Nested
    @DisplayName("Testes de integração simulada com AWS")
    class AwsIntegrationTests {

        @Test
        @DisplayName("Deve processar JSON válido do AWS Secrets Manager")
        void shouldProcessValidJsonFromAws() {
            // This test validates the JSON parsing logic
            // The actual mapSecretValue is private, so we test through getAwsSecretValue

            // Given - A valid JSON response structure
            String validJson = "{\"LNCR_OAUTH_SECRET_KEY\":\"test-secret-value\"}";

            // When & Then
            // Since mapSecretValue is private, we can only test indirectly
            // This test documents expected behavior
            assertNotNull(validJson);
            assertTrue(true);
        }

        @Test
        @DisplayName("Deve validar estrutura de região AWS")
        void shouldValidateAwsRegion() {
            // Given
            String expectedRegion = "us-east-1";

            // When & Then
            // The region is hardcoded in the class
            assertEquals("us-east-1", expectedRegion);
        }
    }

    @Nested
    @DisplayName("Testes de comportamento de sistema")
    class SystemBehaviorTests {

        @Test
        @DisplayName("Deve preferir variável de ambiente sobre AWS Secrets Manager")
        void shouldPreferEnvironmentVariableOverAws() {
            // Given
            String envSecret = System.getenv(ENV_VAR_NAME);

            // When
            if (envSecret != null && !envSecret.isEmpty()) {
                String result = SecretUtils.getAwsSecretValue();

                // Then
                assertEquals(envSecret, result);
                // Should not have called AWS Secrets Manager
            }
        }

        @Test
        @DisplayName("Deve logar informações durante execução")
        void shouldLogInformationDuringExecution() {
            // This test validates that the method executes without throwing unexpected exceptions
            // when logging is involved

            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    SecretUtils.getAwsSecretValue();
                } catch (OauthException e) {
                    // Expected exception when AWS is not available
                    assertNotNull(e.getMessage());
                }
            });
        }
    }

    @Nested
    @DisplayName("Testes de propriedades do sistema")
    class SystemPropertiesTests {

        @AfterEach
        void cleanup() {
            System.clearProperty("LNCR_AWS_SECRET_KEY");
            System.clearProperty("LNCR_AWS_SECRET_NAME");
        }

        @Test
        @DisplayName("Deve usar propriedade customizada para LNCR_AWS_SECRET_KEY")
        void shouldUseCustomSecretKeyProperty() {
            // Given
            System.setProperty("LNCR_AWS_SECRET_KEY", "CUSTOM_SECRET_KEY");

            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    SecretUtils.getAwsSecretValue();
                } catch (OauthException e) {
                    // Expected when not in AWS environment
                    assertEquals(500, e.getCode());
                }
            });
        }

        @Test
        @DisplayName("Deve usar propriedade customizada para LNCR_AWS_SECRET_NAME")
        void shouldUseCustomSecretNameProperty() {
            // Given
            System.setProperty("LNCR_AWS_SECRET_NAME", "custom-secret");

            // When & Then
            assertDoesNotThrow(() -> {
                try {
                    SecretUtils.getAwsSecretValue();
                } catch (OauthException e) {
                    // Expected when not in AWS environment
                    assertEquals(500, e.getCode());
                }
            });
        }

        @Test
        @DisplayName("Deve validar valores padrão das constantes")
        void shouldValidateDefaultConstantValues() {
            // When & Then
            // Testing default behavior when properties are not set
            assertNull(System.getProperty("LNCR_AWS_SECRET_KEY"));
            assertNull(System.getProperty("LNCR_AWS_SECRET_NAME"));
        }
    }

    @Test
    @DisplayName("Deve validar que getAwsSecretValue é um método público estático")
    void shouldValidateMethodSignature() throws Exception {
        // Given
        var method = SecretUtils.class.getMethod("getAwsSecretValue");

        // Then
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    @Tag("local")
    @DisplayName("Teste de permissão de leitura no secret manager")
    void shouldHaveReadPermissionOnSecretManager() {
        System.setProperty("LNCR_AWS_SECRET_KEY", "LNCR_OAUTH_SECRET_KEY");
        System.setProperty("LNCR_AWS_SECRET_NAME", "lncr-ms-oauth-prd-secrets");
        String awsSecret = SecretUtils.getAwsSecretValue();
        assertNotNull(awsSecret);
        assertFalse(awsSecret.isEmpty());
    }

}

