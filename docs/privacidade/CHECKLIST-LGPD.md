# Checklist de Conformidade LGPD

> **Última revisão:** 21/09/2026 · **Versão dos documentos:** `2026-09-1`
> Marque cada item com **OK** (implementado e verificável), **PENDENTE** (falta
> fazer) ou **N/A** (não se aplica, com justificativa).

## 1. Bases legais e transparência

| Item | Fundamento | Status |
|---|---|---|
| Aviso de Privacidade publicado, com finalidade e base legal por operação | arts. 6º, VI; 9º | **OK** — `src/locales/*.json` (chave `legal.privacy`), rota `#/privacidade` |
| Termos de Uso publicados, com regras de uso e limitação de responsabilidade | art. 9º; CDC quando aplicável | **OK** — chave `legal.terms`, rota `#/termos` |
| Controlador identificado (nome, CNPJ/CPF, endereço) | art. 9º, I | **PENDENTE** — trocar os campos `[ ]` em `src/lib/legal.js` |
| Encarregado identificado e publicado com contato | art. 41; Res. 18/2024 | **PENDENTE** — designar (ou manter canal, se pequeno porte) e preencher em `src/lib/legal.js` |
| Informação sobre compartilhamento e transferência internacional | arts. 9º, VII; 33 a 36 | **OK** no texto; **PENDENTE** o instrumento contratual com cada operador |

## 2. Direitos do titular (art. 18)

| Direito | Implementação | Status |
|---|---|---|
| Acesso e portabilidade (II, V) | `GET /usuario/dados` + botão "Baixar meus dados" no Perfil | **OK** — `ContaService.exportar`, `LgpdApiTests` |
| Eliminação da conta e do histórico (VI) | `DELETE /usuario` (com senha) + modal de confirmação | **OK** — `ContaService.excluirConta` |
| Revogação do consentimento do WhatsApp (IX) | `DELETE /usuario/whatsapp` | **OK** — `UsuarioWhatsappController.desvincular` |
| Correção de dados (III) | e-mail e nome ainda não são editáveis no app | **PENDENTE** — expor edição de nome/e-mail (ou tratar por canal do encarregado) |
| Prazo de resposta (art. 19) | compromisso publicado: até 15 dias | **OK** no texto; **PENDENTE** o processo interno de atendimento e registro das solicitações |

## 3. Consentimento

| Item | Fundamento | Status |
|---|---|---|
| Aceite obrigatório no cadastro, com link para os dois documentos | art. 8º, §1º | **OK** — `AuthPage` + validação na API |
| Registro de qual versão foi aceita e quando | art. 8º, §1º | **OK** — `Usuario.aceiteVersao` / `aceiteEm`, devolvidos no JSON de exportação |
| Aceite próprio para o uso do número de WhatsApp, revogável | art. 8º, §5º | **OK** — vínculo opcional + endpoint de revogação |
| Novo aceite quando houver mudança material | art. 8º | **PENDENTE** — processo de re-aceite (bump de `LEGAL_VERSION`) |

## 4. Segurança (art. 46)

| Medida | Onde está | Status |
|---|---|---|
| Senha com hash BCrypt, nunca serializada | `SecurityConfig`, `Usuario.@JsonIgnore` | **OK** |
| Isolamento por titular em todas as consultas | serviços e repositórios | **OK** (testes de IDOR em `SegurancaApiTests`) |
| Rate limit em `/auth/**` e `/webhooks/**` | `RateLimitFilter` | **OK** |
| Webhook fail-closed, comparação sem timing attack | `WhatsAppWebhookController` | **OK** |
| CORS com lista explícita, sem wildcard | `CorsConfig` | **OK** |
| Cabeçalhos de segurança e CSP no site | `vercel.json` | **OK** |
| Log sem dado pessoal em claro | `util/PiiMasker` | **OK** — `PiiMaskerTests` |
| Expurgo de credenciais vencidas | `TokenCleanupJob` | **OK** |
| Criptografia em repouso / backup verificado | provedor | **PENDENTE** — confirmar no Railway e registrar |
| `ddl-auto=update` em produção | `application.properties` | **A REVISAR** — avaliar migração versionada (Flyway) |

## 5. Governança

| Item | Fundamento | Status |
|---|---|---|
| Registro das operações de tratamento (ROPA) | art. 37; Res. 2/2022 | **OK** — `docs/privacidade/ROPA.md` |
| Política de retenção e descarte | arts. 15 e 16 | **OK** — `docs/privacidade/POLITICA-DE-RETENCAO.md` |
| Procedimento de resposta a incidente | art. 48; Res. 15/2024 | **OK** — `docs/privacidade/PLANO-DE-RESPOSTA-A-INCIDENTES.md` |
| Política simplificada de segurança da informação | Res. 2/2022, arts. 12 e 13 | **PENDENTE** — consolidar em documento único |
| Contrato/DPA com operadores (Railway, Vercel, Meta/Twilio) | art. 39 | **PENDENTE** |
| Cláusulas contratuais específicas de transferência internacional | arts. 33 a 36; Res. 19/2024 | **PENDENTE** — prazo da norma encerrou em 23/08/2025 |
| Relatório de impacto (RIPD), se o tratamento for de alto risco | art. 38 | **A AVALIAR** — dados financeiros + autenticação pesam para alto risco |
| Revisão jurídica dos textos publicados | — | **PENDENTE** |
| Treinamento/registro de acessos administrativos | art. 50 | **PENDENTE** |

## 6. Verificação rápida (comandos)

```bash
# API: testes de segurança e de direitos do titular
./mvnw test

# Front: build e lint
cd ../controle-financeiro-front && npm run build && npx eslint src
```

Evidências geradas pelos testes: cadastro sem aceite é recusado (400), aceite
fica gravado com versão e data, exportação traz só os dados do titular,
exclusão apaga cadastro/histórico/tokens e invalida o token em circulação,
vínculo de WhatsApp é revogável, expurgo remove apenas credenciais vencidas.

## 7. Ordem sugerida para fechar as pendências

1. Preencher `src/lib/legal.js` (controlador, encarregado, contato) e conferir
   `LEGAL_TERMS_VERSION` / `LEGAL_VERSION`.
2. Designar o encarregado (ou assumir o canal de comunicação, se pequeno porte)
   e publicar o contato.
3. Formalizar as cláusulas com os operadores (Railway, Vercel, Meta/Twilio).
4. Edição de nome/e-mail no perfil (art. 18, III) + registro das solicitações
   recebidas pelo encarregado.
5. Política simplificada de segurança da informação e revisão jurídica.
