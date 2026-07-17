# Backend Atlas Performance Contract

## Objetivo

Reduzir o tempo de carregamento percebido no app Android diminuindo:

- payloads excessivos em endpoints de lista
- round trips extras apos mutacoes
- recarga de catalogos quase estaticos

Este documento descreve o que o app consome hoje e o contrato recomendado para o backend.

## Principais gargalos observados

### 1. Lista de pedidos com payload profundo

Hoje o app chama:

- `GET /orders?filters[company][id][$eq]={companyId}&filters[dispatcher][id][$eq]={dispatcherId}&populate=deep,3`

Problema:

- `populate=deep,3` em uma listagem tende a trazer relacionamentos demais
- a tela de lista nao precisa de itens e recebiveis completos de cada pedido
- isso aumenta tempo de resposta, parsing e uso de memoria

### 2. Detalhe de pedido ainda usa populate amplo

Hoje o app chama:

- `GET /sales-orders/{id}?populate=deep,3`

Problema:

- para detalhe faz mais sentido que a lista, mas ainda traz mais dados do que o app realmente usa

### 3. Mutacoes sem retorno do pedido atualizado

Hoje estas rotas fazem o app chamar um segundo endpoint logo depois:

- `POST /receivables/{id}/confirm-payment`
- `PUT /order-receivables/{id}`
- `PUT /sales-orders/{id}`

Problema:

- cada acao do usuario gera 2 requests
- o primeiro altera estado e o segundo busca o pedido atualizado

### 4. Catalogos sem contrato enxuto

Hoje o app carrega:

- `GET /vehicle-types`
- `GET /payment-methods`
- `GET /salespeople?page=1&pageSize=100`

Problema:

- sao dados quase estaticos
- parte relevante do payload nao e usada pelo app
- sem `ETag` ou `Cache-Control`, o cliente precisa revalidar com mais frequencia

## Endpoints e campos realmente usados pelo app

### GET /orders

Uso atual no app:

- `id`
- `status`
- `originalAmount`
- `currentAmount`
- `billingDate`
- `createdAt`
- `customers.data.id`
- `customers.data.attributes.name`
- `customers.data.attributes.cpfCnpj`
- `customers.data.attributes.phoneNumber`
- `customers.data.attributes.email`
- `companies.data.attributes.trade_name`
- `vehicle_types.data.id`
- `vehicle_types.data.attributes.name`
- `sales_order_items[].id`
- `sales_order_items[].attributes.total_price`
- `sales_order_items[].attributes.discount`
- `sales_order_items[].attributes.sales_item_id`
- `sales_order_items[].attributes.unit_price`
- `sales_order_items[].attributes.sales_items.data.attributes.name`
- `receivables[].id`
- `receivables[].documentId`
- `receivables[].attributes.status`
- `receivables[].attributes.installments`
- `receivables[].attributes.paymentDate`
- `receivables[].attributes.amountOriginal`
- `receivables[].attributes.amountFinal`
- `receivables[].attributes.tax`
- `receivables[].attributes.card_last4`
- `receivables[].attributes.cardHolder`
- `receivables[].attributes.cardBrand`
- `receivables[].attributes.authorizationCode`
- `receivables[].attributes.pixTxIdCode`
- `receivables[].attributes.payment_methods.data.id`
- `receivables[].attributes.payment_methods.data.attributes.name`
- `receivables[].attributes.payment_methods.data.attributes.max_installments`
- `receivables[].attributes.payment_methods.data.attributes.interest_tax`
- `receivables[].attributes.payment_methods.data.attributes.paymentType`
- `salesman.data.id`
- `salesman.data.attributes.name`

Observacao:

- a tela de lista nao precisa desse nivel de detalhe

### GET /sales-orders/{id}

Uso atual no app:

- todos os campos listados acima fazem sentido para o detalhe

### GET /vehicle-types

Uso atual no app:

- `id`
- `attributes.name`

Nao usado:

- `documentId`
- `attributes.isActive`
- `attributes.createdAt`
- `meta`

### GET /payment-methods

Uso atual no app:

- `id`
- `name`
- `max_installments`
- `interest_tax`
- `paymentType`

Nao usado:

- `documentId`
- `is_online_payment`
- `is_active`
- `transaction_fee_rate`
- `transaction_fixed_fee`
- `createdAt`
- `updatedAt`
- `publishedAt`
- `locale`

### GET /salespeople

Uso atual no app:

- `id`
- `attributes.name`
- `attributes.phoneNumber`
- `attributes.email`
- `attributes.isActive`

Nao usado:

- `documentId`
- `attributes.cpf`
- `attributes.company_id`
- `meta`, exceto se houver paginacao futura na UI

### POST /sales-items/simulation

Uso atual no app:

- `data.attributes.billing_date`
- `data.attributes.cpf_cnpj`
- `data.attributes.name`
- `data.attributes.phone_number`
- `data.attributes.vehicle_price`
- `data.attributes.is_vehicle_financed`
- `data.attributes.is_vehicle_special_plate`
- `data.attributes.current_amount`
- `data.attributes.vehicle_type_id`
- `data.attributes.items[].id`
- `data.attributes.items[].attributes.name`
- `data.attributes.items[].attributes.is_discount_allowed`
- `data.attributes.items[].attributes.price`

## Contrato recomendado

### 1. Criar endpoint de lista resumida

Novo endpoint sugerido:

- `GET /orders/summary?companyId={companyId}&dispatcherId={dispatcherId}&page=1&pageSize=20`

Resposta sugerida:

```json
{
  "data": [
    {
      "id": 123,
      "status": "paid",
      "createdAt": "2026-03-06T10:30:00Z",
      "billingDate": "2026-03-06",
      "customerName": "Joao Silva",
      "vehicleTypeName": "Carro",
      "currentAmount": 1299.90,
      "salesmanName": "Maria",
      "paymentStatusSummary": "1/1 paid"
    }
  ],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "pageCount": 3,
    "total": 54
  }
}
```

Beneficios:

- reduz drasticamente o payload
- facilita paginação real
- evita `populate=deep,3` em listagem

### 2. Manter um endpoint de detalhe focado

Endpoint:

- `GET /sales-orders/{id}`

Resposta sugerida:

```json
{
  "data": {
    "id": 123,
    "status": "paid",
    "createdAt": "2026-03-06T10:30:00Z",
    "billingDate": "2026-03-06",
    "originalAmount": 1299.90,
    "currentAmount": 1299.90,
    "vehiclePrice": 50000.0,
    "isVehicleFinanced": false,
    "isSpecialPlate": false,
    "customer": {
      "id": 1,
      "name": "Joao",
      "cpfCnpj": "00000000000",
      "phoneNumber": "5511999999999",
      "email": "joao@email.com"
    },
    "company": {
      "id": 2,
      "tradeName": "Loja X"
    },
    "vehicleType": {
      "id": 3,
      "name": "Carro"
    },
    "salesman": {
      "id": 4,
      "name": "Maria"
    },
    "items": [
      {
        "id": 10,
        "salesItemId": 7,
        "name": "Servico A",
        "unitPrice": 300.0,
        "discount": 0.0,
        "totalPrice": 300.0
      }
    ],
    "receivables": [
      {
        "id": 20,
        "documentId": "abc",
        "status": "paid",
        "installments": 1,
        "paymentDate": "2026-03-06T10:40:00Z",
        "amountOriginal": 1299.90,
        "amountFinal": 1299.90,
        "tax": 0.0,
        "cardLast4": "1234",
        "cardHolder": "JOAO",
        "cardBrand": "VISA",
        "authorizationCode": "9999",
        "pixTxIdCode": null,
        "paymentMethod": {
          "id": 1,
          "name": "Credito",
          "maxInstallments": 12,
          "interestTax": 0.02,
          "paymentType": "credit"
        }
      }
    ]
  }
}
```

Observacao:

- se o backend ainda usar Strapi internamente, o ideal e montar esse DTO no servidor em vez de expor `populate=deep,3` diretamente ao mobile

### 3. Retornar pedido atualizado nas mutacoes

Recomendacao:

- `POST /receivables/{id}/confirm-payment` deve retornar o pedido atualizado
- `PUT /order-receivables/{id}` deve retornar o pedido atualizado
- `PUT /sales-orders/{id}` deve retornar o pedido atualizado

Alternativa minima:

- retornar um `orderSummary` atualizado contendo pelo menos:
  - `id`
  - `status`
  - `currentAmount`
  - `salesmanName`
  - `receivables`

Beneficio:

- elimina um segundo request apos cada acao

### 4. Enxugar catalogos

#### GET /vehicle-types

Resposta recomendada:

```json
{
  "data": [
    { "id": 1, "name": "Carro" },
    { "id": 2, "name": "Moto" }
  ]
}
```

#### GET /payment-methods

Resposta recomendada:

```json
{
  "data": [
    {
      "id": 1,
      "name": "Credito",
      "maxInstallments": 12,
      "interestTax": 0.02,
      "paymentType": "credit"
    }
  ]
}
```

#### GET /salespeople

Resposta recomendada:

```json
{
  "data": [
    {
      "id": 4,
      "name": "Maria",
      "phoneNumber": "5511999999999",
      "email": "maria@email.com",
      "isActive": true
    }
  ],
  "meta": {
    "page": 1,
    "pageSize": 20,
    "pageCount": 1,
    "total": 1
  }
}
```

## Regras de cache recomendadas

Aplicar em:

- `GET /vehicle-types`
- `GET /payment-methods`
- `GET /salespeople`

Recomendacao:

- `Cache-Control: public, max-age=300`
- `ETag`
- suporte a `If-None-Match`

Beneficio:

- reduz trafego
- acelera abertura de telas
- permite revalidacao barata

## Ordem sugerida de implementacao

### Fase 1

- criar `GET /orders/summary`
- fazer mutacoes retornarem pedido atualizado

### Fase 2

- reduzir payload de `GET /sales-orders/{id}`
- adicionar `ETag` e `Cache-Control` aos catalogos

### Fase 3

- criar `GET /registration/bootstrap?companyId={companyId}`
- retornar em uma unica resposta:
  - `vehicleTypes`
  - `paymentMethods`
  - `salespeople`

## Resultado esperado

Com as alteracoes acima, a expectativa e:

- lista de pedidos mais rapida
- detalhe com menor custo de rede
- menos requests apos pagamento, estorno e troca de vendedor
- abertura de cadastro mais previsivel
- menor custo de parsing no app Android
