package com.example.toolexchange

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

//własna klasa do obsługi komunikacji z serwerem
class DataExchange {
    private val host = "https://graph.microsoft.com/v1.0/"
    private lateinit var  client : HttpClient
    private var recData = ByteArray(0)
    private var token  = ""
    private var con : Context? = null
    enum class Source
    {
        Me, Tools, User, SendEmail, Edit, IdUser, Mag, ToolsNXT, UsrList
    }

    fun init(a : Context, b: String?)
    {
        this.token  = b!!.toString()
        this.con = a
    }

    suspend fun getStart(pMethod: HttpMethod = HttpMethod.Get, src : Source, getData: String = "", postData: String = "", contentT: ContentType = ContentType.Any)
    {
        client = HttpClient(CIO)
        if (token == "" || token.isEmpty()) //JEŻELI NIE MAMY TOKENU TO WRACAMY DO LOGOWANIA
        {
            val act = Intent(this.con!!, CheckActivity::class.java)
            startActivity(this.con!!, act, null)
        }

        println(host+getSrc(src)+getData)
        println(postData)
        println(getData)
        //WYSYŁANIE ZAPYTANIA HTTP
        val response : HttpResponse = client.request(host+getSrc(src)+getData)
        {
            method = pMethod
            headers{
                append(HttpHeaders.Authorization, token)
                append(HttpHeaders.Prefer, "HonorNonIndexedQueriesWarningMayFailRandomly")
                contentType(contentT)
            }
            body = postData
        }

        //OBSŁUGA ODPOWIEDZI
        println(response.status)
        recData = response.receive()
        client.close()
    }

    fun getRecData() : String
    {
        return this.recData.decodeToString()
    }

    private fun getSrc(s : Source) : String
    {
        return when(s) {
            Source.Me -> "me"
            Source.User -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/user information list/items?\$select=id&\$expand=fields(\$select=id,email,workphone,mobilephone,businessphones)&\$filter=startsWith(fields/EMail,"    //"users/"
            Source.Tools -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/toolExchange/items/?\$select=id&\$expand=fields(\$select=id,title,modified,spec_features,serial_no,inv_no,new_inv_no,exp_val,status,user,userLookupId,statusLookupId,next_user)&\$filter=Fields/STATUSLookupId ne '4'&\$top=100"
            Source.Edit -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/toolExchange/items/"
            Source.SendEmail -> "me/sendMail"
            Source.ToolsNXT -> ""
            Source.Mag -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/toolExchange_toolStatus/items/"
            Source.IdUser -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/user information list/items?\$select=id&\$expand=fields&\$filter=startsWith(fields/EMail,"
            Source.UsrList -> "sites/mazakeurope.sharepoint.com:/sites/YMPServiceteam:/lists/user information list/items?\$select=id&\$expand=fields(\$select=id,email,title)&\$filter=Fields/ContentType eq 'Person'"
        }
    }
}
