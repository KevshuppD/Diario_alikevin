package calendario.kevshupp.diariokevinali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import calendario.kevshupp.diariokevinali.compose.CalendarScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

class CalendarFragment : Fragment() {

    private var coupleId: String? = null
    private var userId: String? = null
    private var theme: String? = null
    private val db = FirebaseFirestore.getInstance()
    private var calendarListener: ListenerRegistration? = null
    
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())

    companion object {
        @JvmStatic
        fun newInstance(coupleId: String, userId: String, theme: String): CalendarFragment {
            val fragment = CalendarFragment()
            val args = Bundle()
            args.putString("coupleId", coupleId)
            args.putString("userId", userId)
            args.putString("theme", theme)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            coupleId = it.getString("coupleId")
            userId = it.getString("userId")
            theme = it.getString("theme")
        }
        listenCalendar()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val eventsState = _events.collectAsState()
                val dateState = _selectedDate.collectAsState()
                
                CalendarScreen(
                    theme = theme ?: "Pixel Claro",
                    events = eventsState.value,
                    selectedTimestamp = normalizeDate(dateState.value),
                    onDateSelected = { _selectedDate.value = it },
                    onAddEvent = {
                        (activity as? AppNavigation)?.showAddEventDialog(normalizeDate(_selectedDate.value).toString(), null)
                    },
                    onEditEvent = { event ->
                        (activity as? AppNavigation)?.showAddEventDialog(normalizeDate(event.date).toString(), event)
                    },
                    onDeleteEvent = { event ->
                        event.eventId?.let { id -> db.collection("calendar").document(id).delete() }
                    }
                )
            }
        }
    }

    private fun listenCalendar() {
        calendarListener?.remove()
        calendarListener = db.collection("calendar")
            .whereEqualTo("partnerId", coupleId)
            .addSnapshotListener { snaps, _ ->
                if (snaps != null) {
                    val list = snaps.map { d ->
                        val ev = d.toObject(CalendarEvent::class.java)
                        if (ev.eventId == null) ev.eventId = d.id
                        ev
                    }
                    _events.value = list
                }
            }
    }

    private fun normalizeDate(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
