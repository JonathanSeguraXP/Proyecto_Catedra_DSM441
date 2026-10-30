package com.example.the_white_smile

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.WeakHashMap

/**
 * Base para todas las pantallas de la aplicacion.
 *
 * Desde Android 15 la app se dibuja de borde a borde, por lo que el contenido
 * puede quedar debajo de la camara, la barra de estado, la barra de navegacion
 * o el teclado. Esta clase aplica los margenes necesarios a la vista raiz de
 * cada pantalla para que ninguna vista quede tapada.
 */
abstract class BaseActivity : AppCompatActivity() {

    /** Margen original de cada vista raiz, para no perder el que venga del XML. */
    private val originalPadding = WeakHashMap<View, Rect>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Hace que el comportamiento sea el mismo en todas las versiones de Android.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyWindowInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyWindowInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        applyWindowInsets()
    }

    private fun applyWindowInsets() {
        val content = findViewById<ViewGroup>(android.R.id.content) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )

            val target = (view as? ViewGroup)?.getChildAt(0) ?: view
            val base = originalPadding.getOrPut(target) {
                Rect(
                    target.paddingLeft,
                    target.paddingTop,
                    target.paddingRight,
                    target.paddingBottom
                )
            }
            target.setPadding(
                base.left + insets.left,
                base.top + insets.top,
                base.right + insets.right,
                base.bottom + insets.bottom
            )

            windowInsets
        }

        ViewCompat.requestApplyInsets(content)
    }
}
