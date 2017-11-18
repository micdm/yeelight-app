package micdm.yeelight.tools

import android.util.Log
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.SocketChannel

class DeviceController(private val host: String, private val port: Int) {

    fun toggle(): Completable {
        return sendCommand("{\"id\": 1, \"method\": \"toggle\", \"params\": []}")
    }

    fun setColor(hue: Int, saturation: Int): Completable {
        return sendCommand("{\"id\": 1, \"method\": \"set_hsv\", \"params\": [$hue, $saturation, \"sudden\", 0]}")
    }

    private fun sendCommand(command: String): Completable {
        return Completable
            .create {
                try {
                    val channel = SocketChannel.open(InetSocketAddress(host, port))
                    channel.write(ByteBuffer.wrap("$command\r\n".toByteArray()))
                    channel.close()
                    it.onComplete()
                } catch (e: ClosedByInterruptException) {
                    Log.w("TAG", "Connection closed!")
                    it.onComplete()
                } catch (e: Exception) {
                    it.onError(e)
                }
            }
            .subscribeOn(Schedulers.io())
    }
}
