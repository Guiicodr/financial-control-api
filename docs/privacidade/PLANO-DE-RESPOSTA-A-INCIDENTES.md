# Plano de Resposta a Incidente de Segurança com Dados Pessoais

> **Base normativa:** LGPD art. 48; Resolução CD/ANPD nº 15/2024 (Regulamento de
> Comunicação de Incidente de Segurança) e Resolução CD/ANPD nº 2/2022, art. 14
> (prazo em dobro para agente de pequeno porte).
> **Última revisão:** 21/09/2026 · Responsável pela execução: encarregado (DPO).

## 1. Limiar: quando comunicar é obrigatório

A comunicação (à ANPD **e** ao titular) é obrigatória quando o incidente puder
acarretar **risco ou dano relevante** — isto é, quando afetar de forma
significativa interesses e direitos fundamentais **e**, de forma cumulativa,
envolver ao menos um destes critérios (Res. 15/2024, art. 4º):

- dados pessoais sensíveis;
- dados de crianças, adolescentes ou idosos;
- **dados financeiros**;
- **dados de autenticação em sistemas** (login, token, senha);
- dados protegidos por sigilo legal, judicial ou profissional;
- dados em larga escala.

> ⚠️ **Este produto trata dados financeiros e de autenticação.** Na prática,
> quase todo incidente com acesso indevido à base atinge o limiar: a avaliação
> deve ser feita assumindo que a comunicação é devida, salvo se houver
> demonstração documentada em contrário.

Hipóteses que **não** obrigam comunicação (mas continuam exigindo registro
interno): incidente sem dado pessoal; impacto contido antes de qualquer acesso
externo; dados cifrados sem exposição da chave; escopo mínimo e irrelevante.

## 2. Prazos

| Etapa | Prazo | Observação |
|---|---|---|
| Comunicação **preliminar** à ANPD | **3 dias úteis** contados do conhecimento (a partir do momento em que se identifica a vinculação com dados pessoais) | agente de pequeno porte: **prazo em dobro → 6 dias úteis** (Res. 2/2022, art. 14, II). Não se aplica o dobro quando houver potencial comprometimento à integridade física ou moral |
| Comunicação **complementar** | até **20 dias úteis** do protocolo da preliminar | complementa o formulário com dados que ainda não existiam |
| Comunicação **ao titular** | sem demora, junto da avaliação | linguagem clara: o que ocorreu, quais dados, riscos, medidas e o que o titular pode fazer |
| Registro interno | imediato, mantido mesmo sem notificação | é a prova de que houve análise, e não omissão |

- **Canal com a ANPD:** peticionamento eletrônico no SEI!ANPD, tipo de processo
  "ANPD – Comunicados de Incidentes", formulário preliminar ou completo, com
  documentos que comprovem a representação e o ato de designação do encarregado.
- Não é necessário enviar à ANPD a lista de titulares afetados; a comunicação
  aos titulares pode ser exigida em cópia para fiscalização.
- **Relatório de tratamento do incidente:** manter em arquivo próprio com
  cópias dos dados e informações relevantes e das medidas adotadas.

## 3. Conteúdo mínimo da comunicação (art. 12 da Res. 15/2024)

1. identificação e dados de contato do controlador;
2. dados de contato do encarregado;
3. descrição do incidente, com a natureza e a categoria dos dados afetados e a
   identificação dos possíveis impactos aos titulares;
4. motivos da demora, se a comunicação não ocorreu no prazo;
5. medidas adotadas ou que serão adotadas para reverter ou mitigar os efeitos;
6. data do conhecimento do incidente;
7. contato para obtenção de informações.

## 4. Fluxo de resposta (ordem de execução)

1. **Detectar e registrar**: abrir o registro interno (`docs/privacidade/incidentes/AAAA-MM-DD-assunto.md`)
   com hora do conhecimento, relato e responsável.
2. **Conter**: revogar credenciais/tokens comprometidos (`RefreshToken`), rotacionar
   `JWT_SECRET`, `WHATSAPP_WEBHOOK_TOKEN`, `WHATSAPP_TOKEN` e a senha do banco,
   bloquear acessos indevidos, tirar do ar o que estiver exposto.
3. **Avaliar** (o mais rápido possível, dentro das 72h úteis): quais dados, quantos
   titulares, há risco relevante? Documentar a conclusão e quem decidiu.
4. **Comunicar**: preliminar à ANPD (3 dias úteis) e ao titular; complementar em
   até 20 dias úteis. Usar o modelo do item 5.
5. **Corrigir**: implementar o reparo definitivo e verificar que o vetor fechou
   (ex.: teste de regressão, como os de `SegurancaApiTests` e `LgpdApiTests`).
6. **Aprender**: atualizar ROPA, política de retenção e plano; registrar a lição
   aprendida e o que mudou no código.

> Se a causa for um bug já corrigido no repositório, a correção entra junto do
> commit que documenta o incidente (nunca sem teste de regressão).

## 5. Modelo de comunicado ao titular (ajustar ao caso)

> **Assunto:** Informação sobre incidente de segurança com seus dados — Finanly
>
> Olá, [nome].
>
> Identificamos em [data] um incidente de segurança que pode ter afetado os
> seguintes dados da sua conta: [ex.: nome, e-mail e lançamentos financeiros].
>
> **O que aconteceu:** [descrição objetiva e sem jargão].
> **O que já fizemos:** [contenção e correção adotadas].
> **O que você pode fazer:** [ex.: trocar a senha, ativar cuidado com mensagens
> suspeitas, revisar lançamentos]. Nossa equipe nunca pede sua senha por
> mensagem ou telefone.
> **Por quanto tempo seus dados ficaram expostos:** [janela temporal].
>
> Dúvidas: [e-mail do encarregado]. Você também pode procurar a Autoridade
> Nacional de Proteção de Dados (ANPD).

## 6. Contatos

| Papel | Nome | Contato |
|---|---|---|
| Encarregado (DPO) | preencher | preencher (mesmo e-mail publicado no Aviso de Privacidade) |
| Suporte técnico / infraestrutura | preencher | preencher |
| ANPD | — | peticionamento eletrônico no SEI!ANPD |

## 7. Ensaio anual

Uma vez por ano, simular um incidente (ex.: vazamento do banco de dados) e
cronometrar a execução dos passos 1 a 4. Guardar o registro do ensaio: é
evidência de governança (LGPD art. 50, § 1º) e reduz a chance de perder prazo
no caso real.
