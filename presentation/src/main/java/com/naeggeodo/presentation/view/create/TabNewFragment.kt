package com.naeggeodo.presentation.view.create

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.view.KeyEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.naeggeodo.presentation.R
import com.naeggeodo.presentation.base.BaseFragment
import com.naeggeodo.presentation.databinding.FragmentTabNewBinding
import com.naeggeodo.presentation.utils.Util.hideKeyboard
import com.naeggeodo.presentation.viewmodel.CreateChatViewModel
import com.naeggeodo.presentation.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


private const val PEOPLE_MAX = 5
private const val PEOPLE_MIN = 2

@AndroidEntryPoint
class TabNewFragment : BaseFragment<FragmentTabNewBinding>(R.layout.fragment_tab_new) {
    val createChatViewModel: CreateChatViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var getPictureResult: ActivityResultLauncher<Intent>

    private var bitmap: Bitmap? = null

    override fun init() {
        binding.fragment = this
        binding.createChatViewModel = createChatViewModel
        // init people limit count
        createChatViewModel.setMaxPeopleNum(2)

        getPictureResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Timber.e("activity result ok\n${result.data?.dataString}")
                    result.data?.let {
                        val uri = result.data!!.data
                        Timber.e(uri.toString())
                        bitmap = ImageDecoder
                            .decodeBitmap(
                                ImageDecoder.createSource(
                                    requireContext().contentResolver,
                                    uri!!
                                )
                            )
                            .copy(Bitmap.Config.ARGB_8888, true)
                        Glide.with(requireContext())
                            .load(bitmap)
                            .error(R.drawable.ic_error)
                            .centerCrop()
                            .into(binding.chatImage)

                        // ????????? ?????????????????? ???????????? ?????? ?????? ?????? ?????? ????????????.
                        createChatViewModel.setChatImage(bitmap!!)
                    }

                } else {
                    Timber.e("activity result not ok")
                }
            }
    }

    override fun initView() {
        binding.fragment = this

        createChatViewModel.chatTitle.value?.let {
            binding.chatTitleEditText.setText(it)
        }

//        createChatViewModel.categoryKorean.value?.let{
//            binding.categoryTextView.text = it
//        }

        createChatViewModel.place.value?.let {
            if (it.isNotEmpty()) binding.placeEditText.setText(it)
            else null
        }

        createChatViewModel.link.value?.let {
            if (it.isNotEmpty()) binding.linkEditText.setText(it)
            else null
        }

        createChatViewModel.tag.value?.let {
            if (it.isNotEmpty()) binding.tagEditText.setText(it)
            else null
        }
        createChatViewModel.maxPeopleNum.value?.let {
            binding.peopleCountTextView.text = it.toString()
        }

        bitmap?.let {
            Glide.with(requireContext())
                .load(bitmap)
                .centerCrop()
                .into(binding.chatImage)
        }


        createChatViewModel.setCategory(null)
        binding.createChatViewModel = createChatViewModel
    }

    override fun initListener() {
        binding.chatTitleEditText.addTextChangedListener { text ->
            createChatViewModel.setChatTitle(text.toString())
        }
        binding.categoryBox.setOnClickListener {
            val categories = homeViewModel.categories.value?.categories
            categories?.let {
                CategoryDialogFragment(it)
                    .show(parentFragmentManager, "categoryDialog")
            }
        }
        binding.placeEditText.addTextChangedListener { text ->
            createChatViewModel.setPlace(text.toString())
        }

        binding.linkEditText.addTextChangedListener { text ->
            createChatViewModel.setLink(text.toString())
        }

        binding.tagEditText.addTextChangedListener { text ->
            createChatViewModel.setTag(text.toString())
        }
        // ????????? EditText. ????????? ????????? ????????? ??????
        binding.tagEditText.setOnKeyListener { view, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    hideKeyboard(activity as Activity)
                    return@setOnKeyListener true
                }
                else -> return@setOnKeyListener false
            }
        }
        binding.addButton.setOnClickListener {
            var num = binding.peopleCountTextView.text.toString().toInt()
            // PEOPLE_MAX ??? ??? ????????????
            if (num >= PEOPLE_MAX) return@setOnClickListener

            // PEOPLE_MIN ??? ??? ????????? ?????? ??? ??? ???????????? ?????? ????????????
            if (num <= PEOPLE_MIN) {
                binding.subtractButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_subtract_black
                    )
                )
            }
            num += 1
            binding.peopleCountTextView.text = num.toString()
            createChatViewModel.setMaxPeopleNum(num)
            // ????????? PEOPLE_MAX????????? ?????? ????????? ?????? ????????????
            if (num >= PEOPLE_MAX) {
                binding.addButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_add_grey
                    )
                )
            }
        }
        binding.subtractButton.setOnClickListener {
            var num = binding.peopleCountTextView.text.toString().toInt()
            if (num <= PEOPLE_MIN) return@setOnClickListener
            if (num >= PEOPLE_MAX) {
                binding.addButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_add_black
                    )
                )
            }
            num -= 1
            binding.peopleCountTextView.text = num.toString()
            createChatViewModel.setMaxPeopleNum(num)
            if (num <= PEOPLE_MIN) {
                binding.subtractButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_subtract_grey
                    )
                )
            }
        }
        binding.chatImage.setOnClickListener {
            val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getPictureResult.launch(pickPhoto)
        }
    }

    override fun observeViewModels() {
    }
}