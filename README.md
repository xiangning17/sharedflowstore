# SharedStore

提供一种方便地方式在不同场景共享状态

provid a convenient way to share state between diffrent scenes

[![](https://www.jitpack.io/v/xiangning17/sharedstore.svg)](https://www.jitpack.io/#xiangning17/sharedstore)


## 开始（Getting started）

1. 添加maven仓库地址到项目的build.gradle文件（add maven repositories in project build.gradle）:
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
2. 添加该库的依赖到模块的build.gradle文件（add dependency for this library in module build.gradle）：
```groovy
dependencies {
    implementation 'com.github.xiangning17:sharedstore:1.0.0'
}
```

## 使用方式（Usage）

```kotlin
// 获取UserInfo类型标志的MutableStateFlow, 所有UserInfo对象调用asSharedStoreMutableStateFlow
// 都返回同一个MutableStateFlow对象，是以“共享状态”，内部使用WeakReference实现，当项目中对返回的对象的所有强引用都释放时，会自动释放
// get MutableStateFlow of UserInfo, any instance of UserInfo invoke asSharedStoreMutableStateFlow
// will get the same instance of MutableStateFlow, so we can share state with other.
val user = UserInfo("Tom", logined = false, money = 0f).asSharedStoreMutableStateFlow()
// 也可以使用name参数对同一种类型生成代表不同含义的状态，下面返回一个的Int型名为“like”的共享状态，用来共享获赞的数量
// to get a shared state, you can also pass a 'name' to asSharedStoreMutableStateFlow
// val like = 5.asSharedStoreMutableStateFlow(name = "like")

// 观察user的改变
// observe change event of user
user.onEach { Log.e(TAG, "main collect: $it") }
    .launchIn(mainScope)

delay(200)
// 会将logined为true的状态作为最新状态传递给所有观察者
// this will deliver new state with 'logined=true' to all observers
UserInfo("Tom", logined = true, money = 0f).asSharedStoreMutableStateFlow()
    .onEach { Log.e(TAG, "io collect: $it") }
    .launchIn(mainScope + Dispatchers.IO)

// 通过持有的user对象主动改变MutableStateFlow的状态
// change the MutableStateFlow state
user.value = user.value.apply { logined = true }
```
