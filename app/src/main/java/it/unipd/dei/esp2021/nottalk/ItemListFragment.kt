package it.unipd.dei.esp2021.nottalk

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import it.unipd.dei.esp2021.nottalk.database.Message
import it.unipd.dei.esp2021.nottalk.database.User
import it.unipd.dei.esp2021.nottalk.databinding.FragmentItemListBinding
import it.unipd.dei.esp2021.nottalk.databinding.ItemListContentBinding
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import java.util.concurrent.Executors


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
    private lateinit var floatingAddUserButton: FloatingActionButton

    // reference to the (singleton) notTalkRepository instance
    private val repository: NotTalkRepository = NotTalkRepository.get()

    // reference to adapter, initially emptyList() then populated in OnViewCreated
    private var adapter: UserAdapter? = UserAdapter(emptyList())

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    // Lazy initialization of a UserListViewModel instance, will be instantiated only when needed for the first time
    private val userListViewModel: UserListViewModel by lazy {
        ViewModelProvider(this).get(UserListViewModel::class.java) //ViewModelProvider retrieves the already instantiated view if present (viewModel survives configuration changes as rotation)
    }

    // right-side fragment in Tablet layout configuration, if null we are on a hand-set device
    private var itemDetailFragmentContainer: View? = null


// Overriding fragment lifecycle methods

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        usersRecyclerView = binding.itemList // gets a reference to RecyclerView widget declared in fragment_item_list.xml
        usersRecyclerView.layoutManager = LinearLayoutManager(context) // assign a LayoutManager
        usersRecyclerView.adapter = adapter // pass an adapter (initially emptyList)

        floatingAddUserButton = binding.adduserButton

        // manually insert some users in the database (gives error if already present)
        //if(userListViewModel.userListLiveData.value.isNullOrEmpty()) {
            //userListViewModel.insertUser(User("Andrew"))
            //userListViewModel.insertUser(User("Filippo"))
            //userListViewModel.insertUser(User("Alessandro"))
            //userListViewModel.insertUser(User("Daniele"))
        //}

        //userListViewModel.insertUser(User("Gianni"))

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

    override fun onStart() {
        super.onStart()
        floatingAddUserButton.setOnClickListener{
            //Toast.makeText(context, "FAB pressed", Toast.LENGTH_LONG).show()
            val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
            val alert = AlertDialog.Builder(context)
            alert.setTitle("Add Conversation")
            alert.setMessage("Insert username")
            val input = EditText(context)
            alert.setView(input)
            alert.setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, whichButton ->
                // Do something with value!
                backgroundExecutor.execute{
                    val repo = NotTalkRepository.get()
                    val sa = ServerAdapter()
                    val username = input.text.toString()
                    val result = sa.checkUser(username)
                    val doesExist = repo.checkUser(username)
                    context?.mainExecutor?.execute{
                        if(!result){
                            Toast.makeText(context, "User does not exist", Toast.LENGTH_LONG).show()
                        }
                        else if(result){
                            if(doesExist){
                                Toast.makeText(context, "User updated", Toast.LENGTH_LONG).show()
                            }
                            else repo.insertUser(username)
                            repo.createRelation(username)
                        }
                    }
                }

            })

            alert.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, whichButton ->
                    // Canceled.
                })

            alert.show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // inner classes UserHolder and UserAdapter for UserRecyclerView

    private inner class UserHolder(binding: ItemListContentBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        private lateinit var user: User // stores a reference to the User object
        private val username: TextView = binding.idUser //stores reference to textview field
        private val picture: ImageView = binding.userImage
        private val countText: TextView = binding.count
        private val msgText: TextView = binding.lastMsgText
        private val msgDate: TextView = binding.lastMsgDate

        private lateinit var msgCount: LiveData<Int>
        private lateinit var lastMsg: LiveData<Message>
        private var thisUsername: String

        init{
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
            thisUsername = context?.getSharedPreferences("notTalkPref", MODE_PRIVATE)?.getString("thisUsername", "")?:""
        }

        fun bind(user: User){
            countText.visibility = View.INVISIBLE
            this.user = user
            username.text = this.user.username
            val bArray = this.user.picture
            if(bArray!=null) {
                val bitmap = BitmapFactory.decodeByteArray(bArray, 0, bArray.size)
                picture.setImageBitmap(bitmap)
            }
            else picture.setImageResource(R.drawable.ic_avatar)
            Thread(Runnable{
                msgCount = repository.getUnreadCount(thisUsername!!,user.username)
                lastMsg = repository.getLast(thisUsername!!,user.username)
                activity?.mainExecutor?.execute {
                    msgCount.observe(viewLifecycleOwner, { count ->
                        if (count > 0) {
                            countText.text = count.toString()
                            countText.visibility = View.VISIBLE
                        } else countText.visibility = View.INVISIBLE
                    })
                    lastMsg.observe(viewLifecycleOwner, { msg ->
                        if(msg==null){
                            msgDate.text=""
                            msgText.text=""
                            return@observe
                        }
                        val type = msg.type
                        var text = ""
                        if(msg.fromUser==thisUsername!!) text+="You: "
                        if(type=="text") text+=msg.text
                        if(type=="file"){
                            text += when(msg.mimeType!!.split("/")[0]){
                                "image"-> "\uD83D\uDCF7 Image"
                                "audio"-> "\uD83C\uDFA7 Audio"
                                "video"-> "\uD83C\uDFA5 Video"
                                else -> "\uD83D\uDCCB File"
                            }
                        }
                        var sDate = ""
                        val date = Date(msg.date)
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY,0)
                        cal.set(Calendar.MINUTE,0)
                        cal.set(Calendar.SECOND,0)
                        cal.set(Calendar.MILLISECOND,0)
                        sDate = if(msg.date>=cal.timeInMillis){
                            val simpleDateFormat = SimpleDateFormat("HH:mm")
                            simpleDateFormat.format(date)
                        } else{
                            val simpleDateFormat = SimpleDateFormat("dd/MM/yy")
                            simpleDateFormat.format(date)
                        }
                        msgText.text=text
                        msgDate.text=sDate
                    })
                }

            }).start()
        }

        override fun onClick(v: View?) {

            // uses a bundle to communicate between listFragment and detailFragment
            // the selected username is saved in the bundle and passed as argument to the detailFragment
            val bundle = Bundle()
            bundle.putString(
                ItemDetailFragment.ARG_ITEM_ID,
                user.username
            )
            
            if (itemDetailFragmentContainer != null) {
                // tablet layout
                itemDetailFragmentContainer!!.findNavController().navigate(R.id.fragment_item_detail, bundle)
            } else {
                // hand-set device layout
                // show_item_detail is the action ID that navigates from list fragment to detail fragment
                itemView.findNavController().navigate(R.id.show_item_detail, bundle)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val alert = AlertDialog.Builder(context)
            alert.setTitle("Delete chat")
            alert.setMessage("Are you sure to delete chat with ${username.text}?")
            alert.setPositiveButton("Yes") { _, _ ->
                NotTalkRepository.get().removeRelation(username.text.toString())
            }
            alert.setNegativeButton("No") { _, _ -> }
            alert.show()
            return true
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