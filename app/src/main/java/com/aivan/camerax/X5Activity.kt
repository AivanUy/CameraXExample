package com.aivan.camerax

import android.os.Bundle
import com.aivan.camerax.base.BaseActivity
import com.aivan.camerax.databinding.ActivityX5Binding

/**
 *
 * @author: Aivan
 * @date: 2023/7/28
 * @desc:
 */
class X5Activity : BaseActivity<ActivityX5Binding>(ActivityX5Binding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.x5WebView.loadUrl("http://www.163.com/")
    }
}