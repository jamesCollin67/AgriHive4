package com.example.agrihive.addapiary

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ApiaryRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val apiaryRef = db.child("apiaries")
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    fun addApiary(apiary: Apiary, onResult: (Boolean) -> Unit) {
        val id = apiaryRef.push().key ?: return

        val newApiary = apiary.copy(
            id = id,
            ownerId = uid ?: ""
        )

        apiaryRef.child(id)
            .setValue(newApiary)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun listenApiaries(callback: (List<Apiary>) -> Unit) {

        apiaryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val list = mutableListOf<Apiary>()

                for (child in snapshot.children) {
                    val apiary = child.getValue(Apiary::class.java)
                    if (apiary?.ownerId == uid) {
                        list.add(apiary)
                    }
                }
                callback(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
