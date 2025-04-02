package com.sba.notes

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import com.ncorti.slidetoact.SlideToActView
import com.sba.notes.database.NotesViewModel
import com.sba.notes.databinding.FragmentAllNotesBinding
import kotlin.properties.Delegates

class AllNotesFragment : Fragment(), MenuProvider {

    private var _binding: FragmentAllNotesBinding? = null
    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!
    
    private lateinit var notesViewModel: NotesViewModel
    private val sharedPrefKey = "appSettings"
    private val nightModeKey = "NightMode"
    lateinit var appPref: SharedPreferences
    lateinit var sharedPrefsEdit: SharedPreferences.Editor
    var nightModeStatus by Delegates.notNull<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup menu provider using the modern pattern
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        appPref = requireActivity().getSharedPreferences(sharedPrefKey, 0)
        nightModeStatus = appPref.getInt(nightModeKey, 3)

        setTheme(nightModeStatus)

        // Sync with files when the fragment is created (app opened)
        notesViewModel.syncFromFiles()
        Log.d("AllNotesFragment", "Syncing notes from files")

        val adapter = NotesAdapter()
        binding.noteRecycler.adapter = adapter
        binding.noteRecycler.setHasFixedSize(true)
        binding.noteRecycler.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        notesViewModel.allNotes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
        }

        binding.newNoteFAB.setOnClickListener {
            findNavController().navigate(R.id.action_allNotesFragment_to_editNoteFragment)
        }

        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val mNote = adapter.getNote(position)
                notesViewModel.deleteNote(mNote)

                Snackbar.make(view, "Note Deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        notesViewModel.insertNote(mNote)
                    }.show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.noteRecycler)
    }

    override fun onResume() {
        super.onResume()
        // You can also sync when fragment resumes (user returns to the app)
        notesViewModel.syncFromFiles()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setTheme(nightStatus: Int) {
        when (nightStatus) {
            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Log.d("AllNoteFrag","Light theme SetTheme()")
            }
            2 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Log.d("AllNoteFrag","Dark theme SetTheme()")
            }
            else -> {
                Log.d("AllNoteFrag","System theme SetTheme()")
                // MODE_NIGHT_FOLLOW_SYSTEM is now preferred for all API levels
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun setThemeDialog() {
        val inflater = LayoutInflater.from(requireActivity())
        val view = inflater.inflate(R.layout.alert_dialog_theme_select, null)
        val dialog = AlertDialog.Builder(requireActivity())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val themeRadioGroup = view.findViewById<RadioGroup>(R.id.theme_button_group)

        // Always use "System default" text since MODE_NIGHT_FOLLOW_SYSTEM is now preferred
        view.findViewById<RadioButton>(R.id.deafultRadioButton).text =
            getString(R.string.system_default)

        when (nightModeStatus) {
            1 -> view.findViewById<RadioButton>(R.id.lightRadioButton).isChecked = true
            2 -> view.findViewById<RadioButton>(R.id.darkRadioButton).isChecked = true
            3 -> view.findViewById<RadioButton>(R.id.deafultRadioButton).isChecked = true
        }

        themeRadioGroup.setOnCheckedChangeListener { _, id ->
            sharedPrefsEdit = appPref.edit()
            when (id) {
                R.id.lightRadioButton -> {
                    sharedPrefsEdit.putInt(nightModeKey, 1)
                    sharedPrefsEdit.apply()
                    nightModeStatus = 1
                    Log.d("AllNoteFrag","Light theme")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                R.id.darkRadioButton -> {
                    sharedPrefsEdit.putInt(nightModeKey, 2)
                    sharedPrefsEdit.apply()
                    nightModeStatus = 2
                    Log.d("AllNoteFrag","Dark theme")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }

                R.id.deafultRadioButton -> {
                    sharedPrefsEdit.putInt(nightModeKey, 3)
                    sharedPrefsEdit.apply()
                    nightModeStatus = 3
                    Log.d("AllNoteFrag","System theme")
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
            dialog.dismiss()
        }
    }

    private fun deleteALLDialog() {
        val inflater = LayoutInflater.from(requireActivity())
        val view = inflater.inflate(R.layout.alert_dialog_delete_all, null)
        val slide = view.findViewById<SlideToActView>(R.id.slideConfirm)
        val dialog = AlertDialog.Builder(requireActivity())
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        slide.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(slider: SlideToActView) {
                Log.d("Test", "Deleted")
                deleteALL()
                dialog.dismiss()
            }
        }
    }

    private fun deleteALL() {
        notesViewModel.deleteAllNote()
    }

    // Menu handling using the new MenuProvider interface
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.delete_all_menu -> {
                deleteALLDialog()
                true
            }
            R.id.dark_mode_menu -> {
                setThemeDialog()
                true
            }
            else -> false
        }
    }
}