package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.FragmentFeedBinding
import com.example.unifiltered.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    // ViewBinding setup for Fragments is slightly different than Activities to prevent memory leaks
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    // Grab the ViewModel
    private val viewModel: FeedViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observePosts()

        // Opens the Create Post Screen
        binding.fabCreatePost.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), CreatePostActivity::class.java))
        }

        // NEW: Listen to the search bar and tell the ViewModel what is being typed
        binding.etSearch.addTextChangedListener { editable ->
            viewModel.updateSearchQuery(editable.toString())
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter()
        binding.recyclerViewFeed.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observePosts() {
        // Listen to the StateFlow from the ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { postList ->
                // Feed the data to our adapter. It handles the animations automatically!
                postAdapter.submitList(postList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding to prevent memory leaks
    }
}