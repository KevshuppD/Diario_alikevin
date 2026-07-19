package calendario.kevshupp.diariokevinali

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarAdapter(
    private val eventList: List<CalendarEvent>,
    private val currentUserId: String,
    private val listener: OnEventActionListener
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var theme: String = "Pixel Claro"
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    interface OnEventActionListener {
        fun onDeleteEvent(event: CalendarEvent)
        fun onEditEvent(event: CalendarEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_event, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val event = eventList[position]

        val isDark = "Pixel Oscuro" == theme
        if (isDark) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#1A1A2E"))
            holder.cardView.strokeColor = Color.parseColor("#30304A")
            holder.tvTitle.setTextColor(Color.WHITE)
            holder.tvDescription.setTextColor(Color.LTGRAY)
            holder.tvAuthor.setTextColor(Color.parseColor("#A084CA"))
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.cardView.strokeColor = Color.parseColor("#DDDDDD")
            holder.tvTitle.setTextColor(Color.parseColor("#5D2E7A"))
            holder.tvDescription.setTextColor(Color.parseColor("#333333"))
            holder.tvAuthor.setTextColor(Color.parseColor("#91465F"))
        }

        val timeStr = timeFormat.format(Date(event.date))
        val recurrenceStr = when (event.recurrence) {
            "WEEKLY" -> " (Semanal)"
            "YEARLY" -> " (Anual)"
            else -> ""
        }

        holder.tvTitle.text = "${event.title} - $timeStr$recurrenceStr"
        holder.tvDescription.text = event.description
        holder.tvAuthor.text = "Agendado por: ${event.authorName}"

        // Solo permitir borrar/editar si es el autor
        if (event.authorId == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                listener.onDeleteEvent(event)
            }
            // Al pulsar en cualquier parte de la tarjeta se edita
            holder.itemView.setOnClickListener {
                listener.onEditEvent(event)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = eventList.size

    fun setTheme(theme: String) {
        this.theme = theme
        notifyDataSetChanged()
    }

    class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardEvent)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
        val tvAuthor: TextView = itemView.findViewById(R.id.tvEventAuthor)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteEvent)
    }
}
