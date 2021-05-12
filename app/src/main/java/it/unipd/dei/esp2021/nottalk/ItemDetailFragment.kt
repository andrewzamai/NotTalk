package it.unipd.dei.esp2021.nottalk

import android.app.ActionBar
import android.app.ActionBar.DISPLAY_SHOW_TITLE
import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.Resources
import android.media.Image
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.databinding.FragmentItemDetailBinding
import it.unipd.dei.esp2021.nottalk.databinding.ItemChatContentBinding
import it.unipd.dei.esp2021.nottalk.databinding.ItemListContentBinding
import java.lang.String.format
import java.text.DateFormat
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
        messageEditText = binding.editText!!
        sendButton = binding.sendButton!!

        // retrieves editText content if in onSaveInstanceState
        // could have done it in onCreate but messageEditText reference was not yet get
        val messageText = savedInstanceState?.getString(KEY_MESSAGE).toString()
        messageEditText.setText(messageText)

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
        var activity = activity as ItemDetailHostActivity
        activity.setToolBarTitle(otherUsername)
    }

    override fun onStart() {
        super.onStart()

        val messageWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
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

            //TODO: salvare in persistentstate contenuto edit test nel caso di rotazione mentre sto scrivendo
        }
    }

    // when detached sets toolbar title to "chats" (simple navigation forward/backwards)
    override fun onDetach() {
        super.onDetach()

        var activity = activity as ItemDetailHostActivity
        activity.setToolBarTitle(getString(R.string.toolbar_chatLists))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // saves editText message content in persistentState bundle
        outState.putString(KEY_MESSAGE, messageEditText.text.toString())
    }


    companion object {
        // The fragment argument representing the item ID that this fragment represents.
        const val ARG_ITEM_ID = "item_id"
        private const val KEY_MESSAGE = "textmessage"
    }

    // inner classes MessageHolder and ChatAdapter for ChatRecyclerView

    private inner class MessageHolder(binding: ItemChatContentBinding) : RecyclerView.ViewHolder(binding.root) {
        // TODO: display better item_content_chat layout
        private lateinit var message: Message // stores a reference to the Message object
        private val messageText: TextView = binding.messageSlot //stores reference to textview field
        private val messageSender: TextView = binding.messageSender
        private val messageDate: TextView = binding.messageDate

        fun bind(message: Message){
            this.message = message
            messageText.text = message.text
            if(this.message.fromUser == thisUsername) {
                messageSender.text = "You" //TODO: change hardcoded string
                //this.messageText.setBackgroundColor(resources.getColor(R.color.teal_200))
                this.itemView.setBackgroundColor(resources.getColor(R.color.teal_200))
            } else{
                messageSender.text = otherUsername
                //this.messageText.setBackgroundColor(resources.getColor(R.color.white))
                this.itemView.setBackgroundColor(resources.getColor(R.color.white))
            }

            messageDate.text = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH).format(this.message.date).toString() //TODO: change in only hours
        }

    }

    // takes a list of messages and populates the recycler
    private inner class ChatAdapter(var messages: List<Message>) : RecyclerView.Adapter<MessageHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
            val binding = ItemChatContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return MessageHolder(binding)
        }

        override fun onBindViewHolder(holder: MessageHolder, position: Int) {
            val item = messages[position]
            holder.bind(item)
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