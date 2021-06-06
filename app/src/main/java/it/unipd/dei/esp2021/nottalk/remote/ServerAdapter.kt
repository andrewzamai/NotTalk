package it.unipd.dei.esp2021.nottalk.remote

import it.unipd.dei.esp2021.nottalk.database.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.net.HttpURLConnection
import java.util.stream.Collectors

/**
 * This class provides the functions to dialogue with the server
 * the requests are all POST http request containing a single JSON file
 * with all the data and authentication.
 */
class ServerAdapter {
    private val url = "https://embedded-chat-server.herokuapp.com/" //Server url
    //Each function has a specific url
    private val checkMsgUrl = URL(url + "check_msg")
    private val sendMsgUrl = URL(url + "send_msg")
    private val loginUrl = URL(url + "login")
    private val createUserUrl = URL(url + "create_user")
    private val checkUserUrl = URL(url + "check_user")
    private val deleteMsgUrl = URL(url + "delete_msg")
    private val deleteUserUrl = URL(url + "delete_user")

    private fun sendJson(url: URL, jsonParam: JSONObject): JSONObject{
        val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        //conn.setChunkedStreamingMode(4e10.toInt())
        //conn.readTimeout = 20000
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        conn.setRequestProperty("Accept", "application/json")
        //conn.setRequestProperty("Accept", "*/*")
        conn.doOutput = true
        conn.doInput = true

        val wr = OutputStreamWriter(conn.outputStream,"UTF-8")
        wr.write(jsonParam.toString())
        wr.flush()
        wr.close()

        //val rc = conn.responseCode
        //val rm = conn.responseMessage
        //Log.i("STATUS", java.lang.String.valueOf(conn.responseCode))
        //Log.i("MSG", conn.responseMessage)

        var encoding : String? = conn.contentEncoding
        encoding = encoding ?: "UTF-8"
        val bf = BufferedReader(InputStreamReader(conn.inputStream,encoding))
        val requestBody = bf.lines().collect(Collectors.joining())
        bf.close()
        conn.disconnect()
        //returns the JSON response file
        return JSONObject(requestBody)
    }

    //Some function requires the session token "uuid" as well as the username of the current user

    /**
     * Create the user passing username and password
     * @return "ok" if successful or "username not available" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun createUser(username: String, password: String): String{
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val jsonResponse = sendJson(createUserUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return jsonResponse.getString("uuid")
        val error = jsonResponse.getString("error")
        if(error.equals("username not available")) return error
        throw Exception(error)
    }

    /**
     * Check if the user exists in remote database
     * @return true if exists or false if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun checkUser(username: String): Boolean{
        val json = JSONObject()
        json.put("username", username)

        val jsonResponse = sendJson(checkUserUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return true
        val error = jsonResponse.getString("error")
        if(error.equals("username does not exist")) return false
        throw Exception(error)

    }

    /**
     * Perform login passing username and password
     * @return "ok" if successful or "username does not exists"/"password incorrect" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun login(username: String, password: String): String {
        val json = JSONObject()
        json.put("username", username)
        json.put("password", password)

        val jsonResponse = sendJson(loginUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return jsonResponse.getString("uuid")
        val error = jsonResponse.getString("error")
        if(error.equals("username does not exist")
            or error.equals("password incorrect")) return error
        throw Exception(error)
    }

    /**
     * Send text message to specified user ("toUser")
     * @return "ok" if successful or "invalid client username"/"not logged in"/"Recipient user id invalid" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun sendTextMsg(username: String, uuid: String, toUser: String, date: Long, text: String): String{
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)
        val msg = JSONObject()
        msg.put("touser", toUser)
        msg.put("date", date)
        msg.put("type", "text")
        msg.put("content", text)
        json.put("msg", msg)

        val jsonResponse = sendJson(sendMsgUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return status
        val error = jsonResponse.getString("error")
        if(error.equals("invalid client username")
            or error.equals("not logged in")
            or error.equals("Recipient user id invalid")) return error
        throw Exception(error)
    }

    /**
     * Send file message to specified user ("toUser")
     * @return "ok" if successful or "invalid client username"/"not logged in"/"Recipient user id invalid" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun sendFileMsg(username: String, uuid: String, toUser: String, date: Long,
                    content: String, mimeType: String, fileName: String): String{
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)
        val msg = JSONObject()
        msg.put("touser", toUser)
        msg.put("date", date)
        msg.put("type", "file")
        msg.put("content", content)
        msg.put("mimetype", mimeType)
        msg.put("filename", fileName)
        json.put("msg", msg)

        val jsonResponse = sendJson(sendMsgUrl, json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return status
        val error = jsonResponse.getString("error")
        if(error.equals("invalid client username")
            or error.equals("not logged in")
            or error.equals("Recipient user id invalid")) return error
        throw Exception(error)
    }

    /**
     * Checks the server for new messages
     * @return A pair of values where .first is the list of messages and .second is the list
     * of the messages ids
     * @throws Exception with a message describing the error that has occurred
     */
    fun checkMsg(username: String, uuid: String): Pair<List<Message>,List<Int>>{
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)

        val jsonResponse = sendJson(checkMsgUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")){
            val list = mutableListOf<Message>()
            val num = mutableListOf<Int>()
            val size = jsonResponse.getInt("length")
            if(size==0) return Pair(list,num)
            val messages = jsonResponse.getJSONArray("messages")
            for(i in 0 until size){
                val jsonmsg = messages.getJSONObject(i)
                val type = jsonmsg.getString("type")
                var msg: Message
                if(type=="text") {
                    msg = Message(
                        toUser = username,
                        fromUser = jsonmsg.getString("fromuser"),
                        date = jsonmsg.getLong("date"),
                        type = type,
                        text = jsonmsg.getString("content")
                    )
                }
                else{
                    msg = Message(
                        toUser = username,
                        fromUser = jsonmsg.getString("fromuser"),
                        date = jsonmsg.getLong("date"),
                        type = type,
                        text = jsonmsg.getString("content"))
                    msg.fileName = jsonmsg.getString("filename")
                    msg.mimeType = jsonmsg.getString("mimetype")

                }
                list.add(msg)
                num.add(jsonmsg.getInt("id"))
            }
            return Pair(list,num)
        }
        throw Exception(jsonResponse.getString("error"))
    }

    /**
     * Deletes the current user and his messages from the server
     * @return "ok" if successful or "username does not exist"/"not logged in" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun deleteUser(username: String, uuid: String): String{
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)

        val jsonResponse = sendJson(deleteUserUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return status
        val error = jsonResponse.getString("error")
        if(error.equals("username does not exist")
            or error.equals("not logged in")) return error
        throw Exception(error)
    }

    /**
     * Deletes the messages in the server database where id is in "ids" List
     * @return "ok" if successful or "invalid client username"/"not logged in" if not
     * @throws Exception with a message describing the error that has occurred
     */
    fun deleteMsg(username: String, uuid: String, ids: List<Int>): String {
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)
        val ja = JSONArray()
        for(i in ids) ja.put(i)
        json.put("ids", ja)

        val jsonResponse = sendJson(deleteMsgUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return status
        val error = jsonResponse.getString("error")
        if(error.equals("invalid client username")
            or error.equals("not logged in")) return error
        throw Exception(error)
    }
}