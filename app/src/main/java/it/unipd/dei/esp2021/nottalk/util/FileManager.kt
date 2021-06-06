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

/**
 * This class provides the methods to get the file from uri using a picker
 * or to save the file to shared storage and get back the relative uri.
 */
class FileManager: Application() {
    companion object{
        const val PICK_FILE = 2
        const val PICK_IMAGE = 3
        const val PICK_VIDEO = 4
        const val PICK_AUDIO = 5

        fun saveFileToStorage(context: Context, file: String, filename: String, mimeType: String): String{
            /**
             * Saves the file to the shared storage using contentResolver
             */
            val contentResolver = context.contentResolver
            val fileOutStream: OutputStream //Stream to write in storage
            val type = mimeType.split("/")[0] //Get the first part of the mimetype
            var directory: String
            var mediaContentUri: Uri
            var values: ContentValues
            //Get the correct uri according to the mimetype of the file
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
                //All other mimetypes are treated as generic files
                directory = Environment.DIRECTORY_DOWNLOADS+"/NotTalk/"
                mediaContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, directory)
                }
            }
            var uri: Uri?
            //Get the stream from the specified uri
            contentResolver.run {
                uri = contentResolver.insert(mediaContentUri, values)
                fileOutStream = openOutputStream(uri!!)?: throw Exception("Errore resolver")
            }
            //fileOutStream.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            //Write in memory decoding it first
            fileOutStream.write(Base64.decode(file,DEFAULT))
            fileOutStream.close()
            return uri.toString()
        }

        fun pickFileFromStorage(activity: Activity, code: Int): Intent{
            /**
             * Return the intent to throw to choose a file from a picker and get the file uri
             */
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