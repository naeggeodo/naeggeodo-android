package com.naeggeodo.presentation.view.search

import android.app.Activity
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.naeggeodo.presentation.R
import com.naeggeodo.presentation.base.BaseFragment
import com.naeggeodo.presentation.databinding.FragmentSearchBinding
import com.naeggeodo.presentation.utils.Util
import com.naeggeodo.presentation.utils.Util.hideKeyboard
import com.naeggeodo.presentation.utils.Util.showShortToast
import com.naeggeodo.presentation.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchFragment : BaseFragment<FragmentSearchBinding>(R.layout.fragment_search) {
    private val tagAdapter by lazy { TagAdapter(requireContext(), arrayListOf()) }
    private val chatListAdapter by lazy { ChatListAdapter(requireContext(), arrayListOf()) }

    private val searchViewModel: SearchViewModel by activityViewModels()

    private val searchType = "search"

    override fun onStart() {
        super.onStart()
        searchViewModel.getTags()
    }

    override fun init() {

    }

    override fun initView() {
        // search bar
        binding.searchBarText.hint = "검색어를 입력해 주세요"

        // tags recyclerview
        binding.tagRecyclerView.adapter = tagAdapter
        val lm = FlexboxLayoutManager(requireContext())
        lm.flexDirection = FlexDirection.ROW
        lm.justifyContent = JustifyContent.FLEX_START
        binding.tagRecyclerView.layoutManager = lm


        // chats recyclerview
        binding.chatListRecyclerView.adapter = chatListAdapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecyclerView.itemAnimator = null
    }

    override fun initListener() {
        // 엔터를 누르면 키보드 내림
        binding.searchBarText.setOnKeyListener { view, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    Timber.e("${binding.searchBarText.text}")
                    val keyWord = binding.searchBarText.text.toString()

                    searchChatList(searchType, keyWord)
                    hideKeyboard(activity as Activity)
                    binding.searchBarText.clearFocus()
                    binding.searchBarText.clearComposingText()

                    return@setOnKeyListener true
                }
//                KeyEvent.KEYCODE_BACK -> {
//                    screenChanger(false)
//                    hideKeyboard(activity as Activity)
//
//                    return@setOnKeyListener true
//                }
                else -> return@setOnKeyListener false
            }
        }
        binding.searchBarText.onFocusChangeListener =
            View.OnFocusChangeListener { view, isFocused ->
                if (isFocused) {
                    binding.tagContainer.visibility = View.VISIBLE
//                    binding.chatListRecyclerView.visibility = View.VISIBLE
                } else {
//                    binding.chatListRecyclerView.visibility = View.VISIBLE
                }


            }

        tagAdapter.setItemClickEvent { pos ->
            searchChatList(searchType, tagAdapter.getData(pos))
            hideKeyboard(activity as Activity)
            binding.searchBarText.clearFocus()
            binding.searchBarText.clearComposingText()
        }

        chatListAdapter.setListener { pos ->
            val chat = chatListAdapter.getData(pos)

            Timber.e("chat info : ${chat.chatId} ${chat.title} ${chat.userId}")

            val action = SearchFragmentDirections.actionSearchToChatActivity(chat.chatId)
//            val action = HomeFragmentDirections.actionHomeToNavigationChat()
            findNavController().navigate(action)
        }
    }

    override fun observeViewModels() {
        searchViewModel.tags.observe(viewLifecycleOwner) {
            tagAdapter.setData(ArrayList(it))
        }
        searchViewModel.chatList.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                showShortToast(requireContext(), "검색 결과가 없습니다")
                binding.tagContainer.visibility = View.VISIBLE
            } else {
                binding.tagContainer.visibility = View.GONE
            }
            chatListAdapter.setData(ArrayList(it))
//            recyclerViewChanger(false)
        }

        searchViewModel.mutableErrorType.observe(this) {
            Util.sessionErrorHandle(requireContext(), it)
        }
    }

    private fun searchChatList(searchType: String, keyWord: String) =
        searchViewModel.searchChatList(searchType, keyWord)

    private fun recyclerViewChanger(showTags: Boolean) {
        if (showTags) {
            binding.chatListRecyclerView.visibility = View.INVISIBLE
            binding.tagContainer.visibility = View.VISIBLE
        } else {
            binding.tagContainer.visibility = View.INVISIBLE
            binding.chatListRecyclerView.visibility = View.VISIBLE
        }
    }

}