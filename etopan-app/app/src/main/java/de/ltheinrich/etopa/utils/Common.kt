package de.ltheinrich.etopa.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import kotlin.reflect.KClass

typealias Handler = (response: JSONObject) -> Unit
typealias StringHandler = (response: String) -> Unit
typealias ErrorHandler = (error: VolleyError) -> Unit

var library: Boolean = false

class Common constructor(activity: Activity) {

    lateinit var instance: String
    lateinit var username: String
    lateinit var passwordHash: String
    lateinit var keyHash: String
    lateinit var pinHash: String
    lateinit var token: String
    var offline: Boolean = false

    fun decryptLogin(preferences: SharedPreferences) {
        instance = preferences.getString("instance", encrypt(pinHash, "etopa.de"))?.let {
            decrypt(
                pinHash,
                it
            )
        }.toString()
        username = decrypt(
            pinHash,
            preferences.getString("username", encrypt(pinHash, "")).orEmpty()
        )
        passwordHash = decrypt(
            pinHash,
            preferences.getString("passwordHash", encrypt(pinHash, "")).orEmpty()
        )
        keyHash = decrypt(
            pinHash,
            preferences.getString("keyHash", encrypt(pinHash, "")).orEmpty()
        )
        token = decrypt(
            pinHash,
            preferences.getString("token", encrypt(pinHash, "")).orEmpty()
        )
    }

    fun request(
        url: String,
        handler: Handler,
        vararg data: Pair<String, String>,
        error_handler: ErrorHandler = { error: VolleyError ->
            Log.e(
                "HTTP Request",
                error.toString()
            )
        }
    ) {
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, "https://$instance/$url", null,
            Response.Listener { response ->
                offline = false
                handler(response)
            },
            Response.ErrorListener { error ->
                offline = true
                error_handler(error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return data.toMap()
            }
        }
        http.add(jsonObjectRequest)
    }

    fun requestString(
        url: String,
        handler: StringHandler,
        vararg data: Pair<String, String>,
        error_handler: ErrorHandler = { error: VolleyError ->
            Log.e(
                "HTTP Request",
                error.toString()
            )
        }
    ) {
        val stringRequest = object : StringRequest(
            Method.POST, "https://$instance/$url",
            Response.Listener { response ->
                offline = false
                handler(response)
            },
            Response.ErrorListener { error ->
                offline = true
                error_handler(error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return data.toMap()
            }
        }
        http.add(stringRequest)
    }

    fun <T : Activity> openActivity(
        cls: KClass<T>,
        vararg extras: Pair<String, String>
    ) {
        val app = Intent(activity, cls.java)
        for ((key, value) in extras) {
            app.putExtra(key, value)
        }
        activity.startActivity(app)
    }

    fun toast(stringId: Int, height: Int = 0) {
        val toast = Toast.makeText(activity, stringId, Toast.LENGTH_LONG)
        if (height != 0)
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, height)
        toast.show()
    }

    fun hideKeyboard() {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view: View? = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun copyToClipboard(toCopy: String) {
        val clipboard = ContextCompat.getSystemService(
            activity,
            ClipboardManager::class.java
        )
        val clip = ClipData.newPlainText(toCopy, toCopy)
        clipboard?.setPrimaryClip(clip)
    }

    companion object {
        @Volatile
        private var INSTANCE: Common? = null
        fun getInstance(activity: Activity): Common =
            if (library) {
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                        ?: Common(activity).also {
                            INSTANCE = it
                        }
                }
            } else {
                System.loadLibrary("etopan")
                library = true
                getInstance(activity)
            }
    }

    private val activity: Activity by lazy {
        activity
    }

    private val http: RequestQueue by lazy {
        Volley.newRequestQueue(activity.applicationContext)
    }

    external fun hashKey(key: String): String

    external fun hashPassword(password: String): String

    external fun hashPin(pin: String): String

    external fun encrypt(key: String, data: String): String

    external fun decrypt(key: String, data: String): String

    external fun generateToken(secret: String): String

    /*val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
            object : ImageLoader.ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun getBitmap(url: String): Bitmap {
                    return cache.get(url)
                }

                override fun putBitmap(url: String, bitmap: Bitmap) {
                    cache.put(url, bitmap)
                }
            })
    }*/
}