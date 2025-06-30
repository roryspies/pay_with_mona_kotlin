package ng.mona.paywithmona.data.remote

import ng.mona.paywithmona.BuildConfig

internal object ApiConfig {
    const val HOST = "${BuildConfig.SUB_DOMAIN}.mona.ng"
    const val API_HOST = "api.$HOST"
    const val PAY_HOST = "https://pay.$HOST"
    const val FIREBASE_DB_URL = "https://mona-money-default-rtdb.europe-west1.firebasedatabase.app"
}