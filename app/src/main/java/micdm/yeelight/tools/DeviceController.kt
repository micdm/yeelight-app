package micdm.yeelight.tools

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.yeelight.models.*
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

    private interface IncomingPacket
    private data class ResultPacket(val id: Int, val result: List<String>) : IncomingPacket {

        val isSuccess
            get() = result[0] == "ok"

        fun matches(outgoing: OutgoingPacket): Boolean = (id == outgoing.id)
    }
    private data class PropsPacket(val params: Map<String, String>) : IncomingPacket
    private class UnknownPacket : IncomingPacket

    private data class OutgoingPacket(val method: String, val params: List<Any> = emptyList()) {

        val id = Random().nextInt()
    }

    private class IncomingPacketDeserializer {

        fun newResultPacket(data: JSONObject): ResultPacket {
            val result = data.getJSONArray("result")
            return ResultPacket(
                data.getInt("id"),
                (0 until result.length()).map { result.getString(it) }
            )
        }

        fun newPropsPacket(data: JSONObject): PropsPacket {
            val params = data.getJSONObject("params")
            val items = mutableMapOf<String, String>()
            for (key in params.keys()) {
                items[key] = params.getString(key)
            }
            return PropsPacket(items)
        }
    }

    private val CLOSED_CHANNEL = SocketChannel.open()

    private val incomingPacketDeserializer = IncomingPacketDeserializer()

    private val commands: Subject<Command> = PublishSubject.create()
    private val incoming: Subject<IncomingPacket> = PublishSubject.create()

    private val _connectionState: Subject<State> = BehaviorSubject.createDefault(DisconnectedState())
    val connectionState: Observable<State>
        get() = _connectionState

    private val _deviceState: Subject<DeviceState> = BehaviorSubject.create()
    val deviceState: Observable<DeviceState>
        get() = _deviceState

    fun init() {
        val connectRequests = getConnectRequests().share()
        val channel = getChannel(connectRequests).share()
        subscribeForConnectCommands(connectRequests)
        subscribeForConnectionState(channel)
        subscribeForIncoming(channel)
        subscribeForDeviceCommand(channel)
        subscribeForDeviceState(channel)
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
                        it.onNext(CLOSED_CHANNEL)
                    }
                }
            }
    }

    private fun subscribeForConnectCommands(connectRequests: Observable<Any>) {
        connectRequests.subscribe {
            _connectionState.onNext(ConnectingState())
        }
    }

    private fun subscribeForConnectionState(channel: Observable<SocketChannel>) {
        channel
            .map { if (it !== CLOSED_CHANNEL) ConnectedState() else DisconnectedState() }
            .subscribe(_connectionState::onNext)
    }

    private fun subscribeForDeviceState(channel: Observable<SocketChannel>) {
        val outgoingObservable =
            channel
                .filter { it !== CLOSED_CHANNEL && it.isConnected }
                .map { OutgoingPacket("get_prop", listOf("power", "color_mode", "ct", "hue", "sat")) }
                .share()
        Observable
            .merge(
                channel
                    .filter { it !== CLOSED_CHANNEL && it.isConnected }
                    .map { UNDEFINED_DEVICE_STATE },
                Observable
                    .merge(
                        incoming
                            .ofType(ResultPacket::class.java)
                            .withLatestFrom(
                                outgoingObservable,
                                BiFunction { incoming: ResultPacket, outgoing: OutgoingPacket -> incoming.to(outgoing) }
                            )
                            .filter { (incoming, outgoing) -> incoming.matches(outgoing) }
                            .map { (incoming, _) ->
                                mapOf(
                                    Pair("power", incoming.result[0]),
                                    Pair("color_mode", incoming.result[1]),
                                    Pair("ct", incoming.result[2]),
                                    Pair("hue", incoming.result[3]),
                                    Pair("sat", incoming.result[4])
                                )
                            },
                        incoming
                            .ofType(PropsPacket::class.java)
                            .map { it.params }
                            .scan(Pair<Map<String, String>, Map<String, String>>(emptyMap(), emptyMap()), { (previous, _), params ->
                                if ("color_mode" in params) {
                                    Pair(params, emptyMap())
                                } else {
                                    val merged = previous.toMutableMap()
                                    merged.putAll(params)
                                    Pair(emptyMap(), merged)
                                }
                            })
                            .map { (_, result) -> result }
                            .filter { it.isNotEmpty() }
                    )
                    .scan(emptyMap<String, String>(), { accumulated, packet ->
                        val result = mutableMapOf<String, String>()
                        result.putAll(accumulated)
                        result.putAll(packet)
                        result
                    })
                    .skip(1)
                    .filter { it.size == 5 }
                    .map {
                        val color = if (it["color_mode"] == "2") TemperatureColor(it["ct"]!!.toInt()) else HsvColor(it["hue"]!!.toInt(), it["sat"]!!.toInt())
                        DeviceState(it["power"] == "on", color)
                    }
            )
            .subscribe {
                Log.d("TAG", "Device state is $it")
                _deviceState.onNext(it)
            }
        outgoingObservable.subscribe {
            commands.onNext(DeviceCommand(it))
        }
    }

    private fun subscribeForIncoming(observableChannel: Observable<SocketChannel>) {
        observableChannel
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
            .map {
                when {
                    it.has("id") -> incomingPacketDeserializer.newResultPacket(it)
                    it.has("params") -> incomingPacketDeserializer.newPropsPacket(it)
                    else -> UnknownPacket()
                }
            }
            .subscribe {
                Log.d("TAG", "Incoming packet: $it")
                incoming.onNext(it)
            }
    }

    private fun subscribeForDetachCommands(channelObservable: Observable<SocketChannel>) {
        commands
            .ofType(DetachCommand::class.java)
            .withLatestFrom(
                channelObservable.filter { it !== CLOSED_CHANNEL && it.isConnected },
                BiFunction { _: Command, channel: SocketChannel -> channel }
            )
            .subscribe {
                it.close()
                _deviceState.onNext(UNDEFINED_DEVICE_STATE)
                _connectionState.onNext(DisconnectedState())
            }
    }

    private fun subscribeForDeviceCommand(channelObservable: Observable<SocketChannel>) {
        commands
            .ofType(DeviceCommand::class.java)
            .withLatestFrom(
                channelObservable.filter { it !== CLOSED_CHANNEL && it.isConnected },
                BiFunction { command: DeviceCommand, channel: SocketChannel -> command.outgoing.to(channel) }
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
        val outgoing = OutgoingPacket(method, params)
        return incoming
            .ofType(ResultPacket::class.java)
            .filter { it.matches(outgoing) && it.isSuccess }
            .take(1)
            .timeout(3, TimeUnit.SECONDS)
            .ignoreElements()
            .doOnSubscribe {
                commands.onNext(DeviceCommand(outgoing))
            }
    }
}
