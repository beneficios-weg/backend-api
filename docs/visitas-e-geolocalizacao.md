# Visitas e geolocalização

## Divisão de responsabilidade

O Mobile é responsável por permissões, obtenção de localização e detecção de uma
visita. O briefing descreve uma combinação de proximidade, cerca de três minutos,
movimento, precisão, consistência e comparação entre candidatos.

O Backend deverá definir apenas o contrato, a validação e a persistência do
registro remoto de visita. O algoritmo de detecção e a experiência de permissão
continuam sob autoridade do Mobile.

## Privacidade

Os pontos de GPS analisados pelo Mobile são temporários. Não foi solicitado
trajeto permanente, QR Code, NFC, confirmação manual, BLE, UWB ou IA para
detecção. O Backend não deve ampliar a coleta por inferência.

## Decisões necessárias

**DECISÃO PENDENTE:** definir, com minimização de dados:

- campos aceitos ao registrar uma visita;
- origem do horário e tratamento de fuso;
- tolerância, validação e prevenção de duplicidade;
- chave de idempotência para reenvio offline;
- retenção e exclusão do histórico;
- possibilidade e regra de contestação/correção;
- campos calculados e retornados pelo servidor.
