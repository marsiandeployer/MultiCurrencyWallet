# Execution Plan: Native Mobile Wallet (Android)

**Создан:** 2026-02-26

---

## Overview

**Feature:** Native Android crypto wallet with dApp browser and WalletConnect v2 support
**Total tasks:** 14
**Total waves:** 8
**Parallel execution:** Tasks within same wave run in parallel, no file conflicts

---

## Wave 1 (Project Scaffold — независимая)

### Task 1: Android Project Setup
- **Skill:** infrastructure-setup
- **Reviewers:** code-reviewer, security-auditor, infrastructure-reviewer
- **Verify:** bash — `./gradlew assembleDebug` succeeds
- **Teammate:** project-setup-android

---

## Wave 2 (Core Crypto — зависит от Wave 1)

### Task 2: Crypto Core — BIP39/BIP44 Key Derivation
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:crypto:test` passes, known mnemonic produces expected addresses
- **Teammate:** crypto-engineer

### Task 3: Secure Storage + App Password
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:storage:test` passes
- **Teammate:** storage-engineer

**Parallel execution:** Tasks 2 and 3 work on separate modules (`:core:crypto` and `:core:storage`), no file conflicts.

---

## Wave 3 (Infrastructure — зависит от Wave 2)

### Task 4: Biometric Authentication + Auto-lock
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:auth:test` passes
- **Depends on:** Task 2 (uses SecureStorage from Task 3)
- **Teammate:** auth-engineer

### Task 5: Network Layer + API Failover
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:network:test` passes
- **Depends on:** Task 2 (uses config extraction patterns)
- **Teammate:** network-engineer

**Parallel execution:** Tasks 4 and 5 work on separate modules (`:core:auth` and `:core:network`), no file conflicts.

---

## Wave 4 (Blockchain Operations — зависит от Waves 2+3)

### Task 6: BTC Operations (Balance + Transactions)
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:btc:test` passes (balance parsing, UTXO selection, fee calc, testnet broadcast)
- **Depends on:** Tasks 2, 3
- **Teammate:** btc-engineer

### Task 7: EVM Operations (Balance + Transactions + Fiat)
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :core:evm:test` passes (balance parsing, CoinGecko parsing, gas estimation, Sepolia broadcast)
- **Depends on:** Tasks 2, 3
- **Teammate:** evm-engineer

**Parallel execution:** Tasks 6 and 7 work on separate modules (`:core:btc` and `:core:evm`), no file conflicts.

---

## Wave 5 (UI Shell — зависит от Wave 4)

### Task 8: Wallet UI + Navigation
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `./gradlew :app:testDebugUnitTest :app:lintDebug` passes; user — verify UI on device
- **Depends on:** Task 4
- **Teammate:** ui-engineer

**Note:** This task creates the complete navigation graph with stub screens. Tasks 9-13 will fill in the screens.

---

## Wave 6 (Feature Screens — зависит от Wave 5)

### Task 9: Send Transaction UI
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :app:testDebugUnitTest` passes; user — verify send flow on device
- **Depends on:** Task 5
- **Teammate:** send-ui-engineer

### Task 10: dApp Browser + window.ethereum Provider
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :feature:dapp-browser:test` passes
- **Depends on:** Task 5
- **Teammate:** dapp-browser-engineer

### Task 11: WalletConnect v2 Integration
- **Skill:** code-writing
- **Reviewers:** code-reviewer, security-auditor, test-reviewer
- **Verify:** bash — `./gradlew :feature:walletconnect:test` passes
- **Depends on:** Task 5
- **Teammate:** walletconnect-engineer

**Parallel execution:** Tasks 9, 10, 11 work on separate packages/modules (`:app` send package, `:feature:dapp-browser`, `:feature:walletconnect`), no file conflicts.

---

## Wave 7 (History + Polish — зависит от Wave 6)

### Task 12: Transaction History
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `./gradlew :core:btc:test :core:evm:test` passes (response parsing tests)
- **Depends on:** Tasks 6, 7
- **Teammate:** history-engineer

### Task 13: Settings, White-label, Crashlytics
- **Skill:** code-writing
- **Reviewers:** code-reviewer, infrastructure-reviewer
- **Verify:** bash — `./gradlew assembleRelease` succeeds, verify no secrets in logcat output
- **Depends on:** Task 6
- **Teammate:** settings-engineer

**Parallel execution:** Task 12 works on `:core:btc/`, `:core:evm/`, `:app/history/`. Task 13 works on `:app/settings/` and build config. No file conflicts.

---

## Wave 8 (Final — зависит от всех предыдущих)

### Task 14: Pre-deploy QA
- **Skill:** pre-deploy-qa
- **Reviewers:** none
- **Verify:** bash — `./gradlew test connectedAndroidTest` all green, `./gradlew assembleRelease` produces APK
- **Depends on:** All tasks 1-13
- **Teammate:** qa-engineer

---

## Проверки, требующие участия пользователя

- [ ] **Task 8**: Пользователь проверяет UI layout на реальном устройстве (onboarding screens, wallet screen with balance list, bottom navigation)
- [ ] **Task 9**: Пользователь проверяет send flow на устройстве (address input, amount, fee selector, biometric confirm, result display)
- [ ] **После всех волн**: Финальное тестирование на физическом устройстве:
  - Создание/импорт кошелька с seed phrase confirmation
  - Биометрическая разблокировка + password fallback
  - Отправка testnet транзакции (BTC и ETH)
  - dApp browser с подключением к test dApp (dex.onout.org)
  - WalletConnect QR scan + session approval + sign request
  - Проверка auto-lock (5 min inactivity, 30s background)
  - Проверка offline mode (отключить Wi-Fi/данные → verify "No connection" state)

---

## Критические зависимости

- **Cross-platform address compatibility** — Task 2 unit tests MUST verify that imported mnemonic produces identical BTC/ETH addresses as web wallet
- **Security hardening** — Tasks 3, 4, 10 implement critical security features (encrypted storage, biometric auth, WebView hardening); reviewers include security-auditor
- **No parallel file conflicts** — Wave structure ensures tasks within same wave never modify same files

---

## Команда (10 teammates + 3 reviewer types)

**Teammates (по одному на задачу):**
1. project-setup-android
2. crypto-engineer
3. storage-engineer
4. auth-engineer
5. network-engineer
6. btc-engineer
7. evm-engineer
8. ui-engineer
9. send-ui-engineer
10. dapp-browser-engineer
11. walletconnect-engineer
12. history-engineer
13. settings-engineer
14. qa-engineer

**Reviewer agents (spawned per task when needed):**
- code-reviewer (sonnet)
- security-auditor (opus when available, sonnet fallback)
- test-reviewer (sonnet)
- infrastructure-reviewer (sonnet)

---

## Escalation Scenarios

Escalate to user when:
- 3 review rounds exhausted with unresolved findings
- Cross-platform address test fails (Task 2) — critical blocker
- Android SDK/Gradle issues blocking builds (Task 1)
- WalletConnect relay connectivity issues during testing (Task 11)
- Physical device unavailable for UI verification (Tasks 8, 9)
