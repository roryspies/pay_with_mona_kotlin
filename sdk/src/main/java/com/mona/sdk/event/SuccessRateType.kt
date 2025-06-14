package com.mona.sdk.event

enum class SuccessRateType(val key: String, val displayName: String) {
    MonaSuccess("mona_success", "Mona success"),
    DebitSuccess("debit_success", "Debit success"),
    WalletReceiveInProgress("wallet_received", "Wallet receive in progress"),
    WalletReceiveComplete("wallet_completed", "Wallet receive complete");
}