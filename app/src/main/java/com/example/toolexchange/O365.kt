package com.example.toolexchange

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException


//UZYSKIWANIE DOSTĘPU DO KONTA MICROSOFT O365
open class O365 (private val appCon: Context, private val activity: Activity)
{
    private val scopes = arrayOf("User.Read", "Mail.Send.Shared", "User.ReadBasic.All", "email", "Sites.ReadWrite.All")
    private lateinit var loginApp : ISingleAccountPublicClientApplication
    var succes = false
    var fail = false
    private var first = true
    var acc : IAccount? = null
    var tokens = ""

    fun getToken() : String
    {
        return tokens
    }

    private fun  getAuthInteractiveCallback() : AuthenticationCallback
    {
        return object : AuthenticationCallback
        {
            override fun onSuccess(authenticationResult: IAuthenticationResult?)
            {
                if (authenticationResult != null) {
                    acc = authenticationResult.account
                    tokens = authenticationResult.accessToken

                    Log.d("_O365_ID", acc!!.idToken.toString())
                    Log.d("_O365", tokens)
                }
                Toast.makeText(appCon, R.string.loginOk, Toast.LENGTH_SHORT).show()
                succes = true
                Log.d("_O365", "InteractiveCallbackSucces")
            }

            override fun onError(e: MsalException)
            {
                if (first)
                {
                    first = false
                    loginApp.signInAgain(activity, scopes, null, getAuthInteractiveCallback())
                }
                else
                {
                    succes = false
                    fail = true
                    Toast.makeText(appCon, R.string.loginNok, Toast.LENGTH_SHORT).show()
                    Log.d("_O365 callbackInterErr", e.localizedMessage!! + "\n" + e.errorCode)
                }
            }

            override fun onCancel()
            {
                succes = false
                fail = true
                Toast.makeText(appCon, appCon.getString(R.string.loginNok)+" Anulowano.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //KONFIGURACJA DOSTĘPU
    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
             appCon, R.raw.msal_single_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener
            {
                override fun onCreated( application: ISingleAccountPublicClientApplication)
                {
                    loginApp = application

                   //POZYSKANIE TOKENU W TRYBIE INTERAKTYWNYM
                    if (acc == null)
                        loginApp.signIn(activity, null, scopes, getAuthInteractiveCallback())
                }

                override fun onError(e: MsalException)
                {
                    Log.d("_O365 init", e.localizedMessage!!+"\n"+e.errorCode)
                }
            })
    }


}