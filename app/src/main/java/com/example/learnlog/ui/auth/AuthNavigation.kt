package com.example.learnlog.ui.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.data.db.AppDatabase
import com.example.learnlog.util.AppRestarter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser

/** Shared by Login and Register. */
internal object AuthNavigation {

    /**
     * Goes to the app after sign-in. If this process already opened another account's
     * database (e.g. the guest one), restart so every singleton is rebuilt for this account.
     */
    fun enterApp(fragment: Fragment, user: FirebaseUser, actionId: Int) {
        if (AppDatabase.isOpenForAnotherAccount(user.uid)) {
            AppRestarter.restart(fragment.requireActivity())
        } else {
            fragment.findNavController().navigate(actionId)
        }
    }

    /** Turns the Google sign-in activity result into an account or a friendly error. */
    fun handleGoogleResult(result: ActivityResult, viewModel: AuthViewModel) {
        if (result.resultCode != Activity.RESULT_OK && result.data == null) return
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            if (account != null) viewModel.signInWithGoogle(account)
        } catch (e: ApiException) {
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED, GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> Unit
                GoogleSignInStatusCodes.DEVELOPER_ERROR -> viewModel.reportError(R.string.auth_google_not_configured)
                GoogleSignInStatusCodes.NETWORK_ERROR -> viewModel.reportError(R.string.auth_offline)
                else -> viewModel.reportError(R.string.auth_google_failed)
            }
        }
    }

    fun googleIntent(client: com.google.android.gms.auth.api.signin.GoogleSignInClient): Intent {
        // Always show the account chooser instead of silently reusing the last account
        client.signOut()
        return client.signInIntent
    }
}
