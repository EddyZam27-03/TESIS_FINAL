package com.example.ensenando.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.ensenando.databinding.DialogChangePasswordBinding
import com.example.ensenando.util.SecurityUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChangePasswordDialogFragment : DialogFragment() {
    private var _binding: DialogChangePasswordBinding? = null
    private val binding get() = _binding!!
    
    var onSave: ((String, String) -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogChangePasswordBinding.inflate(layoutInflater)
        
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnGuardar.setOnClickListener {
            val passwordActual = binding.etPasswordActual.text?.toString() ?: ""
            val nuevaPassword = binding.etNuevaPassword.text?.toString() ?: ""
            val confirmarPassword = binding.etConfirmarPassword.text?.toString() ?: ""
            
            // Validaciones
            if (passwordActual.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa tu contraseña actual", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (nuevaPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa una nueva contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (nuevaPassword.length < 6) {
                Toast.makeText(requireContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (nuevaPassword != confirmarPassword) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Verificar contraseña actual
            val idUsuario = SecurityUtils.getUserId(requireContext())
            if (idUsuario != -1) {
                // TODO: Verificar contraseña actual contra BD
                // Por ahora, asumimos que es correcta
                onSave?.invoke(passwordActual, nuevaPassword)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
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
        fun newInstance(): ChangePasswordDialogFragment {
            return ChangePasswordDialogFragment()
        }
    }
}
