package it.unipd.dei.esp2021.nottalk

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.databinding.FragmentItemListBinding
import it.unipd.dei.esp2021.nottalk.databinding.ItemListContentBinding

/**
 * A Fragment representing a list of chats. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of chats, which, when one is pressed,
 * leads to a {@link ItemDetailFragment} representing
 * the touched chat details (list of messages with that user).
 * On larger screens, the Navigation controller presents the list of chats and
 * single chat details (chat messages) side-by-side using two vertical panes.
 */

class ItemListFragment : Fragment() {

    // reference to this fragment view will be get in OnCreateView()
    private var _binding: FragmentItemListBinding? = null

    // reference to recyclerview
    private lateinit var usersRecyclerView: RecyclerView

    // reference to adapter, initially emptyList() then populated in OnViewCreated
    private var adapter: UserAdapter? = UserAdapter(emptyList())

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Lazy initizialization of a UserListViewModel instance
    private val userListViewModel: UserListViewModel by lazy {
        ViewModelProvider(this).get(UserListViewModel::class.java)
    }

    // right fragment in Tablet layout configuration, if null we are on a hand-set device
    private var itemDetailFragmentContainer: View? = null


// Overriding fragment lifecycle methods

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        usersRecyclerView = binding.itemList // gets a reference to RecyclerView widget declared in fragment_item_list.xml
        usersRecyclerView.layoutManager = LinearLayoutManager(context) // assign a LayoutManager
        //usersRecyclerView.adapter = adapter // pass an adapter (initially emptyList)

        // insert some users in the database
    /*
        userListViewModel.insertUser(User("Andrew"))
        userListViewModel.insertUser(User("Filippo"))
        userListViewModel.insertUser(User("Alessandro"))
        userListViewModel.insertUser(User("Daniele"))
    */

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * item_detail_nav_container is the id associated with the (detail)fragment contained in fragment_item_list (sw600dp)
         * This xml file for >=7 inches tablets shows list and details fragments side by side;
         * fragmen_item_list for hand-set devices does not contain this additional fragment.
         * Being the OS to choose which layout to use based on device size, if the findViewById fails (return null) means the app is running on a handset device.
         * (See null check in onClickListener below)
        */
        itemDetailFragmentContainer = view.findViewById(R.id.item_detail_nav_container)

        /**
         * Click Listener to trigger navigation based on if you have
         * a single pane layout or two pane layout
         */
        /*
        val onClickListener = View.OnClickListener { itemView ->
            // item selected is passed to the destination fragment using a Bundle object:
            // ARG_ITEM_ID is a constant in companion object in ItemDetailFragment
            // creates pair ARG_ITEM_ID-idItemPressed and saves it in a bundle passed in navigate method
            // the destination fragment will check in bundle if there is ARG_ITEM_ID key
            /*
            val item = itemView.tag as PlaceholderContent.PlaceholderItem //PlaceHolder numbers to replace
            val bundle = Bundle()
            bundle.putString(
                ItemDetailFragment.ARG_ITEM_ID,
                item.id
            )
            */
            // if not null (side by side fragments) retrieves NavController associated with ItemDetailFragment (NavController in HostActivity) and navigates to fragment_item_detail (sub_nav_graph.xml)
            if (itemDetailFragmentContainer != null) {
                itemDetailFragmentContainer!!.findNavController()
                    .navigate(R.id.fragment_item_detail, Bundle())
            } else {
                // show_item_detail is the action ID that navigates from list fragment to detail fragment
                itemView.findNavController().navigate(R.id.show_item_detail, Bundle())
            }
        }
        */

        // tells the UserListViewModel to observe for changes in userListLiveData, refresh the recycler view if changed
        userListViewModel.userListLiveData.observe(
            viewLifecycleOwner,
            Observer { users ->
                users?.let {
                    updateUI(users)
                }
            }
        )

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // inner classes UserHolder and UserAdapter for UserRecyclerView

    private inner class UserHolder(binding: ItemListContentBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var user: User // stores a reference to the User object
        private val username: TextView = binding.idUser //stores reference to textview field

        init{
            itemView.setOnClickListener(this)
        }

        fun bind(user: User){
            this.user = user
            username.text = this.user.username
        }

        override fun onClick(v: View?) {
            Toast.makeText(context, "${user.username} pressed", Toast.LENGTH_SHORT).show()

            if (itemDetailFragmentContainer != null) {
                itemDetailFragmentContainer!!.findNavController().navigate(R.id.fragment_item_detail, Bundle())
            } else {
                // show_item_detail is the action ID that navigates from list fragment to detail fragment
                itemView.findNavController().navigate(R.id.show_item_detail, Bundle())
            }
        }
    }

    // takes a list of users and populates the recycler
    private inner class UserAdapter(var users: List<User>) : RecyclerView.Adapter<UserHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
            val binding = ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserHolder(binding)
        }

        override fun onBindViewHolder(holder: UserHolder, position: Int) {
            val item = users[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int {
            return users.size
        }

    }

    // called in OnViewCreated to refresh the recycler list
    private fun updateUI(users: List<User>){
        adapter = UserAdapter(users)
        usersRecyclerView.adapter = adapter
    }





}