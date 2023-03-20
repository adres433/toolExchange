package com.example.toolexchange

import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.ktor.http.*
import kotlinx.coroutines.*
import org.json.JSONObject

/*

########---WYMAGANE PARAMETRY DO PRZEKAZANIA---########
idTool
titleTool
logToken

 */

class EmailBox : AppCompatActivity()
{

    private var bindAC : AlertDialog? = null
    private var tmpNextUser = -1
    private var listUser : List<UserList> = listOf()
    private var mags = emptyArray<CheckActivity.Magss>()
    private lateinit var errREST : Toast
    private var rest = DataExchange()
    private lateinit var builder : AlertDialog.Builder
    private var tempNameNextUser = ""
    private var id = -1
    private var title = ""
    private var dismType = -1

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_box)
        rest.init(applicationContext, intent.getStringExtra("logToken"))

        builder = AlertDialog.Builder(this)
        errREST = Toast.makeText(this, getString(R.string.errREST), Toast.LENGTH_LONG)
        id = intent.getIntExtra("idTool", -1)
        title = intent.getStringExtra("titleTool") as String
        dismType = -1

        //wywołujemy pobieranie listy użytkowników do zmiennych
        listUserFnc()

        //oczekujemy do zakończenia pobierania listy
        runBlocking {
            launch {
                var time = 0
                while (listUser.isNullOrEmpty())
                {
                    println("Ładowanie listy urzytkowników. - ${time*250/1000}s")
                    delay(250)
                    ++time
                }
            }
        }

        //UTWORZENIE I WYŚWIELTENIE OKNA WYBORU UŻYTKOWNIKA
        with(builder)
        {
            setTitle(R.string.sendToolTitle)
            //akcja po zatwierdzeniu przyciskiem OK
            setPositiveButton(R.string.saveBox) {_,_ ->
                dismType = 0x1  //potwierdzenie sposobu opuszczenia okna dialogowego
                try {
                    runBlocking {
                        launch {
                            rest.getStart(
                                HttpMethod.Patch,
                                DataExchange.Source.Edit,
                                "$id/fields",
                                """{"STATUSLookupId": "2", "NEXT_USERLookupId": "$tmpNextUser"}""",
                                contentT = ContentType.Application.Json
                            )
                        }
                    }
                }
                catch (e: Exception)
                {
                    Log.d("_DELETE", e.message.toString())
                    errREST.show()
                    delayTime()
                    finish()
                }

                CoroutineScope(Dispatchers.IO).launch{
                    saveItem()
                }

                    return@setPositiveButton
            }
            //akcja po anulowaniu przyciskiem
            setNegativeButton(R.string.cancelBox) { _, _ ->
                errREST.setText(getString(R.string.manualErr)+" - NEGATIVE BUTTON")
                errREST.show()
                delayTime() //funkcja opuźniająca o 1s
                finish()
                return@setNegativeButton
            }
            //akcja dla porzucenia okna dialogowego
            setOnDismissListener {
                if (dismType == 0x1)
                    return@setOnDismissListener
                errREST.setText(getString(R.string.manualErr)+" - DISMISS LISTENER")
                errREST.show()
                delayTime() //funkcja opuźniająca 1s
                finish()
                return@setOnDismissListener
            }
            setView(R.layout.email_box)

            bindAC = builder.show()

            //podpięcie autocomplete pod pole tekstowe komunikatu
            val nxtUsrAdapter = MyAdapterList(bindAC!!.context, listUser)
            bindAC!!.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!
                .setAdapter(nxtUsrAdapter)

            //akcja po wybraniu elementu listy podpowiedzi
            bindAC!!.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!
                .setOnItemClickListener { _, view, _, _ ->
                    //zapisujemy ID użytkownika do zmiennej
                    tmpNextUser =
                        view.findViewById<TextView>(R.id.user_id).text.toString()
                            .toInt()
                    tempNameNextUser =
                    view.findViewById<TextView>(R.id.user_full_name).text.toString()
                    //wprowadzamy dla efektu wizualnego adres email w pole textowe
                    bindAC!!.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!
                        .setText(
                            view.findViewById<TextView>(
                                R.id.user_email
                            ).text
                        )
                }
        }
    }

    private fun listUserFnc()
    {
        try {
            CoroutineScope(Dispatchers.IO).launch {
//-------------------------------------START DANE MAGAZYNÓW -------------------------

                if (mags.isNullOrEmpty()) {
                    val temp = mags.toMutableList()

                    //MAG | PHONE | TITLE | PERSON
                    rest.getStart(
                        src = DataExchange.Source.Mag,
                        getData = "?\$select=id&\$expand=fields(\$select=id,mag,person,phone,Title)&filter=Fields/MAG eq 1"
                    )

                    val row2 = JSONObject(rest.getRecData()).getJSONArray("value")
                    for (i in row2.length() - 1 downTo 0) {
                        temp.add(
                            CheckActivity.Magss(
                                row2.getJSONObject(i).getJSONObject("fields")
                                    .getInt("id"),
                                row2.getJSONObject(i).getJSONObject("fields")
                                    .getString("PERSON"),       //email
                                row2.getJSONObject(i).getJSONObject("fields")
                                    .getString("PHONE"),
                                row2.getJSONObject(i).getJSONObject("fields")
                                    .getString("Title")
                            )
                        )
                    }
                    mags = temp.toTypedArray()
                }

//----------------------------------END DANE MAGAZYNÓW -------------------------------
//----------------------------------START NEXT_USER-----------------------------------

                //POBRANIE LISTY UŻYTKOWNIKÓW - jeżeli ta nie jest jeszcze pobrana
                if (listUser.isNullOrEmpty()) {
                    rest.getStart(
                        src = DataExchange.Source.UsrList,
                        contentT = ContentType.Application.Json
                    )

                    val lists =
                        JSONObject(rest.getRecData()).getJSONArray("value") //pomocnicza lista przechowująca wynik zapytania z MS GRAPH

                    //USUWAMY Z LISTY POZYCJE NIE BĘDĄCE OSOBĄ, CZYLI UŻYTKOWNICY WBUDOWANI
                    for (i in lists.length() - 1 downTo 0) {
                        val tList = lists.getJSONObject(i).getJSONObject("fields")
                        if (!tList.has("EMail") || tList.getString("EMail")
                                .indexOf("@mazak.com.pl") == -1
                        ) {
                            lists.remove(i)
                        }
                    }

                    //STOWRZENIE LISTY OBIEKTÓW Z URZYTKOWNIKAMI
                    val tmpListUserMutable =
                        mutableListOf<UserList>() //lista pomocnicza dla pętli for
                    for (j in lists.length() - 1 downTo 0) {
                        val tmpItmObj = UserList()
                        var tmpUserOrMag: List<CheckActivity.Magss>?
                        Log.d("_MANUAL_DEL", "MAKE j: ${j - 1}")
                        val items = lists.getJSONObject(lists.length() - j - 1)
                            .getJSONObject("fields")
                        tmpUserOrMag =
                            mags.filter { it.user.trim().lowercase().normalize() == (items.getString("EMail").trim().lowercase().normalize()) }

                        if (tmpUserOrMag.isNullOrEmpty())
                        {
                            tmpItmObj.set(
                                items.getInt("id"),
                                items.getString("Title"),
                                items.getString("EMail")
                            )
                        }
                        else
                        {
                            tmpItmObj.set(
                                items.getInt("id"),
                                tmpUserOrMag.first().name,
                                items.getString("EMail")
                            )
                        }
                        tmpListUserMutable.add(tmpItmObj)
                    }

                    listUser = tmpListUserMutable.toList() //gotowa lista użytkowników
                }

//-----------------------------END NEXT_USER------------------------------------------
            }
        }
        catch (e: Exception)
        {
            Log.d("_DELETE", e.message.toString())
            errREST.show()
            finish()
        }

    }

    private fun delayTime(time: Long = 1)
    {
        runBlocking { launch { delay(time*1000) } }
    }

    private fun saveItem()
    {
        val infoBar = Snackbar.make(findViewById(R.id.linear_emailBox), "sxvxcvxc", Snackbar.LENGTH_LONG)
        infoBar.setText(this@EmailBox.getString(R.string.manualSucces)+" $title \n- LOGISTYKA: $tempNameNextUser")
        infoBar.show()
        delayTime(1)
        finish()
    }
}