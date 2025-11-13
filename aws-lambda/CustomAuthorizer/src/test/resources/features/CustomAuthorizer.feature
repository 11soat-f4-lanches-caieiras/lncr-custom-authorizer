#language: pt

Funcionalidade: Autorizador Customizado AWS Lambda
  Como um API Gateway
  Eu quero validar tokens JWT e autorizar requisições
  Para garantir que apenas requisições autorizadas sejam permitidas

  Cenário: Autorizar requisição com token válido e escopo admin
    Dado que existe um token JWT válido com escopo "admin"
    E a requisição é do tipo "GET" para o recurso "/customers"
    Quando o autorizador processar a requisição
    Então a requisição deve ser autorizada
    E o contexto deve conter o escopo "admin"
    E o contexto deve conter o método HTTP "GET"
    E o contexto deve conter o recurso "/customers"

  Cenário: Autorizar requisição com token válido e escopo customer
    Dado que existe um token JWT válido com escopo "customer"
    E a requisição é do tipo "GET" para o recurso "/customers/{customerId}"
    Quando o autorizador processar a requisição
    Então a requisição deve ser autorizada
    E o contexto deve conter o escopo "customer"

  Cenário: Negar requisição com escopo insuficiente
    Dado que existe um token JWT válido com escopo "customer"
    E a requisição é do tipo "DELETE" para o recurso "/foodItems/{foodItemId}"
    Quando o autorizador processar a requisição
    Então a requisição não deve ser autorizada

  Cenário: Autorizar criação de pedido com escopo customer
    Dado que existe um token JWT válido com escopo "customer"
    E a requisição é do tipo "POST" para o recurso "/customerOrders"
    Quando o autorizador processar a requisição
    Então a requisição deve ser autorizada


  Esquema do Cenário: Validar autorização por escopo e recurso
    Dado que existe um token JWT válido com escopo "<escopo>"
    E a requisição é do tipo "<metodo>" para o recurso "<recurso>"
    Quando o autorizador processar a requisição
    Então a requisição deve ter resultado "<resultado>"

    Exemplos:
      | escopo    | metodo | recurso                              | resultado   |
      | admin     | DELETE | /foodItems/{foodItemId}              | autorizado  |
      | admin     | POST   | /foodItems                           | autorizado  |
      | admin     | GET    | /kitchenOrders                       | autorizado  |
      | customer  | GET    | /customers/{customerId}              | autorizado  |
      | customer  | POST   | /customerOrders                      | autorizado  |
      | customer  | DELETE | /foodItems/{foodItemId}              | negado      |
      | totem     | POST   | /customers                           | autorizado  |
      | totem     | DELETE | /customers/{customerId}              | negado      |
      | monitor   | GET    | /kitchenOrders                       | autorizado  |
      | monitor   | DELETE | /foodItems/{foodItemId}              | negado      |

