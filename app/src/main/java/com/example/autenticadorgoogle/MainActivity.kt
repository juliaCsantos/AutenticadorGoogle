 package com.example.autenticadorgoogle

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

 class MainActivity : AppCompatActivity() {
     private lateinit var signInRequest: BeginSignInRequest
     private lateinit var auth: FirebaseAuth
     private lateinit var oneTapClient: SignInClient

     private val REQ_ONE_TAP = 2
     private var showOneTapUI = true

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)

         when (requestCode) {
             REQ_ONE_TAP -> {
                 try {
                     val credential = oneTapClient.getSignInCredentialFromIntent(data)
                     val idToken = credential.googleIdToken
                     val username = credential.id
                     val password = credential.password
                     when {
                         idToken != null -> {
                             // Got an ID token from Google. Use it to authenticate
                             // with your backend.
                             Log.d(TAG, "Got ID token.")
                         }
                         password != null -> {
                             // Got a saved username and password. Use them to authenticate
                             // with your backend.
                             Log.d(TAG, "Got password.")
                         }
                         else -> {
                             // Shouldn't happen.
                             Log.d(TAG, "No ID token or password!")
                         }
                     }
                 } catch (e: ApiException) {
                     when (e.statusCode) {
                         CommonStatusCodes.CANCELED -> {
                             Log.d(TAG, "One-tap dialog was closed.")
                             // Don't re-prompt the user.
                             showOneTapUI = false
                         }
                         CommonStatusCodes.NETWORK_ERROR -> {
                             Log.d(TAG, "One-tap encountered a network error.")
                             // Try again or just ignore.
                         }
                         else -> {
                             Log.d(TAG, "Couldn't get credential from result." +
                                     " (${e.localizedMessage})")
                         }
                     }
                 }
             }
         }

         val googleCredential = oneTapClient.getSignInCredentialFromIntent(data)
         val idToken = googleCredential.googleIdToken
         when {
             idToken != null -> {
                 // Got an ID token from Google. Use it to authenticate
                 // with Firebase.
                 val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                 auth.signInWithCredential(firebaseCredential)
                     .addOnCompleteListener(this) { task ->
                         if (task.isSuccessful) {
                             // Sign in success, update UI with the signed-in user's information
                             Log.d(TAG, "signInWithCredential:success")
                             val user = auth.currentUser
                         } else {
                             // If sign in fails, display a message to the user.
                             Log.w(TAG, "signInWithCredential:failure", task.exception)
                         }
                     }
             }
             else -> {
                 // Shouldn't happen.
                 Log.d(TAG, "No ID token!")
             }
         }
     }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                .setSupported(true)
                .build())
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.your_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            // Automatically sign in when exactly one credential is retrieved.
            .setAutoSelectEnabled(true)
            .build()

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this) { result ->
                    try {
                        startIntentSenderForResult(
                            result.pendingIntent.intentSender, REQ_ONE_TAP,
                            null, 0, 0, 0, null)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                    }
                }
                .addOnFailureListener(this) { e ->
                    Log.d(TAG, e.localizedMessage)
                }

            signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.your_web_client_id))
                        // Only show accounts previously used to sign in.
                        .setFilterByAuthorizedAccounts(true)
                        .build())
                .build()
        }
        }

     override fun onStart() {
         super.onStart()
         // Check if user is signed in (non-null) and update UI accordingly.
         val currentUser = auth.currentUser
     }

 }