package com.example.tasklly.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.tasklly.R
import com.example.tasklly.ui.auth.LoginActivity
import com.example.tasklly.ui.common.archive.ArchiveActivity
import com.example.tasklly.ui.common.profile.EditProfileActivity
import com.example.tasklly.ui.common.settings.ChangePasswordActivity
import com.example.tasklly.ui.common.settings.NotificationsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class ClientProfileFragment : Fragment(R.layout.fragment_profile_client) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val storage by lazy { FirebaseStorage.getInstance().reference }

    private lateinit var imgAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvRole: TextView

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                uploadAvatar(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatar = view.findViewById(R.id.imgAvatar)
        tvName = view.findViewById(R.id.tvName)
        tvRole = view.findViewById(R.id.tvRole)

        val btnChangePhoto = view.findViewById<ImageView>(R.id.btnChangePhoto)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        val btnArchive = view.findViewById<Button>(R.id.btnArchive)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val optNotif = view.findViewById<View>(R.id.optNotifications)
        val optPass = view.findViewById<View>(R.id.optChangePassword)

        tvRole.text = "Client"

        val uid = auth.currentUser?.uid ?: return

        // Click avatar / icon => pick image
        imgAvatar.setOnClickListener { pickImage.launch("image/*") }
        btnChangePhoto.setOnClickListener { pickImage.launch("image/*") }

        // Read profile data (name + photoUrl) realtime
        db.child("users").child(uid).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)

                tvName.text = name

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(imgAvatar)
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnArchive.setOnClickListener {
            startActivity(Intent(requireContext(), ArchiveActivity::class.java).putExtra("mode", "client"))
        }

        optNotif.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }

        optPass.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        Glide.with(requireContext())
            .load(uri)
            .placeholder(R.drawable.ic_profile_placeholder)
            .into(imgAvatar)

        val ref = storage.child("avatars/$uid.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    db.child("users").child(uid).child("photoUrl").setValue(downloadUri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Profile photo updated ✅", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to save photo url", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
