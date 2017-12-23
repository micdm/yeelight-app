package micdm.yeelight.tools

interface Result<T> {

    fun isSuccess(): Boolean
    fun isError(): Boolean
    fun getData(): T
}

private class SuccessResult<T>(private val data: T) : Result<T> {

    override fun isSuccess(): Boolean = true
    override fun isError(): Boolean = false
    override fun getData(): T = data
}

private class ErrorResult<T> : Result<T> {

    override fun isSuccess(): Boolean = false
    override fun isError(): Boolean = true
    override fun getData(): T = throw IllegalStateException("no data for error")
}

fun <T> newSuccess(data: T): Result<T> = SuccessResult(data)
fun <T> newError(): Result<T> = ErrorResult()
