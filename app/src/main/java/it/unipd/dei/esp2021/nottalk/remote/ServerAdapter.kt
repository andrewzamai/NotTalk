package it.unipd.dei.esp2021.nottalk.remote

import it.unipd.dei.esp2021.nottalk.database.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.net.HttpURLConnection
import java.util.stream.Collectors


class ServerAdapter {
    private val url = "https://embedded-chat-server.herokuapp.com/"
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
        return JSONObject(requestBody)
    }

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

    fun sendTextMsg(username: String, uuid: String, toUser: String, date: String, text: String): String{
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

    fun sendFileMsg(username: String, uuid: String, toUser: String, date: String,
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

        val jsonResponse = sendJson(sendMsgUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")) return status
        val error = jsonResponse.getString("error")
        if(error.equals("invalid client username")
            or error.equals("not logged in")
            or error.equals("Recipient user id invalid")) return error
        throw Exception(error)
    }

    fun checkMsg(username: String, uuid: String): List<Message>{
        val json = JSONObject()
        json.put("username", username)
        json.put("uuid", uuid)

        val jsonResponse = sendJson(checkMsgUrl,json)
        val status = jsonResponse.getString("status")
        if(status.equals("ok")){
            val list = mutableListOf<Message>()
            val size = jsonResponse.getInt("length")
            if(size==0) return list
            val messages = jsonResponse.getJSONArray("messages")
            for(i in 0..size){
                val jsonmsg = messages.getJSONObject(i)
                val msg = Message(username=jsonmsg.getString("fromuser"),
                    date= jsonmsg.getString("date"),
                    type=jsonmsg.getString("type"),
                    text=jsonmsg.getString("content"))
                list.add(msg)
            }
            return list
        }
        throw Exception(jsonResponse.getString("error"))
    }

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