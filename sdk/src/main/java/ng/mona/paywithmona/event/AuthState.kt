package ng.mona.paywithmona.event

/**
 * Defines the possible authentication states of the user.
 */
enum class AuthState {
    /** The user is authenticated and logged in */
    LoggedIn,

    /** The user is not authenticated and logged out */
    LoggedOut,

    /** An error occurred during authentication */
    Error,

    /** The user is not a Mona user */
    NotAMonaUser,

    /** Currently doing login with strong auth token */
    PerformingLogin
}