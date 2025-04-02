package com.sba.notes

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.sba.notes.database.NoteSaveViewModel
import com.sba.notes.database.Notes
import com.sba.notes.databinding.FragmentEditNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class EditNoteFragment : Fragment() {

    private var _binding: FragmentEditNoteBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notesSaveViewModel: NoteSaveViewModel
    private val args: EditNoteFragmentArgs by navArgs()
    private var note: Notes? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        notesSaveViewModel = ViewModelProvider(this)[NoteSaveViewModel::class.java]

        // Get note argument using SafeArgs
        note = args.updateNote
        
        // Setup UI with note data if editing existing note
        note?.let {
            binding.titleEdittext.setText(it.title)
            binding.descEditText.setText(it.description)
            binding.lastEditText.text = setDate(it.date)
        }

        binding.saveFAB.setOnClickListener {
            val title = binding.titleEdittext.text.toString()
            val desc = binding.descEditText.text.toString()
            
            if (title.isBlank() && desc.isBlank()) {
                Toast.makeText(requireContext(), "Title and Note is Empty", Toast.LENGTH_SHORT).show()
            } else {
                val mNote = Notes(title = title, description = desc)
                
                if (note == null) {
                    notesSaveViewModel.insertNote(mNote)
                    Toast.makeText(requireContext(), "Note Saved!!", Toast.LENGTH_SHORT).show()
                } else {
                    mNote.noteId = note!!.noteId
                    mNote.date = System.currentTimeMillis()

                    notesSaveViewModel.updateNote(mNote)
                    Toast.makeText(requireContext(), "Note Updated!!", Toast.LENGTH_SHORT).show()
                }
                
                hideKeyboard(requireActivity())
                findNavController().navigateUp()
            }
        }
    }

    private fun setDate(date: Long): CharSequence {
        val currentMilli = System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        val actualDate = Date(date)
        val currentDate = Date(currentMilli)
        
        return if (sdf.format(currentDate) != sdf.format(actualDate)) {
            "Edited ${sdf.format(actualDate)}"
        } else {
            "Edited ${timeFormat.format(actualDate)}"
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val inputManager = activity
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        activity.currentFocus?.let { focusedView ->
            inputManager.hideSoftInputFromWindow(
                focusedView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}