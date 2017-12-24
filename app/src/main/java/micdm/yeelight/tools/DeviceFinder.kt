package micdm.yeelight.tools

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.yeelight.models.Device
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DeviceFinder {

    interface State
    class DiscoveringState : State
    class DiscoveredState(val devices: Set<Device>) : State
    class FinishedState(val devices: Set<Device>) : State
    class FailedState : State

    private val LOCAL_PORT = 43210
    private val REMOTE_HOST = "239.255.255.250"
    private val REMOTE_PORT = 1982
    private val RECEIVE_TIMEOUT = 10000
    private val BUFFER_SIZE = 1024
    private val LOCATION_HEADER_PATTERN = Pattern.compile("^Location: yeelight://([\\d.]+):([\\d]+)$")

    private val isStarted: Subject<Boolean> = BehaviorSubject.create()
    private val requests: Subject<Any> = PublishSubject.create()

    private val _state: Subject<State> = BehaviorSubject.createDefault(FinishedState(emptySet()))
    val state: Observable<State>
        get() = _state

    fun init() {
        val socket: Subject<DatagramSocket> = BehaviorSubject.create()
        val activeSocket: Subject<DatagramSocket> = BehaviorSubject.create()
        subscribeForDiscoverRequests(socket)
        subscribeForSocket(socket, activeSocket)
        subscribeForActiveSocket(activeSocket)
    }

    private fun subscribeForDiscoverRequests(socketSubject: Subject<DatagramSocket>): Disposable {
        val discoverRequests =
            Observable
                .merge(
                    isStarted
                        .filter { it }
                        .switchMap {
                            Observable.interval(0, 30, TimeUnit.SECONDS)
                                .takeUntil(isStarted.filter { !it })
                        },
                    requests
                )
                .share()
        return CompositeDisposable(
            discoverRequests.subscribe {
                _state.onNext(DiscoveringState())
            },
            discoverRequests
                .switchMap {
                    Observable.create<DatagramSocket> {
                        val socket = DatagramSocket(LOCAL_PORT)
                        socket.soTimeout = RECEIVE_TIMEOUT
                        it.setCancellable { socket.close() }
                        it.onNext(socket)
                    }
                }
                .subscribe(socketSubject::onNext)
        )
    }

    private fun subscribeForSocket(socketSubject: Subject<DatagramSocket>, activeSocketSubject: Subject<DatagramSocket>): Disposable {
        return socketSubject
            .observeOn(Schedulers.io())
            .subscribe {
                try {
                    val payload = "M-SEARCH * HTTP/1.1\r\nHOST: $REMOTE_HOST:$REMOTE_PORT\r\nMAN: \"ssdp:discover\"\r\nST: wifi_bulb"
                    it.send(DatagramPacket(payload.toByteArray(), payload.length, InetAddress.getByName(REMOTE_HOST), REMOTE_PORT))
                    _state.onNext(DiscoveredState(emptySet()))
                    activeSocketSubject.onNext(it)
                } catch (e: Exception) {
                    Timber.w(e, "Cannot discover devices")
                    it.close()
                    _state.onNext(FailedState())
                }
            }
    }

    private fun subscribeForActiveSocket(activeSocketSubject: Subject<DatagramSocket>): Disposable {
        return activeSocketSubject
            .observeOn(Schedulers.io())
            .switchMap { socket ->
                Observable.create<State> {
                    val devices = mutableSetOf<Device>()
                    while (!socket.isClosed) {
                        val buffer = ByteArray(BUFFER_SIZE)
                        val packet = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(packet)
                            devices.add(parseDevice(String(packet.data)))
                            it.onNext(DiscoveredState(devices))
                        } catch (e: Exception) {
                            socket.close()
                            it.onNext(FinishedState(devices))
                        }
                    }
                }
            }
            .subscribe(_state::onNext)
    }

    private fun parseDevice(data: String): Device {
        val (host, port) = parseLocation(data)
        return Device(parseId(data), host, port)
    }

    private fun parseId(data: String): String {
        for (line: String in data.split("\r\n")) {
            if (!line.startsWith("id")) {
                continue
            }
            return line.split(" ")[1]
        }
        throw IllegalStateException("no id header found")
    }

    private fun parseLocation(data: String): Pair<String, Int> {
        for (line: String in data.split("\r\n")) {
            if (!line.startsWith("Location")) {
                continue
            }
            val matcher = LOCATION_HEADER_PATTERN.matcher(line)
            if (matcher.matches()) {
                return Pair(matcher.group(1), matcher.group(2).toInt())
            }
        }
        throw IllegalStateException("no location header found")
    }

    fun start() {
        isStarted.onNext(true)
    }

    fun stop() {
        isStarted.onNext(false)
    }

    fun discover() {
        requests.onNext(Any())
    }
}
