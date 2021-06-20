package xiangning.coroutines.mutablesharedflowholder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainScope.launch { test() }
    }

    suspend fun test() {
        Log.e(TAG, "test run!")

        // hold the MutableStateFlow object, we can change it later
        val version = MutableSharedFlowHolder.getStateFlow(name = "version", newValue = 1001)
            .also { flow ->
                // set this subscription only take 4,
                flow.take(4)
                    .onEach { Log.e(TAG, "main collect: $it") }
                    .launchIn(mainScope)
            }

        Log.e(TAG, "test go on!")
        delay(200)
        version.value = 1002

        delay(400)
        val job = MutableSharedFlowHolder.getStateFlow(name = "version", newValue = 1003)
            .onEach { Log.e(TAG, "io collect: $it") }
            .launchIn(mainScope + Dispatchers.IO)

        delay(200)
        version.value = 1004


        delay(400)
        version.value = 1005

        val new = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        val job2 = mainScope.launch(new) {
            MutableSharedFlowHolder.getStateFlow("version_empty")
                .onEach { Log.e(TAG, "new collect: $it") }

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