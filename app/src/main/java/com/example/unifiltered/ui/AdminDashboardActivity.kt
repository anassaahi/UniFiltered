package com.example.unifiltered.ui

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unifiltered.R
import com.example.unifiltered.repository.SocietyRepository
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private val repository = SocietyRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val container = findViewById<LinearLayout>(R.id.layoutRequestsContainer)

        lifecycleScope.launch {
            val result = repository.getPendingRequests()
            if (result.isSuccess) {
                val requests = result.getOrDefault(emptyList())

                if (requests.isEmpty()) {
                    Toast.makeText(this@AdminDashboardActivity, "No pending requests!", Toast.LENGTH_SHORT).show()
                }

                for (req in requests) {
                    // Create a visual ticket for each request via code
                    val ticket = LinearLayout(this@AdminDashboardActivity)
                    ticket.orientation = LinearLayout.VERTICAL
                    ticket.setPadding(16, 16, 16, 16)
                    ticket.setBackgroundColor(getColor(android.R.color.darker_gray))

                    val text = TextView(this@AdminDashboardActivity)
                    text.text = "Society: ${req.societyName}\nRequested by: ${req.requesterId}"
                    text.setTextColor(getColor(android.R.color.white))

                    val btnApprove = Button(this@AdminDashboardActivity)
                    btnApprove.text = "APPROVE & VERIFY"
                    btnApprove.setOnClickListener {
                        lifecycleScope.launch {
                            val approveResult = repository.approveSociety(req.requestId, req.societyId)
                            if (approveResult.isSuccess) {
                                Toast.makeText(this@AdminDashboardActivity, "${req.societyName} is now Official!", Toast.LENGTH_SHORT).show()
                                ticket.visibility = android.view.View.GONE // Hide ticket
                            }
                        }
                    }

                    ticket.addView(text)
                    ticket.addView(btnApprove)

                    // Add some margin between tickets
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, 16)
                    container.addView(ticket, params)
                }
            }
        }
    }
}