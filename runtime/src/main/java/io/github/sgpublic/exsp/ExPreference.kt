package io.github.sgpublic.exsp

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

object ExPreference {
    private var context: WeakReference<Context>? = WeakReference(null)

    @JvmStatic
    fun init(context: Context) {
        this.context = WeakReference(context)
    }

    @JvmStatic
    fun getSharedPreference(name: String, mode: Int): Reference {
        return Reference(requiredContext(), name, mode)
    }

    private fun requiredContext() = context?.get()
        ?: throw IllegalStateException("Context are not initialized, did you call ExPreference.init(context)?")

    class Reference internal constructor(
        private val context: Context,
        private val name: String,
        private val mode: Int,
    ): SharedPreferences.OnSharedPreferenceChangeListener, Lazy<SharedPreferences> {
        private var sp: SharedPreferences? = null

        override val value: SharedPreferences get() {
            sp?.let { return it }
            synchronized(this) {
                sp?.let { return it }
                sp = context.getSharedPreferences(name, mode)
                return sp!!
            }
        }

        fun edit(): SharedPreferences.Editor {
            return value.edit()
        }

        private var onChange: ((SharedPreferences, String) -> Unit)? = null
        fun setOnSharedPreferenceChangedListener(onChange: ((SharedPreferences, String) -> Unit)? = null) {
            if (isInitialized()) {
                if (onChange == null) {
                    value.unregisterOnSharedPreferenceChangeListener(this)
                } else {
                    this.onChange = onChange
                }
            } else {
                this.onChange = onChange
                value.registerOnSharedPreferenceChangeListener(this)
            }
        }

        override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
            onChange?.invoke(sp, key)
        }

        override fun isInitialized(): Boolean = sp != null
    }

    inline fun <reified T> get(): T {
        return get(T::class.java)
    }

    @JvmStatic
    fun <T> get(clazz: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return (Class.forName("io.github.sgpublic.exsp.ExPrefs")
            .getMethod("get", Class::class.java).invoke(null, clazz) as Lazy<T>).value
    }
}