# Revisão de coordenadas de estabelecimentos — 2026-10-27

Arquivo de preparação, não é payload da API. Em 2026-10-27, o usuário confirmou as 13 sugestões de coordenadas e pediu para adiar os 3 casos sem resultado. As 2 coordenadas já existentes foram preservadas. A confirmação se refere aos pontos selecionados; endereços divergentes e IDs de origem permanecem preservados, e nenhum registro foi cadastrado ou atualizado na API.

18 estabelecimentos: 13 com sugestão Geoapify, 2 com coordenadas originais preservadas e 3 sem candidato adequado. Entre as 13 sugestões, 6 são por endereço e 7 por estabelecimento/campus. Confidence mede correspondência da busca, não garante posição exata de entrada. Coordenadas de campus/hotel podem ser centro da área.

O pacote local atualizado revisao-geoapify-confirmada-2026-10-27.zip contém os resultados das consultas, JSON/CSV de revisão, `aprovacao-coordenadas.json`, `08_establishments_confirmed_coordinates_not_api_payload.json` (rascunho com os 13 pontos aplicados), `09_catalog_coverage.json` (cobertura dos vínculos) e a origem preservada. A aprovação das coordenadas está registrada separadamente de `approvedForImport=false`: UUIDs e outros campos ainda precisam ser preparados. O pacote não contém a chave Geoapify.

## Cobertura do arquivo recebido

O ZIP original contém 18 estabelecimentos e 18 registros de benefício; todos esses benefícios possuem vínculo com um estabelecimento da origem. O conjunto é limitado às unidades fornecidas no arquivo e não comprova que todas as unidades de todos os parceiros estejam incluídas.

| Grupo | Estabelecimentos no arquivo | Registros de benefício |
|---|---|---|
| Malwee | 8 | 8 |
| Univali | 7 | 7 |
| Beto Carrero World | 1 | 1 |
| Vila Encantada | 1 | 1 |
| Hotel Fazenda Casarão do Vale | 1 | 1 |
| **Total** | **18** | **18** |

Os payloads dos 8 benefícios Malwee são iguais entre si; o mesmo ocorre com os 7 da Univali. Portanto, os 18 registros representam 5 ofertas distintas no conteúdo recebido. O modelo atual permite compartilhar um benefício entre várias unidades, mas os IDs não foram unificados nesta etapa. Ao adiar os estabelecimentos 1003, 1004 e 1013, seus registros de benefício correspondentes continuam na origem e dependem de vínculo posterior se os 18 benefícios forem importados separadamente.

| ID | Estabelecimento | Revisão | Latitude | Longitude | Abrir ponto |
|---|---|---|---|---|---|
| 1001 | Malwee — Linhares | Confirmado pelo usuário (endereço) | -19.400813 | -40.067323 | [Conferir](https://www.openstreetmap.org/?mlat=-19.400813&mlon=-40.067323#map=18/-19.400813/-40.067323) |
| 1002 | Malwee — Araçuaí | Confirmado pelo usuário (endereço) | -16.850144 | -42.065697 | [Conferir](https://www.openstreetmap.org/?mlat=-16.850144&mlon=-42.065697#map=18/-16.850144/-42.065697) |
| 1003 | Malwee — Mamanguape | Sem candidato adequado — adiado |  |  | — |
| 1004 | Malwee — Palmas | Sem candidato adequado — adiado |  |  | — |
| 1005 | Malwee — Pederneiras | Confirmado pelo usuário (endereço) | -22.353285 | -48.774667 | [Conferir](https://www.openstreetmap.org/?mlat=-22.353285&mlon=-48.774667#map=18/-22.353285/-48.774667) |
| 1006 | Malwee — Paracatu | Confirmado pelo usuário (endereço) | -17.220928 | -46.874703 | [Conferir](https://www.openstreetmap.org/?mlat=-17.220928&mlon=-46.874703#map=18/-17.220928/-46.874703) |
| 1007 | Malwee — Cianorte | Confirmado pelo usuário (endereço) | -23.659124 | -52.608818 | [Conferir](https://www.openstreetmap.org/?mlat=-23.659124&mlon=-52.608818#map=18/-23.659124/-52.608818) |
| 1008 | Malwee — Palotina | Confirmado pelo usuário (endereço) | -24.283033 | -53.838967 | [Conferir](https://www.openstreetmap.org/?mlat=-24.283033&mlon=-53.838967#map=18/-24.283033/-53.838967) |
| 1009 | UNIVALI — Campus Itajaí | Confirmado pelo usuário (POI) | -26.916490 | -48.664587 | [Conferir](https://www.openstreetmap.org/?mlat=-26.916490&mlon=-48.664587#map=18/-26.916490/-48.664587) |
| 1010 | UNIVALI — Campus Balneário Camboriú | Confirmado pelo usuário (POI) | -27.006660 | -48.635198 | [Conferir](https://www.openstreetmap.org/?mlat=-27.006660&mlon=-48.635198#map=18/-27.006660/-48.635198) |
| 1011 | UNIVALI — Campus Balneário Piçarras | Confirmado pelo usuário (POI) | -26.754454 | -48.687639 | [Conferir](https://www.openstreetmap.org/?mlat=-26.754454&mlon=-48.687639#map=18/-26.754454/-48.687639) |
| 1012 | UNIVALI — Campus Centro Biguaçu | Confirmado pelo usuário (POI) | -27.500775 | -48.657449 | [Conferir](https://www.openstreetmap.org/?mlat=-27.500775&mlon=-48.657449#map=18/-27.500775/-48.657449) |
| 1013 | UNIVALI — Campus Florianópolis | Sem candidato adequado — adiado |  |  | — |
| 1014 | UNIVALI — Campus Kobrasol | Confirmado pelo usuário (POI) | -27.597894 | -48.619821 | [Conferir](https://www.openstreetmap.org/?mlat=-27.597894&mlon=-48.619821#map=18/-27.597894/-48.619821) |
| 1015 | UNIVALI — Campus Tijucas | Confirmado pelo usuário (POI) | -27.239506 | -48.644211 | [Conferir](https://www.openstreetmap.org/?mlat=-27.239506&mlon=-48.644211#map=18/-27.239506/-48.644211) |
| 1016 | Beto Carrero World — Parque | Coordenada original preservada | -26.804230 | -48.617470 | [Conferir](https://www.openstreetmap.org/?mlat=-26.804230&mlon=-48.617470#map=18/-26.804230/-48.617470) |
| 1017 | Vila Encantada — Pomerode | Coordenada original preservada | -26.737120 | -49.174090 | [Conferir](https://www.openstreetmap.org/?mlat=-26.737120&mlon=-49.174090#map=18/-26.737120/-49.174090) |
| 1018 | Hotel Fazenda Casarão do Vale — Massaranduba | Confirmado pelo usuário (POI) | -26.745240 | -49.030284 | [Conferir](https://www.openstreetmap.org/?mlat=-26.745240&mlon=-49.030284#map=18/-26.745240/-49.030284) |

## Observações de qualidade preservadas

As observações abaixo registram a qualidade dos dados e a seleção feita. A confirmação dos 13 pontos pelo usuário foi registrada; ela não substitui campos obrigatórios ausentes nem resolve por suposição divergências de endereço.

### 1001 — Malwee — Linhares

- Candidato encontrado somente com a variante Avenida Governador Carlos Lindemberg. Preservar a grafia original até confirmar.
- CEP de retorno 29900-201 diverge de 29900-202 da origem; não substituir automaticamente.

### 1002 — Malwee — Araçuaí

- Rua e número correspondem; CEP de retorno 39600-000 difere de 39600-002 da origem.

### 1003 — Malwee — Mamanguape

- Todas as buscas retornaram apenas centro de município; nenhum pin foi sugerido.

### 1004 — Malwee — Palmas

- Busca por nome trouxe loja em outra cidade e busca textual trouxe rua diferente. Resultados rejeitados.
- Bairro ausente e indicação de Rua SE incompleta; confirmar endereço da loja.

### 1005 — Malwee — Pederneiras

- Rua e número correspondem; conferir o acesso da loja no mapa.

### 1006 — Malwee — Paracatu

- Rua e número correspondem; conferir o acesso da loja no mapa.

### 1007 — Malwee — Cianorte

- Rua e número correspondem; conferir o acesso da loja no mapa.

### 1008 — Malwee — Palotina

- Rua e número correspondem; CEP de retorno 85952-150 difere de 85950-000 da origem.

### 1009 — UNIVALI — Campus Itajaí

- Escolhido POI Universidade do Vale do Itajaí, não o primeiro prédio/UBS com o mesmo endereço. Ponto do campus pode não ser a entrada.

### 1010 — UNIVALI — Campus Balneário Camboriú

- POI Univali corresponde à cidade, mas retorna Rua Armazém; origem informa 5ª Avenida, 1100. Conferir acesso do campus.

### 1011 — UNIVALI — Campus Balneário Piçarras

- Escolhido POI do campus, não o museu no mesmo complexo. Origem usa Rua Sambaqui e retorno Avenida Sambaqui.

### 1012 — UNIVALI — Campus Centro Biguaçu

- Escolhido POI Univali; endereço/CEP/bairro diferem entre páginas oficiais. Origem preservada até revisão.

### 1013 — UNIVALI — Campus Florianópolis

- Fonte atual/origem informa 502, enquanto outras páginas e documentos oficiais informam 5025.
- Consulta original teve falha de rede; buscas complementares retornaram locais incompatíveis. Nenhum pin foi sugerido.

### 1014 — UNIVALI — Campus Kobrasol

- POI corresponde ao nome completo da universidade, bairro e município; confidence retornado é 0, exigindo confirmação explícita.
- Número predial ausente. Km 207 não deve ser convertido em número 207 nem preenchido com 0 por suposição.
- Há divergências de CEP entre fonte original, páginas oficiais e geocoder.

### 1015 — UNIVALI — Campus Tijucas

- POI Univali corresponde ao nome e município; há divergência Rua Pará/Rua Paraná entre páginas oficiais e Areias/Universitário entre geocoder e origem.

### 1016 — Beto Carrero World — Parque

- Coordenadas da origem preservadas, sem nova verificação pelo provedor; conferir acesso do parque.

### 1017 — Vila Encantada — Pomerode

- Coordenadas da origem preservadas, sem nova verificação pelo provedor; conferir acesso da atração.

### 1018 — Hotel Fazenda Casarão do Vale — Massaranduba

- POI tem o nome exato do hotel e município, mas rua e CEP retornados divergem do endereço publicado pelo hotel.
- CEP da origem permanece ausente; não usar o CEP de retorno automaticamente.

## Como concluir

1. Usar as 13 coordenadas confirmadas e preservar as 2 já existentes. Os estabelecimentos 1003, 1004 e 1013 ficam adiados, com os dados originais mantidos.
2. Permanecem pendentes o número de 1014 e o CEP de 1018 para incluí-los num lote válido. O bairro ausente de 1004 permanece como está, pois essa unidade foi adiada. Não substituir os endereços divergentes por suposição.
3. Obter UUIDs reais de categorias, benefícios e serviços já cadastrados. Montar um payload separado com o DTO atual; este arquivo não pode ser enviado à API.
4. Rever ativação: a fonte marca as unidades como inativas e a API cria estabelecimentos ativos. Nenhuma importação foi executada nesta etapa.

## Fontes e atribuições

- [Powered by Geoapify](https://www.geoapify.com/). Atribuições/licenças específicas foram preservadas em cada candidato JSON.
- [© OpenStreetMap contributors](https://www.openstreetmap.org/copyright), [© OpenAddresses contributors](https://openaddresses.io/) e [Who's On First](https://www.whosonfirst.org/docs/licenses/).
- [Malwee: lojas oficiais](https://www.malwee.com.br/nossas-lojas).
- [Univali: portal de contatos](https://portal.univali.br/telefones-e-enderecos), [página institucional com endereços divergentes](https://www.univali.br/institucional/vrgdi/gerencia-de-atencao-ao-estudante/Paginas/default.aspx), [evento Tijucas 2026](https://portal.univali.br/eventos/10525) e [evento Biguaçu 2026](https://portal.univali.br/eventos/10524).
- [Casarão do Vale: endereço publicado pelo hotel](https://www.casaraodovale.com.br/contato/).

A disponibilidade do benefício WEG em cada unidade continua dependente da confirmação da equipe; um endereço ou POI encontrado não confirma parceria comercial.
