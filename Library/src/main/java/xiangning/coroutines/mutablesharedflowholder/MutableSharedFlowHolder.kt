package xiangning.coroutines.mutablesharedflowholder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xiangning.coroutines.mutablesharedflowholder.MutableSharedFlowHolder.getSharedFlow
import xiangning.coroutines.mutablesharedflowholder.MutableSharedFlowHolder.getStateFlow
import kotlin.reflect.KClass

/**
 * a convenient way to retrieve a [MutableSharedFlow] and keep it in MutableSharedFlowHolder,
 * MutableSharedFlowHolder will release the [MutableSharedFlow] automatically when it has no subscription.
 * retrieve a [MutableSharedFlow] by [getStateFlow] or a [MutableStateFlow] by [getSharedFlow]
 */
object MutableSharedFlowHolder {
    private data class Key(val cls: KClass<*>, val name: String?)

    private val store: MutableMap<Key, MutableSharedFlow<*>> = mutableMapOf()

    /**
     * background run check time interval, default 30s
     */
    var checkInterval: Long = 30 * 1000L

    init {
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                delay(checkInterval)
                checkEmpty()
            }
        }
    }

    /**
     * check which has no subscription and release it
     */
    private fun checkEmpty() {
        val iterator = store.iterator()
        var entry: MutableMap.MutableEntry<Key, MutableSharedFlow<*>>
        while (iterator.hasNext()) {
            entry = iterator.next()
            if (entry.value.subscriptionCount.value == 0) {
                iterator.remove()
                println("check empty remove key: ${entry.key}")
            }
        }
    }

    /**
     * retrieve a [MutableSharedFlow] if us holder one now, otherwise creator a new.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any, R: MutableSharedFlow<T>> getOrCreated(cls: KClass<T>, name: String?, creator: () -> R): R {
        val k = Key(cls, name)
        var sharedFlow = store[k]
        if (sharedFlow == null) {
            sharedFlow = creator()
            store[k] = sharedFlow
            println("add key: $k")

            GlobalScope.launch(Dispatchers.Default) {
                sharedFlow.subscriptionCount
                    .map { it > 0 }
                    .distinctUntilChanged()
                    .take(3) // 0 -> not 0 -> 0
                    .onCompletion {
                        store.remove(k)
                        println("remove key: $k")
                    }.collect()
            }
        }

        return sharedFlow as R
    }


    /**
     * retrieve a [MutableStateFlow] if us holder one now and set value to [newValue],
     * otherwise creator a new [MutableStateFlow] with initial value set to [newValue].
     * [name] may be null if the type T is enough as a key to locate the flow from MutableSharedFlowHolder.
     */
    fun <T: Any> getStateFlow(newValue: T, name: String? = null) =
        getOrCreated(newValue::class as KClass<T>, name) { MutableStateFlow(newValue) }
            .apply {
                if (value != newValue) {
                    value = newValue
                }
            }

    /**
     * retrieve a [MutableSharedFlow] if us holder one now,
     * otherwise creator a new [MutableSharedFlow] with given params.
     * [name] may be null if the type T is enough as a key to locate the flow from MutableSharedFlowHolder.
     */
    inline fun <reified T : Any> getSharedFlow(
        name: String? = null,
        replay: Int = 0,
        extraBufferCapacity: Int = 0,
        onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
    ) = getOrCreated(T::class, name) { MutableSharedFlow(replay, extraBufferCapacity, onBufferOverflow) }

//    fun Any.asMutableStateFlow
}