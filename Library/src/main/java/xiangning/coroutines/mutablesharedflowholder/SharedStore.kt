package xiangning.coroutines.mutablesharedflowholder

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Created by xiangning on 2021/6/20.
 *
 * a store easy shared object, locate a object by 'Class' and 'name'(optional).
 *
 * will release the object when no strong reference.
 *
 */
object SharedStore {

    private data class Key(val cls: KClass<*>, val name: String?)

    private val store = ConcurrentHashMap<Key, WeakReference<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> getOrCreated(cls: KClass<T>, name: String?, creator: () -> T): T {
        val k = Key(cls, name)
        val obj = (store[k]?.get() as? T) ?: creator().also {
            store[k] = WeakReference(it)
            println("SharedStore: store obj: $k")
        }
        trim()
        return obj
    }

    private fun trim() {
        val iterator = store.iterator()
        var entry: MutableMap.MutableEntry<Key, WeakReference<*>>
        while (iterator.hasNext()) {
            entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
                println("SharedStore: trim key: ${entry.key}")
            }
        }
    }

}