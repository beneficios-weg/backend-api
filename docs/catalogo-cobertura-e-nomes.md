# Cobertura do catálogo e nomes — 2026-10-30

O Mobile percorre as páginas da API e não contém corte em dez estabelecimentos. A consulta no Neon (production/neondb) confirmou as contagens abaixo. A carga existente é parcial: dos 112 benefícios do PDF, 63 têm estabelecimento ativo; são 69 estabelecimentos novos. Há ainda um estabelecimento/benefício histórico de teste, preservado: total de 70 estabelecimentos ativos, 113 benefícios e 49 sem estabelecimento ativo. Não reativar as 69 cópias desativadas anteriormente para aumentar a contagem.

| Categoria | Ofertas do PDF | Benefícios vinculados | Estabelecimentos ativos | Benefícios sem estabelecimento |
|---|---:|---:|---:|---:|
| Clientes WEG | 4 | 3 | 3 | 1 |
| Educação | 21 | 14 | 18 | 7 |
| Idiomas | 36 | 25 | 25 | 11 |
| Cursos e Treinamentos | 4 | 4 | 4 | 0 |
| Saúde e Bem-Estar | 30 | 10 | 10 | 20 |
| Lazer | 12 | 6 | 8 | 6 |
| Estética e Vestuário | 5 | 1 | 1 | 4 |
| Total do PDF | 112 | 63 | 69 | 49 |

Um benefício pode ser compartilhado por várias unidades. Benefício sem estabelecimento não forma um cartão de estabelecimento no Mobile, embora exista no administrativo. A preparação incluiu somente unidades com endereço/coordenadas completos e revisados. Também existem 24 unidades candidatas pendentes, algumas relacionadas a benefícios já publicados; esses conjuntos de pendências não são somáveis.

## Nome Lazer

O usuário confirmou **Lazer**, também visível no menu do PDF (`fontes/cartoes/p01-00.png` do pacote fiel). A carga usou **Lazer e Turismo**, enquanto o protótipo dizia Lazer. A API é a fonte dos nomes exibidos. As outras seis categorias do menu correspondem às importadas; não foram renomeadas.

DTOs e constraint V2 exigiam seis caracteres e impediam Lazer (cinco). A correção mínima aceita 5–75 na criação individual/em lote e edição, validando também o nome após remover espaços externos. V9 altera somente a constraint; preserva nomes, UUIDs e vínculos. Não editar migrações anteriores.

Após publicar a API com V9, corrigir a categoria existente pelo endpoint ADMIN:

```http
PATCH /v1/admin/categories/559285ef-2424-4237-ad43-460e35c4c052
Content-Type: application/json

{"name":"Lazer"}
```

O UUID foi confirmado na consulta real. Conferir antes que ainda identifica Lazer e Turismo; se já for Lazer, não repetir. Não criar outra categoria nem reenviar o lote. Verificar ID, ícone, cor, estado e os oito estabelecimentos depois do PATCH. O preparador reconhece a chave histórica do mapa de UUIDs e produz Lazer mantendo o ID; mapas com IDs contraditórios interrompem a preparação. **V9 não executa esse PATCH automaticamente e nenhuma escrita no catálogo hospedado foi feita nesta tarefa.**

O Mobile receberá o nome na próxima sincronização online/Atualizar; cache offline conserva o nome anterior até sincronizar. Não precisa substituir o texto no frontend.

## Textos e cobertura

A comparação dos 112 benefícios exportados em 2026-10-28 com o manifesto fiel não encontrou diferenças em público, contato, instruções e pares valor/descrição dos descontos. Essa conferência usa snapshots, não leitura ao vivo de todos os textos em 2026-10-30. Não corrigir erros de digitação da fonte nem preencher textos para satisfazer validações.

O preparador agora produz `09_catalog_coverage.json` com cada benefício sem estabelecimento pronto, incluindo os que não possuem unidade candidata. Trata-se de preparação, não importação. A execução com fontes/UUIDs existentes confirmou 69 prontos, 24 unidades pendentes e 49 benefícios sem estabelecimento pronto. Arquivos anteriores foram preservados. A nova preparação é revisão, não um lote incremental seguro para reenviar.

Auditoria detalhada em `target/auditoria-catalogo-2026-10-30/`: resumo JSON, CSV dos 49 benefícios com referências/páginas/UUIDs/pendências e PATCH de Lazer preparado. Copiar o pacote antes de limpar target. Endereços e coordenadas ausentes exigem pesquisa/revisão, mantendo Jaraguá do Sul e cidades próximas. Não transformar atendimento físico/híbrido em ONLINE nem fabricar localização. Conferir vínculos existentes para evitar duplicatas.

## Benefícios sem estabelecimento ativo

Referências derivadas dos snapshots pós-correção e UUIDs reais, com contagens corroboradas no Neon. Pendências detalhadas no CSV local; novos endereços e pontos exigem revisão antes de cadastro.

| Referência | Parceiro na fonte | Categoria | Páginas |
|---|---|---|---|
| PDF-002 | MRV Engenharia e Participações | Clientes WEG | 2 |
| PDF-005 | Centro Universitário Uninter | Educação | 4 |
| PDF-007 | Faculdade Estácio Brasil | Educação | 5 |
| PDF-008 | Faculdade UNINTER | Educação | 5 |
| PDF-015 | MULTIVIX | Educação | 9 |
| PDF-019 | Unidavi - Centro Universitário para o Desenvolvimento do Alto Vale do Itajaí | Educação | 11 |
| PDF-020 | Uningá - EAD | Educação | 12 |
| PDF-023 | Universidade Uningá EAD | Educação | 13, 14 |
| PDF-062 | Deivi Pereira Consultor de Imagem / Personal Stylist | Estética e Vestuário | 35 |
| PDF-063 | Grupo Elian | Estética e Vestuário | 35 |
| PDF-064 | Grupo Lampert (Hair Clinic, Nutrition e Fitness Clothes) | Estética e Vestuário | 36 |
| PDF-065 | Loja Malwee | Estética e Vestuário | 36 |
| PDF-026 | Alexander Díaz - Aulas de Espanhol para turmas presenciais ou online | Idiomas | 17 |
| PDF-028 | Beetools - Escola de Inglês | Idiomas | 18 |
| PDF-037 | Escola de Inglês Pércutz - Presencial (Gravataí) e Online | Idiomas | 22 |
| PDF-039 | HC School | Idiomas | 23 |
| PDF-040 | In.Glish Escola de Inglês | Idiomas | 24 |
| PDF-045 | KNN Idiomas | Idiomas | 26 |
| PDF-052 | The Camp | Idiomas | 30 |
| PDF-053 | The Fools - Imersão em Línguas | Idiomas | 30 |
| PDF-054 | TOP Idiomas | Idiomas | 31 |
| PDF-059 | WisEnglish Language School - Escola de Idiomas - Sertãozinho | Idiomas | 33 |
| PDF-061 | Zaira Francener Muniz - Professora de Inglês. | Idiomas | 34 |
| PDF-068 | Beto Carrero World | Lazer | 39 |
| PDF-070 | Hotel Fazenda Casarão do Vale | Lazer | 40 |
| PDF-071 | Hoteles Bagu | Lazer | 40 |
| PDF-073 | Loja Jardim Home & Design (Loja de fábrica Estofados Jardim) | Lazer | 41 |
| PDF-075 | Parque Vila Encantada - Pomerode/SC | Lazer | 42 |
| PDF-078 | Travelum Turismo | Lazer | 44 |
| PDF-079 | Ana Carolina Oliveira Psicologia LTDA | Saúde e Bem-Estar | 45 |
| PDF-081 | Clínica de Psicoterapia e Avaliação Psicológica Mente em Foco (Jaraguá do Sul) | Saúde e Bem-Estar | 46 |
| PDF-082 | Fonoaudióloga Larissa Tavares | Saúde e Bem-Estar | 47 |
| PDF-083 | Fonoaudióloga Paula Andressa Gnatkowski | Saúde e Bem-Estar | 47 |
| PDF-084 | Grupo Ethernos | Saúde e Bem-Estar | 48 |
| PDF-085 | Larissa Tavares - Consultora de amamentação e especialista em furo especializado | Saúde e Bem-Estar | 48 |
| PDF-086 | Lucilena Vogel Psicologia | Saúde e Bem-Estar | 49 |
| PDF-087 | Luiz Antonio Furlani - Psicólogo CRP-12/29354 | Saúde e Bem-Estar | 49 |
| PDF-089 | Nutricionista Larissa Lourenço | Saúde e Bem-Estar | 50 |
| PDF-091 | Psicóloga Alice Müller - Presencial e on-line | Saúde e Bem-Estar | 50 |
| PDF-092 | Psicóloga Anelise Buzzi | Saúde e Bem-Estar | 51 |
| PDF-093 | Psicóloga Ariela Mazuim Pfeifer (CRP07/33570) | Saúde e Bem-Estar | 51 |
| PDF-094 | Psicóloga Bruna Dias - Presencial e Online | Saúde e Bem-Estar | 51 |
| PDF-095 | Psicóloga Camila Oliveira | Saúde e Bem-Estar | 52 |
| PDF-096 | Psicóloga Elaine Cristina Cisz | Saúde e Bem-Estar | 52 |
| PDF-097 | Psicóloga Fernanda Vieira Biajoli | Saúde e Bem-Estar | 52 |
| PDF-101 | Psicóloga Naiara Meirelles | Saúde e Bem-Estar | 54 |
| PDF-103 | Psicóloga Verônica Tavares – CRP 12/31473 | Saúde e Bem-Estar | 55 |
| PDF-105 | Susie Heymer — Meu Templo Yoga | Saúde e Bem-Estar | 56 |
| PDF-107 | Viver Mais Psicologia - Atendimento Psicológico (Online e Presencial) | Saúde e Bem-Estar | 57 |
