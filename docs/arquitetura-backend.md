# Arquitetura do Backend

## Situação

**DECISÃO PENDENTE:** não há stack nem arquitetura implementada ou aprovada no
repositório.

## Responsabilidades mínimas esperadas

Independentemente da tecnologia escolhida, a solução precisa separar de modo
compreensível:

- entrada HTTP e serialização;
- autenticação e autorização;
- regras de negócio e validações;
- acesso a dados e migrações;
- integração com serviços externos, se houver;
- observabilidade e tratamento de erros;
- testes.

Essa separação é uma diretriz, não uma estrutura de diretórios já escolhida.

## Restrições conhecidas

- preservar repositórios separados para Backend e Mobile;
- publicar um contrato consumível pelo Mobile;
- não transferir ao Backend decisões de tela, navegação ou UX;
- não persistir trajetos de localização sem requisito e decisão de privacidade;
- evitar overengineering diante do prazo informado.

## Critérios para escolher a stack

A equipe deve registrar a decisão considerando domínio da equipe, tempo de
entrega, suporte a autenticação, validação, migrações, testes, documentação do
contrato e operação no ambiente de destino.
