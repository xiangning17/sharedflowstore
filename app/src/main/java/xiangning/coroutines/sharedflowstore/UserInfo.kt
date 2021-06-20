package xiangning.coroutines.sharedflowstore

data class UserInfo(val name: String, var logined: Boolean, var money: Float) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}