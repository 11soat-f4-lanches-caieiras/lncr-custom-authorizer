package br.com.tp.lncr.aws.lambda.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;
import java.util.Map;

public class AllowResourcesRules {

    public AllowResourcesRules() {
    }

    public Map<String, Object> read() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream in = AllowResourcesRules.class.getClassLoader().getResourceAsStream("allow-paths-rules.yaml")) {
            if (in == null) {
                throw new IllegalArgumentException("Arquivo YAML não encontrado");
            }
            return mapper.readValue(in, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao ler o arquivo YAML", e);
        }
    }
}
