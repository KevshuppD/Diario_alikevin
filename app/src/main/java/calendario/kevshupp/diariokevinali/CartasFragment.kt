package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CartasFragment : Fragment() {
    companion object {
        fun newInstance(coupleId: String, theme: String): CartasFragment {
            val f = CartasFragment()
            val a = Bundle()
            a.putString("coupleId", coupleId)
            a.putString("theme", theme)
            f.arguments = a
            return f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return TextView(context).apply { text = "Pantalla de Cartas (En Limpieza)" }
    }
}
