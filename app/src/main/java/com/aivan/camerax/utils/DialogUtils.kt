package com.aivan.camerax.utils

import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 *
 * @author: Aivan
 * @date: 2023/6/5
 * @desc:
 */
object DialogUtils {
    fun showDialog(
        clz: DialogFragment, @NonNull supportFragmentManager: FragmentManager,
        tag: String
    ) {
        try {
            if(!supportFragmentManager.isDestroyed){
                val ft = supportFragmentManager.beginTransaction()
                val fragment = supportFragmentManager.findFragmentByTag(tag)
                fragment?.let {
                    ft.remove(it)
                }
                ft.add(clz, tag)
                ft.commitAllowingStateLoss()
            }
        }catch (e:Exception){

        }
    }
}