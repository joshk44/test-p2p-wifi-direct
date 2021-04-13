package com.joseferreyra.wirelesschat

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException

class ChatActivity : AppCompatActivity() {
    private var serverSocket: ServerSocket? = null
    var updateConversationHandler: Handler? = null
    var serverThread: Thread? = null
    private var socket: Socket? = null
    var receivedText: TextView? = null
    var yourMessage: EditText? = null
    var send: Button? = null
    var owner = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val intent = intent
        owner = intent.getBooleanExtra("Owner?", false)
        SERVER_IP = intent.getStringExtra("Owner Address")
        receivedText = findViewById<View>(R.id.text_incoming) as TextView
        yourMessage = findViewById<View>(R.id.text_send) as EditText
        send = findViewById<View>(R.id.btn_send) as Button
        updateConversationHandler = Handler()

        // If we're the owner start a server, else connect to the server
        if (owner) {
            serverThread = Thread(ServerThread())
            serverThread?.start()
        } else {
            Thread(ClientThread()).start()
        }

        // Sends the text to the server
        send!!.setOnClickListener {
            try {
                val str = yourMessage!!.text.toString()
                val out = PrintWriter(BufferedWriter(
                        OutputStreamWriter(socket!!.getOutputStream()
                        )), true)
                out.println(str)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    // Opens a socket on the owner device to get messages
    internal inner class ServerThread : Runnable {
        override fun run() {
            var socket: Socket? = null
            try {
                // Create a socket on port 6000
                serverSocket = ServerSocket(SERVERPORT)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            while (!Thread.currentThread().isInterrupted) {
                try {
                    // Start listening for messages
                    serverSocket?.let {
                        socket = it.accept()
                        val commThread = CommunicationThread(socket)
                        Thread(commThread).start()
                    }
                } catch (e: IOException) {
                    Log.d("ServerThread", "----: issues  ")
                    Thread.currentThread().interrupt()
                    e.printStackTrace()
                }
            }
        }
    }

    // Handles received messages from clients
    internal inner class CommunicationThread(private val clientSocket: Socket?) : Runnable {
        private var input: BufferedReader? = null
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val read = input!!.readLine()
                    updateConversationHandler?.post(UpdateUIThread(read))
                    Log.d("CommunicationThread", "run : message $read")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.d("CommunicationThread", "----: issues ")
                }
            }
        }

        init {
            try {
                // read received data
                input = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("CommunicationThread", "----: issues  ")
            }
        }
    }

    // Handles showing received messages on screen
    internal inner class UpdateUIThread(private val msg: String) : Runnable {
        // Print message on screen
        override fun run() {
            val text = receivedText?.text
            val message = msg.trimIndent()
            val value = "$text Incoming message: $message"
            receivedText?.text = value
        }
    }

    // Handles connection to server
    internal inner class ClientThread : Runnable {
        override fun run() {
            try {
                val serverAddress = InetAddress.getByName(SERVER_IP)
                socket = Socket(serverAddress, SERVERPORT)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
                Log.d("ClientThread", "----: issues   ")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("ClientThread", "----: issues  ")
            }
        }
    }

    companion object {
        const val SERVERPORT = 6000
        private var SERVER_IP: String? = null
    }
}