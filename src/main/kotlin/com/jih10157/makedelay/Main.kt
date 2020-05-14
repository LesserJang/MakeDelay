package com.jih10157.makedelay

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*

private const val TIMEOUT = 5000

fun main() {
    var port = 0
    var address = ""
    var serverPort = 0
    var delay = 0
    Scanner(System.`in`).use { scanner ->
        print("포트: ")
        port = scanner.nextInt()
        print("연결할 서버 주소: ")
        address = scanner.next().trim()
        print("연결할 서버 포트: ")
        serverPort = scanner.nextInt()
        print("딜레이(ms): ")
        delay = scanner.nextInt()
    }
    val host = ServerSocket(port)
    while (true) {
        val client = host.accept()
        val server = Socket(address, serverPort)
        ThreadHandler(client, server, delay).start()
    }
}
class ThreadHandler(private val client: Socket, private val server: Socket, private val delay: Int): Thread() {
    private val clientQueue: Queue<ByteArray> = LinkedList()
    private val serverQueue: Queue<ByteArray> = LinkedList()
    private val clientTimeQueue: Queue<Long> = LinkedList()
    private val serverTimeQueue: Queue<Long> = LinkedList()

    override fun run() {
        val clientInput = client.getInputStream()
        val clientOutput = client.getOutputStream()
        val serverInput = server.getInputStream()
        val serverOutput = server.getOutputStream()

        var csClosed = false
        var scClosed = false
        var timeOut = System.currentTimeMillis()+ TIMEOUT

        while (true) {
            if(timeOut <= System.currentTimeMillis()||csClosed||scClosed) {
                serverInput.close()
                serverOutput.close()
                server.close()
                clientInput.close()
                clientOutput.close()
                client.close()
                break
            }
            if(server.isClosed&&client.isClosed) break
            if(!csClosed) {
                try {
                    val clientAvailable = clientInput.available()
                    if(clientAvailable!=0) {
                        val byteArray = ByteArray(clientAvailable)
                        clientInput.read(byteArray)
                        clientQueue.add(byteArray)
                        clientTimeQueue.add(System.currentTimeMillis()+ delay)
                        timeOut = System.currentTimeMillis()+ TIMEOUT
                    }
                } catch (e: IOException) {
                    csClosed = true
                }
            }
            if(!scClosed) {
                try {
                    val serverAvailable = serverInput.available()
                    if(serverAvailable!=0) {
                        val byteArray = ByteArray(serverAvailable)
                        serverInput.read(byteArray)
                        serverQueue.add(byteArray)
                        serverTimeQueue.add(System.currentTimeMillis()+ delay)
                        timeOut = System.currentTimeMillis()+ TIMEOUT
                    }
                } catch (e: IOException) {
                    scClosed = true
                }
            }
            if(!csClosed && clientQueue.isNotEmpty() &&
                clientTimeQueue.peek() <= System.currentTimeMillis()) {
                clientTimeQueue.poll()
                try {
                    serverOutput.write(clientQueue.poll())
                    timeOut = System.currentTimeMillis()+ TIMEOUT
                } catch (e: SocketException) {
                    csClosed = true
                    clientInput.close()
                }
            }
            if(!scClosed && serverQueue.isNotEmpty() &&
                serverTimeQueue.peek() <= System.currentTimeMillis()) {
                serverTimeQueue.poll()
                try {
                    clientOutput.write(serverQueue.poll())
                    timeOut = System.currentTimeMillis()+ TIMEOUT
                } catch (e: SocketException) {
                    scClosed = true
                    serverInput.close()
                }
            }
        }
    }
}