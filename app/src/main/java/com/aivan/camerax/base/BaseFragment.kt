package com.aivan.camerax.base

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.aivan.camerax.utils.DialogUtils

/**
 * author : Administrator
 * date : 2019-12-30
 * desc :
 * Fragment lifecycle see: https://developer.android.google.cn/guide/components/fragments
 * FragmentTransaction see: https://www.jianshu.com/p/5761ee2d3ea1
 *
 * https://img2020.cnblogs.com/blog/595094/202004/595094-20200429150020682-753845082.png
 *
 *
 * 1、保存 fragment 所在的容器id（fragmentContainerId）
 * 2、拓展方法
 */
abstract class BaseFragment<VB : ViewBinding>(val bindingBlock: (LayoutInflater, ViewGroup?, Boolean) -> VB) :
    Fragment() {
    private lateinit var _binding: VB
    protected val binding get() = _binding
    /**
     * 获取容器id
     *
     * @return 容器id
     */
    /**
     * 设置容器id
     *
     * @param containerId 容器id
     */
    // ====================================================================================
    // container
    // ====================================================================================
    // 容器id
    var containerId = 0

    // ====================================================================================
    // extension
    // ====================================================================================
    open val isRegisterOnCreateEvent: Boolean
        /**
         * @return onCreate是否注册事件  onDestroy 解绑事件
         */
        get() = false

    open val isRegisterOnStartEvent: Boolean
        /**
         * @return onStart是否注册事件  onPause 解绑事件
         */
        get() = false

    /*fun <T : View?> findViewById(@IdRes id: Int): T {
        val view = view ?: throw NullPointerException("rootView is null")
        return view.findViewById(id)
    }*/

    /*protected fun replaceRootView(layoutId: Int) {
        val root: ViewGroup = view as ViewGroup?
            ?: throw NullPointerException("rootView is null")
        root.removeAllViewsInLayout()
        View.inflate(root.getContext(), layoutId, root)
    }*/

    /*val fragment: BaseFragment
        get() = this*/

    // ====================================================================================
    // fragment life
    // ====================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isRegisterOnCreateEvent) {
//            if (!EventBus.getDefault().isRegistered(this)) {
//                LogUtils.e("----------------------isRegisterOnCreateEvent-------------------onCreate")
//                EventBus.getDefault().register(this)
//            }
        }
    }

    /**
     * 创建视图
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = bindingBlock(inflater, container, false)
        return _binding.root
    }

    /**
     * 初始化视图
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }


    private var onStartCount = 0
    private var onResumeCount = 0
    var isForeground = false
        private set

    override fun onStart() {
        super.onStart()
        isForeground = true
        onStart(++onStartCount)
        if (isRegisterOnStartEvent) {
//            if(!EventBus.getDefault().isRegistered(this)) {
//                LogUtils.e("----------------------isRegisterOnStartEvent-------------------onStart")
//                EventBus.getDefault().register(this)
//            }
        }

    }

    fun onStart(count: Int) {}
    override fun onResume() {
        super.onResume()
        onResume(++onResumeCount)
    }

    fun onResume(count: Int) {}

    override fun onPause() {
        super.onPause()
        if (isRegisterOnStartEvent) {
            //LogUtils.e("--------------------isRegisterOnStartEvent---------------------onPause")
            //EventBus.getDefault().unregister(this)
        }
    }

    /**
     * 销毁视图
     */
    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRegisterOnCreateEvent) {
            //LogUtils.e("--------------------isRegisterOnCreateEvent---------------------onDestroy")
            //EventBus.getDefault().unregister(this)
        }
        // 取消网络请求
        //TmdHttpClient.cancel(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onStop() {
        super.onStop()
        isForeground = false
    }

    // ====================================================================================
    // 使用ViewModelProvider简单的创建ViewModel
    // https://www.cnblogs.com/guanxinjing/p/13442423.html
    //
    // ViewModelProvider.Factory
    // https://www.cnblogs.com/guanxinjing/p/12198971.html
    // https://blog.csdn.net/qq_43377749/article/details/100856599
    //
    // ViewModel是什么
    // https://www.jianshu.com/p/35d143e84d42
    //
    // 注意事项：由于 ViewModel 生命周期可能比 activity 长，所以为了避免内存泄漏，
    //          禁止在 ViewModel 中持有 Context 或 activity 引用
    // ====================================================================================
    /**
     * 无返回实体对话框
     */
    fun showDialog(clz: DialogFragment, tag: String) {
        //防止 报 has not been attached yet.
        activity?.let {
            if(!it.isFinishing && !it.isDestroyed){
                DialogUtils.showDialog(clz, childFragmentManager, tag)
            }
        }
    }

    fun getActivity(block : (activity: FragmentActivity) -> Unit){
        activity?.let { _act ->
            if(!_act.isFinishing && !_act.isDestroyed){
                block(_act)
            }
        }
    }
}
