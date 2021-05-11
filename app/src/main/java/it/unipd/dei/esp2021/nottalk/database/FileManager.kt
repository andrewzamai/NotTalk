package it.unipd.dei.esp2021.nottalk.database

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64.DEFAULT
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class FileManager: Application() {
    companion object{
        fun saveFileToStorage(context: Context, file: String, filename: String, mimeType: String): String{
            val contentResolver = context.contentResolver
            val fileOutStream: OutputStream
            val type = mimeType.split("/")[0]
            var directory: String
            var mediaContentUri: Uri
            var values: ContentValues
            if(type=="image") {
                directory = Environment.DIRECTORY_PICTURES+"/provaDB/"
                mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, directory)
                }
            }
            if(type=="video"){
                directory = Environment.DIRECTORY_MOVIES+"/provaDB/"
                mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, directory)
                }
            }
            if(type=="audio"){
                directory = Environment.DIRECTORY_MUSIC+"/provaDB/"
                mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, directory)
                }
            }
            else{
                directory = Environment.DIRECTORY_DOWNLOADS+"/provaDB/"
                mediaContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, directory)
                }
            }
            var uri: Uri?
            contentResolver.run {
                uri = contentResolver.insert(mediaContentUri, values)
                fileOutStream = openOutputStream(uri!!)?: throw Exception("Errore resolver")
            }
            //fileOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            fileOutStream.write(Base64.decode(file,DEFAULT))
            fileOutStream.close()
            return uri.toString()
        }
    }
}