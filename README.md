# lncr-custom-authorizer

## Índice

1. [Descrição](#descrição)
2. [Funcionalidades](#funcionalidades)
3. [Arquitetura](#arquitetura)
4. [Sonar Quality Gate](#sonar-quality-gate)
5. [Tecnologias Utilizadas](#tecnologias-utilizadas)
6. [Configuração](#configuração)
   - [Variáveis de Ambiente](#variáveis-de-ambiente)
   - [AWS Secrets Manager](#aws-secrets-manager)
   - [Regras de Autorização](#regras-de-autorização)
7. [Escopos de Usuário](#escopos-de-usuário)
   - [Customer (Cliente)](#customer-cliente)
   - [Admin (Administrador)](#admin-administrador)
   - [Totem (Terminal de Autoatendimento)](#totem-terminal-de-autoatendimento)
   - [Monitor (Sistema de Monitoramento)](#monitor-sistema-de-monitoramento)
8. [Pipeline de CI/CD](#pipeline-de-cicd)
   - [Visão Geral do Pipeline](#visão-geral-do-pipeline)
   - [Triggers do Pipeline](#triggers-do-pipeline)
   - [Etapas do Pipeline](#etapas-do-pipeline)
   - [Configurações do Pipeline](#configurações-do-pipeline)
   - [Infraestrutura do Pipeline](#infraestrutura-do-pipeline)
   - [Integração com AWS](#integração-com-aws)
   - [Benefícios do Pipeline Automatizado](#benefícios-do-pipeline-automatizado)
9. [Build e Deploy](#build-e-deploy)
   - [Pré-requisitos](#pré-requisitos)
   - [Build Local](#build-local)
   - [Executar Testes](#executar-testes)
   - [Gerar JAR para Deploy](#gerar-jar-para-deploy)
   - [Deploy AWS Lambda](#deploy-aws-lambda)

## Descrição

O **lncr-custom-authorizer** é uma função AWS Lambda que implementa um autorizador personalizado para o API Gateway. Esta função é responsável por validar tokens JWT e autorizar requisições baseadas em escopos de usuário e regras de acesso definidas.

## Funcionalidades

- ✅ Validação de tokens JWT
- ✅ Autorização baseada em escopos (customer/admin/totem/monitor)
- ✅ Integração com AWS Secrets Manager para chaves de segurança
- ✅ Regras de acesso configuráveis via YAML
- ✅ Logs detalhados para auditoria
- ✅ Suporte a múltiplos métodos HTTP (GET, POST, PUT, PATCH, DELETE)

## Arquitetura

```
API Gateway Request
       ↓
Custom Authorizer Lambda
       ↓
Token Validation (JWT)
       ↓
Scope & Resource Validation
       ↓
IAM Policy Response (Allow/Deny)
       ↓
lncr-app (EKS) 
```

## Sonar Quality Gate

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=11soat-f4-lanches-caieiras_lncr-custom-authorizer&metric=alert_status&token=f3119e65b6d0749ec05af7abef29cd2daaae04cd)](https://sonarcloud.io/summary/new_code?id=11soat-f4-lanches-caieiras_lncr-custom-authorizer)

Acesse o dashboard completo: [SonarCloud - lncr-custom-authorizer](https://sonarcloud.io/project/overview?id=11soat-f4-lanches-caieiras_lncr-custom-authorizer)

## Tecnologias Utilizadas

- **Java 21**
- **lncr-core 2.0**
- **AWS Lambda Java Core 1.2.2**
- **AWS Lambda Java Events 3.11.0**
- **AWS SDK v2 (Secrets Manager)**
- **Auth0 Java JWT 4.4.0**
- **Jackson (JSON/YAML processing)**
- **SLF4J (Logging)**
- **JUnit 5 (Testes)**

## Configuração

### Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|---------|
| `LNCR_AWS_SECRET_KEY` | Nome da chave no AWS Secrets Manager | `LNCR_OAUTH_SECRET_KEY` |
| `LNCR_AWS_SECRET_NAME` | Nome do secret no AWS Secrets Manager | `lncr-prd-sm` |

### AWS Secrets Manager

A função busca a chave JWT no AWS Secrets Manager para não deixar secret exposto como variável de ambiente. Configure um secret com a estrutura:

```json
{
  "LNCR_OAUTH_SECRET_KEY": "sua-chave-jwt-secreta"
}
```

### Regras de Autorização

As regras de acesso são definidas no arquivo `allow-paths-rules.yaml`:

```yaml
customer:
  POST:
    - "/customers"
    - "/customerOrders"
  GET:
    - "/customers/{customerId}"
    - "/foodItems"
  # ... outros métodos

admin:
  POST:
    - "/customers"
    - "/foodItems"
    - "/kitchenOrders"
  GET:
    - "/customers"
    - "/kitchenOrders"
  # ... outros métodos e escopos
```

## Escopos de Usuário

### Customer (Cliente)
- Acesso limitado a operações básicas
- Pode gerenciar seus próprios dados
- Visualizar itens do cardápio
- Realizar pedidos

### Admin (Administrador)
- Acesso completo a todas as operações
- Gerenciar clientes, pedidos e itens do cardápio
- Acessar relatórios e notificações
- Controlar status de pedidos da cozinha

### Totem (Terminal de Autoatendimento)
- Acesso específico para terminais de autoatendimento
- Cadastro de clientes no ponto de venda
- Realização de pedidos
- Visualização do cardápio e imagens
- Consulta de pedidos e pagamentos
- Atualização limitada de dados de clientes

### Monitor (Sistema de Monitoramento)
- Acesso focado em visualização e controle de pedidos
- Consulta de pedidos e status
- Gerenciamento de pedidos da cozinha
- Visualização de notificações
- Atualização de status de pedidos
- Monitoramento do fluxo operacional

## Pipeline de CI/CD

O projeto utiliza **GitHub Actions** com **AWS CodeBuild** para automação do processo de desenvolvimento, integração e entrega contínua da função Lambda.

### Visão Geral do Pipeline

```
Trigger → Build (CodeBuild) → Package → Deploy → AWS Secrets Manager Update
```

### Triggers do Pipeline

O pipeline é executado nos seguintes cenários:
- **Manual**: Via `workflow_dispatch` no GitHub Actions
- **Automático**: Via `repository_dispatch` quando a infraestrutura base é completada

### Etapas do Pipeline

#### **Job: Build**
Executa em runners AWS CodeBuild personalizados

1. **Checkout e Setup**
   - Checkout do código fonte
   - Configuração do ambiente Java 21 (Temurin)
   - Definição de caminhos e variáveis de ambiente
   - Cache do Maven

2. **Extração de Metadados**
   - Obtenção do ArtifactId e Version do pom.xml
   - Geração do hash do commit para versionamento

3. **Build e Testes**
   - Compilação com Maven usando settings customizados
   - Execução de testes unitários
   - Resolução de dependências do GitHub Packages

4. **Empacotamento**
   - Geração do JAR da função Lambda
   - Renomeação do JAR com hash do commit
   - Upload do artefato para uso no job de deploy

#### **Job: Deploy**
Depende do job Build e executa o deployment

1. **Download do Artefato**
   - Download do JAR construído no job anterior

2. **Deploy AWS Lambda**
   - Atualização do código da função Lambda
   - Configuração do handler e runtime (Java 21)
   - Definição de variáveis de ambiente
   - Aguarda confirmação das atualizações

3. **Gerenciamento de Secrets**
   - Criação/atualização do OAuth Secret no AWS Secrets Manager
   - Configuração automática das chaves de autenticação

### Configurações do Pipeline

**Variáveis Gerenciadas (`vars`):**
- `LNCR_FUNCTION_NAME`: Nome da função Lambda
- `LNCR_FUNCTION_HANDLER`: Handler da função
- `LNCR_AWS_SECRET_KEY`: Nome da chave no Secrets Manager
- `LNCR_AWS_SECRET_NAME`: Nome do secret no Secrets Manager

**Secrets Gerenciados:**
- `LNCR_MAVEN_ACTOR`: Usuário para acesso ao GitHub Packages
- `LNCR_MAVEN_PAT`: Token de acesso pessoal
- `LNCR_OAUTH_SECRET_KEY`: Chave secreta para JWT

### Infraestrutura do Pipeline

- **Runners**: AWS CodeBuild personalizados (`codebuild-github-lncr-custom-authorizer`)
- **Permissões**: contents:write, packages:write, actions:write
- **Artifacts**: Upload/download automático entre jobs
- **Environments**: Proteção de ambientes dev/prd

### Integração com AWS

- **Lambda**: Deploy automático com configuração completa
- **Secrets Manager**: Criação/atualização automática de secrets
- **CodeBuild**: Runners dedicados para isolamento e performance
- **GitHub Packages**: Resolução automática de dependências privadas

### Benefícios do Pipeline Automatizado

- ✅ **Isolamento**: Runners CodeBuild dedicados por execução
- ✅ **Versionamento**: JAR nomeado com hash do commit
- ✅ **Segurança**: Secrets centralizados e protegidos
- ✅ **Rastreabilidade**: Logs completos e artifacts versionados
- ✅ **Automação**: Deploy end-to-end sem intervenção manual

## Build e Deploy

### Pré-requisitos

- Java 21
- Maven 3.8+
- AWS CLI configurado
- Acesso ao GitHub Packages (dependência lncr-core)

### Build Local

```bash
cd aws-lambda/CustomAuthorizer
mvn clean compile
```

### Executar Testes

O projeto possui uma suíte abrangente de testes automatizados que validam toda a lógica de autorização.

#### Estrutura dos Testes

```bash
mvn test
```

#### Cobertura de Testes

**Testes Parametrizados:**
- **144 cenários individuais** testando cada combinação de escopo, método HTTP e recurso
- **4 escopos** × **36 rotas** = 144 combinações de autorização
- Validação detalhada de cada regra de acesso definida no `allow-paths-rules.yaml`

**Tipos de Teste:**

1. **Testes de Validação Individual** (`validateIndividualResource`)
   - Executa 144 testes parametrizados
   - Valida cada combinação escopo/método/recurso
   - Verifica se o resultado corresponde às regras definidas
   - Confirma que o contexto retornado está correto

2. **Teste de Resumo Geral** (`validateAllowedsResourceSummary`)
   - Executa validação completa de todos os recursos
   - Fornece estatísticas de testes aprovados/reprovados
   - Gera relatório consolidado de cobertura

#### Recursos Testados

**36 endpoints testados** incluindo:
- `/customers` (GET, POST, DELETE, PATCH)
- `/customerOrders` (GET, POST, PATCH)
- `/foodItems` (GET, POST, PUT, DELETE, PATCH)
- `/kitchenOrders` (GET, POST, PATCH)
- `/payments/mercadoPago/*` (GET, POST, PATCH)
- `/notifications/*` (GET)
- `/webhooks/*` (POST)

#### Escopos Validados

Cada endpoint é testado contra os **4 escopos**:
- **customer**: Permissões limitadas do cliente
- **admin**: Acesso administrativo completo
- **totem**: Operações de autoatendimento
- **monitor**: Monitoramento e controle

#### Exemplo de Execução

```bash
# Executar todos os testes
mvn test

# Executar com logs detalhados
mvn test -Dtest=CustomAuthorizerTest -Dlogging.level.br.com.tp.lncr=DEBUG

# Executar apenas testes específicos
mvn test -Dtest=CustomAuthorizerTest#validateIndividualResource
```

#### Saída dos Testes

```
[INFO] ✓ Teste passou para GET /customers com escopo admin
[INFO] ✓ Teste passou para POST /customers com escopo customer
[INFO] ✗ Teste falhou para DELETE /customers com escopo customer
[INFO] Teste de resumo: 120/144 testes aprovados (83.3%)
```

#### Validações Realizadas

Para cada teste, o sistema valida:
- ✅ **Autorização correta**: Se o acesso foi permitido/negado conforme esperado
- ✅ **Contexto preservado**: Se escopo, método e recurso estão corretos na resposta
- ✅ **Token JWT válido**: Se a validação do token funciona adequadamente
- ✅ **Tratamento de erros**: Se exceções são capturadas e tratadas
- ✅ **Conformidade com regras**: Se todas as regras YAML são respeitadas

### Gerar JAR para Deploy

```bash
mvn clean package
```

### Deploy AWS Lambda

```bash
aws lambda update-function-code --function-name <nome-da-sua-funcao> --zip-file fileb://target/lncr-custom-authorizer-1.0-SNAPSHOT.jar
```
