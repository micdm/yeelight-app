package micdm.yeelight.tools

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class DeviceController(private val host: String, private val port: Int) {

    fun toggle(): Completable {
        return Completable
            .create {
                try {
                    val channel = SocketChannel.open(InetSocketAddress(host, port))
                    channel.write(ByteBuffer.wrap("{\"id\": 1, \"method\": \"toggle\", \"params\": []}\r\n".toByteArray()))
                    channel.close()
                    it.onComplete()
                } catch (e: Exception) {
                    it.onError(e)
                }
            }
            .subscribeOn(Schedulers.io())
    }
}
