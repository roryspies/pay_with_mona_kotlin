package com.mona.sdk.event

sealed interface TransactionState {
    sealed class WithInfo(
        open val transactionId: String? = null,
        open val friendlyId: String? = null,
        open val amount: Number? = null
    ) : TransactionState

    object Idle : TransactionState

    data class Initiated(
        override val transactionId: String? = null,
        override val friendlyId: String? = null,
        override val amount: Number? = null,
    ) : WithInfo(transactionId, friendlyId, amount)

    data class ProgressUpdate(
        override val transactionId: String? = null,
        override val friendlyId: String? = null,
        override val amount: Number? = null,
    ) : WithInfo(transactionId, friendlyId, amount)

    data class Completed(
        override val transactionId: String? = null,
        override val friendlyId: String? = null,
        override val amount: Number? = null
    ) : WithInfo(transactionId, friendlyId, amount)

    data class Failed(
        val reason: String? = null,
        override val transactionId: String? = null,
        override val friendlyId: String? = null,
        override val amount: Number? = null
    ) : WithInfo(transactionId, friendlyId, amount)

//    data class RequestOTPTask(val task: TransactionTaskModel) : TransactionState
//
//    data class RequestPINTask(val task: TransactionTaskModel) : TransactionState

    data class NavToResult(
        override val transactionId: String? = null,
        override val friendlyId: String? = null,
        override val amount: Number? = null
    ) : WithInfo(transactionId, friendlyId, amount)
}