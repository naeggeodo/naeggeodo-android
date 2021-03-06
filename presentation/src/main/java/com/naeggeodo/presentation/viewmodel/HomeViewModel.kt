package com.naeggeodo.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.naeggeodo.domain.model.Categories
import com.naeggeodo.domain.model.Chat
import com.naeggeodo.domain.model.MyNickName
import com.naeggeodo.domain.usecase.CategoryUseCase
import com.naeggeodo.domain.usecase.GetMyNickNameUseCase
import com.naeggeodo.domain.usecase.SearchChatListByCategoryUseCase
import com.naeggeodo.presentation.base.BaseViewModel
import com.naeggeodo.presentation.utils.ScreenState
import com.naeggeodo.presentation.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCategoriesUseCase: CategoryUseCase,
    private val searchChatListByCategoryUseCase: SearchChatListByCategoryUseCase,
    private val getMyNickNameUseCase: GetMyNickNameUseCase
) : BaseViewModel() {

    companion object {
        const val EVENT_CATEGORIES_CHANGED = 222
        const val EVENT_CHAT_LIST_CHANGED = 223
    }

    private val _categories: SingleLiveEvent<Categories> = SingleLiveEvent()
    val categories: LiveData<Categories> get() = _categories

    private val _chatList: SingleLiveEvent<List<Chat>> = SingleLiveEvent()
    val chatList: LiveData<List<Chat>> get() = _chatList

    private val _myNickName: SingleLiveEvent<MyNickName> = SingleLiveEvent()
    val myNickName: LiveData<MyNickName> get() = _myNickName


    fun getCategories() = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            getCategoriesUseCase.execute(this@HomeViewModel)
        }
        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _categories.postValue(response!!)
            mutableScreenState.postValue(ScreenState.RENDER)
            viewEvent(EVENT_CATEGORIES_CHANGED)
        }
    }

    fun getChatList(category: String?, buildingCode: String) = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            searchChatListByCategoryUseCase.execute(this@HomeViewModel, category, buildingCode)
        }

        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _chatList.postValue(response!!.chatList)
            mutableScreenState.postValue(ScreenState.RENDER)
            viewEvent(EVENT_CHAT_LIST_CHANGED)
        }
    }

    fun getMyInfo(userId: String) = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            getMyNickNameUseCase.execute(this@HomeViewModel, userId)
        }

        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _myNickName.postValue(response!!)

            viewEvent(EVENT_CHAT_LIST_CHANGED)
        }
    }

    fun setScreenState(state: ScreenState) {
        mutableScreenState.postValue(state)
    }
}