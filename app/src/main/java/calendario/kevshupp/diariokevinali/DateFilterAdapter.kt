package calendario.kevshupp.diariokevinali

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateFilterAdapter(
    private val dateList: List<Long>,
    private val listener: OnDateSelectedListener
) : RecyclerView.Adapter<DateFilterAdapter.DateViewHolder>() {

    private val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
    private val yearSdf = SimpleDateFormat("yyyy", Locale.getDefault())

    interface OnDateSelectedListener {
        fun onDateSelected(timestamp: Long)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_filter, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val timestamp = dateList[position]
        val date = Date(timestamp)

        var formattedDate = sdf.format(date)
        if (formattedDate.isNotEmpty()) {
            formattedDate = formattedDate.substring(0, 1).uppercase() + formattedDate.substring(1)
        }

        holder.tvDate.text = formattedDate
        holder.tvYear.text = yearSdf.format(date)

        holder.itemView.setOnClickListener { listener.onDateSelected(timestamp) }
    }

    override fun getItemCount(): Int = dateList.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvFilterDateText)
        val tvYear: TextView = itemView.findViewById(R.id.tvFilterYearText)

        init {
            try {
                val pixelFont: Typeface? = ResourcesCompat.getFont(itemView.context, R.font.vt323)
                tvDate.typeface = pixelFont
                tvYear.typeface = pixelFont
            } catch (ignored: Exception) {
            }
        }
    }
}
