package xiangning.coroutines.sharedflowstore

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import xiangning.coroutines.sharedflowstore.SharedFlowStore.sharingStateFlow

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainScope.launch {
            test()
        }
    }

    suspend fun test() {
        Log.e(TAG, "test run!")

        // 获取UserInfo类型标志的MutableStateFlow, 所有UserInfo对象调用sharingStateFlow
        // 都返回同一个MutableStateFlow对象，是以“共享状态”，内部使用WeakReference实现，
        // 当项目中对返回的对象的所有强引用都释放时，会自动释放
        // get MutableStateFlow of UserInfo, any instance of UserInfo invoke sharingStateFlow
        // will get the same instance of MutableStateFlow, so we can share state with other.
        val user = UserInfo("Tom", logined = false, money = 0f).sharingStateFlow()
        // 也可以使用name参数对同一种类型生成代表不同含义的状态，下面返回一个的Int型名为“like”的共享状态，用来共享获赞的数量
        // to get a shared state, you can also pass a 'name' to sharingStateFlow
        // val like = 5.asSharedStoreMutableStateFlow(name = "like")

        // 观察user的改变
        // observe change event of user
        user.onEach { Log.e(TAG, "main collect: $it") }
            .launchIn(mainScope)

        delay(200)
        // 会将logined为true的状态作为最新状态传递给所有观察者
        // this will deliver new state with 'logined=true' to all observers
        UserInfo("Tom", logined = true, money = 0f).sharingStateFlow()
            .onEach { Log.e(TAG, "io collect: $it") }
            .launchIn(mainScope + Dispatchers.IO)

        delay(200)
        // 通过持有的user对象主动改变MutableStateFlow的状态
        // change the MutableStateFlow state
        user.value = user.value.apply { money = 100f }

        Log.e(TAG, "complete")
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}