# Android MVP Wallet

Native Android app (Kotlin + Jetpack Compose) for the mobile epic.

## Implemented MVP scope

- Create wallet (new entropy)
- Generate and display 12-word mnemonic phrase
- Derive and display private key
- Save generated data in encrypted local storage
- Restore last saved wallet on app launch

Exchange flows are intentionally excluded at this stage.

## Project path

`android/`

## CI

GitHub Actions workflow: `.github/workflows/android-ci.yml`
- `assembleDebug`
- `test`
