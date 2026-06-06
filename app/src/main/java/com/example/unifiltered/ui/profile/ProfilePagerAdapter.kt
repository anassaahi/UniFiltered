package com.example.unifiltered.ui.profile

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // We have 3 tabs: Posts, Comments, Saved
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MyPostsFragment()    // Tab 1: Will hold the RecyclerView of your posts
            1 -> MyCommentsFragment() // Tab 2: Future feature for comments
            2 -> SavedPostsFragment() // Tab 3: Future feature for bookmarked posts
            else -> MyPostsFragment()
        }
    }
}