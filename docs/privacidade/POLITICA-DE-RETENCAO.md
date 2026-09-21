# Política de Retenção e Descarte de Dados Pessoais

> **Base normativa:** LGPD arts. 15 (término do tratamento) e 16 (conservação).
> **Última revisão:** 21/09/2026

## 1. Princípio

Dado pessoal fica guardado **enquanto houver finalidade** que o justifique. Ao
terminar a finalidade, o dado é eliminado — não "arquivado por precaução". O que
a lei permite conservar (art. 16) é: cumprimento de obrigação legal, estudo por
órgão de pesquisa (com anonimização quando possível), transferência a terceiro
com os limites da lei, e uso exclusivo do controlador com anonimização.

## 2. Prazos por categoria

| Categoria | Dado | Prazo | Como é descartado |
|---|---|---|---|
| Cadastro | nome, e-mail, senha (hash), versão/data do aceite | enquanto a conta existir | exclusão da conta (`DELETE /usuario`) → apaga o registro |
| Financeiro | lançamentos, rendas, metas, movimentos, orçamentos, cartões | enquanto a conta existir | exclusão da conta → `deleteAllByUsuario` em cada repositório |
| WhatsApp | número vinculado | enquanto o vínculo existir | `DELETE /usuario/whatsapp` (revogação, art. 18, IX) ou exclusão da conta |
| Sessão | refresh token (30 dias), token de recuperação (1 hora) | até o vencimento, no máximo | `TokenCleanupJob` apaga os vencidos todo dia (cron `RETENTION_TOKEN_CLEANUP_CRON`, padrão 03:30) |
| Logs da aplicação | mensagens de erro e de auditoria, IP, sessão — com dado pessoal mascarado (`PiiMasker`) | até 6 meses | rotação/expurgo no provedor (Railway) — configurar a retenção do serviço |
| Logs de acesso do site | IP e user-agent registrados pela Vercel | até 6 meses | retenção padrão do provedor — revisar nas configurações da conta |
| Exportação de dados (`GET /usuario/dados`) | arquivo JSON | não é retido no servidor | gerado no navegador do titular e salvo por ele |

## 3. O que acontece na exclusão da conta

1. O titular confirma a senha e a palavra de confirmação no modal do app.
2. `ContaService.excluirConta` apaga, na ordem das chaves estrangeiras:
   movimentos → transações → cartões → metas → orçamentos → rendas → refresh
   tokens → tokens de recuperação (por e-mail) → usuário.
3. O access token em circulação deixa de valer imediatamente (o filtro busca o
   usuário pelo e-mail, que não existe mais).
4. A mensagem de resposta informa que **não guardamos cópia** do histórico
   financeiro.

> **Backups:** se o provedor mantiver backup automático do banco, a rotina de
> descarte precisa contemplá-lo. Definir com o Railway a janela de retenção de
> backup e registrar aqui — inclusive porque o dado do titular excluído não pode
> ressuscitar num restore.

## 4. Como comprovar na prática

- Rodar `GET /usuario/dados` antes e `DELETE /usuario` depois, confirmando que a
  conta e o histórico não respondem mais (é o que `LgpdApiTests` faz).
- Conferir contagens no banco após a exclusão: transações, cartões, metas,
  orçamentos, rendas e tokens do titular devem ser zero.
- Verificar no log da aplicação a linha `Expurgo de retenção: X refresh token(s)
  e Y token(s) de recuperação vencidos removidos.` — é a evidência de que a
  rotina está ativa.
- Nunca registrar dado pessoal em claro: usar `PiiMasker.email` /
  `PiiMasker.telefone` em qualquer log novo (há teste unitário cobrindo o formato).
