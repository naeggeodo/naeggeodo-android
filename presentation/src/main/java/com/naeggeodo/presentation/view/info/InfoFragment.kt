package com.naeggeodo.presentation.view.info

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.naeggeodo.domain.utils.ReportType
import com.naeggeodo.presentation.R
import com.naeggeodo.presentation.base.BaseFragment
import com.naeggeodo.presentation.databinding.FragmentInfoBinding
import com.naeggeodo.presentation.di.App
import com.naeggeodo.presentation.utils.Util
import com.naeggeodo.presentation.utils.Util.goToLoginScreen
import com.naeggeodo.presentation.utils.Util.hideKeyboard
import com.naeggeodo.presentation.utils.Util.showShortToast
import com.naeggeodo.presentation.view.CommonDialogFragment
import com.naeggeodo.presentation.viewmodel.InfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@AndroidEntryPoint
class InfoFragment : BaseFragment<FragmentInfoBinding>(R.layout.fragment_info) {
    private val infoViewModel by activityViewModels<InfoViewModel>()
    var prevNickName = ""
    override fun init() {
    }

    override fun initView() {
    }

    override fun onStart() {
        super.onStart()
        infoViewModel.getMyInfo(App.prefs.userId!!)
    }

    override fun initListener() {
        binding.nicknameEditText.onFocusChangeListener =
            View.OnFocusChangeListener { view, isFocused ->
                if (isFocused) prevNickName = binding.nicknameEditText.text.toString()
            }
        binding.nicknameEditText.setOnKeyListener { view, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    changeNickname()
                    return@setOnKeyListener true
                }
                else -> return@setOnKeyListener false
            }
        }
        binding.participatingContainer.setOnClickListener {
            val navOptions = navOptions {
                popUpTo(R.id.home) {
                    inclusive = false
                }
            }

            findNavController()
                .navigate(R.id.my_chat, null, navOptions)
        }
        binding.editNicknameButton.setOnClickListener {
            if (binding.nicknameEditText.hasFocus()) {
                changeNickname()
            } else {
                binding.nicknameEditText.requestFocus()
            }
            // request to change nickname

        }

        binding.noticeButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://fog-cowl-888.notion.site/63ad9843bb874e5797cff765419f47d7")
            )
            startActivity(browserIntent)
        }
        binding.suggestButton.setOnClickListener {
            val dialog = SuggestDialogFragment(
                normalButtonText = "??????",
                colorButtonText = "????????????",
                colorButtonListener = { contents ->
                    // suggest!
                    val type = ReportType.FEEDBACK.name
                    Timber.e("suggest, type $type / contents $contents")
                    reportUser(contents, type)
                }
            )
            dialog.show(childFragmentManager, "SuggestDialog")
        }
        binding.reportButton.setOnClickListener {
            val dialog = ReportDialogFragment(
                normalButtonText = "??????",
                colorButtonText = "????????????",
                colorButtonListener = { type, contents ->
                    // report!
                    Timber.e("report, type $type / contents $contents")
                    reportUser(contents, type)
                }
            )
            dialog.show(childFragmentManager, "ReportDialog")
        }
        binding.termsButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://fog-cowl-888.notion.site/b7b93231fbff405084d0a043025189e8")
            )
            startActivity(browserIntent)
        }
        binding.policyButton.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://fog-cowl-888.notion.site/b976365add8f40ecac4a54a5b67d156d")
            )
            startActivity(browserIntent)
        }
        binding.logoutButton.setOnClickListener {
            val dialog = CommonDialogFragment(
                contentText = "???????????? ???????????????????",
                colorButtonText = "??????",
                normalButtonText = "????????????",
                normalButtonListener = {
                    goToLoginScreen(requireContext())
                }
            )
            dialog.show(childFragmentManager, "Dialog")
        }
    }

    private fun changeNickname() {
        Timber.e("${binding.nicknameEditText.text}")
        hideKeyboard(requireActivity())
        binding.nicknameEditText.clearFocus()
        binding.nicknameEditText.clearComposingText()

        // ?????? ???????????? ????????? ?????? x
        Timber.e("!! ${prevNickName} ${binding.nicknameEditText.text}")
        if (prevNickName == binding.nicknameEditText.text.toString()) return
        infoViewModel.changeNickname(App.prefs.userId!!, binding.nicknameEditText.text.toString())
    }

    override fun observeViewModels() {
        infoViewModel.nickname.observe(viewLifecycleOwner) {
            binding.nicknameEditText.setText(it.nickname)
            showShortToast(requireContext(), "???????????? ?????????????????????")
        }
        infoViewModel.myInfo.observe(viewLifecycleOwner) {
            binding.participatingTextView.text = "${it.participatingChatCount}???"
            binding.recentOrderTextView.text = "${it.myOrdersCount}???"
            binding.nicknameEditText.setText(it.nickname)
        }
        infoViewModel.mutableErrorType.observe(this) {
            Util.sessionErrorHandle(requireContext(), it)
        }
    }

    private fun reportUser(contents: String, type: String) {
        val body = HashMap<String, String>()
        body["user_id"] = App.prefs.userId!!
        body["contents"] = contents
        body["type"] = type

        CoroutineScope(Dispatchers.IO).launch {
            val result = infoViewModel.report(body)
            withContext(Dispatchers.Main) {
                if (result) showShortToast(requireContext(), "?????????????????????")
                else showShortToast(requireContext(), "????????? ??????????????????")
            }
        }
    }
}