package com.example.agrihive.addapiary

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AddApiaryViewModel : ViewModel() {

    private val repository = ApiaryRepository()

    private val _addStatus = MutableLiveData<Boolean>()
    val addStatus: LiveData<Boolean> = _addStatus

    fun addApiary(
        name: String,
        temp: String,
        hum: String,
        weight: String,
        isActive: Boolean
    ) {

        if (name.isEmpty()) {
            _addStatus.value = false
            return
        }

        val apiary = Apiary(
            name = name,
            temperature = temp,
            humidity = hum,
            weight = weight,
            isActive = isActive
        )

        repository.addApiary(apiary) {
            _addStatus.postValue(it)
        }
    }
}
