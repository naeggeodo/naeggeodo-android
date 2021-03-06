package com.naeggeodo.presentation.view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.naeggeodo.presentation.databinding.DialogFragmentCommonBinding

class CommonDialogFragment(
    private val contentText: String = "",
    private val normalButtonText: String = "예",
    private val colorButtonText: String = "아니오",
    private val normalButtonListener: () -> Unit = {},
    private val colorButtonListener: () -> Unit = {}
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //false로 설정해 주면 화면밖 혹은 뒤로가기 버튼시 다이얼로그라 dismiss 되지 않는다.
        isCancelable = true
    }

    private lateinit var binding: DialogFragmentCommonBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFragmentCommonBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.contentTextView.text = contentText
        binding.normalTextView.text = normalButtonText
        binding.normalTextView.setOnClickListener {
            normalButtonListener()
            dismiss()
        }
        binding.colorTextView.text = colorButtonText
        binding.colorButton.setOnClickListener {
            colorButtonListener()
            dismiss()
        }
    }
}