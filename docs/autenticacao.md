# Autenticação e autorização

## Situação

**DECISÃO PENDENTE:** o repositório não define mecanismo de identidade, formato
de credencial, expiração, renovação, recuperação ou perfis de autorização.

## Fluxos que precisam de contrato

- login;
- primeiro acesso;
- recuperação de senha;
- encerramento e renovação de sessão;
- acesso a favoritos e visitas do usuário autenticado.

## Definições necessárias

Antes da implementação, a equipe deve decidir e documentar:

- origem da identidade corporativa;
- responsabilidades do Backend e de eventual provedor externo;
- proteção e armazenamento de credenciais/tokens;
- expiração, revogação e renovação;
- limitação de tentativas e proteção contra abuso;
- códigos e payloads de erro sem vazamento de informação;
- autorização por proprietário para favoritos e visitas.

O Mobile decide o armazenamento seguro no dispositivo, mas depende do Backend
para a semântica e o ciclo de vida da autenticação.
