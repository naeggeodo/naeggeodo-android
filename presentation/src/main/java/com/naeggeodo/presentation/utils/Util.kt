package com.naeggeodo.presentation.utils

import android.content.Context
import android.widget.Toast

object Util {
    fun shortShowToast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun shortLongToast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}