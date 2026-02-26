# Execution Plan: Apps Bridge Auto-Connect

**Создан:** 2026-02-26

---

## Wave 1 (независимые)

### Task 1: Update MCW bridge client `isMetaMask` flag
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** bash — `git show issue-5268-apps-layout:src/front/client/wallet-apps-bridge-client.js | grep 'isMetaMask: true'`
- **Teammate:** coder-bridge-flag

### Task 2: Create unifactory bridge utils module
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `npm run test:unit` in unifactory
- **Teammate:** coder-bridge-utils

### Task 3: Add inline bridge loading script to unifactory index.html
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** user — inspect PR preview `view-source:` for inline script
- **Teammate:** coder-inline-script

## Wave 2 (зависит от Task 2)

### Task 4: Extend useEagerConnect with bridge auto-connect
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `npm run test:unit` in unifactory
- **Depends on:** Task 2
- **Teammate:** coder-eager-connect

### Task 5: Suppress WalletModal in bridge mode
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** user — PR preview, no modal visible when auto-connected
- **Depends on:** Task 2
- **Teammate:** coder-wallet-modal

### Task 6: Add bridge ready handler in Web3ReactManager
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `npm test` (React Testing Library)
- **Depends on:** Task 2
- **Teammate:** coder-web3-manager

## Wave 3 (tests)

### Task 7: Add MCW bridge unit tests
- **Skill:** code-writing
- **Reviewers:** test-reviewer
- **Verify:** bash — `npm run test:unit` in MCW
- **Depends on:** Task 1
- **Teammate:** tester-unit

### Task 8: Extend MCW E2E smoke test for auto-connect
- **Skill:** code-writing
- **Reviewers:** test-reviewer
- **Verify:** bash — `jest tests/e2e/walletAppsBridge.smoke.js`
- **Depends on:** Tasks 1, 4, 5, 6
- **Teammate:** tester-e2e

## Wave 4 (QA + Deploy + Post-deploy)

### Task 9: Pre-deploy QA
- **Skill:** pre-deploy-qa
- **Reviewers:** none (QA is its own verification)
- **Verify:** N/A (QA report)
- **Depends on:** Tasks 7, 8
- **Teammate:** qa-runner

### Task 10: Deploy MCW (GitHub Pages)
- **Skill:** infrastructure
- **Reviewers:** none
- **Verify:** bash — check GitHub Actions status, curl bridge client URL
- **Depends on:** Task 9
- **Teammate:** deployer

### Task 11: Deploy unifactory (dex.onout.org)
- **Skill:** infrastructure
- **Reviewers:** none
- **Verify:** bash — curl dex.onout.org
- **Depends on:** Task 9
- **Teammate:** deployer-unifactory

### Task 12: Post-deploy verification
- **Skill:** post-deploy-qa
- **Reviewers:** none
- **Verify:** N/A (manual live environment checks)
- **Depends on:** Tasks 10, 11
- **Teammate:** qa-post-deploy

---

## Проверки, требующие участия пользователя

- [ ] **Task 3:** Пользователь проверяет PR preview (`view-source:`) — inline script видим в `public/index.html`
- [ ] **Task 5:** Пользователь открывает PR preview DEX через `#/apps` — убедиться что wallet modal НЕ показывается, адрес отображается
- [ ] **Task 9 (Pre-deploy QA):** Пользователь выполняет manual browser tests на PR preview URLs:
  - Открыть MCW `#/apps` → кликнуть DEX → адрес виден, no modal
  - Открыть `dex.onout.org` standalone → wallet modal показывается
  - Переключить сеть в MCW → DEX обновляет сеть
- [ ] **Task 12 (Post-deploy):** Пользователь проверяет live production:
  - `https://swaponline.github.io/#/apps` → Onout DEX → auto-connect работает
  - `https://dex.onout.org` standalone → standard wallet modal
  - Network switch + disconnect tests

---

## Примечание о репозиториях

**MCW tasks** (1, 7, 8, 10): работают в текущем репозитории `/root/MultiCurrencyWallet` на ветке `issue-5268-apps-layout`.

**unifactory tasks** (2, 3, 4, 5, 6, 11): работают в **external repository** `noxonsu/unifactory`. Teammates должны иметь доступ к unifactory repo или получить инструкции от пользователя.

---

## User Acceptance Criteria (из user-spec)

- [ ] AC1: Открыть `#/apps` → Onout DEX → DEX показывает подключённый адрес без модалок
- [ ] AC2: Баланс в DEX соответствует балансу ETH в MCW
- [ ] AC3: `dex.onout.org` standalone работает как раньше (стандартный wallet selection)
- [ ] AC4: Когда MetaMask не подключён в MCW, DEX показывает модалку выбора кошелька
- [ ] AC5: Смена сети в MCW отражается в DEX iframe
- [ ] AC6: E2E smoke тест проходит
