package it.unipd.dei.esp2021.nottalk

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import it.unipd.dei.esp2021.nottalk.placeholder.PlaceholderContent;
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

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // gets a reference to RecyclerView widget declared in fragment_item_list.xml
        val recyclerView: RecyclerView = binding.itemList

        /**
         * item_detail_nav_container is the id associated with the (detail)fragment contained in fragment_item_list (sw600dp)
         * This xml file for >=7 inches tablets shows list and details fragments side by side;
         * fragmen_item_list for hand-set devices does not contain this additional fragment.
         * Being the OS to choose which layout to use based on device size, if the findViewById fails (return null) means the app is running on a handset device.
         * (See null check in onClickListener below)
        */
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        /**
         * Click Listener to trigger navigation based on if you have
         * a single pane layout or two pane layout
         */
        val onClickListener = View.OnClickListener { itemView ->
            // item selected is passed to the destination fragment using a Bundle object:
            // ARG_ITEM_ID is a constant in companion object in ItemDetailFragment
            // creates pair ARG_ITEM_ID-idItemPressed and saves it in a bundle passed in navigate method
            // the destination fragment will check in bundle if there is ARG_ITEM_ID key
            val item = itemView.tag as PlaceholderContent.PlaceholderItem //PlaceHolder numbers to replace
            val bundle = Bundle()
            bundle.putString(
                ItemDetailFragment.ARG_ITEM_ID,
                item.id
            )
            // if not null (side by side fragments) retrieves NavController associated with ItemDetailFragment (NavController in HostActivity) and navigates to fragment_item_detail (sub_nav_graph.xml)
            if (itemDetailFragmentContainer != null) {
                itemDetailFragmentContainer.findNavController()
                    .navigate(R.id.fragment_item_detail, bundle)
            } else {
                // show_item_detail is the action ID that navigates from list fragment to detail fragment
                itemView.findNavController().navigate(R.id.show_item_detail, bundle)
            }
        }


        /**
         * instantiates ViewHolder and Adapter and passes lambda onClickListener defined above to handle clicks/navigation
         */
        setupRecyclerView(recyclerView, onClickListener)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun setupRecyclerView(recyclerView: RecyclerView,
                                  onClickListener: View.OnClickListener) {
        recyclerView.adapter = SimpleItemRecyclerViewAdapter(PlaceholderContent.ITEMS, onClickListener)
    }


    class SimpleItemRecyclerViewAdapter(private val values: List<PlaceholderContent.PlaceholderItem>,
                                        private val onClickListener: View.OnClickListener) : RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val binding = ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id
            holder.contentView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        /**
         * ViewHolder class that extends from Recycler.ViewHolder to wrap an item view: stores a reference to the item view and some widgets within the view.
         */
        inner class ViewHolder(binding: ItemListContentBinding) : RecyclerView.ViewHolder(binding.root) {
            // gets references to the two textView within the item_list_content.xml
            val idView: TextView = binding.idText
            val contentView: TextView = binding.content
        }

    }




}