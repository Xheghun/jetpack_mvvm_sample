package com.xheghun.dishapp.utils

import android.content.Context
import android.widget.Toast

fun showToast(context: Context, message: String, length: Int = Toast.LENGTH_SHORT) {
  Toast.makeText(context, message, length).show()
}