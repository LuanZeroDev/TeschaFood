package com.tescha.food.data.local

import android.content.Context
import com.tescha.food.data.model.User

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("food_session", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userIsMerchant: Boolean
        get() = prefs.getBoolean(KEY_IS_MERCHANT, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MERCHANT, value).apply()

    var locationLatitude: Double?
        get() {
            val raw = prefs.getFloat(KEY_LAT, Float.MIN_VALUE)
            return if (raw == Float.MIN_VALUE) null else raw.toDouble()
        }
        set(value) = if (value != null) {
            prefs.edit().putFloat(KEY_LAT, value.toFloat()).apply()
        } else {
            prefs.edit().remove(KEY_LAT).apply()
        }

    var locationLongitude: Double?
        get() {
            val raw = prefs.getFloat(KEY_LNG, Float.MIN_VALUE)
            return if (raw == Float.MIN_VALUE) null else raw.toDouble()
        }
        set(value) = if (value != null) {
            prefs.edit().putFloat(KEY_LNG, value.toFloat()).apply()
        } else {
            prefs.edit().remove(KEY_LNG).apply()
        }

    fun saveUser(user: User) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHONE, user.phone)
            .putBoolean(KEY_IS_MERCHANT, user.isMerchant)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_IS_MERCHANT = "user_is_merchant"
        private const val KEY_LAT = "location_lat"
        private const val KEY_LNG = "location_lng"
    }
}
