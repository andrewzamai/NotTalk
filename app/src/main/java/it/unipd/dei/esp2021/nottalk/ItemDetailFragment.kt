package it.unipd.dei.esp2021.nottalk

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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

/**
 * A fragment representing a single chat details: list of messages.
 * This fragment is either contained in a [ItemListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null

    // the username of the selected detail chat (since username are unique it's possible to retrieve the User from its username)
    private lateinit var otherUsername: String
    private lateinit var thisUsername: String

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Declaration of a ChatViewModel instance
    private lateinit var chatViewModel: ChatViewModel

    // reference to recyclerview
    private lateinit var chatRecyclerView: RecyclerView

    // reference to adapter, initially emptyList() then populated in OnViewCreated
    private var adapter: ItemDetailFragment.ChatAdapter? = ChatAdapter(emptyList())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // returns the arguments supplied when the fragment was instantiated, if any
        // let notation allows to invoke one or more functions on results of call chains
        arguments?.let {
            //checks if the bundle passed in navigate contains ARG_ITEM_ID key (this fragment constant)
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the placeholder content specified by the fragment
                // arguments.
                otherUsername = it.getString(ARG_ITEM_ID).toString()
            }
        }

        //TODO: crash when rotates, ChatViewModel is re-created on configuration change?
            val parentActivity: ItemDetailHostActivity = activity as ItemDetailHostActivity
            thisUsername = parentActivity.thisUser
            //Log.d("ItemDetailFragment", parentActivity.thisUser)
            chatViewModel = ChatViewModel(thisUsername, otherUsername)

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        chatRecyclerView = binding.chatList!! // gets a reference to RecyclerView widget declared in fragment_item_list.xml
        chatRecyclerView.layoutManager = LinearLayoutManager(context) // assign a LayoutManager
        chatRecyclerView.adapter = adapter
        val rootView = binding.root


        //itemDetailTextView = binding.itemDetail
        // Show the placeholder content as text in a TextView.
        //item?.let {
            //itemDetailTextView.text = it.details
        //}

        Log.d("ItemDetailFragment", "ItemDetailFragment Created, Otherusername: ${otherUsername}")

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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        /**
         * The fragment argument representing the item ID that this fragment represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }

    // inner classes MessageHolder and ChatAdapter for ChatRecyclerView

    private inner class MessageHolder(binding: ItemChatContentBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var message: Message // stores a reference to the Message object
        private val messageText: TextView = binding.messageSlot //stores reference to textview field

        fun bind(message: Message){
            this.message = message
            messageText.text = message.text
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