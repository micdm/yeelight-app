package micdm.yeelight.tools

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.yeelight.models.Address
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.TimeUnit

class DeviceController(private val address: Address) {

    interface State
    class ConnectingState : State
    class ConnectedState : State
    class DisconnectedState : State

    private interface Command
    private class AttachCommand : Command
    private class ConnectCommand : Command
    private class DeviceCommand(val outgoing: OutgoingPacket) : Command
    private class DetachCommand : Command

    private data class IncomingPacket(val id: Int, val result: String) {

        fun matches(outgoing: OutgoingPacket): Boolean = (id == outgoing.id)
    }

    private data class OutgoingPacket(val id: Int, val method: String, val params: List<Any> = emptyList())

    private val CLOSED_SOCKET = SocketChannel.open()

    private val commands: Subject<Command> = PublishSubject.create()
    private val incoming: Subject<IncomingPacket> = PublishSubject.create()

    private val _state: Subject<State> = BehaviorSubject.createDefault(DisconnectedState())
    val state: Observable<State>
        get() = _state

    fun init() {
        val connectRequests = getConnectRequests().share()
        val channel = getChannel(connectRequests).share()
        subscribeForConnectCommands(connectRequests)
        subscribeForState(channel)
        subscribeForIncoming(channel)
        subscribeForDeviceCommand(channel)
        subscribeForDetachCommands(channel)
    }

    private fun getConnectRequests(): Observable<Any> {
        return Observable.merge(
            commands
                .distinctUntilChanged()
                .ofType(AttachCommand::class.java),
            commands
                .ofType(ConnectCommand::class.java)
        )
    }

    private fun getChannel(connectRequests: Observable<Any>): Observable<SocketChannel> {
        return connectRequests
            .observeOn(Schedulers.io())
            .switchMap {
                Observable.create<SocketChannel> {
                    try {
                        it.onNext(SocketChannel.open(InetSocketAddress(address.host, address.port)))
                    } catch (e: Exception) {
                        it.onNext(CLOSED_SOCKET)
                    }
                }
            }
    }

    private fun subscribeForConnectCommands(connectRequests: Observable<Any>) {
        connectRequests.subscribe {
            _state.onNext(ConnectingState())
        }
    }

    private fun subscribeForState(channel: Observable<SocketChannel>) {
        channel
            .map { if (it != CLOSED_SOCKET) ConnectedState() else DisconnectedState() }
            .subscribe(_state::onNext)
    }

    private fun subscribeForIncoming(channel: Observable<SocketChannel>) {
        channel
            .observeOn(Schedulers.newThread())
            .switchMap { channel ->
                val buffer = ByteBuffer.allocate(1024)
                Observable.create<JSONObject> {
                    try {
                        while (channel.isConnected) {
                            val count = channel.read(buffer)
                            val string = String(buffer.array(), 0, count)
                            Log.d("TAG", "Incoming JSON: $string")
                            it.onNext(JSONObject(string))
                            buffer.rewind()
                        }
                    } catch (e: Exception) {

                    }
                }
            }
            .filter { it.has("id") }
            .map { IncomingPacket(it.getInt("id"), it.getString("result")) }
            .subscribe {
                Log.d("TAG", "Incoming packet: $it")
                incoming.onNext(it)
            }
    }

    private fun subscribeForDetachCommands(channel: Observable<SocketChannel>) {
        commands
            .ofType(DetachCommand::class.java)
            .withLatestFrom(
                channel.filter { it != CLOSED_SOCKET && it.isConnected },
                BiFunction<Command, SocketChannel, SocketChannel> { _, channel -> channel }
            )
            .subscribe {
                it.close()
                _state.onNext(DisconnectedState())
            }
    }

    private fun subscribeForDeviceCommand(channel: Observable<SocketChannel>) {
        commands
            .ofType(DeviceCommand::class.java)
            .withLatestFrom(
                channel.filter { it != CLOSED_SOCKET && it.isConnected },
                BiFunction<DeviceCommand, SocketChannel, Pair<OutgoingPacket, SocketChannel>> { command, channel -> command.outgoing.to(channel) }
            )
            .observeOn(Schedulers.io())
            .subscribe { (outgoing, channel) ->
                try {
                    val data = JSONObject()
                    data.put("id", outgoing.id)
                    data.put("method", outgoing.method)
                    data.put("params", JSONArray(outgoing.params))
                    val string = "$data\r\n"
                    Log.d("TAG", "Sending packet $data")
                    channel.write(ByteBuffer.wrap(string.toByteArray()))
                } catch (e: Exception) {
                    Log.w("TAG", "Cannot send packet", e)
                }
            }
    }

    fun attach() {
        commands.onNext(AttachCommand())
    }

    fun connect() {
        commands.onNext(ConnectCommand())
    }

    fun detach() {
        commands.onNext(DetachCommand())
    }

    fun toggle(): Completable = sendDeviceCommand("toggle")

    fun setColor(hue: Int, saturation: Int): Completable =
        sendDeviceCommand("set_hsv", listOf(hue, saturation, "sudden", "0"))

    private fun sendDeviceCommand(method: String, params: List<Any> = emptyList()): Completable {
        val outgoing = OutgoingPacket(Random().nextInt(), method, params)
        commands.onNext(DeviceCommand(outgoing))
        return incoming
            .filter { it.matches(outgoing) }
            .take(1)
            .timeout(1, TimeUnit.SECONDS)
            .ignoreElements()
    }
}
