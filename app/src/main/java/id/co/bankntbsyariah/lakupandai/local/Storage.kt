package id.co.bankntbsyariah.lakupandai.local

import android.content.Context
import androidx.core.content.edit
import id.co.bankntbsyariah.lakupandai.common.Constants

interface Storage {
    val context: Context

    fun updateVersion(ver: Int) {
        context.getSharedPreferences(Constants.SP_APPS, 0).edit {
            putInt(Constants.SP_VERSION, ver)
            apply()
        }
    }

    fun fetchVersion(): Int {
        return context.getSharedPreferences(Constants.SP_APPS, 0)
            .getInt(Constants.SP_VERSION, Constants.DEFAULT_VERSION)
    }

    fun updateRootMenuId(id: String) {
        context.getSharedPreferences(Constants.SP_APPS, 0).edit {
            putString(Constants.SP_ROOT_ID, id)
            apply()
        }
    }

    fun fetchRootMenuId(): String {
        return context.getSharedPreferences(Constants.SP_APPS, 0)
            .getString(Constants.SP_ROOT_ID, Constants.DEFAULT_ROOT_ID) ?:""
    }

    fun updateMenu(id: String, value: String) {
        context.getSharedPreferences(Constants.SP_APPS, 0).edit {
            putString(id, value)
            apply()
        }
    }

    fun updateForm(id: String, value: String) {
        context.getSharedPreferences(Constants.SP_APPS, 0).edit {
            putString(id, value)
            apply()
        }
    }

    fun fetchMenu(id: String): String? {
        return context.getSharedPreferences(Constants.SP_APPS, 0)
            .getString(id, "")
    }

    fun fetchForm(id: String): String? {
        return context.getSharedPreferences(Constants.SP_APPS, 0)
            .getString(id, "")
    }
}