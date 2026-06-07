package com.example.unifiltered.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unifiltered.databinding.FragmentFeedBinding
import com.example.unifiltered.viewmodel.AiSummaryState
import com.example.unifiltered.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

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
        observeAiSummary() // NEW!

        binding.fabCreatePost.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), CreatePostActivity::class.java))
        }

        binding.btnMenu.setOnClickListener { view ->
            val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
            popup.menu.add("FAQ")
            popup.menu.add("Help")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "FAQ" -> {
                        startActivity(android.content.Intent(requireContext(), com.example.unifiltered.ui.FAQActivity::class.java))
                        true
                    }
                    "Help" -> {
                        startActivity(android.content.Intent(requireContext(), com.example.unifiltered.ui.HelpActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Filters the list instantly as they type
        binding.etSearch.addTextChangedListener { editable ->
            viewModel.updateSearchQuery(editable.toString())
        }

        // NEW: Triggers the AI ONLY when they hit "Search" on the keyboard
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString()
                viewModel.generateAiSummaryForSearch(query)

                // Optional: Hide the keyboard after searching
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onPostClick = { clickedPost ->
                val intent = android.content.Intent(requireContext(), PostDetailActivity::class.java).apply {
                    putExtra("POST_ID", clickedPost.postId)
                    putExtra("AUTHOR_NAME", clickedPost.authorName)
                    putExtra("CONTENT", clickedPost.content)
                    putExtra("LIKES_COUNT", clickedPost.likedBy.size)
                    putExtra("IMAGE_URL", clickedPost.imageUrl)
                }
                startActivity(intent)
            },
            onLikeClick = { clickedPost, isCurrentlyLiked ->
                viewModel.toggleLike(clickedPost.postId, isCurrentlyLiked)
            }
        )

        binding.recyclerViewFeed.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observePosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collect { postList ->
                postAdapter.submitList(postList)
            }
        }
    }

    // NEW: Observe the AI state and animate the UI
    private fun observeAiSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiSummaryState.collect { state ->
                when (state) {
                    is AiSummaryState.Idle -> {
                        binding.cardAiSummary.visibility = View.GONE
                    }
                    is AiSummaryState.Loading -> {
                        binding.cardAiSummary.visibility = View.VISIBLE
                        binding.pbAiLoading.visibility = View.VISIBLE
                        binding.tvAiResponse.text = "Thinking..."
                    }
                    is AiSummaryState.Success -> {
                        binding.cardAiSummary.visibility = View.VISIBLE
                        binding.pbAiLoading.visibility = View.GONE
                        binding.tvAiResponse.text = state.response
                    }
                    is AiSummaryState.Error -> {
                        binding.cardAiSummary.visibility = View.VISIBLE
                        binding.pbAiLoading.visibility = View.GONE
                        binding.tvAiResponse.text = state.message
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}