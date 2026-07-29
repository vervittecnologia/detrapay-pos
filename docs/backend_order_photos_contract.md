# Contrato mobile para fotos do pedido

O POS usa o cadastro existente `sales_order_documents`. A captura e a interface Android
estao prontas, mas a Edge Function `mobile` precisa expor as operacoes abaixo. O handler
generico de `/orders` nao deve responder a estas rotas.

## Listar documentos

`GET /functions/v1/mobile/orders/{salesOrderId}/documents`

Resposta `200`:

```json
{
  "data": [
    {
      "id": 123,
      "sales_order_id": 555,
      "file_name": "pedido_555_20260729.jpg",
      "file_url": "uploads/uuid__pedido_555_20260729.jpg",
      "download_url": "https://url-assinada-temporaria",
      "mime_type": "image/jpeg",
      "file_size": 824,
      "created_at": "2026-07-29T19:00:00Z"
    }
  ]
}
```

`download_url` deve ser uma URL assinada temporaria do bucket privado
`sales-order-docs`. `file_size` usa KB, como o cadastro web atual.

## Anexar foto

`POST /functions/v1/mobile/orders/{salesOrderId}/documents`

- Corpo `multipart/form-data`.
- Parte obrigatoria `file`.
- Tipos aceitos inicialmente: `image/jpeg`, `image/png` e `image/webp`.
- Tamanho maximo: 10 MB.
- O backend grava o arquivo no bucket `sales-order-docs` e cria a linha em
  `sales_order_documents` com `sales_order_id`, `file_name`, `file_url`, `mime_type`
  e `file_size`.
- `document_type_id` pode ser nulo para fotos livres do pedido.

Resposta `200` ou `201`: mesmo objeto individual acima, envolvido por `{ "data": ... }`.

## Seguranca e erros

- Exigir a sessao Bearer usada pelos demais endpoints mobile.
- Validar que o usuario pode acessar a empresa/despachante do pedido.
- Retornar `401` para sessao invalida, `403` para pedido fora do escopo, `404` para
  pedido inexistente e `422` para arquivo invalido.
- A funcao legada `upload-document` nao atende este contrato: ela usa tabelas de vinculo
  antigas, service role sem validar o usuario e URL publica.

Sem essas duas rotas, a UI mostra a demanda objetivamente e conserva a foto apenas como
arquivo temporario para permitir nova tentativa; ela nunca informa que o anexo remoto foi
concluido.
