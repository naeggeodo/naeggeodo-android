package com.naeggeodo.presentation.viewmodel

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.naeggeodo.domain.model.*
import com.naeggeodo.domain.usecase.*
import com.naeggeodo.domain.utils.ChatDetailType
import com.naeggeodo.presentation.base.BaseViewModel
import com.naeggeodo.presentation.data.Message
import com.naeggeodo.presentation.di.App
import com.naeggeodo.presentation.utils.ScreenState
import com.naeggeodo.presentation.utils.SingleLiveEvent
import com.uasang01.stomp.lib.Event
import com.uasang01.stomp.lib.StompClient
import com.uasang01.stomp.lib.StompMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getChatInfoUseCase: GetChatInfoUseCase,
    private val getPrevChatHistoryUseCase: GetPrevChatHistoryUseCase,
    private val getQuickChatUseCase: GetQuickChatUseCase,
    private val patchQuickChatUseCase: PatchQuickChatUseCase,
    private val changeChatRoomStateUseCase: ChangeChatRoomStateUseCase
) : BaseViewModel() {
    companion object {
        const val EVENT_STOMP_CONNECTED = 319
        const val FAILED_TO_SEND_MESSAGE = 321
        const val EVENT_CHAT_FINISHED = 322
        const val ERROR_SESSION_DUPLICATION = 330
        const val ERROR_BANNED_FROM_CHAT = 331
        const val ERROR_BAD_REQUEST = 332
        const val ERROR_UNAUTHORIZED = 333
        const val ERROR_INVALID_STATE = 334
        const val ERROR_INVALID_ACCESS = 335
    }

    var chatId: Int? = null

    private val _chatInfo: SingleLiveEvent<Chat> = SingleLiveEvent()
    val chatInfo: LiveData<Chat> get() = _chatInfo
    private val _users: SingleLiveEvent<List<User>> = SingleLiveEvent()
    val users: LiveData<List<User>> get() = _users
    private val _history: SingleLiveEvent<List<ChatHistory>> = SingleLiveEvent()
    val history: LiveData<List<ChatHistory>> get() = _history
    private val _message: SingleLiveEvent<Message> = SingleLiveEvent()
    val message: LiveData<Message> get() = _message
    private val _quickChat: SingleLiveEvent<List<QuickChat>> = SingleLiveEvent()
    val quickChat: LiveData<List<QuickChat>> get() = _quickChat

    fun getChatInfo() = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            getChatInfoUseCase.execute(this@ChatViewModel, chatId!!)
        }
        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _chatInfo.postValue(response!!)
            mutableScreenState.postValue(ScreenState.RENDER)
        }
    }

    fun getChatHistory() = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            getPrevChatHistoryUseCase.execute(this@ChatViewModel, chatId!!, App.prefs.userId!!)
        }
        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _history.postValue(response.messages)
            mutableScreenState.postValue(ScreenState.RENDER)
        }
    }

    fun getQuickChats(userId: String) = viewModelScope.launch {
        mutableScreenState.postValue(ScreenState.LOADING)
        val response = withContext(Dispatchers.IO) {
            getQuickChatUseCase.execute(this@ChatViewModel, userId)
        }
        if (response == null) {
            mutableScreenState.postValue(ScreenState.ERROR)
        } else {
            _quickChat.postValue(response.quickChats)
            mutableScreenState.postValue(ScreenState.RENDER)
        }
    }

    fun updateQuickChats(userId: String, body: HashMap<String, List<String?>>) =
        viewModelScope.launch {
            mutableScreenState.postValue(ScreenState.LOADING)
            val response = withContext(Dispatchers.IO) {
                patchQuickChatUseCase.execute(this@ChatViewModel, userId, body)
            }
            if (response == null) {
                mutableScreenState.postValue(ScreenState.ERROR)
            } else {
                _quickChat.postValue(response.quickChats)
                mutableScreenState.postValue(ScreenState.RENDER)
            }
        }

    fun finishChat(chatId: Int, state: String) =
        viewModelScope.launch {
            mutableScreenState.postValue(ScreenState.LOADING)
            val response = withContext(Dispatchers.IO) {
                changeChatRoomStateUseCase.execute(this@ChatViewModel, chatId, state)
            }
            if (response == null) {
                mutableScreenState.postValue(ScreenState.ERROR)
            } else {
//                _quickChat.postValue(response.quickChats)
                Timber.e("test response / $response")
                mutableScreenState.postValue(ScreenState.RENDER)
                viewEvent(EVENT_CHAT_FINISHED)
            }
        }


    fun banUser(targetId: String) {
        sendMsg(targetId, ChatDetailType.BAN)
    }

    fun exitChat() {
        sendMsg("", ChatDetailType.EXIT)
    }


    //    ### ????????? END Point
    //    ??? https//api.naeggeodo.com/api/chat

    //    Subscribe url
    //    1.  ???/topic/???+?????????id   : ?????? ?????????
    //    2. ???/user/queue/???+??????Id : ?????? ????????? ex)?????? , alert

    private var compositeDisposable: CompositeDisposable? = null


    // init
    val userId = App.prefs.userId

    // http -> ws
    // https -> wss
    // url ?????? '/websocket'??? ??? ??????????????????.
    private val url = "wss://api.naeggeodo.com/api/chat" // ????????? ???????????? ?????????????????? /socket?????? ????????? ??????


    val stompClient = StompClient(OkHttpClient())
    fun runStomp() {
        Timber.e("chatId : $chatId userId: $userId  ")

        resetSubscriptions()

        stompClient.url = "$url/websocket"
        // ????????? ?????? ??????
        val headers = HashMap<String, String>()
        headers["chatMain_id"] = "$chatId"
        headers["sender"] = "${userId}"
        headers["Authorization"] = "Bearer ${App.prefs.accessToken}"

        val connectionDisposable = stompClient.connect(headers).subscribe(
            {
                when (it.type) {
                    Event.Type.OPENED -> {
                        Timber.i("OPENED")
                        // ?????? ??? ?????? ????????? ??????
                        Handler(Looper.getMainLooper())
                            .postDelayed({
                                sendMsg("", ChatDetailType.WELCOME)
                            }, 200L)
                        // ?????? ?????? ??? history, quick chats ??????
                        getChatHistory()
                        getQuickChats(App.prefs.userId!!)
                    }
                    Event.Type.CLOSED -> {
                        Timber.i("CLOSED ${it.type} / ${it.exception}")
                    }
                    Event.Type.ERROR -> {
                        Timber.e("CONNECT ERROR / ${it.type} / ${it.exception}")
                    }
                    else -> {
                        Timber.e("NPE / ${it.type} / ${it.exception}")
                    }
                }
            }, {
                Timber.e("STOMP ERROR OCCURRED ON CONNECT / ${it.message}")
                connectErrorHandler(it.message!!)
            })
        compositeDisposable?.add(connectionDisposable)
        RxJavaPlugins.setErrorHandler { }

        // ???????????? ???????????? ??????
        val topicDisposable = stompClient.join("/topic/$chatId")
            .subscribe(
                { message ->
                    Timber.e("message arrived / $message")
                    try {
                        var jsonObject = JsonParser.parseString(message)
                        val stompMessage = Gson().fromJson(jsonObject, StompMessage::class.java)

                        jsonObject = if (stompMessage.payload.isNullOrEmpty()) {
                            JsonParser.parseString(message)
                        } else {
                            JsonParser.parseString(stompMessage.payload)
                        }

                        val msgInfo = Gson().fromJson(jsonObject, Message::class.java)
                        _message.postValue(msgInfo)

                        when (msgInfo.type) {
                            ChatDetailType.CNT.name -> {
                                // CNT ????????? ???????????? ????????? 1??? ????????????.
                                val currentCount = JSONObject(msgInfo.contents).get("currentCount")
                                val users = Gson().fromJson(
                                    "{\"users\":${JSONObject(msgInfo.contents).get("users")}}",
                                    Users::class.java
                                )
                                _users.postValue(users.users)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("test ${e.message} / ${e.stackTraceToString()}")
                    }
                }, {
                    Timber.e("STOMP TOPIC ERROR / ${it.message} / ${it.stackTraceToString()} ")
                })
        compositeDisposable?.add(topicDisposable)

        // ???????????????
        // ??????
//        data = {
//            'chatMain_id': '????????? id',
//            'sender' :'???????????????id',
//            'contents': '????????????',
//            'type' : "TEXT",  // ???????????????
//            'nickname' : '??????????????? ?????????'
//        }

        // ??????
//        data = {
//            'chatMain_id': '????????? id',
//            'sender' :'???????????????id',
//            'contents': '????????? ???????????????',
//            'target_id':'????????????????????????',
//            'type' : "TEXT",  // ???????????????
//            'nickname' : '??????????????? ?????????'
//        }


        //    SEND url
        //    ??? prefix : ???/app/chat???
        //    1. ????????? ?????? : ???/send???
        //    2. ??????????????? : ???/enter???
        //    3. ??????????????? : ???/exit???
        //    4. ?????? : ???/ban???


    }

    private fun connectErrorHandler(error: String) {
        when (error) {
            "BANNED_CHAT_USER" -> {
                viewEvent(ERROR_BANNED_FROM_CHAT)
            }
            "SESSION_DUPLICATION" -> {
                viewEvent(ERROR_SESSION_DUPLICATION)
            }
            "INVALID_STATE" -> {
                viewEvent(ERROR_INVALID_STATE)
            }
            "BAD_REQUEST" -> {
                viewEvent(ERROR_BAD_REQUEST)
            }
            "UNAUTHORIZED" -> {
                viewEvent(ERROR_UNAUTHORIZED)
            }
            else -> {
                viewEvent(ERROR_INVALID_ACCESS)
            }
        }
    }

    private fun resetSubscriptions() {
        compositeDisposable?.dispose()
        compositeDisposable = CompositeDisposable()
    }


    fun sendMsg(content: String, type: ChatDetailType) {
        if (!stompClient.isConnected()) {
            viewEvent(FAILED_TO_SEND_MESSAGE)
            return
        }

        val prefix = "/app/chat"
        val send = "/send"
        val image = "/image"
        val enter = "/enter"
        val ban = "/ban"
        val exit = "/exit"

        val data = JSONObject()
        var destination = prefix
        val nickname = App.prefs.nickname

        data.put("chatMain_id", chatId)
        data.put("sender", App.prefs.userId)
        data.put("type", type.name)
        data.put("nickname", nickname)
        when (type) {
            ChatDetailType.TEXT -> {
                data.put("contents", content)
                destination += send
            }
            ChatDetailType.IMAGE -> {
                data.put("contents", content)
                destination += send
            }
            ChatDetailType.WELCOME -> {
                data.put("contents", "?????? ?????????????????????")
                destination += enter
            }
            ChatDetailType.EXIT -> {
                data.put("contents", "?????? ?????????????????????")
                destination += exit
            }
            ChatDetailType.BAN -> {
                data.put("target_id", content)
                data.put("contents", content)
                destination += ban
            }
            else -> {
                Timber.e("can not send message with unknown type")
                return
            }
        }

        // ????????? ??????
        val msgSenderDisposable = stompClient
            .send(destination, data.toString())
            .subscribe({
                //onComplete
            }, { throwable ->
                Timber.e("ERROR OCCURRED ON SEND MESSAGE\nmessage: ${throwable.message} / cause: ${throwable.cause}")
            })
        compositeDisposable?.add(msgSenderDisposable)

    }

    fun stopStomp() {
        compositeDisposable?.dispose()
    }

    fun getAllImagePaths(activity: Activity): ArrayList<String> {
        val listOfAllImages = ArrayList<String>()

        var absolutePathOfImage: String?
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,

            )
        val cursor = activity.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )
        val columnIndexData: Int = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData)
            listOfAllImages.add(absolutePathOfImage)
        }
        cursor.close()
        return listOfAllImages
    }
}