package com.example.unifiltered.ui.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.FragmentNewsBinding
import com.example.unifiltered.ui.CreateSocietyActivity
import com.example.unifiltered.viewmodel.NewsViewModel
import kotlinx.coroutines.launch


class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsViewModel by viewModels()
    private lateinit var societyAdapter: SocietyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        societyAdapter = SocietyAdapter { clickedSociety ->
            val intent = android.content.Intent(requireContext(), SocietyDetailActivity::class.java).apply {
                putExtra("SOCIETY_ID", clickedSociety.societyId)
                putExtra("SOCIETY_NAME", clickedSociety.name)
                putExtra("SOCIETY_BIO", clickedSociety.description)
                putExtra("SOCIETY_IS_OFFICIAL", clickedSociety.isOfficial)
                putExtra("SOCIETY_CREATOR_ID", clickedSociety.creatorId)
            }
            startActivity(intent)
        }
        binding.recyclerViewSocieties.apply {
            adapter = societyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Observe Societies from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.societies.collect { societyList ->
                societyAdapter.submitList(societyList)
            }
        }

        // FAB Click Listener
        binding.fabCreateSociety.setOnClickListener {
            val intent = android.content.Intent(requireContext(), CreateSocietyActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}