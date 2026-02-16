package com.example.tasklly.ui.home.provider

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasklly.R
import com.example.tasklly.data.model.Task
import com.example.tasklly.ui.adapter.ProviderTasksAdapter
import com.example.tasklly.ui.home.provider.task.ProviderTaskDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProviderHomeFragment : Fragment(R.layout.fragment_provider_home) {

    private lateinit var rvTasks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spLocation: Spinner
    private lateinit var btnClear: Button

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val allTasks = mutableListOf<Task>()
    private lateinit var adapter: ProviderTasksAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTasks = view.findViewById(R.id.rvTasks)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        etSearch = view.findViewById(R.id.etSearch)
        spCategory = view.findViewById(R.id.spCategory)
        spLocation = view.findViewById(R.id.spLocation)
        btnClear = view.findViewById(R.id.btnClearFilters)

        setupRecycler()
        setupSpinners()
        loadOpenTasks()

        btnClear.setOnClickListener {
            etSearch.setText("")
            spCategory.setSelection(0)
            spLocation.setSelection(0)
            applyFilters()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilters()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupRecycler() {
        adapter = ProviderTasksAdapter(
            items = emptyList(),
            onView = { task ->
                val i = Intent(requireContext(), ProviderTaskDetailsActivity::class.java)
                i.putExtra("taskId", task.taskId)
                startActivity(i)
            },
            onApply = { task ->
                applyToTask(task)
            },
            isApplied = { _ ->
                // (optional) можеш да го надградиш да чита од providerApplications
                false
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = adapter
    }

    private fun setupSpinners() {
        spCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All", "Cleaning", "Repair", "Delivery", "Other")
        )

        spLocation.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All", "Bitola", "Skopje", "Ohrid")
        )

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spCategory.onItemSelectedListener = listener
        spLocation.onItemSelectedListener = listener
    }

    private fun loadOpenTasks() {
        db.child("tasks")
            .orderByChild("status")
            .equalTo("open")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allTasks.clear()

                    for (s in snapshot.children) {
                        val task = s.getValue(Task::class.java) ?: continue

                        // ✅ најважно: taskId секогаш = Firebase key
                        task.taskId = s.key ?: ""

                        // ако поради некоја причина нема key -> skip
                        if (task.taskId.isBlank()) continue

                        allTasks.add(task)
                    }

                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim().lowercase()
        val cat = spCategory.selectedItem?.toString() ?: "All"
        val loc = spLocation.selectedItem?.toString() ?: "All"

        val filtered = allTasks.filter { t ->
            val title = (t.title ?: "").lowercase()

            // ✅ кај тебе се снима "desc", не "description"
            val desc = (t.desc ?: "").lowercase()

            val matchesSearch = query.isBlank() || title.contains(query) || desc.contains(query)
            val matchesCat = (cat == "All") || (t.category == cat)
            val matchesLoc = (loc == "All") || (t.location == loc)

            matchesSearch && matchesCat && matchesLoc
        }

        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(filtered)
    }

    private fun applyToTask(task: Task) {
        val providerId = auth.currentUser?.uid
        if (providerId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "❌ Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val taskId = task.taskId
        if (taskId.isBlank()) {
            Toast.makeText(requireContext(), "❌ Missing taskId", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ важно: createdAt како Long за да ти се мапира лесно во Application model
        val application = hashMapOf<String, Any>(
            "taskId" to taskId,
            "providerId" to providerId,
            "providerName" to (auth.currentUser?.email ?: "Provider"),
            "message" to "I'm interested in this task.",
            "price" to (task.budget),
            "status" to "pending",
            "createdAt" to System.currentTimeMillis()
        )

        // ✅ multi-path write (најсигурно)
        val updates = hashMapOf<String, Any>(
            "taskApplications/$taskId/$providerId" to application,
            "providerApplications/$providerId/$taskId" to application
        )

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ Applied!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "❌ Apply failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
