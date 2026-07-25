package com.benjamin.factura.data.repository

import com.benjamin.factura.data.model.AppUser
import com.benjamin.factura.data.model.BusinessProfile
import com.benjamin.factura.data.model.Client
import com.benjamin.factura.data.model.Expense
import com.benjamin.factura.data.model.Invoice
import com.benjamin.factura.data.model.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    val currentUser: AppUser?
        get() {
            val firebaseUser = auth?.currentUser ?: return null
            return AppUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString() ?: ""
            )
        }

    suspend fun syncBusinessProfile(profile: BusinessProfile) {
        val db = firestore ?: return
        val userId = auth?.currentUser?.uid ?: profile.id
        try {
            db.collection("businesses").document(userId).set(profile).await()
        } catch (e: Exception) {
            // Silently ignore sync failures when offline
        }
    }

    suspend fun syncClient(client: Client) {
        val db = firestore ?: return
        try {
            db.collection("clients").document(client.id).set(client).await()
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    suspend fun syncInvoice(invoice: Invoice) {
        val db = firestore ?: return
        try {
            db.collection("invoices").document(invoice.id).set(invoice).await()
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    suspend fun syncPayment(payment: Payment) {
        val db = firestore ?: return
        try {
            db.collection("payments").document(payment.id).set(payment).await()
        } catch (e: Exception) {
            // Silently ignore
        }
    }

    suspend fun syncExpense(expense: Expense) {
        val db = firestore ?: return
        try {
            db.collection("expenses").document(expense.id).set(expense).await()
        } catch (e: Exception) {
            // Silently ignore
        }
    }
}
