package com.naeggeodo.presentation.view.chat

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.naeggeodo.domain.model.ChatHistory
import com.naeggeodo.domain.model.User
import com.naeggeodo.domain.utils.ChatDetailType
import com.naeggeodo.presentation.R
import com.naeggeodo.presentation.base.BaseFragment
import com.naeggeodo.presentation.data.Message
import com.naeggeodo.presentation.databinding.*
import com.naeggeodo.presentation.di.App
import com.naeggeodo.presentation.utils.ScreenState
import com.naeggeodo.presentation.utils.Util
import com.naeggeodo.presentation.utils.Util.decodeString
import com.naeggeodo.presentation.utils.Util.encodeImage
import com.naeggeodo.presentation.utils.Util.getMessageTimeString
import com.naeggeodo.presentation.utils.Util.loadImageAndSetView
import com.naeggeodo.presentation.utils.Util.showShortToast
import com.naeggeodo.presentation.utils.dpToPx
import com.naeggeodo.presentation.viewmodel.ChatViewModel
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


class ChatFragment : BaseFragment<FragmentChatBinding>(R.layout.fragment_chat),
    ChatActivity.OnBackPressedListener {
    private val chatViewModel: ChatViewModel by activityViewModels()

    private val galleryAdapter by lazy { GalleryAdapter(requireContext(), arrayListOf()) }

    private var totalImageSize = -1
    private var encodedImageString = ""
    private var quickChatDialog: QuickChatBottomDialogFragment? = null
    private var isGalleryVisible = false

    override fun init() {

    }

    override fun onStart() {
        super.onStart()

        chatViewModel.getChatInfo()
        if (!chatViewModel.stompClient.isConnected) {
            chatViewModel.runStomp()
        } else {
            chatViewModel.getChatHistory()
            chatViewModel.getQuickChats(App.prefs.userId!!)
        }
    }


    override fun initView() {
        super.initView()

        binding.galleryRecyclerview.adapter = galleryAdapter
        val lm = LinearLayoutManager(requireContext())
        lm.orientation = LinearLayoutManager.HORIZONTAL
        binding.galleryRecyclerview.layoutManager = lm


        // make recycler view not flickering
        binding.galleryRecyclerview.itemAnimator?.changeDuration = 0L

        // initialize bottom sheet dialog
        val phraseClickListener: (String) -> Unit = { str ->
            Timber.e("hihi $str")
            sendMessage(str, ChatDetailType.TEXT)
        }
        val updateListener: (List<String?>) -> Unit = { list ->
            Timber.e("hihi $list")
            val body = HashMap<String, List<String?>>()
            body["quickChat"] = list
            chatViewModel.updateQuickChats(App.prefs.userId!!, body)
        }
        quickChatDialog = QuickChatBottomDialogFragment(
            phraseClickListener = phraseClickListener,
            updateListener = updateListener
        )
    }

    override fun initListener() {
        binding.hambergerButton.setOnClickListener {
            val drawerLayout = binding.drawerLayout
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        }

        binding.checkDepositButton.setOnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToDepositFragment()
            findNavController().navigate(action)
        }

        binding.drawer.exitChatButton.setOnClickListener {
            val exitDialog = AlertDialog.Builder(requireContext())
            exitDialog.setTitle("나가기")
            exitDialog.setMessage("정말 나가시겠습니까?")
            exitDialog.setPositiveButton("나가기") { _, _ ->
                chatViewModel.sendMsg("", ChatDetailType.EXIT)
                requireActivity().finish()
            }
            exitDialog.setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            exitDialog.show()
        }

        binding.sendMessageButton.setOnClickListener {
            if (isGalleryVisible) {
                //send image
                val uriString = galleryAdapter.getSelectedPicture()
                if (uriString != null) {

                    val encodedString =
                        encodeImage(uriString).replace("\n", "")    // encoded with Base64
                    val chopSize = 10240
                    val loopCount = encodedString.length / chopSize + 1
                    sendMessage(
                        encodedString.length.toString(),
                        ChatDetailType.IMAGE
                    )
                    Handler(Looper.getMainLooper()).postDelayed({
                        for (i in 0 until loopCount) {
                            if (i != loopCount - 1) {
                                sendMessage(
                                    encodedString.slice(i * chopSize until (i + 1) * chopSize),
                                    ChatDetailType.IMAGE
                                )
                            } else {
                                sendMessage(
                                    encodedString.slice(i * chopSize until encodedString.length),
                                    ChatDetailType.IMAGE
                                )
                            }
                        }
                    }, 100)
                    galleryAdapter.clearSelected()
                    binding.galleryRecyclerview.visibility = View.GONE

                    return@setOnClickListener
                }
            }
            // send text
            sendMessage(binding.messageEdittext.text.toString(), ChatDetailType.TEXT)
        }

        binding.quickChatDrawerButton.setOnClickListener {
            quickChatDialog!!.show(parentFragmentManager, "QuickChatDialog")
            quickChatDialog!!.dialog?.window?.attributes?.gravity = Gravity.BOTTOM
        }

        binding.showGalleryButton.setOnClickListener {
            if (isGalleryVisible) {
                binding.galleryRecyclerview.visibility = View.GONE
                isGalleryVisible = false
            } else {
                val list = chatViewModel.getAllImagePaths(requireActivity())
                Timber.e("list $list")
                if (list.isEmpty()) {
                    showShortToast(requireContext(), "사진이 없습니다")
                    return@setOnClickListener
                }

                binding.galleryRecyclerview.post {
                    galleryAdapter.setData(list)
                    binding.galleryRecyclerview.visibility = View.VISIBLE
                }
                isGalleryVisible = true
            }
        }
        galleryAdapter.setItemClickEvent { pos ->
            // 없어도 됨
            val uri = galleryAdapter.getSelectedPicture()
            Timber.e("uri $uri")
        }
    }


    override fun observeViewModels() {
        chatViewModel.viewEvent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { event ->
                when (event) {
                    ChatViewModel.ERROR_OCCURRED -> {
                        showShortToast(requireContext(), "Error Occurred")
                        requireActivity().finish()
                    }
                    ChatViewModel.EVENT_STOMP_CONNECTED -> {
                        chatViewModel.getChatHistory()
                        chatViewModel.getQuickChats(App.prefs.userId!!)
                    }
                }
            }
        }
        chatViewModel.mutableScreenState.observe(viewLifecycleOwner) { state ->
            val layout = binding.loadingView.root
            val view = binding.loadingView.progressImage
            when (state!!) {
                ScreenState.LOADING -> Util.loadingAnimation(requireContext(), layout, view, true)
                ScreenState.RENDER -> Util.loadingAnimation(requireContext(), layout, view, false)
                ScreenState.ERROR -> Util.loadingAnimation(requireContext(), layout, view, false)
            }
        }

        chatViewModel.chatInfo.observe(viewLifecycleOwner) { chat ->
//            Timber.e("chat received")
            Timber.e("chat received ${chat}")


            // 헤더 뷰 설정
            binding.chatTitleText.text = chat.title
            loadImageAndSetView(requireContext(), chat.imgPath, binding.chatImage)
            binding.numOfPeople.text = "인원 ${chat.currentCount}명 / ${chat.maxCount}명"

            val masterId = chatViewModel.chatInfo.value!!.userId
            if (masterId == App.prefs.userId) {
                binding.checkDepositButton.visibility = View.VISIBLE
            }
        }

        chatViewModel.users.observe(viewLifecycleOwner) { users ->
            initDrawerView(users)
        }

        chatViewModel.history.observe(viewLifecycleOwner) { historyList ->
            Timber.e("history received size: ${historyList.size}")
            historyList.forEach { h ->
                Timber.e("history received ${h}")
                if (h.userId == App.prefs.userId) {
                    when (h.type) {
                        ChatDetailType.TEXT.name -> {
                            addMyMsgView(h.contents, LocalDateTime.parse(h.regDate))
                        }
                        ChatDetailType.IMAGE.name -> {
                            imageReceiver(h)
                        }
                        ChatDetailType.WELCOME.name -> {
                            addNoticeView(h.contents)
                        }
                        ChatDetailType.EXIT.name -> {
                            addNoticeView(h.contents)
                        }
//                        ChatDetailType.BAN -> {
//
//                        }
//                        ChatDetailType.BAN -> {
//
//                        }
//                        ChatDetailType.TEXT.name -> {
//
//                        }
                    }
                } else {
                    when (h.type) {
                        ChatDetailType.TEXT.name -> {
                            addOthersMsgView(h.contents, h.userId, h.nickname!!, LocalDateTime.parse(h.regDate))
                        }
                        ChatDetailType.IMAGE.name -> {
                            imageReceiver(h)
                        }
                        ChatDetailType.WELCOME.name, ChatDetailType.EXIT.name -> {
                            addNoticeView(h.contents)
                        }
                    }
                }
            }
            chatViewModel.runStomp()
        }

        chatViewModel.message.observe(viewLifecycleOwner) { msgInfo ->
            // 다른 유저의 이미지 추가해야 함
            when (msgInfo.type) {
                ChatDetailType.CNT.name -> {
                    // 총인원, 현재인원, 유저리스트
                    Timber.e("type cnt message received $msgInfo")
                    val currentCount = JSONObject(msgInfo.contents).get("currentCount")
                    sendMessage("", ChatDetailType.WELCOME)
                    binding.numOfPeople.text =
                        "인원 ${currentCount}명 / ${chatViewModel.chatInfo.value?.maxCount}명"
                }
                ChatDetailType.WELCOME.name -> {
                    addNoticeView(msgInfo.contents)
                }
                ChatDetailType.EXIT.name -> {
                    addNoticeView(msgInfo.contents)
                    if (msgInfo.contents.contains(App.prefs.nickname!!)) {
                        binding.checkDepositButton.visibility = View.VISIBLE
                    }
                }
                ChatDetailType.TEXT.name -> {
                    // 내가 보낸 메세지는 추가하지 않음
                    if (msgInfo.sender != App.prefs.userId) {
                        addOthersMsgView(msgInfo.contents, msgInfo.sender, msgInfo.nickname)
                    } else {
                        addMyMsgView(msgInfo.contents)
                    }
                }
                ChatDetailType.IMAGE.name -> {
                    imageReceiver(msgInfo)
                }
                else -> {

                }
            }
        }
    }

    private fun imageReceiver(msgInfo: Message) {
        try {
            val isStartOfLoading = msgInfo.contents.toIntOrNull()
            if (isStartOfLoading is Int) {
                initImageLoadVariables()
                totalImageSize = isStartOfLoading
            } else {
                if (totalImageSize < 0) {
                    initImageLoadVariables()
                    return
                }
                Timber.e("loading image ${encodedImageString.length}/$totalImageSize ")
                encodedImageString += msgInfo.contents
                if (encodedImageString.length >= totalImageSize) {
                    Timber.e("load image finish ${encodedImageString.length}/$totalImageSize ")

                    if (msgInfo.sender != App.prefs.userId) {
                        addOthersImageView(encodedImageString, msgInfo.sender, msgInfo.nickname)
                    } else {
                        addMyImageView(encodedImageString)
                    }
                    initImageLoadVariables()
                }
            }
        } catch (e: Exception) {
            showShortToast(requireContext(), "이미지 로드 에러")
            Timber.e("이미지 로드 에러 / $e")
            initImageLoadVariables()
        }
    }

    private fun imageReceiver(chatHistory: ChatHistory) {
        try {
            // 이미지를 전송할 때 처음엔 파일 사이즈를 보내기 때문에 인트형으로 케스팅된다면 이미지 전송의 처음임.
            val isStartOfLoading = chatHistory.contents.toIntOrNull()
            if (isStartOfLoading is Int) {
                initImageLoadVariables()
                totalImageSize = isStartOfLoading
            } else {
                if (totalImageSize < 0) {
                    initImageLoadVariables()
                    return
                }
                Timber.e("loading  image ${encodedImageString.length}/$totalImageSize ")
                encodedImageString += chatHistory.contents
                if (encodedImageString.length >= totalImageSize) {
                    Timber.e("load image finish ${encodedImageString.length}/$totalImageSize ")

                    if (chatHistory.userId != App.prefs.userId) {
                        addOthersImageView(encodedImageString, chatHistory.userId, chatHistory.nickname!!)
                    } else {
                        addMyImageView(encodedImageString)
                    }
                    initImageLoadVariables()
                }
            }
        } catch (e: Exception) {
            showShortToast(requireContext(), "이미지 로드 에러")
            Timber.e("이미지 로드 에러 / $e")
            initImageLoadVariables()
        }
    }

    private fun initImageLoadVariables() {
        totalImageSize = -1
        encodedImageString = ""
    }

    private fun initDrawerView(users: List<User>) {
        val inflater = LayoutInflater.from(requireContext())
        binding.drawer.participantContainer.removeAllViews()
        users.forEach {
            val userView = inflater.inflate(R.layout.item_participant, null)
            val profileView = userView.findViewById<ImageView>(R.id.profile_image_view)
            val nicknameView = userView.findViewById<TextView>(R.id.nickname_text_view)
            val masterView = userView.findViewById<CardView>(R.id.me_text_view)
            val masterId = chatViewModel.chatInfo.value?.userId

            nicknameView.text = it.nickname

            if (App.prefs.userId != it.userId) {
                masterView.visibility = View.GONE
            }
            if (masterId == it.userId) {
                profileView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_king
                    )
                )
            } else {
                profileView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_user
                    )
                )
            }
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, 40.dpToPx(requireContext()))
            lp.topMargin = 4.dpToPx(requireContext())
            userView.layoutParams = lp
            binding.drawer.participantContainer.addView(userView)
        }
    }

    private fun addMyMsgView(str: String, time: LocalDateTime? = null) {
        val inflater = LayoutInflater.from(requireContext())
        val layoutBinding = ItemMyMessageBoxBinding.inflate(inflater)

        layoutBinding.myMsgView.text = str
        layoutBinding.myTimeView.text = getMessageTimeString(time ?: LocalDateTime.now(ZoneId.of("Asia/Seoul")))
        binding.msgContainer.addView(layoutBinding.root)
        binding.msgScrollview.apply { post { binding.msgScrollview.fullScroll(View.FOCUS_DOWN) } }
    }

    private fun addMyImageView(encodedString: String, time: LocalDateTime? = null) {
        val byteArray = decodeString(encodedString)
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        val inflater = LayoutInflater.from(requireContext())
        val layoutBinding = ItemMyImageBoxBinding.inflate(inflater)
        layoutBinding.apply {
            // scale image size
            val screenSize = Util.getScreenSize(requireContext())
            val maxWidth = ((screenSize.x - 58.dpToPx(requireContext())) * 0.5).toInt()
            val imageWith = if (bitmap.width > maxWidth) maxWidth else bitmap.width
            val imgLp = myImageView.layoutParams
            imgLp.width = imageWith
            myImageView.layoutParams = imgLp
            Timber.e("${((screenSize.x - 58.dpToPx(requireContext())) * 0.5).toInt()} / ${bitmap.width}")

            myTimeView.text = getMessageTimeString(time ?: LocalDateTime.now(ZoneId.of("Asia/Seoul")))
            Glide.with(requireContext())
                .load(bitmap)
                .into(myImageView)
        }

        binding.msgContainer.addView(layoutBinding.root)
        binding.msgScrollview.apply {
            postDelayed(
                { binding.msgScrollview.fullScroll(View.FOCUS_DOWN) },
                100
            )
        }
    }

    private fun addOthersImageView(encodedString: String, id: String, nickname: String, time: LocalDateTime? = null) {

        val byteArray = decodeString(encodedString)
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)

        // view, image
        val inflater = LayoutInflater.from(requireContext())
        val layoutBinding = ItemOthersImageBoxBinding.inflate(inflater)
        layoutBinding.apply{
            val masterId = chatViewModel.chatInfo.value?.userId
            profileImage.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (id == masterId) R.drawable.ic_king else R.drawable.ic_user
                )
            )

            // scale image size
            val screenSize = Util.getScreenSize(requireContext())
            val maxWidth = ((screenSize.x - 58.dpToPx(requireContext())) * 0.5).toInt()
            val imageWith = if (bitmap.width > maxWidth) maxWidth else bitmap.width
            val imgLp = othersImageView.layoutParams
            imgLp.width = imageWith
            othersImageView.layoutParams = imgLp

            othersTimeView.text = getMessageTimeString(time ?: LocalDateTime.now(ZoneId.of("Asia/Seoul")))
            Glide.with(requireContext())
                .load(bitmap)
                .into(othersImageView)
            othersNameView.text = nickname
        }


        binding.msgContainer.addView(layoutBinding.root)
        binding.msgScrollview.apply {
            postDelayed(
                { binding.msgScrollview.fullScroll(View.FOCUS_DOWN) },
                200
            )
        }
    }


    private fun addOthersMsgView(str: String, id: String, nickname: String, time: LocalDateTime? = null) {
        // view, image
        val inflater = LayoutInflater.from(requireContext())
        val layoutBinding = ItemOthersMessageBoxBinding.inflate(inflater)
        layoutBinding.apply{
            val masterId = chatViewModel.chatInfo.value?.userId
            profileImage.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (id == masterId) R.drawable.ic_king else R.drawable.ic_user
                )
            )
            othersTimeView.text = getMessageTimeString(time ?: LocalDateTime.now(ZoneId.of("Asia/Seoul")))
            othersMsgView.text = str
            othersNameView.text = nickname
        }
        binding.msgContainer.addView(layoutBinding.root)
        binding.msgScrollview.apply { post { binding.msgScrollview.fullScroll(View.FOCUS_DOWN) } }
    }


    private fun addNoticeView(str: String) {
        // view, image
        val inflater = LayoutInflater.from(requireContext())
        val layoutBinding = ItemChatNoticeBinding.inflate(inflater)

        layoutBinding.noticeTextview.text = str

        binding.msgContainer.addView(layoutBinding.root)
        binding.msgScrollview.apply { post { binding.msgScrollview.fullScroll(View.FOCUS_DOWN) } }
    }

    private fun sendMessage(content: String, type: ChatDetailType) {
        Timber.e("content: $content")
        if (content.isEmpty()) return
        when (type) {
            ChatDetailType.TEXT -> {
                chatViewModel.sendMsg(content, type)
                binding.messageEdittext.text.clear()
            }
            ChatDetailType.IMAGE -> {
                chatViewModel.sendMsg(content, type)
            }
            else -> {}
        }
    }

    override fun onStop() {
        super.onStop()
        chatViewModel.stopStomp()
    }

    override fun onDestroy() {
//        chatViewModel.stopStomp()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val drawerLayout = binding.drawerLayout
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            requireActivity().finish()
        }
    }
}