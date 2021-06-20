package xiangning.coroutines.sharedflowstore

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * a convenient way to retrieve a [MutableSharedFlow] and keep it in [SharedStore],
 * so we can share this [MutableSharedFlow] object from different scenes,
 * and it will release automatically when no strong reference to it.
 */
object SharedFlowStore {

    /**
     * retrieve a [MutableStateFlow] if us holder one now, and set value to [newValue],
     * otherwise creator a new [MutableStateFlow] with initial value set to [newValue].
     * [name] may be null if the type T is enough as a key to locate the flow from SharedFlowStore.
     * [skipEquals] not deliver the new value if it equals the old one, otherwise always deliver new value.
     */
    fun <T: Any> getStateFlow(newValue: T, name: String? = null, skipEquals: Boolean = false): MutableStateFlow<T> =
        SharedStore.getOrCreated(newValue::class as KClass<T>, name, onCache = { state ->
            state.value = newValue
        }) {
            if (skipEquals) MutableStateFlow(newValue) else NoSkipEqualsStateFlow(newValue)
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
    ) = SharedStore.getOrCreated(T::class, name) { MutableSharedFlow<T>(replay, extraBufferCapacity, onBufferOverflow) }

    /**
     * sharing this to a [MutableStateFlow] (skip deliver equals value)
     */
    inline fun <reified T : Any> T.sharingStateFlowSkipEquals(
        name: String? = null,
    ): MutableStateFlow<T> {
        return getStateFlow(this, name, true)
    }

    /**
     * sharing this to a [MutableStateFlow] (will deliver equals value)
     */
    inline fun <reified T : Any> T.sharingStateFlow(
        name: String? = null,
    ): MutableStateFlow<T> {
        return getStateFlow(this, name, false)
    }

    /**
     * create a MutableStateFlow which will deliver value even it equals the old one.
     */
    @Suppress("FunctionName", "UNCHECKED_CAST")
    fun <T> NoSkipEqualsStateFlow(initialState: T): MutableStateFlow<T> =
        NoSkipEqualsStateFlowImpl(
            initialState, MutableSharedFlow(
                replay = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        )

    /**
     * implement with MutableSharedFlow
     */
    private class NoSkipEqualsStateFlowImpl<T>(
        initialState: T,
        private val sharedFlow: MutableSharedFlow<T>
    ) : MutableStateFlow<T>, MutableSharedFlow<T> by sharedFlow {

        init {
            update(initialState)
        }

        @Volatile
        private var _value: T = initialState

        override var value: T
            get() = _value
            set(value) { update(value) }

        @Synchronized
        private fun update(value: T) {
            _value = value
            sharedFlow.tryEmit(value)
        }

        override fun compareAndSet(expect: T, update: T): Boolean {
            if (expect == null || _value == expect) {
                update(update)
                return true
            }

            return false
        }

    }
}