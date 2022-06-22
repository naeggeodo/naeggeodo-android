package com.naeggeodo.presentation.view.home

import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naeggeodo.domain.utils.ApartmentFlag
import com.naeggeodo.presentation.R
import com.naeggeodo.presentation.base.BaseFragment
import com.naeggeodo.presentation.databinding.FragmentHomeBinding
import com.naeggeodo.presentation.utils.ScreenState
import com.naeggeodo.presentation.utils.Util
import com.naeggeodo.presentation.utils.Util.showShortSnackbar
import com.naeggeodo.presentation.viewmodel.HomeViewModel
import com.naeggeodo.presentation.viewmodel.LocationViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(R.layout.fragment_home) {
    private val categoryAdapter by lazy { CategoryAdapter(requireContext(), arrayListOf()) }
    private val chatListAdapter by lazy { ChatListAdapter(requireContext(), arrayListOf()) }
    private val homeViewModel: HomeViewModel by viewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun init() {
    }

    override fun onStart() {
        super.onStart()
        requestCategory()
    }


    override fun initView() {
        // 채팅 바 텍스트
        binding.searchBarText.text = getText(R.string.not_apartment)

        // 카테고리 텝
        binding.categoryRecyclerView.adapter = categoryAdapter
        val lm = LinearLayoutManager(requireContext())
        lm.orientation = LinearLayoutManager.HORIZONTAL
        binding.categoryRecyclerView.layoutManager = lm

        // 채팅방 리스트
        binding.chatListRecyclerView.adapter = chatListAdapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun initListener() {
        binding.searchBarText.setOnClickListener {
            // 인터넷 연결 확인
            if (Util.isNetworkConnected(requireContext())) {
                // 주소검색 웹 뷰를 띄울 DialogFragment 로 이동
                AddressSearchDialogFragment().show(parentFragmentManager, "addressDialog")
            } else {
                showShortSnackbar(binding.root, "인터넷 연결을 확인해주세요.")
            }
        }

        categoryAdapter.setItemClickEvent { pos ->
            locationViewModel.addressInfo.value?.let { value ->
                val address = value.first
                val buildingCode = value.second
                val apartment = value.third
                if (apartment == ApartmentFlag.N.name) {
                    showShortSnackbar(binding.root, getString(R.string.not_apartment))
                    return@setItemClickEvent
                }

                requestChatList(
                    category = categoryAdapter.getSelectedCategory(),
                    buildingCode = buildingCode
                )
                return@setItemClickEvent
            }

            showShortSnackbar(binding.root, getString(R.string.not_apartment))
        }

        binding.chatListRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

                val itemCount = chatListAdapter.itemCount
                Timber.d("$itemCount")

                // 스크롤이 끝에 도달했는지 확인
                if (
                    !recyclerView.canScrollVertically(1) &&
                    lastVisibleItemPosition == itemCount - 1
                ) {
//                    Timber.d("touched chat list end")
                }
                // 스크롤이 처음에 도달했는지 확인
                val firstVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (homeViewModel.mutableScreenState.value != ScreenState.LOADING &&
                    !recyclerView.canScrollVertically(-1) && firstVisibleItemPosition == 0
                ) {
//                    Timber.d("touched chat list top")
                    refreshChatList()
                }
            }
        })
    }

    override fun observeViewModels() {
        homeViewModel.categories.observe(viewLifecycleOwner) { value ->
            value.categories.map {
                Timber.e("${it.idx} ${it.category} ")
            }
            categoryAdapter.setData(ArrayList(value.categories))
        }

        locationViewModel.addressInfo.observe(viewLifecycleOwner) { value ->
            Timber.e("address $value")
            val address = value.first
            val buildingCode = value.second
            val apartment = value.third

            val textview = binding.searchBarText
            if (apartment == ApartmentFlag.Y.name) {
                textview.text = address
                requestChatList(
                    category = categoryAdapter.getSelectedCategory(),
                    buildingCode = buildingCode
                )
            } else {
                textview.text = getText(R.string.not_apartment)
            }
        }

        homeViewModel.chatList.observe(viewLifecycleOwner) { value ->
            Timber.e("$value")
            chatListAdapter.setData(ArrayList(value))
        }

    }

    private fun refreshChatList() {
        Timber.e("Refresh chat list")
    }

    private fun requestChatList(category: String? = null, buildingCode: String) {
        homeViewModel.getChatList(category, buildingCode)
    }

    private fun requestCategory() {
        homeViewModel.getCategories()
    }
}