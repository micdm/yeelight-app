package micdm.yeelight.tools

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
import timber.log.Timber
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.TimeUnit

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

class DeviceController(private val address: Address) {

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
        val clientCount = getClientCount().share()
        val connectRequests = getConnectRequests(clientCount).share()
        val openedChannel = getChannel(connectRequests).share()
        val incoming = getIncoming(openedChannel).share()
        val outgoing = getOutgoing(openedChannel).share()
        val actualChannel = getActualChannel(openedChannel, incoming, outgoing)
        subscribeForClientCount(clientCount)
        subscribeForConnectionState(connectRequests, actualChannel)
        subscribeForIncoming(incoming)
        subscribeForDeviceCommand(outgoing)
        subscribeForDeviceState(actualChannel)
        subscribeForDisconnect(clientCount, actualChannel)
    }

    private fun getClientCount(): Observable<Int> {
        return Observable
            .merge(
                commands
                    .filter { it is AttachCommand }
                    .map { 1 },
                commands
                    .filter { it is DetachCommand }
                    .delay(5, TimeUnit.SECONDS)
                    .map { -1 }
            )
            .scan(0, { accumulated, delta -> accumulated + delta })
    }

    private fun getConnectRequests(clientCount: Observable<Int>): Observable<Any> {
        return Observable.merge(
            clientCount
                .distinctUntilChanged { previous, current -> previous != 0 || current == 0 },
            commands
                .ofType(ConnectCommand::class.java)
        )
    }

    private fun getChannel(connectRequests: Observable<Any>): Observable<Result<SocketChannel>> {
        return connectRequests
            .observeOn(Schedulers.io())
            .switchMap {
                Observable.create<Result<SocketChannel>> {
                    try {
                        it.onNext(newSuccess(SocketChannel.open(InetSocketAddress(address.host, address.port))))
                    } catch (e: Exception) {
                        it.onNext(newError())
                    }
                }
            }
    }

    private fun getIncoming(channelObservable: Observable<Result<SocketChannel>>): Observable<Result<JSONObject>> {
        return channelObservable
            .filter { it.isSuccess() }
            .map { it.getData() }
            .observeOn(Schedulers.newThread())
            .switchMap { channel ->
                Observable.create<Result<JSONObject>> {
                    try {
                        val buffer = ByteBuffer.allocate(1024)
                        while (channel.isConnected) {
                            val count = channel.read(buffer)
                            val string = String(buffer.array(), 0, count)
                            Timber.d("Incoming JSON: $string")
                            it.onNext(newSuccess(JSONObject(string)))
                            buffer.rewind()
                        }
                        it.onNext(newError())
                    } catch (e: Exception) {
                        it.onNext(newError())
                    }
                }
            }
    }

    private fun getOutgoing(channelObservable: Observable<Result<SocketChannel>>): Observable<Result<Any>> {
        return commands
            .ofType(DeviceCommand::class.java)
            .withLatestFrom(
                channelObservable
                    .filter { it.isSuccess() }
                    .map { it.getData() },
                BiFunction { command: DeviceCommand, channel: SocketChannel -> command.outgoing.to(channel) }
            )
            .observeOn(Schedulers.io())
            .switchMap { (outgoing, channel) ->
                Observable.create<Result<Any>> {
                    try {
                        val data = JSONObject()
                        data.put("id", outgoing.id)
                        data.put("method", outgoing.method)
                        data.put("params", JSONArray(outgoing.params))
                        val string = "$data\r\n"
                        Timber.d("Sending packet $data")
                        channel.write(ByteBuffer.wrap(string.toByteArray()))
                        it.onNext(newSuccess(Any()))
                    } catch (e: Exception) {
                        it.onNext(newError())
                    }
                }
            }
    }

    private fun getActualChannel(channelObservable: Observable<Result<SocketChannel>>, incomingObservable: Observable<Result<JSONObject>>,
                                 outgoingObservable: Observable<Result<Any>>): Observable<Result<SocketChannel>> {
        return Observable
            .merge(
                channelObservable,
                incomingObservable
                    .filter { it.isError() }
                    .map { newError<SocketChannel>() },
                outgoingObservable
                    .filter { it.isError() }
                    .map { newError<SocketChannel>() }
            )
    }

    private fun subscribeForClientCount(clientCount: Observable<Int>) {
        clientCount.subscribe {
            Timber.d("Clients attached now: $it")
        }
    }

    private fun subscribeForConnectionState(connectRequests: Observable<Any>, channelObservable: Observable<Result<SocketChannel>>) {
        Observable
            .merge(
                connectRequests
                    .map { ConnectingState() },
                channelObservable
                    .map { if (it.isSuccess()) ConnectedState() else DisconnectedState() }
            )
            .subscribe(_connectionState::onNext)
    }

    private fun subscribeForDeviceState(channelObservable: Observable<Result<SocketChannel>>) {
        val outgoingObservable =
            channelObservable
                .filter { it.isSuccess() }
                .map { OutgoingPacket("get_prop", listOf("power", "color_mode", "ct", "hue", "sat")) }
                .share()
        Observable
            .merge(
                channelObservable
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
                                val merged = previous.toMutableMap()
                                merged.putAll(params)
                                if ("color_mode" in merged) {
                                    if ("hue" in merged || "ct" in merged) {
                                        Pair(emptyMap(), merged)
                                    } else {
                                        Pair(merged, emptyMap())
                                    }
                                } else {
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
                Timber.d("Device state is $it")
                _deviceState.onNext(it)
            }
        outgoingObservable.subscribe {
            commands.onNext(DeviceCommand(it))
        }
    }

    private fun subscribeForIncoming(incomingObservable: Observable<Result<JSONObject>>) {
        incomingObservable
            .filter { it.isSuccess() }
            .map { it.getData() }
            .map {
                when {
                    it.has("id") -> incomingPacketDeserializer.newResultPacket(it)
                    it.has("params") -> incomingPacketDeserializer.newPropsPacket(it)
                    else -> UnknownPacket()
                }
            }
            .subscribe {
                Timber.d("Incoming packet: $it")
                incoming.onNext(it)
            }
    }

    private fun subscribeForDisconnect(clientCount: Observable<Int>, channelObservable: Observable<Result<SocketChannel>>) {
        clientCount
            .filter { it == 0 }
            .withLatestFrom(
                channelObservable
                    .filter { it.isSuccess() }
                    .map { it.getData() },
                BiFunction { _: Int, channel: SocketChannel -> channel }
            )
            .subscribe {
                Timber.d("Disconnecting from $address...")
                it.close()
            }
    }

    private fun subscribeForDeviceCommand(outgoingObservable: Observable<Result<Any>>) {
        outgoingObservable.subscribe()
    }

    fun attach() {
        Timber.d("Client attached to device controller")
        commands.onNext(AttachCommand())
    }

    fun connect() {
        commands.onNext(ConnectCommand())
    }

    fun detach() {
        Timber.d("Client detached from device controller")
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
