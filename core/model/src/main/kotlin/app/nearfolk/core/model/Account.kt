package app.nearfolk.core.model

import java.time.LocalDate

enum class AccountState {
    /** No profile on this device, or the person signed out. */
    SignedOut,
    Active,

    /** Profile hidden from the network until reactivated. */
    Deactivated,
}

data class ProfileDraft(
    val displayName: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val countryCode: String,
)

data class IdentityUpdate(
    val displayName: String,
    val catchphrase: String,
    val bio: String,
    val countryCode: String,
    val city: String?,
    val gender: Gender,
    val birthDate: LocalDate,
    val streakStart: LocalDate,
)

enum class DeletionReason { NotUseful, PrivacyConcerns, TooManyMessages, FoundSomeone, TakingABreak, Other }

object ProfileRules {
    const val MIN_AGE = 18
    const val NAME_MIN = 2
    const val NAME_MAX = 30
    const val CATCHPHRASE_MAX = 80
    const val BIO_MAX = 400
    const val CAPTION_MAX = 500
    const val COMMENT_MAX = 300
    const val MESSAGE_MAX = 1000
    const val FIRST_CONTACT_LIMIT = 3
}
