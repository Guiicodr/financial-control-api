# ROPA — Registro das Operações de Tratamento de Dados Pessoais

> **Base normativa:** LGPD (Lei nº 13.709/2018), art. 37; Resolução CD/ANPD nº 2/2022
> (registro simplificado para agentes de tratamento de pequeno porte).
> **Última revisão:** 21/09/2026 · **Versão dos documentos do app:** `2026-09-1`
> (ver `app.legal.terms-version` na API e `src/lib/legal.js` no front).

## 1. Agentes

| Papel | Quem |
|---|---|
| **Controlador** | preencher: razão social / nome, CNPJ/CPF, cidade/UF |
| **Encarregado (DPO)** | preencher: nome e e-mail — canal publicado no Aviso de Privacidade |
| **Operadores** | Railway (API + PostgreSQL), Vercel (site), Meta WhatsApp Cloud API / Twilio (mensageria) |

> O controlador decide as finalidades; o operador apenas executa. Cada operador
> precisa de contrato com obrigações de confidencialidade e de tratamento
> conforme a LGPD (art. 39) — ver pendências no `CHECKLIST-LGPD.md`.

## 2. Operações de tratamento

| # | Operação | Dados pessoais | Titulares | Finalidade | Base legal | Compartilhamento | Retenção |
|---|---|---|---|---|---|---|---|
| O1 | Cadastro e autenticação | nome, e-mail, senha (hash BCrypt), data/hora e versão do aceite | usuários cadastrados | criar e manter a conta, autenticar, provar o consentimento | execução de contrato (art. 7º, V); consentimento para o aceite (art. 7º, I) | Railway | enquanto a conta existir |
| O2 | Lançamentos financeiros | descrição, valor, tipo, data, categoria | usuário titular | registrar e exibir o histórico, saldo, indicadores e projeção | execução de contrato (art. 7º, V) | Railway | enquanto a conta existir |
| O3 | Rendas | descrição, valor, tipo (BASE/EXTRA), data | usuário titular | compor o saldo e as médias dos relatórios | execução de contrato (art. 7º, V) | Railway | enquanto a conta existir |
| O4 | Metas e movimentos | nome da meta, valor alvo/atual, prazo, tipo, aportes/resgates | usuário titular | acompanhar objetivos e reservas | execução de contrato (art. 7º, V) | Railway | enquanto a conta existir |
| O5 | Orçamentos e alertas | categoria, limite mensal, percentual de uso | usuário titular | avisar sobre estouro de limite | execução de contrato (art. 7º, V) | Railway | enquanto a conta existir |
| O6 | Cartões de crédito | nome, limite, dia de fechamento e de vencimento | usuário titular | acompanhar limites e vencimentos | execução de contrato (art. 7º, V) | Railway | enquanto a conta existir |
| O7 | Bot de WhatsApp | número de telefone (só dígitos), texto da mensagem, respostas com valor/categoria/saldo | usuário que vinculou o número | permitir lançar gastos por mensagem e consultar saldo | consentimento (art. 7º, I), revogável pelo endpoint `DELETE /usuario/whatsapp` | Meta / Twilio (mensagens), Railway (conteúdo gravado como lançamento) | número: enquanto o vínculo existir; mensagem: não é persistida em texto |
| O8 | Sessão e recuperação de senha | refresh token (30 dias), token de recuperação (1 hora), e-mail | usuário titular | manter a sessão e permitir redefinir a senha | execução de contrato (art. 7º, V) | Railway | prazo do token, com expurgo diário (`TokenCleanupJob`) |
| O9 | Segurança e diagnóstico | registros de erro/acesso, IP, identificador de sessão, dados pessoais **mascarados** (`PiiMasker`) | usuários e visitantes | prevenir abuso, investigar falhas, cumprir prazo de segurança | legítimo interesse (art. 7º, IX) | Railway (logs da aplicação), Vercel (logs de acesso) | até 6 meses (ver política de retenção) |
| O10 | Direitos do titular | exportação completa dos dados (JSON) e exclusão da conta | usuário titular | atender arts. 18, II, V e VI | cumprimento de obrigação legal (art. 7º, II) | nenhum: o arquivo é gerado no navegador do titular | o arquivo não é retido no servidor |

### Onde isso está no código

| Operação | Implementação |
|---|---|
| O1 | `controller/AuthController`, `service/AuthService`, `model/Usuario.aceiteVersao/aceiteEm` |
| O2–O6 | `service/TransacaoService`, `IncomeService`, `ObjetivoService`, `MovimentoMetaService`, `OrcamentoService`, `CartaoCreditoService` (todos filtram por usuário) |
| O7 | `service/WhatsAppService`, `controller/WhatsAppWebhookController`, `UsuarioWhatsappController` |
| O8 | `service/AuthService.emitir`, `config/TokenCleanupJob` |
| O9 | `util/PiiMasker`, `config/RestExceptionHandler`, `config/RateLimitFilter` |
| O10 | `controller/UsuarioController`, `service/ContaService`, `GET /usuario/dados`, `DELETE /usuario` |

## 3. Avaliações

- **Dados sensíveis (art. 5º, II):** não são coletados. Não há campo de saúde,
  biometria, origem racial, convicção religiosa, opinião política, filiação
  sindical ou vida sexual.
- **Crianças e adolescentes (art. 14):** o serviço é declarado 18+ nos Termos e
  não há tratamento intencional de dados de menores.
- **Decisões automatizadas (art. 20):** os indicadores de painel (diagnóstico
  financeiro, projeções) são informativos e não produzem efeito jurídico ou
  decisão sobre o titular; não há perfilização para crédito ou publicidade.
- **Transferência internacional (arts. 33 a 36):** ocorre para os Estados Unidos
  pelos operadores de hospedagem e mensageria. Instrumento exigido: cláusulas
  contratuais específicas por fornecedor (Resolução CD/ANPD nº 19/2024) —
  pendência registrada no checklist.
- **Alto risco / RIPD (art. 38):** a avaliar. Como o tratamento inclui **dados
  financeiros e dados de autenticação**, um incidente aqui tende a atingir o
  limiar de comunicação obrigatória (Resolução CD/ANPD nº 15/2024, art. 4º).

## 4. Como manter este registro

1. Toda funcionalidade nova que colete ou compartilhe dado pessoal entra aqui
   **antes** de ir para produção.
2. O encarregado revisa o registro a cada mudança material (ao menos 1x por ano).
3. Ao publicar nova versão dos documentos do app, atualize `LEGAL_VERSION`
   (front) e `LEGAL_TERMS_VERSION` (API) e registre a data nesta tabela.
