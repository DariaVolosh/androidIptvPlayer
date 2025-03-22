package com.example.iptvplayer.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.getField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


// view model for testing purposes
// injecting repository directly into view model btw
@HiltViewModel
class DummyViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
): ViewModel() {
    private val _isTrial: MutableLiveData<Boolean> = MutableLiveData()
    val isTrial: LiveData<Boolean> = _isTrial

    fun checkIfTrial() {
        viewModelScope.launch {
            val doc = firestore.collection("trial")
                .document("trial")
                .get().await()

            val isTrial = doc.getField<Boolean>("isTrial") ?: false
            _isTrial.value = isTrial
        }
    }
}