package it.unipd.dei.esp2021.nottalk

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unipd.dei.esp2021.nottalk.database.FileManager
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.databinding.*
import it.unipd.dei.esp2021.nottalk.util.PlayerService
import java.text.SimpleDateFormat
import java.util.*

/**
 * A fragment representing a single chat details: list of messages.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    // reference to Fragment View
    private var _binding: FragmentItemDetailBinding? = null
    // editText view reference
    private lateinit var messageEditText: EditText
    // sendButton view reference
    private lateinit var sendButton: ImageButton
    private lateinit var fileButton: ImageButton

    // reference to the (singleton) notTalkRepository instance
    private val repository: NotTalkRepository = NotTalkRepository.get()

    // since usernames are unique it's possible to re-create a User from its username
    private lateinit var otherUsername: String // receiver (user's chat pressed in listFragment)
    private lateinit var thisUsername: String // sender (this application user)
    private lateinit var uuid: String // uuid of this application user (sender)

    // reference to chat's recyclerView
    private lateinit var chatRecyclerView: RecyclerView
    // reference to adapter, initially emptyList() then populated in OnViewCreated
    private var adapter: ItemDetailFragment.ChatAdapter? = ChatAdapter(emptyList())

    // Lazy initialization of a ChatViewModel instance, uses a ViewModelFactory to retrieve the correct ViewModel instance within ViewModelProvider
    private val chatViewModel: ChatViewModel by lazy {
        ViewModelProvider(this, ChatViewModelFactory(thisUsername, otherUsername)).get(ChatViewModel::class.java)
    }

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!


    // Fragment Lifecycle methods override

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // retrieves thisUsername and uuid from sharedPreferences
        thisUsername = context.getSharedPreferences("notTalkPref", MODE_PRIVATE).getString("thisUsername", "")!!
        uuid = context.getSharedPreferences("notTalkPref", MODE_PRIVATE).getString("uuid", "")!!
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // returns the arguments supplied when the fragment was instantiated, if any
        arguments?.let {
            //checks if the bundle passed in navigate contains ARG_ITEM_ID key (this fragment constant)
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the username content specified by the fragment arguments.
                otherUsername = it.getString(ARG_ITEM_ID).toString()
            }
        }

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        chatRecyclerView = binding.chatList // gets a reference to RecyclerView widget declared in fragment_item_list.xml
        val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.reverseLayout = true // populates recycler view from bottom (messages are ordered in de-crescent order of date)
        chatRecyclerView.layoutManager = linearLayoutManager // assign a LayoutManager
        chatRecyclerView.adapter = adapter // emptylist initially passed
        val rootView = binding.root

        Log.d("ItemDetailFragment", "ItemDetailFragment Created and Inflated, Otherusername: ${otherUsername}")

        // TODO: Modificare layout tablet
        messageEditText = binding.editText
        sendButton = binding.sendButton
        fileButton = binding.fileButton

        // retrieves editText content if in onSaveInstanceState
        // could have done it in onCreate but messageEditText reference was not yet get
        //val messageText = savedInstanceState?.getString(KEY_MESSAGE).toString()
        //if (messageText != "null") messageEditText.setText(messageText)

        return rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatViewModel.chatListLiveData.observe(
            viewLifecycleOwner,
            Observer { messages ->
                messages?.let {
                    updateUI(messages)
                }
            }
        )
    }

    // to set toolbar title as otherUsername, needs to retrieve a reference to toolbar from ItemDetailHostActivity
    // after a configuration change as rotation this is the right to retrieve this fragment parent activity and it's toolbar reference
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity as ItemDetailHostActivity
        activity.setToolBarTitle(otherUsername)
        // retrieves messageDraft of this chat, if any, and sets it in messageEditText
        val messageDraft = activity.getMessageDraft(otherUsername)
        if (messageDraft != null) messageEditText.setText(messageDraft)
    }

    override fun onStart() {
        super.onStart()

        val messageWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // saves messageDraft in map passed to ItemDetailsHostActivity savedInstanceState bundle
                val activity = activity as ItemDetailHostActivity
                val message = messageEditText.text
                if (message!=null) {
                    activity.saveMessageDraft(otherUsername, messageEditText.text.toString())
                }
            }
        }
        messageEditText.addTextChangedListener(messageWatcher)

        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString()
            // doesn't send message if empty
            if(messageText.isNotEmpty()){
                // Log.d("sendButton", messageText)
                repository.sendTextMessage(thisUsername, uuid, messageText, otherUsername)
            }
            // clears the editText
            messageEditText.text.clear()

        }

        fileButton.setOnClickListener { view ->
            val popUp = PopupMenu(requireContext(),view)
            popUp.setOnMenuItemClickListener { item ->
                var intent: Intent? = null
                var code: Int? = null
                when(item.itemId){
                    R.id.popup_image -> activity?.let { code=FileManager.PICK_IMAGE; intent=FileManager.pickFileFromStorage(it,code!!) }
                    R.id.popup_video -> activity?.let { code=FileManager.PICK_VIDEO; intent=FileManager.pickFileFromStorage(it,code!!) }
                    R.id.popup_audio -> activity?.let { code=FileManager.PICK_AUDIO; intent=FileManager.pickFileFromStorage(it,code!!) }
                    R.id.popup_file -> activity?.let { code=FileManager.PICK_FILE; intent=FileManager.pickFileFromStorage(it,code!!) }
                    else -> return@setOnMenuItemClickListener false
                }
                startActivityForResult(intent, code!!, null)
                true
            }
            popUp.inflate(R.menu.file_popup_menu)
            popUp.show()
        }
    }

    // when detached sets toolbar title to "chats" (simple navigation forward/backwards)
    override fun onDetach() {
        super.onDetach()

        val activity = activity as ItemDetailHostActivity
        activity.setToolBarTitle(getString(R.string.toolbar_chatLists))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // saves editText message content in persistentState bundle

        //outState.putString(KEY_MESSAGE, messageEditText.text.toString()) //TODO: crash lateinit property messageEditText has not been initialized
        val activity = activity as ItemDetailHostActivity
        val message = messageEditText.text
        if (message!=null) {
            activity.saveMessageDraft(otherUsername, messageEditText.text.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if( requestCode == FileManager.PICK_IMAGE||
                requestCode == FileManager.PICK_VIDEO||
                requestCode == FileManager.PICK_AUDIO||
                requestCode == FileManager.PICK_FILE  ){
                data?.data?.also { uri ->
                    repository.sendFileMessage(requireContext(),uri,thisUsername,uuid,otherUsername)
                    // Perform operations on the document using its URI.
                }
            }
        }
    }


    companion object {
        // The fragment argument representing the item ID that this fragment represents.
        const val ARG_ITEM_ID = "item_id" // to pass otherUsername as argument from itemList fragment to this fragment

        // constants used to inflate correct layout in chatRecycler view depending on content type and mimeType
        const val TEXT_MESSAGE = 2
        const val IMAGE_MESSAGE = 4
        const val AUDIO_MESSAGE = 6
    }

    // inner classes MessageHolder and ChatAdapter for ChatRecyclerView

    private inner class MessageHolderText(binding: ItemChatContentBinding) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var message: Message // stores a reference to the Message object
        private val messageText: TextView = binding.messageSlot //stores reference to textview field
        private val messageSender: TextView = binding.messageSender
        private val messageDate: TextView = binding.messageDate

        fun bind(message: Message){
            this.message = message

            if(this.message.fromUser == thisUsername) {
                messageSender.text = "You" //TODO: change hardcoded string
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.outgoing_message_container) }
                //messageText.textAlignment = View.TEXT_ALIGNMENT_VIEW_END //TODO: justify right
            } else{
                messageSender.text = otherUsername
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.incoming_message_container) }
            }

            val currentDate = Date(this.message.date)
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm")
            messageDate.text = simpleDateFormat.format(currentDate)

            messageText.text = message.text

        }

    }

    private inner class MessageHolderImage(binding: ImageItemChatContentBinding) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var message: Message // stores a reference to the Message object
        private val messageSender: TextView = binding.messageSender
        private val messageDate: TextView = binding.messageDate
        private val image: ImageView = binding.image

        fun bind(message: Message){
            this.message = message

            if(this.message.fromUser == thisUsername) {
                messageSender.text = "You" //TODO: change hardcoded string
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.outgoing_message_container) }
                //messageText.textAlignment = View.TEXT_ALIGNMENT_VIEW_END //TODO: justify right
            } else{
                messageSender.text = otherUsername
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.incoming_message_container) }
            }

            val currentDate = Date(this.message.date)
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm")
            messageDate.text = simpleDateFormat.format(currentDate)

            image.adjustViewBounds = true
            image.maxHeight = 500
            image.maxWidth = 500
            image.setImageURI(Uri.parse(message.text))
            Log.d("UsernameImageBug", message.fromUser)

        }

    }


    private inner class MessageHolderAudio(binding: AudioItemChatContentBinding) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var message: Message // stores a reference to the Message object
        private val messageSender: TextView = binding.messageSender
        private val messageDate: TextView = binding.messageDate
        private val playButton: ImageButton = binding.playButton
        private val stopButton: ImageButton = binding.stopButton

        fun bind(message: Message){
            this.message = message

            if(this.message.fromUser == thisUsername) {
                messageSender.text = "You" //TODO: change hardcoded string
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.outgoing_message_container) }
                //messageText.textAlignment = View.TEXT_ALIGNMENT_VIEW_END //TODO: justify right
            } else{
                messageSender.text = otherUsername
                this.itemView.background = context?.let { ContextCompat.getDrawable(it, R.drawable.incoming_message_container) }
            }

            val currentDate = Date(this.message.date)
            val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm")
            messageDate.text = simpleDateFormat.format(currentDate)
        }

        init{
            playButton.setOnClickListener {
                Log.d("PlayService", "Play Button pressed")
                Log.d("PlayService", message.text)
                val intent = Intent(activity!!.applicationContext, PlayerService::class.java)
                intent.putExtra(PlayerService.PLAY_START, true)
                intent.putExtra(PlayerService.URI_PATH, message.text)
                activity!!.startService(intent)
            }

            stopButton.setOnClickListener {
                Log.d("PlayService", "Stop Button pressed")
                val intent = Intent(activity!!.applicationContext, PlayerService::class.java)
                activity!!.applicationContext.stopService(intent)
            }

        }

    }


    // takes a list of messages and populates the recycler
    private inner class ChatAdapter(var messages: List<Message>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {

            if (messages[position].type == "text") {
                return TEXT_MESSAGE
            }
            else if (messages[position].type == "file") {
                if (messages[position].mimeType!!.split("/")[0] == "image")
                    return IMAGE_MESSAGE
                else if (messages[position].mimeType!!.split("/")[0] == "audio")
                    return AUDIO_MESSAGE
            }

            return TEXT_MESSAGE // default
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            when (viewType) {

                TEXT_MESSAGE -> {
                    val binding = ItemChatContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return MessageHolderText(binding)
                }
                IMAGE_MESSAGE -> {
                    val binding = ImageItemChatContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return MessageHolderImage(binding)
                }
                else -> {
                    val binding = AudioItemChatContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return MessageHolderAudio(binding)
                }
            }

        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            when (getItemViewType(position)) {

                TEXT_MESSAGE -> {
                    val holderText = holder as MessageHolderText
                    holderText.bind(messages[position])
                }
                IMAGE_MESSAGE -> {
                    val holderImage = holder as MessageHolderImage
                    holderImage.bind(messages[position])
                }
                else -> {
                    val holderAudio = holder as MessageHolderAudio
                    holderAudio.bind(messages[position])
                }
            }
        }

        override fun getItemCount(): Int {
            return messages.size
        }

    }


    // called in OnViewCreated to refresh the recycler list
    private fun updateUI(messages: List<Message>){
        adapter = ChatAdapter(messages)
        chatRecyclerView.adapter = adapter
    }


}