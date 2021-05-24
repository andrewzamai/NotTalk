package it.unipd.dei.esp2021.nottalk.util

import android.app.Activity
import android.app.Application
import android.content.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64.DEFAULT
import java.io.OutputStream


class FileManager: Application() {
    companion object{
        const val PICK_FILE = 2
        const val PICK_IMAGE = 3
        const val PICK_VIDEO = 4
        const val PICK_AUDIO = 5

        fun saveFileToStorage(context: Context, file: String, filename: String, mimeType: String): String{
            val contentResolver = context.contentResolver
            val fileOutStream: OutputStream
            val type = mimeType.split("/")[0]
            var directory: String
            var mediaContentUri: Uri
            var values: ContentValues
            if(type=="image") {
                directory = Environment.DIRECTORY_PICTURES+"/NotTalk/"
                mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, directory)
                }
            }
            else if(type=="video"){
                directory = Environment.DIRECTORY_MOVIES+"/NotTalk/"
                mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Video.Media.RELATIVE_PATH, directory)
                }
            }
            else if(type=="audio"){
                directory = Environment.DIRECTORY_MUSIC+"/NotTalk/"
                mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, directory)
                }
            }
            else{
                directory = Environment.DIRECTORY_DOWNLOADS+"/NotTalk/"
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

        fun pickFileFromStorage(activity: Activity, code: Int): Intent{
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = when(code){
                    PICK_IMAGE -> "image/*"
                    PICK_VIDEO -> "video/*"
                    PICK_AUDIO -> "audio/*"
                    else -> "*/*"
                }
                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                //putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }
    }
}