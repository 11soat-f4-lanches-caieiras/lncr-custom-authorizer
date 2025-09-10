package br.com.tp.lncr.aws.lambda.utils;

import java.util.List;
import java.util.Map;

public class AuthorizatedUtils {

    public static Boolean isAuthorizedResult(Map<String, Object> allowResourcesRules, String scope, String method, String path) {
        Boolean result = false;
        if (allowResourcesRules.containsKey(scope)) {
            Map<String, List<String>> scopeData = (Map<String, List<String>>) allowResourcesRules.get(scope);

            if (scopeData.containsKey(method)) {
                List<String> paths = scopeData.get(method).stream().toList();
                if (paths.contains(path)) {
                    result = true;
                }
            }
        }
        return result;
    }
}