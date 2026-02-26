---
created: 2026-02-26
status: approved
branch: issue-5268-apps-layout
size: M
---

# Tech Spec: Apps Bridge Auto-Connect

## Solution

Implement seamless wallet auto-connection for dApps (Onout DEX) loaded in iframe within MCW's Apps page. Currently, the bridge injects `window.ethereum` in the iframe but the dApp still shows a wallet selection modal because:

1. **MCW bridge client:** `isMetaMask: false` prevents MetaMask UI recognition
2. **MCW bridge client:** No explicit `connect` event emission after handshake (expected by some libraries)
3. **dApp (unifactory):** Bridge client script not loaded dynamically
4. **dApp (unifactory):** No bridge-aware logic in `useEagerConnect()` to bypass wallet modal

**Solution approach:**
- **MCW side:** Set `isMetaMask: true` on bridge provider to enable MetaMask UI recognition
- **unifactory side:** Add dynamic bridge client loading + bridge detection + auto-connect logic

The solution ensures backward compatibility: dApps work in standalone mode without changes, and the bridge only activates when `?walletBridge=swaponline` parameter + iframe context are both present.

## Architecture

### What we're building/modifying

**MCW (MultiCurrencyWallet) side:**
- **`wallet-apps-bridge-client.js`** — Client-side IIFE bridge provider (branch: issue-5268-apps-layout). Change `isMetaMask: false` to `isMetaMask: true` for UI recognition.
- **`walletBridge.ts`** — Host-side bridge (no changes needed, already implements full protocol).
- **`walletAppsBridge.smoke.js`** — E2E test. Extend to verify auto-connect: check iframe has connected address, no wallet modal visible.

**unifactory (noxonsu/unifactory) side:**
- **New module: `src/utils/walletBridge.ts`** — Bridge detection, dynamic script loading, bridge-ready detection
- **`src/hooks/index.ts`** — Extend `useEagerConnect()` with bridge-aware auto-connect logic
- **`src/components/WalletModal/index.tsx`** — Suppress modal when bridge is active and connected
- **`src/components/Web3ReactManager/index.tsx`** — Handle bridge ready event for late connection
- **`public/index.html`** — Add inline script for dynamic bridge client loading

### How it works

**Phase 1: Bridge Script Loading (unifactory dApp side)**

1. User opens MCW wallet at `#/apps` → clicks Onout DEX
2. MCW renders iframe: `<iframe src="dex.onout.org?walletBridge=swaponline" />`
3. dApp loads, `public/index.html` runs before React app initializes
4. New inline script in `index.html` detects:
   - URL contains `?walletBridge=swaponline`
   - AND `window.parent !== window` (iframe context)
5. Script dynamically loads bridge client:
   - Extracts wallet host from `document.referrer` (e.g., `https://swaponline.github.io`)
   - Creates `<script src="https://swaponline.github.io/wallet-apps-bridge-client.js">`
   - Fallback: if referrer empty/blocked, use hardcoded `https://swaponline.github.io` (mainnet default)
6. Bridge client IIFE runs → injects `window.ethereum` with `isMetaMask: true`
7. Bridge client sends `BRIDGE_HELLO` → host responds `BRIDGE_READY` (with accounts, chainId)

**Phase 2: Auto-Connect (unifactory dApp side)**

8. React app mounts → `Web3ReactManager` runs → calls `useEagerConnect()`
9. `useEagerConnect()` detects bridge mode:
   - Checks `window.ethereum?.isSwapWalletAppsBridge === true`
   - Waits for bridge ready: polls `window.ethereum.isConnected()` up to 5 seconds
   - If bridge ready → calls `activate(injected)` immediately (bypass `isAuthorized()` check)
   - If timeout (5 sec) → fallback to standard eager connect logic
10. `activate(injected)` succeeds → web3-react context set to active
11. `WalletModal` checks bridge state → suppresses modal if bridge active and connected
12. User sees DEX with address and balance displayed — no modal interaction needed

**Phase 3: Runtime Events (both sides)**

13. User switches network in MCW → host emits `chainChanged` → bridge client forwards to dApp → web3-react updates context
14. User disconnects MetaMask in MCW → host emits `accountsChanged([])` → bridge client forwards → web3-react clears account → UI shows Connect button
15. User closes dApp → MCW calls `bridge.destroy()` → cleanup listeners

**Data Flow Diagram:**

```
MCW Host (Apps.tsx)
  │
  ├─ createWalletAppsBridge(iframe, appUrl)
  │  └─ Listens for postMessage from iframe
  │
  └─ iframe src: dex.onout.org?walletBridge=swaponline
      │
      ├─ index.html inline script detects ?walletBridge + iframe
      │  └─ Dynamically loads wallet-apps-bridge-client.js from referrer
      │
      ├─ Bridge client IIFE injects window.ethereum (isMetaMask: true)
      │  ├─ Sends BRIDGE_HELLO → Host responds BRIDGE_READY
      │  └─ Emits 'bridgeReady' event
      │
      └─ React app mounts
          ├─ useEagerConnect() detects bridge → activate(injected)
          ├─ WalletModal suppressed if bridge active
          └─ UI shows connected address
```

## Decisions

### Decision 1: Bridge client loading strategy
**Decision:** Dynamic `<script>` tag loading from wallet host (via `document.referrer`) in dApp's `index.html`, NOT embedding bridge client as npm module in unifactory.

**Rationale:**
- **Always up-to-date:** dApp always uses latest bridge client version from wallet host, no need to sync npm package versions across repos
- **No code duplication:** Avoids copying bridge client into unifactory repo
- **Universal deployment:** Works for all unifactory-based dApps without code changes (only need to redeploy after unifactory update)
- **Fallback safety:** If referrer blocked/empty, fallback to hardcoded mainnet URL (`https://swaponline.github.io`)

**Alternatives considered:**
- **Alt A: npm package** — Requires publishing `wallet-apps-bridge-client` as npm package, versioning, updates in all dApp repos. Rejected: adds maintenance overhead.
- **Alt B: Copy bridge client to unifactory repo** — Simple but creates version drift. Rejected: violates DRY, hard to keep in sync.
- **Alt C: Wallet host injects script via postMessage** — Not possible due to CSP and cross-origin restrictions. Rejected: technically infeasible.

### Decision 2: Set `isMetaMask: true` on bridge provider
**Decision:** Change bridge client to set `isMetaMask: true` instead of `false`.

**Rationale:**
- **UI recognition:** unifactory's WalletModal checks `window.ethereum.isMetaMask` to show MetaMask option. If false, only "Injected" option appears (less familiar to users).
- **MCW detection:** MCW's `Web3Connect.getInjectedType()` checks `isMetaMask` flag. Returns `METAMASK` type for better UX.
- **Standard practice:** Most wallet bridges (Rainbow, Frame, etc.) set `isMetaMask: true` for compatibility.
- **Backward compatible:** `isSwapWalletAppsBridge` flag remains for explicit bridge detection when needed.

**Alternatives considered:**
- **Alt A: Keep `isMetaMask: false`** — Requires patching all dApp WalletModal logic to recognize bridge. Rejected: not scalable.
- **Alt B: Add new flag `isBridgeMetaMask`** — Non-standard, libraries won't recognize it. Rejected: doesn't solve the problem.

### Decision 3: Wait for bridge READY before auto-connect
**Decision:** `useEagerConnect()` polls `window.ethereum.isConnected()` up to 5 seconds before calling `activate()` in bridge mode. Fallback to standard flow if timeout.

**Rationale:**
- **Timing safety:** postMessage handshake (HELLO → READY) is asynchronous. If dApp calls `eth_accounts` before READY, bridge may not have accounts yet.
- **5 second balance:** Long enough for 99% of cases (handshake typically <1s), short enough to not annoy users.
- **Graceful degradation:** If bridge fails/timeouts, dApp shows standard wallet modal (same as non-iframe mode).
- **No blocking:** Timeout ensures UI never hangs waiting for bridge.

**Alternatives considered:**
- **Alt A: No timeout, immediate activate()** — Causes race condition, `eth_accounts` returns `[]` before READY. Rejected: unreliable.
- **Alt B: Wait for `bridgeReady` event** — Cleaner but requires dApp to listen to custom event before React mounts. Rejected: complex initialization order.
- **Alt C: 10+ second timeout** — Too slow, users perceive hang. Rejected: poor UX.

### Decision 4: Suppress WalletModal in bridge mode
**Decision:** WalletModal checks `window.ethereum?.isSwapWalletAppsBridge && active` and returns `null` (don't render) when true.

**Rationale:**
- **Seamless UX:** User already connected wallet in MCW. Showing modal is redundant friction.
- **Preserve functionality:** If bridge active but NOT connected (no MetaMask in MCW), modal DOES show (fallback behavior).
- **Minimal code change:** One conditional check in WalletModal, doesn't affect other components.

**Alternatives considered:**
- **Alt A: Remove Connect button entirely** — Breaks disconnect/reconnect flow. Rejected: users need Connect button for manual reconnection.
- **Alt B: Show modal but auto-close** — Janky UX (flash of modal). Rejected: not seamless.

### Decision 5: Security model
**Decision:** Host-side allowlist (`EXTERNAL_ALLOWED_HOSTS`) + dApp-side iframe + URL parameter checks. No additional origin validation.

**Rationale:**
- **Host allowlist:** MCW only creates bridge for whitelisted domains (`dex.onout.org`). Prevents malicious iframes.
- **dApp iframe check:** Bridge client only activates if `window.parent !== window` (iframe context).
- **URL parameter:** Bridge client checks `?walletBridge=swaponline` (explicit opt-in).
- **Private keys stay on host:** Bridge only proxies signing requests, never exposes keys to iframe.
- **Sufficient for threat model:** Combination of three checks (allowlist + iframe + URL param) prevents both accidental and malicious activation.

**Alternatives considered:**
- **Alt A: Additional origin validation on dApp side** — Redundant, host already validates origin. Rejected: over-engineering.
- **Alt B: User confirmation modal on auto-connect** — Defeats purpose of seamless UX. Rejected: adds friction we're trying to remove.

### Decision 6: Deploy order and backward compatibility
**Decision:** Both repos (MCW and unifactory) can be deployed independently, in any order. Backward compatible.

**Rationale:**
- **Before both deployed:** Old behavior preserved (dApp shows wallet modal as before).
- **MCW deployed first:** unifactory still shows wallet modal (no bridge script loaded). No breakage.
- **unifactory deployed first:** Bridge script loading code added, but MCW hasn't updated `isMetaMask` flag yet. Modal still shows MetaMask option via "Injected" connector. Slight UX degradation but no breakage.
- **After both deployed:** Full auto-connect works.

**Alternatives considered:**
- **Alt A: Require synchronized deploy** — Fragile, hard to coordinate. Rejected: unnecessary coupling.

### Decision 7: Only update unifactory repo (not definance/appsource)
**Decision:** Only modify `noxonsu/unifactory` repo. Don't update `definance/dex` or `appsource/dex`.

**Rationale:**
- **unifactory is upstream:** Other dApp repos fork from unifactory as source of truth.
- **One-time update:** After unifactory updated, other repos can pull changes on their schedule.
- **Scope control:** Feature development focuses on one repo, simplifies testing and rollout.

**Alternatives considered:**
- **Alt A: Update all dApp repos simultaneously** — Triples work, delays rollout. Rejected: inefficient.

## Data Models

No database or persistent storage changes. All state is in-memory (React context, web3-react state).

**Runtime State (unifactory dApp):**

```typescript
// web3-react context (via @web3-react/core)
interface Web3ReactContextInterface {
  connector?: AbstractConnector  // injected connector instance
  library?: Web3Provider         // ethers.js provider from window.ethereum
  chainId?: number               // current network ID
  account?: string | null        // connected address (0x...)
  active: boolean                // true if wallet connected
  error?: Error                  // connection error
}

// Bridge detection state (new module: utils/walletBridge.ts)
interface BridgeDetectionResult {
  isBridgeMode: boolean          // URL param + iframe context
  bridgeReady: boolean           // window.ethereum.isConnected()
  provider: any | null           // window.ethereum (if bridge mode)
}
```

**postMessage Protocol (already implemented):**

```typescript
// Client → Host
interface BridgeHelloMessage {
  source: 'swap.wallet.apps.bridge.client'
  type: 'WALLET_APPS_BRIDGE_HELLO'
  payload: {
    version: string
    ua: string  // navigator.userAgent
  }
}

interface BridgeRequestMessage {
  source: 'swap.wallet.apps.bridge.client'
  type: 'WALLET_APPS_BRIDGE_REQUEST'
  payload: {
    requestId: string
    method: string  // e.g., 'eth_requestAccounts'
    params?: any[]
  }
}

// Host → Client
interface BridgeReadyMessage {
  source: 'swap.wallet.apps.bridge.host'
  type: 'WALLET_APPS_BRIDGE_READY'
  payload: {
    providerAvailable: boolean
    chainId: string | null        // hex chainId (e.g., '0x1')
    accounts: string[]            // addresses (e.g., ['0x123...'])
    methods: string[]             // allowed exact methods
    methodPrefixes: string[]      // allowed prefixes
  }
}

interface BridgeResponseMessage {
  source: 'swap.wallet.apps.bridge.host'
  type: 'WALLET_APPS_BRIDGE_RESPONSE'
  payload: {
    requestId: string
    result?: any
    error?: {
      code: number
      message: string
    }
  }
}

interface BridgeEventMessage {
  source: 'swap.wallet.apps.bridge.host'
  type: 'WALLET_APPS_BRIDGE_EVENT'
  payload: {
    eventName: string  // 'accountsChanged', 'chainChanged', etc.
    data: any
  }
}
```

## Dependencies

### New packages
None. All required packages already in both projects.

### Using existing (from projects)

**MCW dependencies (already installed):**
- `@web3-react/injected-connector@6.0.7` — Used by MCW's InjectedProvider class
- `web3@1.10` — Web3 instance wrapping MetaMask provider

**unifactory dependencies (already installed):**
- `@web3-react/core@6.1.9` — React context for wallet state management
- `@web3-react/injected-connector@6.0.7` — Injected connector for window.ethereum
- `ethers@5.x` — Library provider for web3-react

**Shared pattern:**
Both projects use `@web3-react` v6 (NOT v8), so connector APIs are identical.

## Testing Strategy

**Feature size:** M

### Unit tests

**MCW side (`tests/unit/walletBridge.test.ts` — new file):**
- **Scenario 1: Bridge provider properties** — Verify `window.ethereum.isMetaMask === true`, `isSwapWalletAppsBridge === true`, `chainId` set after READY
- **Scenario 2: postMessage handshake** — Mock postMessage, verify HELLO → READY flow, check accounts and chainId in READY payload
- **Scenario 3: Request forwarding** — Mock host response, call `window.ethereum.request({ method: 'eth_accounts' })`, verify postMessage sent and Promise resolved
- **Scenario 4: Event forwarding** — Mock host BRIDGE_EVENT message, verify `accountsChanged`/`chainChanged` events emitted on provider

**unifactory side (`src/utils/walletBridge.test.ts` — new file):**
- **Scenario 1: Bridge detection** — Mock URL params and iframe context, verify `detectBridgeMode()` returns correct booleans
- **Scenario 2: Script loading** — Mock `document.createElement('script')`, verify script src constructed from referrer, verify fallback URL
- **Scenario 3: Bridge ready detection** — Mock `window.ethereum.isConnected()`, verify `waitForBridgeReady()` resolves when ready, rejects on timeout

**unifactory hooks (`src/hooks/index.test.ts` — extend existing):**
- **Scenario 4: useEagerConnect in bridge mode** — Mock bridge provider, verify `activate(injected)` called without `isAuthorized()` check
- **Scenario 5: useEagerConnect timeout** — Mock bridge never ready, verify fallback to standard flow after 5 seconds
- **Scenario 6: useEagerConnect in standalone mode** — Verify standard behavior unchanged (calls `isAuthorized()` first)

### Integration tests

**Not applicable for M feature.** Cross-origin iframe postMessage cannot be tested in Jest (requires real iframe + parent window context). E2E test covers integration.

### E2E tests

**Extend existing `tests/e2e/walletAppsBridge.smoke.js`:**

**Critical flow 1: Auto-connect happy path**
1. Setup: Configure Puppeteer with test wallet (MetaMask connected in MCW)
2. Open MCW at `#/apps`
3. Click Onout DEX app
4. Wait for iframe load (existing check: iframe src contains `dex.onout.org`)
5. **NEW:** Wait for bridge ready (poll iframe's `window.ethereum.isConnected()` up to 10 seconds)
6. **NEW:** Check iframe DOM: wallet modal NOT visible (no `.wallet-modal` or `[data-testid="wallet-modal"]`)
7. **NEW:** Check iframe DOM: address displayed (look for pattern `0x[a-fA-F0-9]{40}` in body text)
8. **NEW:** Verify address matches MCW wallet address
9. Take screenshot (existing behavior)
10. Assert: `frameChecks.isConnected === true`, `frameChecks.addressDisplayed === true`, `frameChecks.modalVisible === false`

**Critical flow 2: Fallback when no wallet connected**
1. Setup: Configure Puppeteer WITHOUT test wallet (no MetaMask connected in MCW)
2. Open MCW at `#/apps` → click Onout DEX
3. Wait for iframe load
4. **NEW:** Wait 5 seconds (bridge ready timeout)
5. **NEW:** Check iframe DOM: wallet modal IS visible
6. Assert: `frameChecks.modalVisible === true`

**Critical flow 3: Standalone mode unchanged**
1. Open `dex.onout.org` directly (NOT via MCW iframe)
2. Wait for page load
3. Check: wallet modal visible, standard behavior
4. Assert: No bridge provider (`window.swapWalletAppsBridgeProvider === undefined`)

**E2E test runner:** Puppeteer (existing in MCW repo)

**Test execution:** Manual for now (E2E CI commented out in MCW GitHub Actions due to setup issues). Run locally before PR merge: `npm run test:e2e`

## Agent Verification Plan

**Source:** user-spec "Как проверить" section.

### Verification approach

Agent verifies beyond automated tests by:
1. Running E2E smoke test on PR preview URL
2. Inspecting Puppeteer screenshots for visual confirmation (address visible, no modal)
3. Checking browser console logs in screenshots for errors
4. Verifying unit test coverage (all new modules have tests)

### Per-task verification

| Task | verify: | What to check |
|------|---------|--------------|
| 1    | bash    | `git show issue-5268-apps-layout:src/front/client/wallet-apps-bridge-client.js \| grep 'isMetaMask: true'` — verify flag changed |
| 2    | bash    | `npm run test:unit` in unifactory repo — all tests pass |
| 3    | bash    | `npm run build` in unifactory repo — build succeeds |
| 4    | user    | User opens PR preview at `#/apps`, clicks DEX, confirms auto-connect |
| 5    | bash    | `npm run test:unit` in MCW repo — new bridge tests pass |
| 6    | bash    | Run Puppeteer smoke test: `npm run test:e2e` — check output JSON for `success: true` |
| 7    | user    | User checks screenshots in `/tmp/mcw_apps_onout_iframe.png` for address visible |

### Tools required

- **bash** — Run npm commands, check file contents, git operations
- **user verification** — Manual browser testing (agent cannot render UI or interact with cross-origin iframes)

## Risks

| Risk | Mitigation |
|------|-----------|
| **R1: Timing race** — dApp calls `eth_accounts` before bridge READY, returns empty accounts, auto-connect fails | Wait for `isConnected()` up to 5 sec before `activate()`. Timeout → fallback to standard wallet modal. E2E test verifies timing. |
| **R2: Breaking standalone mode** — Bridge code activates outside iframe, breaks normal dApp behavior | Strict dual check: URL param `?walletBridge=swaponline` AND `window.parent !== window`. E2E test verifies standalone mode unchanged. |
| **R3: Security — malicious iframe auto-connects** | Host-side allowlist (`EXTERNAL_ALLOWED_HOSTS`) blocks non-whitelisted domains. dApp-side requires explicit URL param + iframe context. Private keys stay on host, bridge only proxies signing. No additional risk vs. standard MetaMask usage. |
| **R4: Script loading failure** — Network error, CORS, CSP blocks bridge client download | Timeout after 5 sec, fallback to standard wallet modal. User sees familiar UI, can connect via WalletConnect/other methods. E2E test verifies fallback. |
| **R5: Version drift** — MCW updates bridge protocol, old unifactory deploy breaks | Bridge protocol includes version in HELLO message. Host can check version and send error if incompatible. Currently version is `1.0.0`, no breaking changes planned. If needed in future, add version check in host's `handleMessage()`. |
| **R6: Referrer blocked by privacy extensions** — `document.referrer` returns empty string, can't determine wallet host URL | Fallback to hardcoded mainnet URL (`https://swaponline.github.io`). Works for 95% of users. If user on testnet/custom deploy, script loading fails → fallback to wallet modal (acceptable degradation). |
| **R7: Multiple iframes** — User opens multiple dApp tabs, bridge instances collide | Non-issue: each iframe gets separate `window` context, separate bridge provider instance. Host creates separate bridge object per iframe (existing architecture). No shared state. |

## Acceptance Criteria

Technical acceptance criteria (complement user-spec ACs):

- [ ] **AC-T1:** `wallet-apps-bridge-client.js` has `isMetaMask: true` (verify via grep)
- [ ] **AC-T2:** Bridge client emits `connect` event on BRIDGE_READY (verify in unit test)
- [ ] **AC-T3:** unifactory `utils/walletBridge.ts` module exists with `detectBridgeMode()`, `loadBridgeClient()`, `waitForBridgeReady()` functions
- [ ] **AC-T4:** `useEagerConnect()` has bridge-aware path that bypasses `isAuthorized()` check when bridge detected and ready
- [ ] **AC-T5:** `WalletModal` suppressed when `window.ethereum?.isSwapWalletAppsBridge && active` (verify in code review)
- [ ] **AC-T6:** All unit tests pass in both repos (`npm run test:unit`)
- [ ] **AC-T7:** E2E smoke test extended to check `addressDisplayed` and `modalVisible === false` in bridge mode
- [ ] **AC-T8:** E2E smoke test passes on PR preview URL before merge
- [ ] **AC-T9:** No regressions in existing unit tests (both repos)
- [ ] **AC-T10:** Standalone mode E2E test passes (dApp behavior unchanged without iframe)
- [ ] **AC-T11:** TypeScript builds succeed in both repos (no type errors)
- [ ] **AC-T12:** Code passes ESLint/Prettier checks (unifactory has linting)

## Implementation Tasks

<!-- Tasks are brief scope descriptions. AC, TDD, and detailed steps are created during task-decomposition. -->

### Wave 1 (independent)

#### Task 1: Update MCW bridge client `isMetaMask` flag
- **Description:** Change `wallet-apps-bridge-client.js` line 111 from `isMetaMask: false` to `isMetaMask: true`. Commit to branch `issue-5268-apps-layout`. Result: Bridge provider recognized as MetaMask by dApp libraries.
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** bash — `git show issue-5268-apps-layout:src/front/client/wallet-apps-bridge-client.js | grep 'isMetaMask: true'` returns match
- **Files to modify:** `src/front/client/wallet-apps-bridge-client.js` (line 111)
- **Files to read:** None (single-line change)

#### Task 2: Create unifactory bridge utils module
- **Description:** Create `src/utils/walletBridge.ts` in unifactory repo with three functions: (1) `detectBridgeMode()` — checks URL param + iframe context, (2) `loadBridgeClient(referrer)` — dynamically creates script tag with fallback, (3) `waitForBridgeReady(timeout)` — polls `window.ethereum.isConnected()`. Result: Reusable bridge utilities for hooks/components.
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `npm run test:unit` in unifactory, check new test file passes
- **Files to modify:** `src/utils/walletBridge.ts` (new file), `src/utils/walletBridge.test.ts` (new file)
- **Files to read:** `src/connectors/index.ts` (understand injected connector)

#### Task 3: Add inline bridge loading script to unifactory index.html
- **Description:** Add inline `<script>` in `public/index.html` (before React bundle) that: (1) checks `window.location.search.includes('walletBridge=swaponline')` AND `window.parent !== window`, (2) extracts wallet host from `document.referrer`, (3) dynamically creates script tag `src="<referrer>/wallet-apps-bridge-client.js"`, (4) fallback to `https://swaponline.github.io/wallet-apps-bridge-client.js` if referrer empty. Result: Bridge client loaded before React app mounts.
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** user — user inspects PR preview `view-source:` and sees inline script
- **Files to modify:** `public/index.html`
- **Files to read:** `public/index.html` (current structure)

### Wave 2 (depends on Wave 1)

#### Task 4: Extend useEagerConnect with bridge auto-connect
- **Description:** Modify `src/hooks/index.ts` `useEagerConnect()` hook to: (1) detect bridge mode via `window.ethereum?.isSwapWalletAppsBridge`, (2) wait for `waitForBridgeReady(5000)`, (3) call `activate(injected)` immediately (skip `isAuthorized()`), (4) on timeout, fallback to standard eager connect flow. Result: dApp auto-connects in bridge mode without user interaction.
- **Skill:** code-writing
- **Reviewers:** code-reviewer, test-reviewer
- **Verify:** bash — `npm run test:unit` in unifactory, new hook tests pass
- **Files to modify:** `src/hooks/index.ts`, `src/hooks/index.test.ts` (extend tests)
- **Files to read:** `src/hooks/index.ts` (current useEagerConnect), `src/utils/walletBridge.ts` (bridge utils from Task 2)

#### Task 5: Suppress WalletModal in bridge mode
- **Description:** Modify `src/components/WalletModal/index.tsx` to add early return: if `window.ethereum?.isSwapWalletAppsBridge && active` (web3-react context), return `null` (don't render modal). If bridge active but NOT connected, allow modal to show (fallback). Result: No wallet modal when bridge auto-connected.
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** user — user opens PR preview, DEX auto-connects, no modal visible
- **Files to modify:** `src/components/WalletModal/index.tsx`
- **Files to read:** `src/components/WalletModal/index.tsx` (current modal logic), `src/hooks/index.ts` (understand active state)

#### Task 6: Add bridge ready handler in Web3ReactManager
- **Description:** Modify `src/components/Web3ReactManager/index.tsx` to listen for custom `bridgeReady` event on `window.ethereum` (emitted by bridge client after READY). On event, retry eager connect if not already active. Result: Handles late bridge ready (if initial eager connect ran before bridge READY arrived).
- **Skill:** code-writing
- **Reviewers:** code-reviewer
- **Verify:** bash — `npm test` (React Testing Library test)
- **Files to modify:** `src/components/Web3ReactManager/index.tsx`
- **Files to read:** `src/components/Web3ReactManager/index.tsx` (current structure), `src/hooks/index.ts` (useEagerConnect)

### Wave 3 (depends on Wave 2)

#### Task 7: Add MCW bridge unit tests
- **Description:** Create `tests/unit/walletBridge.test.ts` in MCW repo with Jest tests: (1) verify bridge provider properties (`isMetaMask`, `chainId`, `selectedAddress`), (2) mock postMessage, verify HELLO → READY handshake, (3) verify request forwarding and Promise resolution, (4) verify event forwarding (accountsChanged, chainChanged). Result: Bridge protocol covered by unit tests.
- **Skill:** code-writing
- **Reviewers:** test-reviewer
- **Verify:** bash — `npm run test:unit` in MCW, new test file passes
- **Files to modify:** `tests/unit/walletBridge.test.ts` (new file)
- **Files to read:** `src/front/client/wallet-apps-bridge-client.js` (bridge client code), `tests/unit/appsCatalog.test.ts` (example unit test structure)

#### Task 8: Extend MCW E2E smoke test for auto-connect
- **Description:** Modify `tests/e2e/walletAppsBridge.smoke.js` to add new checks after iframe loads: (1) wait for `window.ethereum.isConnected()` up to 10 sec, (2) query iframe DOM for wallet modal (`.wallet-modal` or `[data-testid="wallet-modal"]`), verify NOT visible, (3) extract address from iframe body text (regex `/0x[a-fA-F0-9]{40}/`), verify present, (4) compare address to MCW test wallet address, (5) add new E2E test case for no-wallet fallback (modal visible). Result: E2E test verifies full auto-connect flow.
- **Skill:** code-writing
- **Reviewers:** test-reviewer
- **Verify:** bash — `npm run test:e2e` in MCW, smoke test passes
- **Files to modify:** `tests/e2e/walletAppsBridge.smoke.js`
- **Files to read:** `tests/e2e/walletAppsBridge.smoke.js` (current smoke test), `tests/testWallets.json` (test wallet address)

### Final Wave

#### Task 9: Pre-deploy QA
- **Description:** Acceptance testing on PR preview URLs (both MCW and unifactory deploys). Run all tests, verify all acceptance criteria from user-spec and tech-spec. Check: (1) MCW unit tests pass, (2) unifactory unit tests pass, (3) E2E smoke test passes, (4) Manual browser test: open PR preview `#/apps` → DEX auto-connects, address visible, no modal, (5) Standalone test: open `dex.onout.org` directly → standard wallet modal shows, (6) No-wallet test: open PR preview without MetaMask connected → DEX shows wallet modal.
- **Skill:** pre-deploy-qa
- **Reviewers:** none

#### Task 10: Deploy MCW (GitHub Pages)
- **Description:** Merge PR to `master` branch in MCW repo. GitHub Actions builds and deploys to `swaponline.github.io`. Verify: (1) GitHub Actions workflow succeeds, (2) wallet-apps-bridge-client.js accessible at `https://swaponline.github.io/wallet-apps-bridge-client.js`, (3) Check browser console for errors on `#/apps` page.
- **Skill:** infrastructure
- **Reviewers:** none

#### Task 11: Deploy unifactory (dex.onout.org)
- **Description:** Merge PR to `master` branch in unifactory repo (or deploy branch configured for dex.onout.org). Deploy to production. Verify: (1) `dex.onout.org` loads without errors, (2) Standalone mode works (wallet modal shows), (3) Open via MCW `#/apps` → auto-connect works.
- **Skill:** infrastructure
- **Reviewers:** none

#### Task 12: Post-deploy verification
- **Description:** Live environment verification on production URLs: (1) Open `https://swaponline.github.io/#/apps` → click Onout DEX → verify auto-connect (address visible, no modal), (2) Open `https://dex.onout.org` directly → verify standard wallet modal, (3) Test network switch in MCW → verify DEX updates, (4) Test disconnect in MCW → verify DEX shows Connect button, (5) Check browser console for errors. Document any issues in GitHub issue.
- **Skill:** post-deploy-qa
- **Reviewers:** none
