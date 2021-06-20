package xiangning.coroutines.sharedstore

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * a convenient way to retrieve a [MutableSharedFlow] and keep it in [SharedStore],
 * so we can share this [MutableSharedFlow] object from different scenes,
 * and it will release automatically when no strong reference to it.
 */
object SharedStoreStateFlow {

    /**
     * retrieve a [MutableStateFlow] if us holder one now, and set value to [newValue],
     * otherwise creator a new [MutableStateFlow] with initial value set to [newValue].
     * [name] may be null if the type T is enough as a key to locate the flow from MutableSharedFlowHolder.
     */
    fun <T: Any> get(newValue: T, name: String? = null): MutableStateFlow<T> =
        SharedStore.getOrCreated(newValue::class as KClass<T>, name) { MutableStateFlow(newValue) }
            .apply {
                if (value != newValue) {
                    value = newValue
                }
            }

    /**
     * extend function for object to get a [MutableStateFlow] from [SharedStore], see [get]
     */
    inline fun <reified T: Any> T.asSharedStoreMutableStateFlow(name: String? = null): MutableStateFlow<T> {
        return get(this, name)
    }
}