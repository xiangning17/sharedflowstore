package xiangning.coroutines.sharedstore

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import xiangning.coroutines.sharedstore.SharedStoreStateFlow.asSharedStoreMutableStateFlow
import java.util.concurrent.Executors

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

        // get the MutableStateFlow from SharedStore which type is 'Int' and named "version",
        // and update the state value to '1001' if it exists before us.
        val version = 1001.asSharedStoreMutableStateFlow("version")

        version.take(4)
            .onEach { Log.e(TAG, "main collect: $it") }
            .launchIn(mainScope)

        Log.e(TAG, "test go on!")
        delay(200)
        version.value = 1002

        "version_empty".asSharedStoreMutableStateFlow()
            .onEach { Log.e(TAG, "new collect: $it") }

        delay(400)
        val job = 1003.asSharedStoreMutableStateFlow("version")
            .onEach { Log.e(TAG, "io collect: $it") }
            .launchIn(mainScope + Dispatchers.IO)

        delay(200)
        version.value = 1004


        delay(400)
        version.value = 1005

        val new = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        val job2 = mainScope.launch(new) {
            repeat(6) {
                Log.e(TAG, "new set value: ${1006 + it}")
                version.value = 1006 + it
                delay(1000)
            }
        }

        delay(3000)
        job.cancel()


        job2.join()
        new.close()

        Log.e(TAG, "complete")
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}