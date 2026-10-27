# Referência HTTP da API WEG Benefits

Referência prática das rotas presentes no código. Use a especificação gerada pela aplicação em `/v1/api-docs` como complemento. Todas as respostas de negócio usam JSON, salvo as respostas `204 No Content`.

## Endereços

| Ambiente | URL base |
|---|---|
| Local | `http://localhost:8081` |
| Demonstração | `https://weg-benefits-api.onrender.com` |

O prefixo de negócio é `/v1`. Rotas protegidas exigem sessão autenticada. O login retorna um cookie de sessão; preserve e envie esse cookie nas requisições seguintes. Rotas `/v1/admin/**` exigem papel `ADMIN`. Não existe cadastro público.

## Fluxo de autenticação

### Entrar

`POST /v1/auth/login` — público. Recebe username ou e-mail e senha. Sucesso: `200`, perfil do usuário no corpo e cookie de sessão `HttpOnly` na resposta.

```json
{
  "login": "admin",
  "password": "senha informada no login"
}
```

Resposta:

```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@gmail.com",
  "role": "ADMIN",
  "active": true,
  "createdAt": "2026-10-26T12:00:00Z",
  "updatedAt": "2026-10-26T12:00:00Z"
}
```

Mantenha o cookie retornado pelo servidor. Em PowerShell, `Invoke-RestMethod` pode armazená-lo em uma variável de sessão:

```powershell
$loginBody = @{ login = "admin"; password = "sua-senha" } | ConvertTo-Json
Invoke-RestMethod -Uri "https://weg-benefits-api.onrender.com/v1/auth/login" `
  -Method Post -ContentType "application/json" -Body $loginBody `
  -SessionVariable benefitsSession
```

### Consultar a própria conta e sair

| Método | Rota | Acesso | Resultado |
|---|---|---|---|
| `GET` | `/v1/me` | Sessão | Perfil autenticado (`200`) |
| `POST` | `/v1/auth/logout` | Sessão, se existente | Invalida sessão (`204`) |

```powershell
Invoke-RestMethod -Uri "https://weg-benefits-api.onrender.com/v1/me" -WebSession $benefitsSession
Invoke-RestMethod -Uri "https://weg-benefits-api.onrender.com/v1/auth/logout" `
  -Method Post -WebSession $benefitsSession
```

## Catálogo: leitura

Todas as rotas abaixo exigem sessão. Os itens desativados não aparecem nas consultas normais.

| Método | Rota | Parâmetros | Resultado |
|---|---|---|---|
| `GET` | `/v1/categories/options` | — | Paleta de cores e provedor de busca de ícones |
| `GET` | `/v1/categories` | `page`, `size` | Categorias ativas, paginadas |
| `GET` | `/v1/services` | `page`, `size` | Serviços ativos, paginados |
| `GET` | `/v1/establishments` | `query`, `categoryId`, `serviceId`, `channel`, `forMap`, `page`, `size` | Parceiros ativos, paginados; lista inclui online |
| `GET` | `/v1/establishments/{id}` | UUID em `id` | Detalhe do estabelecimento ativo |
| `GET` | `/v1/establishments/{id}/benefits` | `page`, `size` | Benefícios ativos do estabelecimento |

Paginação começa em `page=0`; `size` padrão é `20`. A listagem administrativa aceita até 100 itens por página e a busca pública de estabelecimentos limita o tamanho a 50. A busca `query` aceita até 100 caracteres.

## Catálogo: administração

Todas as rotas desta seção exigem papel `ADMIN`. As listas administrativas usam `page=0` e `size=20` por padrão, com tamanho máximo de 100. As rotas de desativação preservam o registro e suas relações.

| Recurso | Listar | Consultar por ID | Criar | Criar em lote | Editar | Desativar |
|---|---|---|---|---|---|---|
| Categorias | `GET /v1/admin/categories` | `GET /v1/admin/categories/{id}` | `POST /v1/admin/categories` | `POST /v1/admin/categories/batch` | `PATCH /v1/admin/categories/{id}` | `POST /v1/admin/categories/{id}/deactivation` |
| Serviços | `GET /v1/admin/services` | `GET /v1/admin/services/{id}` | `POST /v1/admin/services` | `POST /v1/admin/services/batch` | `PATCH /v1/admin/services/{id}` | `POST /v1/admin/services/{id}/deactivation` |
| Estabelecimentos | `GET /v1/admin/establishments` | `GET /v1/admin/establishments/{id}` | `POST /v1/admin/establishments` | `POST /v1/admin/establishments/batch` | `PATCH /v1/admin/establishments/{id}` | `POST /v1/admin/establishments/{id}/deactivation` |
| Benefícios | `GET /v1/admin/benefits` | `GET /v1/admin/benefits/{id}` | `POST /v1/admin/benefits` | `POST /v1/admin/benefits/batch` | `PATCH /v1/admin/benefits/{id}` | `POST /v1/admin/benefits/{id}/deactivation` |

Criações individuais retornam `201 Created` com o objeto criado; atualizações retornam `200`; desativações retornam `204`. IDs do catálogo são UUID. Em `PATCH`, envie somente os campos que deseja alterar.

### Criar vários itens do catálogo de uma vez

Cada endpoint `/batch` recebe um objeto com uma lista chamada conforme o recurso (`categories`, `services`, `establishments` ou `benefits`). Cada lista aceita de 1 a 100 objetos e cada objeto usa o mesmo formato e validações da rota de criação individual. A resposta é `201 Created` com uma lista na mesma ordem. O lote é atômico: se qualquer item falhar na validação, nas regras de negócio ou ao salvar, nenhum item daquele lote é mantido.

Exemplo de categorias:

```json
{
  "categories": [
    { "name": "Alimentação", "iconKey": "mdi:food", "colorHex": "#E65100" },
    { "name": "Mobilidade", "iconKey": "mdi:car", "colorHex": "#0D47A1" }
  ]
}
```

Exemplo de serviços:

```json
{
  "services": [
    { "name": "Estacionamento" },
    { "name": "Delivery" }
  ]
}
```

Para estabelecimentos, envie os objetos completos descritos na seção abaixo dentro de `establishments`. Cada um precisa referenciar uma categoria ativa e um benefício existente com pelo menos um desconto. Portanto, cadastre categorias e benefícios antes de enviar o lote de estabelecimentos; o mesmo benefício pode ser compartilhado. Os lotes são separados por recurso e não formam uma transação única entre endpoints.

Os lotes de benefícios continuam aceitando `benefits` e também criam seus descontos. Todos os endpoints `/batch` compartilham o limite de 100 itens. Em timeout sem resposta, consulte a lista administrativa antes de repetir, pois esses endpoints não usam chave de idempotência.

### Criar um benefício

`POST /v1/admin/benefits` cadastra um benefício e seus descontos; não associa estabelecimento. Ele começa inativo e pode ser associado depois ao criar ou editar um estabelecimento.

O mesmo benefício pode ser usado em vários estabelecimentos. Edições ou desativações feitas no benefício compartilhado se aplicam a todos os estabelecimentos associados.

```json
{
  "audience": "Colaboradores WEG e familiares",
  "participationInstructions": "Apresente o crachá WEG ou documento funcional no momento do pagamento.",
  "contact": "(47) 99914-1233",
  "discounts": [
    {
      "value": "Até 50%",
      "description": "Em produtos selecionados no estabelecimento participante."
    },
    {
      "value": "Frete grátis",
      "description": "Nas compras acima do valor mínimo informado pelo estabelecimento."
    }
  ]
}
```

Campos obrigatórios: `audience`, `participationInstructions`, `contact` e pelo menos um `discount`. Cada desconto exige `value` (2–15 caracteres) e `description` não vazia. Instruções e descrições são texto sem limite de domínio; a API mantém a escrita recebida, sem truncar, preencher ou corrigir. O benefício não tem nome, título ou atributo de condições.

### Criar vários benefícios de uma vez

`POST /v1/admin/benefits/batch` aceita **de 1 a 100 benefícios**. Cada entrada de `benefits` tem exatamente o formato do corpo de `POST /v1/admin/benefits`, incluindo pelo menos um desconto.

```json
{
  "benefits": [
    {
      "audience": "Colaboradores WEG e familiares",
      "participationInstructions": "Apresente o crachá WEG ou documento funcional no momento do pagamento.",
      "contact": "(47) 99914-1233",
      "discounts": [
        {
          "value": "Até 50%",
          "description": "Em produtos selecionados no estabelecimento participante."
        }
      ]
    },
    {
      "audience": "Colaboradores WEG",
      "participationInstructions": "Apresente o crachá WEG ou documento funcional antes de finalizar o pagamento.",
      "contact": "contato@empresa.com",
      "discounts": [
        {
          "value": "10% OFF",
          "description": "Em todos os serviços participantes desta oferta."
        }
      ]
    }
  ]
}
```

Sucesso: `201 Created`, com um array de respostas na mesma ordem dos itens enviados. Cada benefício terá UUID, `active: false` e `establishmentCount: 0` até ser associado. As respostas dos endpoints administrativos de benefício informam `establishmentCount`, a quantidade de estabelecimentos associados.

O lote é atômico: se qualquer item estiver inválido ou ocorrer erro ao salvar, a transação inteira é revertida e nenhum benefício do lote é mantido. A rota cria benefícios e descontos; ela **não** cria estabelecimentos nem categorias, não associa benefícios e não envia e-mail. O campo `conditions` não faz parte do contrato. Em erro, corrija o lote inteiro antes de reenviar. Após um timeout sem resposta, consulte `GET /v1/admin/benefits` para confirmar se o servidor concluiu a chamada antes de repeti-la; o endpoint não usa chave de idempotência.

### Campos para criar categoria, serviço e estabelecimento

Exemplos mínimos de criação:

Exemplo de `POST /v1/admin/categories`:

```json
{
  "name": "Alimentação",
  "iconKey": "mdi:food",
  "colorHex": "#E65100"
}
```

Exemplo de `POST /v1/admin/services`:

```json
{ "name": "Estacionamento" }
```

Exemplo de `POST /v1/admin/establishments`:

```json
{
  "name": "Restaurante Exemplo",
  "description": "Restaurante participante do programa de benefícios para colaboradores WEG.",
  "postalCode": "89200000",
  "streetNumber": 100,
  "street": "Rua de Exemplo",
  "district": "Centro",
  "city": "Joinville",
  "state": "SC",
  "latitude": -26.3,
  "longitude": -48.8,
  "categoryId": "UUID_DA_CATEGORIA",
  "benefitId": "UUID_DO_BENEFICIO",
  "serviceIds": []
}
```

Para criar estabelecimento, categoria ativa e benefício existente são obrigatórios; o benefício deve ter desconto e pode já estar associado a outros estabelecimentos. Cada estabelecimento mantém apenas um benefício, e o mesmo benefício pode ser compartilhado. `serviceIds` pode ser uma lista vazia. `facadeImageUrl` HTTPS e `altitude` são opcionais. O estado é a sigla de duas letras.

O nome do estabelecimento exige pelo menos 2 caracteres; sua descrição é obrigatória/não vazia.
Ambos usam TEXT, sem máximo de domínio. O campo `channel` aceita `PHYSICAL` (padrão quando omitido),
`ONLINE` ou `HYBRID`. PHYSICAL/HYBRID exigem endereço completo e latitude/longitude; ONLINE exige
endereço e coordenadas ausentes/nulos, inclusive altitude. Exemplo exclusivamente online:

```json
{
  "name": "Parceiro online",
  "description": "20%",
  "channel": "ONLINE",
  "categoryId": "UUID_REAL_DA_CATEGORIA",
  "benefitId": "UUID_REAL_DO_BENEFICIO",
  "serviceIds": []
}
```

Substitua os dois marcadores pelos UUIDs retornados pela API. O objeto também pode integrar
`establishments` no endpoint `/batch`. A resposta inclui `channel` e `mappable` (false para ONLINE).
`GET /v1/establishments?channel=ONLINE` filtra parceiros online; `?forMap=true` retorna somente
PHYSICAL/HYBRID com coordenadas. Sem filtro, todos os canais aparecem. Não criar pins com coordenadas nulas.

`PATCH /v1/admin/establishments/{id}` com `{"channel":"ONLINE"}` limpa o endereço anterior
na mesma transação. Para voltar a PHYSICAL/HYBRID, enviar a localização completa. Informar endereço
ou coordenadas junto de ONLINE retorna `400`. Favoritos de ONLINE são permitidos; visitas presenciais
a esse canal retornam `400`, preservando histórico anterior.

Preparação dos lotes e mapeamento de UUIDs: [guia de carga](carga-catalogo.md).

## Usuários: administração

Todas as rotas exigem `ADMIN`.

| Método | Rota | Resultado |
|---|---|---|
| `GET` | `/v1/admin/users?page=0&size=20` | Lista paginada |
| `POST` | `/v1/admin/users` | Cria conta e envia credenciais por e-mail |
| `GET` | `/v1/admin/users/{id}` | Detalhe não sensível |
| `PATCH` | `/v1/admin/users/{id}` | Atualiza username, e-mail ou papel conforme as regras de Identity |
| `POST` | `/v1/admin/users/{id}/deactivation` | Desativa colaborador (`204`) |

`POST /v1/admin/users` exige `username`, `email` e `password`; `role` é opcional e assume `COLLABORATOR`. A conta só é persistida se a Gmail API aceitar o envio. O primeiro ADMIN é criado pelo CLI, não por esta rota.

## Favoritos e visitas

Estas operações pertencem à conta da sessão autenticada, inclusive para ADMIN.

| Método | Rota | Resultado |
|---|---|---|
| `GET` | `/v1/me/favorites?page=0&size=20` | Favoritos paginados |
| `PUT` | `/v1/me/favorites/{establishmentId}` | Adiciona favorito de forma idempotente (`204`) |
| `DELETE` | `/v1/me/favorites/{establishmentId}` | Remove favorito (`204`) |
| `GET` | `/v1/me/visits?page=0&size=20` | Histórico paginado |
| `POST` | `/v1/me/visits` | Registra evento final de visita |

```json
{
  "establishmentId": "UUID_DO_ESTABELECIMENTO",
  "occurredAt": "2026-10-26T15:30:00Z"
}
```

Uma visita nova retorna `201`; reenvio idempotente ou visita duplicada dentro da janela de 30 minutos retorna `200` com o evento já registrado. A API recebe somente o horário do evento, não trilha GPS.

## Saúde e documentação gerada

| Método | Rota | Acesso | Resultado |
|---|---|---|---|
| `GET` | `/actuator/health` | Público | Estado resumido da aplicação |
| `GET` | `/actuator/health/liveness` | Público | Liveness |
| `GET` | `/actuator/health/readiness` | Público | Readiness, inclui dependências necessárias |
| `GET` | `/v1/api-docs` | Público | OpenAPI JSON |
| `GET` | `/swagger-ui.html` | Público | Interface Swagger UI |

URLs locais: `http://localhost:8081/actuator/health`, `http://localhost:8081/v1/api-docs` e `http://localhost:8081/swagger-ui.html`.

## Erros e rastreamento

Os erros usam Problem Detail JSON e incluem `status`, `title`, `detail` e `requestId`. Respostas e logs não devem expor senha, hash, token, stack trace ou localização precisa. O servidor retorna `X-Request-Id` para correlacionar a resposta com os logs.

| HTTP | Significado usual |
|---|---|
| `400` | JSON inválido ou campo fora das validações |
| `401` | Sessão ausente/inválida ou credenciais inválidas |
| `403` | Sessão autenticada sem o papel necessário |
| `404` | Registro inexistente ou indisponível |
| `409` | Conflito de unicidade ou de associação |
| `5xx` | Falha interna; resposta sem detalhes sensíveis |

