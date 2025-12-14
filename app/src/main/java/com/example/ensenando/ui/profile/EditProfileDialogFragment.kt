package com.example.ensenando.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.ensenando.databinding.DialogEditProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EditProfileDialogFragment : DialogFragment() {
    private var _binding: DialogEditProfileBinding? = null
    private val binding get() = _binding!!
    
    var onSave: ((String) -> Unit)? = null
    var nombreActual: String = ""
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditProfileBinding.inflate(layoutInflater)
        
        // Pre-llenar con nombre actual
        binding.etNombre.setText(nombreActual)
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnGuardar.setOnClickListener {
            val nuevoNombre = binding.etNombre.text?.toString()?.trim() ?: ""
            
            if (nuevoNombre.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (nuevoNombre.length < 2) {
                Toast.makeText(requireContext(), "El nombre debe tener al menos 2 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            onSave?.invoke(nuevoNombre)
            dismiss()
        }
        
        binding.btnCancelar.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(nombreActual: String): EditProfileDialogFragment {
            return EditProfileDialogFragment().apply {
                this.nombreActual = nombreActual
            }
        }
    }
}
