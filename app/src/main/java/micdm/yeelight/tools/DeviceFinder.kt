package micdm.yeelight.tools

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.yeelight.models.Device
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DeviceFinder {

    private val LOCAL_PORT = 43210
    private val REMOTE_HOST = "239.255.255.250"
    private val REMOTE_PORT = 1982
    private val RECEIVE_TIMEOUT = 10000
    private val BUFFER_SIZE = 1024
    private val LOCATION_HEADER_PATTERN = Pattern.compile("^Location: yeelight://([\\d.]+):([\\d]+)$")

    private val isStarted: Subject<Boolean> = BehaviorSubject.create()
    private val requests: Subject<Any> = PublishSubject.create();
    private val devices: Subject<Set<Device>> = BehaviorSubject.create()

    fun init() {
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
            .observeOn(Schedulers.io())
            .subscribe {
                val socket = DatagramSocket(LOCAL_PORT)
                socket.soTimeout = RECEIVE_TIMEOUT
                val payload = "M-SEARCH * HTTP/1.1\r\nHOST: $REMOTE_HOST:$REMOTE_PORT\r\nMAN: \"ssdp:discover\"\r\nST: wifi_bulb"
                socket.send(DatagramPacket(payload.toByteArray(), payload.length, InetAddress.getByName(REMOTE_HOST), REMOTE_PORT))
                val devices = mutableSetOf<Device>()
                while (!socket.isClosed) {
                    val buffer = ByteArray(BUFFER_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        devices.add(parseDevice(String(packet.data)))
                        this.devices.onNext(devices)
                    } catch (e: SocketTimeoutException) {
                        socket.close()
                    }
                }
            }
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

    fun getDevices(): Observable<Set<Device>> = devices

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
