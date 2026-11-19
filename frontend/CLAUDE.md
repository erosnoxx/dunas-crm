# frontend

Web responsiva do crm (a mesma base vira o aplicativo, via Capacitor). **Fonte de verdade: [`docs/rules/frontend.md`](../docs/rules/frontend.md)**: stack, estrutura de `src/`, tema, sessão e regras. Identidade visual: [`docs/design-system/`](../docs/design-system/README.md). Nada disso é repetido aqui.

```
src/app/       React Router (framework mode, SPA)
src/features/  uma pasta por feature, só quando a spec existe em docs/system-design/
src/shared/    ui/, api/, lib/
src/styles/    theme.css (gerado de tokens.json) e fontes
```

## Verificação

A partir de `frontend/`, **pelo PowerShell**: o Vitest falha de forma intermitente ("Cannot read properties of undefined (reading 'config')") quando disparado pelo Git Bash, e passa pelo PowerShell.

```
npm run typecheck && npm run lint && npm test && npm run build && npm run format:check
```

- O `package-lock.json` é o do Lume (`vite` 8.3.1, `vitest` 5.0.2, `jsdom` 27): versões mais novas do `vitest` (5.1.x) e do `@testing-library/jest-dom` (8.x) quebraram o teste. Suba versão como decisão à parte, rodando a suíte inteira.
- `lint` tem 1 aviso conhecido de `react-refresh` em `shared/ui/form.tsx` (o `useFormField` exportado junto dos componentes), herdado do Lume.

## Regras

- Tela só com mockup aprovado em `docs/system-design/<feature>/design/` (regra 7 de [`docs/CLAUDE.md`](../docs/CLAUDE.md)).
- Cor, fonte e corte só pelos tokens do tema; um teste barra cor literal.
