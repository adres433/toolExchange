package com.example.toolexchange

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import kotlinx.coroutines.*
import androidx.core.content.ContextCompat.*
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.http.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import com.example.toolexchange.DataExchange.Source.*
import com.google.android.material.snackbar.Snackbar
import com.example.toolexchange.databinding.*
import kotlinx.coroutines.CancellationException

private var bind : ActivityCheckBinding? = null
private var job : Job? = null
private var job1 : Job? = null
private var job2 : Job? = null
private var adapter: MyAdapter? = null
private var mags = emptyArray<CheckActivity.Magss>()

class CheckActivity : AppCompatActivity()
{
    private var myToast : TextView? = null
    private var end = ""
    private var search = false
    private var toolArray : Array<ItemTool>? = null
    private var recyclerView : RecyclerView? = null
    private val rest = DataExchange()
    private var tempChar = ""
    private var job3 : Job? = null

    class Magss(var index : Int, var user : String, var phone : String, var name: String)

    override fun onStop()
    {
        super.onStop()
        job!!.cancel()
        job1!!.cancel()
        job2!!.cancel()
    }

    override fun onPause()
    {
        super.onPause()
        job!!.cancel()
        job1!!.cancel()
        job2!!.cancel()
    }

    override fun onResume() {
        super.onResume()
        bind!!.hideButton.callOnClick()
        //bind!!.exitSearch.callOnClick()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        bind = ActivityCheckBinding.inflate(layoutInflater)
        myToast = bind!!.waitList
        rest.init(applicationContext, intent.getStringExtra("logToken"))

        setContentView(bind!!.root)

        //----------------Tworzymy liste elementów-----------------------------
        recyclerView = findViewById<View>(R.id.list) as RecyclerView
        // w celach optymalizacji
        recyclerView!!.setHasFixedSize(true)

        // ustawiamy LayoutManagera
        recyclerView!!.layoutManager = LinearLayoutManager(this)

        // ustawiamy animatora, który odpowiada za animację dodania/usunięcia elementów listy
        recyclerView!!.itemAnimator = DefaultItemAnimator()

        // wstawienie pustych rekordów do listy
        toolArray = Array(7){ItemTool()}
        adapter = MyAdapter(toolArray!!.toMutableList(), recyclerView!!, this)
        recyclerView!!.adapter = adapter
        bind!!.list.visibility = View.INVISIBLE

        //----------------END----------------------------------------------------


        //PRZESZUKIWANIE LISTY
        bind!!.search.doAfterTextChanged {    //działanie po wpisaniu
            searchHelp()
            return@doAfterTextChanged
        }

        //OBSŁUGA UKRYTEGO PRZYCISKU POBIERANIA LISTY
        bind!!.hideButton.setOnClickListener {
            getList()
        }

        bind!!.exitSearch.setOnClickListener {
            bind!!.search.text.clear()
            adapter = MyAdapter(toolArray!!.toMutableList(), recyclerView!!, this)
            recyclerView!!.adapter = adapter
            search = false
            tempChar = ""
            if (job3 != null)
            {
                job3!!.cancel()
                job3 = null
            }
            return@setOnClickListener
        }

        //wstawić zapytanie o nowe dane
        getList()
    }

    private fun searchHelp()
    {
        if (intent.hasExtra("logToken") && bind!!.search.text.length > 2)
        {
            search = true
            tempChar = bind!!.search.text.toString()
            if (job3 == null)
            {
                job3 = CoroutineScope(Dispatchers.IO).launch {
                    delay(500)

                    if (tempChar == bind!!.search.text.toString())
                        withContext(Dispatchers.Main) {searchList(bind!!.search.text.toString())}
                }
            }
            else
            {
                job3!!.cancel()
                job3 = CoroutineScope(Dispatchers.IO).launch {
                    delay(500)

                    if (tempChar == bind!!.search.text.toString())
                        withContext(Dispatchers.Main) {searchList(bind!!.search.text.toString())}
                }
            }
        }
        if (bind!!.search.text.isEmpty())
        {
            bind!!.exitSearch.callOnClick()
        }
    }

    //funkcja pobierająca i wyświetlająca pełną listę
    private fun getList(pNext: Boolean = false)
    {
        if (!intent.hasExtra("logToken"))
        {
            Toast.makeText(applicationContext, R.string.loginNok, Toast.LENGTH_SHORT).show()
            startActivity(Intent(application, LoginActivity::class.java))
        }
        println("Ciąg dalszy: $pNext")
        println("END: \n\n${end}\n\n")
        if (pNext)
        {
            job!!.cancel()
            job1!!.cancel()
            job2!!.cancel()
        }
        var json : JSONArray
        var tempLength = 0
        val excToast = Toast.makeText(applicationContext, R.string.errREST, Toast.LENGTH_SHORT)

        if (!pNext)
        {
            myToast!!.visibility = View.VISIBLE
            job1 = CoroutineScope(Dispatchers.IO).launch {
                var i = 10
                while (i < 100) {
                    delay(100)
                    bind!!.progressCheck.incrementProgressBy(10)
                    i += 10
                    if (i == 100)
                        i = 0
                    if (job!!.isCompleted)
                        break
                }
                if (job!!.isCancelled || job!!.isCompleted)
                {
                    withContext(Dispatchers.Main) {
                        bind!!.progressCheck.visibility = View.INVISIBLE
                        bind!!.list.visibility = View.VISIBLE
                    }
                }

                job1!!.cancel()
            }
        }
        else
        {
            if (job1!!.isCancelled || job1!!.isCompleted)
            {
                bind!!.progressCheck.visibility = View.INVISIBLE
                bind!!.list.visibility = View.VISIBLE
            }
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            try
            {
                //POBIERAMY TABELĘ Z NARZĘDZIAMI
                if(end.isEmpty() || !pNext)
                    rest.getStart(src = Tools)
                else
                    rest.getStart(src = ToolsNXT, getData = end)

                //JEŻELI ZWRÓCONE DANE ZOSTAŁY PODZIELONE TO
                end =   if (JSONObject(rest.getRecData()).has("@odata.nextLink"))
                            JSONObject(rest.getRecData()).getString("@odata.nextLink")
                                .replaceBefore("sites/", "")
                        else
                            ""
                if (pNext && end.isEmpty())
                {
                    withContext(Dispatchers.Main)
                    {
                        myToast!!.visibility = View.INVISIBLE
                        Toast.makeText(applicationContext, R.string.listOK, Toast.LENGTH_SHORT).show()
                        if(bind!!.search.text.isNotEmpty())
                            searchHelp()

                    }
                }
                //TABLICĘ Z REKORDAMI ZAPISUJEMY W ZMIENNEJ
                json = JSONObject(rest.getRecData()).getJSONArray("value")

                if (!pNext)
                {
                    toolArray = Array(json.length()) { ItemTool() }
                }
                else
                {
                    tempLength = toolArray!!.size
                    val tempToolArray = toolArray
                    toolArray = Array(json.length()+tempLength) {ItemTool()}
                    //UZUPEŁNIAMY TABLICĘ POPRZEDNIMI REKORDAMI
                    for(j in tempToolArray!!.size-1 downTo 0)
                    {
                        toolArray!![j] = tempToolArray[j]
                    }
                }

                for (i in toolArray!!.size - 1 downTo tempLength)
                {
                    val row = json.getJSONObject(i-tempLength).getJSONObject("fields")
                    json.remove(i-tempLength)
                    var features = ""
                    var user = ""
                    var nxtUser = ""
                    var userId = -1
                    var userPhone = "+48123456789"
                    var downInfo = true

                    //POBIERAMY NR TELEFONU UŻYTKOWNIKA POSIADAJĄCEGO NARZĘDZIE
                    if (mags.isNotEmpty()) {
                        for (a in mags) {
                            if (a.index == row.getInt("STATUSLookupId")) {
                                userPhone = a.phone
                                user = a.user
                                downInfo = false
                            }
                        }
                    }
                    if (row.getInt("STATUSLookupId") >= 5 && downInfo) {
                        //MAG | PHONE | TITLE | PERSON
                        rest.getStart(src = Mag,
                            getData = row.getInt("STATUSLookupId")
                                .toString() + "/?\$select=id&\$expand=fields(\$select=mag,person,phone,Title)"
                        )
                        val row2 = JSONObject(rest.getRecData()).getJSONObject("fields")

                        if (row2.getBoolean("MAG")) {
                            if (row.has("USER")) {
                                row.remove("USER")
                            }
                            row.put("USER", row2.getString("PERSON"))

                            userPhone = row2.getString("PHONE")

                            val temp = mags.toMutableList()
                            temp.add(
                                mags.size,
                                Magss(
                                    row.getInt("STATUSLookupId"),
                                    row2.getString("PERSON"),
                                    row2.getString("PHONE"),
                                    row2.getString("Title")
                                )
                            )
                            mags = temp.toTypedArray()
                        }
                    }

                    if (row.has("SPEC_FEATURES"))
                        features = row.getString("SPEC_FEATURES")
                    if (row.has("USER")) {
                        user = row.getString("USER")
                    }
                    if (row.has("NEXT_USER")) {
                        nxtUser = row.getString("NEXT_USER")
                    }
                    if (row.has("USERLookupId"))
                        userId = row.getInt("USERLookupId")
                    var date = LocalDate.parse(row.getString("EXP_VAL").substringBefore("T"))
                    date = date.plusDays(1)
                    row.remove("EXP_VAL")
                    val month: String = if (date.month.ordinal + 1 < 10)
                        "0" + (date.month.ordinal + 1).toString()
                    else
                        (date.month.ordinal + 1).toString()
                    val day: String = if (date.dayOfMonth < 10)
                        "0" + (date.dayOfMonth).toString()
                    else
                        (date.dayOfMonth).toString()
                    val data = date.year.toString() + "-" + month + "-" + day
                    row.put("EXP_VAL", data)
                    var serial = ""
                    if(row.has("SERIAL_NO"))
                        serial = row.getString("SERIAL_NO")
                    var invNr = ""
                    if(row.has("INV_NO"))
                        invNr = row.getString("INV_NO")

                    toolArray!![i].init(
                        row.getString("Title"),
                        features,
                        serial,
                        row.getString("EXP_VAL"),
                        row.getString("NEW_INV_NO"),
                        invNr,
                        user,
                        row.getString("STATUS"),
                        userPhone,
                        intent.getStringExtra("logToken")!!,
                        nxtUser,
                        row.getInt("id"),
                        row.getInt("STATUSLookupId"),
                        userId
                    )
                }

                if (pNext)
                {
                    withContext(Dispatchers.Main)
                    {
                        if (!search)
                        {
                            adapter!!.refreshList(toolArray!!.toMutableList(), tempLength)
                        }
                    }
                }
                else
                {
                    withContext(Dispatchers.Main)
                    {
                        if(!search)
                        {
                            adapter = MyAdapter(toolArray!!.toMutableList(), recyclerView!!, this@CheckActivity)
                            recyclerView!!.adapter = adapter

                            job1!!.cancel()
                        }
                    }
                }
            }
            catch (e: CancellationException)
            {
                println("Coroutine przerwano.")
            }
            catch(e: Exception)
            {
                println(resources.getString(R.string.errREST) + e.message)
                excToast.show()
            }
        }
        job2 = CoroutineScope(Dispatchers.IO).launch {
            //CZEKAMY NA ZAKOŃCZENIE JOB

            while(!job!!.isCompleted)
            {
                println("JOB1 wait JOB finish.\n\n")
                delay(500)
            }
            //JEŻELI POSIADAMY KOLEJNE ELEMENTY
            if (end.isNotEmpty() && job!!.isCompleted)
            {
                println("Przechodzimy do wątku głównego.")
                //PRZECHODZĄC DO GŁÓWNEGO WĄTKU
                withContext(Dispatchers.Main)
                {
                    println("Pobieramy listę z linku")
                    //POBIERAMY KOLEJNE LELEMENTY LISTY
                    getList( true)
                }
            }
        }
    }

    //funkcja obsługująca przeszukiwanie listy i wyświetlanie wyników
    private fun searchList(pFind: String)
    {
        Log.d("_SEARCH", pFind)
        if (end.isNotEmpty())
        {
            Toast.makeText(applicationContext, R.string.searchErr, Toast.LENGTH_SHORT).show()
            Log.d("_SEARCH", "WAIT TO LIST")
            //return
        }
        val tempArrays : MutableList<ItemTool> = MutableList(0) { ItemTool() }
        val size = toolArray!!.size
        val a = pFind.normalize()

        for (i in size-1 downTo 0)
        {
            Log.d("_SEARCH", "INDEX: $i")
            if (toolArray!![i].getName().normalize().indexOf(a, ignoreCase = true) != -1) {
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "NAME")
                continue
            }
            else if (toolArray!![i].getFeatures().normalize().indexOf(a, ignoreCase = true) != -1) {
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "FEATURES")
                continue
            }
            else if (toolArray!![i].getSerial().normalize().indexOf(a, ignoreCase = true) != -1){
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "SERIAL")
                continue
            }
            else if (toolArray!![i].getEwidenceOld().normalize().indexOf(a, ignoreCase = true) != -1){
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "NR_EW")
                continue
            }
            else if (toolArray!![i].getEwidence().normalize().indexOf(a, ignoreCase = true) != -1){
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "NEW_EW")
                continue
            }
            else if (toolArray!![i].getUser().normalize().indexOf(a, ignoreCase = true) != -1 && toolArray!![i].getIdState() == 1){
                tempArrays.add(toolArray!![i])
                Log.d("_SEARCH", "USER")
                continue
            }
            else
                continue
        }
        Log.d("_SEARCH", "SZUKANA FRAZA: $a")
        Log.d("_SEARCH", tempArrays.toString())
        adapter = MyAdapter(tempArrays, recyclerView!!, this)
        recyclerView!!.adapter = adapter
    }

    //funkcja pomocnicz w celu
    /*fun emailBoxRun(pid: Int, ptitle: String)
    {
        val act = Intent(applicationContext, EmailBox::class.java)
        act.putExtra("logToken", intent.getStringExtra("logToken"))
        act.putExtra("idTool", pid)
        act.putExtra("titleTool", ptitle)

        startActivity(act)
    }*/
}


//ZAMIANA POLSKICH ZNAKÓW
fun String.normalize(): String {
    val original = arrayOf("Ą", "ą", "Ć", "ć", "Ę", "ę", "Ł", "ł", "Ń", "ń", "Ó", "ó", "Ś", "ś", "Ź", "ź", "Ż", "ż")
    val normalized = arrayOf("A", "a", "C", "c", "E", "e", "L", "l", "N", "n", "O", "o", "S", "s", "Z", "z", "Z", "z")

    return this.map { char ->
        val index = original.indexOf(char.toString())
        if (index >= 0) normalized[index] else char
    }.joinToString("")
}


private class MyAdapter(private var arrayItem: MutableList<ItemTool>, private val mRecyclerView: RecyclerView, val th: CheckActivity) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>()
{
    var job4 : Job? = null
    // implementacja wzorca ViewHolder
    // każdy obiekt tej klasy przechowuje odniesienie do layoutu elementu listy
    // dzięki temu wywołujemy findViewById() tylko raz dla każdego elementu
    inner class MyViewHolder(pItem: View) : RecyclerView.ViewHolder(pItem)
    {
        var img: ImageView = pItem.findViewById<View>(R.id.img_) as ImageView
        var name: TextView = pItem.findViewById<View>(R.id.name_) as TextView
        var features : TextView = pItem.findViewById<View>(R.id.feature_) as TextView
        var cert : TextView = pItem.findViewById<View>(R.id.certificate_) as TextView
        var state : TextView = pItem.findViewById<View>(R.id.state_) as TextView
        var ewid : TextView = pItem.findViewById<View>(R.id.ewid_) as TextView
        var serial : TextView = pItem.findViewById<View>(R.id.serial_) as TextView
        var number : TextView = pItem.findViewById<View>(R.id.numberDays) as TextView
    }

    //AKTUALIZACJA LISTY
    fun refreshList(pNewList: MutableList<ItemTool>, index: Int = 0)
    {
        arrayItem = pNewList
        adapter!!.notifyItemRangeInserted(index, pNewList.size-1)
    }

    //POPUP MENU DLA KAŻDEGO ELEMENTU LISTY
    private fun showPopupMenu(view: View,  pos: Int, con: Context)
    {
        val restPost = DataExchange()
        restPost.init(con, arrayItem[pos].getToken())

        val popup = PopupMenu(con, view)
        popup.inflate(R.menu.context_menu)

        popup.setOnMenuItemClickListener { item: MenuItem? ->
            var userPhone = ""
            val infoBar = Snackbar.make(view, "", Snackbar.LENGTH_SHORT)
            val errREST = Toast.makeText(con, con.getString(R.string.errREST), Toast.LENGTH_SHORT)
            val errState = Snackbar.make(view, con.getString(R.string.errStateAdd), Snackbar.LENGTH_SHORT)

            try
            {
                runBlocking {
                    launch {
                        //CZEKAMY NA DANE O UŻYTKOWNIKU
                        Log.d("_DATA", arrayItem[pos].getPhone())

                        if (arrayItem[pos].getPhone() == "+48123456789") {
                            restPost.getStart(src = User, getData = "'" + arrayItem[pos].getUser() + "')")
                            var data = JSONObject(restPost.getRecData())
                            data =
                                data.getJSONArray("value").getJSONObject(0).getJSONObject("fields")
                            Log.d("_DATA", data.toString())

                            when {
                                data.has("MobilePhone") -> {
                                    userPhone = data.getString("MobilePhone")
                                }
                                data.has("BusinessPhones") -> {
                                    userPhone = data.getString("BusinessPhones")
                                }
                                data.has("WorkPhone") -> {
                                    userPhone = data.getString("WorkPhone")
                                }
                            }
                        } else
                            userPhone = arrayItem[pos].getPhone()
                    }
                }
            }
            catch (e: Exception)
            {
                Toast.makeText(con, R.string.errREST, Toast.LENGTH_SHORT).show()
            }

            when (item!!.itemId)
            {
                //ZADZWOŃ POD NUMER
                R.id.callNumber ->
                {
                    if(arrayItem[pos].getIdState() == 2)
                        return@setOnMenuItemClickListener true
                    if(userPhone.isNotEmpty())
                    {
                        Log.d("ITEM_", "CALL")
                        val intent = Intent(Intent.ACTION_CALL)
                        intent.data = Uri.parse("tel:$userPhone")
                        try
                        {
                            startActivity(con, intent, null)
                        }
                        catch (e: Exception)
                        {
                            Log.d("_ITEM", e.message.toString())
                            Toast.makeText(con, R.string.emailNOK, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                //WYŚLIJ SMS
                R.id.sendSMS ->
                {
                    if(arrayItem[pos].getIdState() == 2)
                        return@setOnMenuItemClickListener true
                    if(userPhone.isNotEmpty())
                    {
                        var currUser = ""
                        job4 = CoroutineScope(Dispatchers.IO).launch {
                                try
                                {
                                    restPost.getStart(HttpMethod.Get, Me)
                                    currUser = JSONObject(restPost.getRecData()).getString("displayName")
                                }
                                catch (e: Exception)
                                {
                                    Log.d("_ITEM", e.message.toString())
                                    Toast.makeText(con, R.string.emailNOK, Toast.LENGTH_SHORT).show()
                                }

                                Log.d("ITEM_", "SMS")
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("smsto:$userPhone")
                                intent.putExtra("sms_body", con.getString(R.string.SMS_message)+"\n\n"+arrayItem[pos].getName()+"\n"+arrayItem[pos].getFeatures()+
                                        "\n"+arrayItem[pos].getSerial()+"\n"+arrayItem[pos].getEwidence()+" | "+arrayItem[pos].getEwidenceOld()+"\n\n"+currUser)

                                try
                                {
                                    startActivity(con, Intent.createChooser(intent, con.getString(R.string.emailOK)), null)
                                }
                                catch (e: Exception)
                                {
                                    Log.d("_ITEM", e.message.toString())
                                    Toast.makeText(con, R.string.emailNOK, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    return@setOnMenuItemClickListener true
                }
                //WYŚLIJ EMAIL
                R.id.sendEmail ->
                {
                    if(arrayItem[pos].getIdState() == 2)
                        return@setOnMenuItemClickListener true
                    var curUser : JSONObject
                    job4 = CoroutineScope(Dispatchers.IO).launch {
                        try
                        {
                            restPost.getStart(src = Me)
                            curUser = JSONObject(restPost.getRecData())

                            val bodyMail ="""{
                                                "message":
                                                    {
                                                        "subject": "toolExchange - Prośba o kontakt w sprawie wymiany narzędzia serwisowego.",
                                                        "body": {"contentType": "HTML", "content": "<p style='color: rgb(32, 31, 30); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial; font-size: 11pt; font-family: Calibri, sans-serif; margin: 0px;'>Cześć [Hello],<br><br><strong>Użytkownik [User]:</strong><br>
                                                                                    ${curUser.getString("displayName")}<br/><br/><strong>Prosi o pilny kontakt w sprawie wysyłki narzędzia serwisowego</br>[It is asking for immediate contact in service tool sending]:
                                                                                    </strong><br>${arrayItem[pos].getName()}<br/><br/>
                                                                                    <strong>Numery ewidencyjne: [AKTUALNY | POPRZEDNI]</br>[Inner ID numbers (CURRENT | OLD)]:</strong><br>${arrayItem[pos].getEwidence()} | ${arrayItem[pos].getEwidenceOld()}<br/><br/>
                                                                                    <strong>Numer seryjny:</br>[Serial number]:</strong><br>${arrayItem[pos].getSerial()}<br><br>
                                                                                    <strong>O poniższych cechach </br>[About these features]:</strong><br>${arrayItem[pos].getFeatures()}<br><br>Skontaktuj się z nim proszę
                                                                                    poniższymi kanałami </br> [Please, contact him thease channels]:<br><strong>EMAIL: </strong>${curUser.getString("mail")}<br><strong>TELEFON [PHONE]: </strong>${curUser.getString("mobilePhone")}</p>
                                                                                    <p style='color: rgb(0, 0, 0); font-style: normal; font-variant-ligatures: normal; font-variant-caps:
                                                                                    normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px;
                                                                                    text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width:
                                                                                    0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style:
                                                                                    initial; text-decoration-color: initial; font-size: 11pt; font-family: Calibri, sans-serif; margin:
                                                                                    0px 0px 0px 0.75pt;'><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit;
                                                                                    font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 10pt; line-height:
                                                                                    inherit; font-family: Helv, sans-serif; vertical-align: baseline; color: black;'><br></span><span 
                                                                                    style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit; font-weight:
                                                                                    inherit; font-stretch: inherit; font-size: 9pt; line-height: inherit; font-family: Arial, sans-serif;
                                                                                    vertical-align: baseline; color: black;'>Pozdrowienia / Kind regards,</span></p>
                                                                                    <p style='color: rgb(0, 0, 0); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial; font-size: 11pt; font-family: Calibri, sans-serif; margin: 0px 0px 0px 0.75pt;'><strong><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 10pt; line-height: inherit; font-family: Arial, sans-serif; vertical-align: baseline; color: black;'>toolExchange</span></strong></p>
                                                                                    <p style='color: rgb(32, 31, 30); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial; font-size: 11pt; font-family: Calibri, sans-serif; margin: 0px;'><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 9pt; line-height: inherit; font-family: Arial, sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>Prosimy nie odpowiadać, wiadomość generowana automatycznie.</span>
                                                                                    <br><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit;
                                                                                    font-weight: inherit; font-stretch: inherit; font-size: 9pt; line-height: inherit; font-family: Arial,
                                                                                    sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>Please do not reply,
                                                                                    automatically&nbsp; generated message. &nbsp;&nbsp;</span><br/><strong><span style='margin: 0px;
                                                                                    padding: 0px; border: 0px; font-style: inherit; font-variant: inherit; font-weight: inherit;
                                                                                    font-stretch: inherit; font-size: 9pt; line-height: inherit; font-family: Arial, sans-serif;
                                                                                    vertical-align: baseline; color: rgb(79, 79, 79);'>&nbsp; &nbsp; &nbsp;&nbsp;</span></strong>
                                                                                    <strong><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit;
                                                                                    font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 9pt; line-height: inherit;
                                                                                    font-family: Arial, sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>&nbsp; &nbsp; &nbsp;
                                                                                    </span></strong></p>
                                                                                    <p style='color: rgb(32, 31, 30); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; white-space: normal; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial; font-size: 11pt; font-family: Calibri, sans-serif; margin: 0px;'><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 9pt; line-height: inherit; font-family: Arial, sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>Yamazaki Mazak Central Europe Sp. z o.o., Oddział w Polsce</span><br>
                                                                                    <span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit;
                                                                                    font-weight: inherit; font-stretch: inherit; font-size: 8pt; line-height: inherit; font-family: Arial,
                                                                                    sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>Technology Center Poland,<strong>&nbsp;
                                                                                    </strong>Trasa Renc&oacute;w 33<strong>,&nbsp;</strong>40-865 Katowice, Poland<strong>&nbsp;
                                                                                    </strong></span><strong><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit;
                                                                                    font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 10pt; line-height: inherit;
                                                                                    font-family: Helv, sans-serif; vertical-align: baseline; color: rgb(79, 79, 79);'>&nbsp;&nbsp;</span>
                                                                                    </strong><strong><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit;
                                                                                    font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 10pt;
                                                                                    line-height: inherit; font-family: Helv, sans-serif; vertical-align: baseline;
                                                                                    color: rgb(79, 79, 79);'>&nbsp; &nbsp;</span></strong><span style='margin: 0px; padding: 0px;
                                                                                    border: 0px; font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit;
                                                                                    font-size: 10pt; line-height: inherit; font-family: Helv, sans-serif; vertical-align: baseline;
                                                                                    color: rgb(79, 79, 79);'><br></span><strong><span style='margin: 0px; padding: 0px; border: 0px;
                                                                                    font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit;
                                                                                    font-size: 8pt; line-height: inherit; font-family: Arial, sans-serif; vertical-align: baseline;
                                                                                    color: rgb(255, 66, 30);'>W:</span></strong><span style='margin: 0px; padding: 0px; border: 0px;
                                                                                    font-style: inherit; font-variant: inherit; font-weight: inherit; font-stretch: inherit; font-size: 8pt;
                                                                                    line-height: inherit; font-family: Arial, sans-serif; vertical-align: baseline; color: gray;'>&nbsp;
                                                                                    </span><strong><span style='margin: 0px; padding: 0px; border: 0px; font-style: inherit; font-variant: inherit;
                                                                                    font-weight: inherit; font-stretch: inherit; font-size: 8pt; line-height: inherit; font-family: Arial, sans-serif;
                                                                                    vertical-align: baseline; color: rgb(79, 79, 79);'>www.mazakeu.pl</span></strong></p>"},
                                                        "toRecipients": [
                                                            {
                                                                "emailAddress": {
                                                                    "address": "${arrayItem[pos].getUser()}"
                                                                }
                                                            }
                                                        ],
                                                        
                                                        ${""/*from": 
                                                        {
                                                            "emailAddress": 
                                                            {
                                                                "name": "toolExchange - YAMAZAKI Mazak CEPL",
                                                                "address": "toolExchange@mazak.com.pl"
                                                            }
                                                        }*/}
                                                        
                                                    }
                                            }
                                            """
                            restPost.getStart(HttpMethod.Post, SendEmail, postData = bodyMail.replace("  ", "").replace("\n", ""), contentT = ContentType.Application.Json)
                            Toast.makeText(con, R.string.emailOK, Toast.LENGTH_SHORT).show()
                        }
                        catch (e: Exception)
                        {
                            println(e.message)
                            Toast.makeText(con, R.string.errREST, Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                //RĘCZNIE PRZYJMIJ NARZĘDZIE
                R.id.manualAdd ->
                {
                    job4 = CoroutineScope(Dispatchers.IO).launch{
                        try
                        {
                            //POBIERAMY INFORMACJE KTO JEST ZALOGOWANY - ADRES EMAIL
                            restPost.getStart(src = Me, contentT = ContentType.Application.Json)
                            val me = JSONObject(restPost.getRecData()).getString("mail")

                            //POBIERAMY ID SharePoint ZALOGOWANEGO UŻYTKOWNIKA
                            restPost.getStart(src = IdUser, getData = "'$me')")
                            val idUser = JSONObject(restPost.getRecData()).getJSONArray("value").getJSONObject(0).getString("id")

                            //POBRANIE AKTUALNEGO STATUSU NARZĘDZIA
                            restPost.getStart(src = Edit, getData = "${arrayItem[pos].getId()}?\$select=id&\$expand=fields(\$select=STATUSLookupId)")
                            val tool = JSONObject(restPost.getRecData()).getJSONObject("fields").getInt("STATUSLookupId")

                            //JEŻELI STATUS == 4 - WYCOFANE WIĘC NIE MOŻNA PRZYJĄĆ
                            if(tool == 4)
                            {
                                errState.show()
                                return@launch
                            }
                            else if(tool == 1 && me.normalize().indexOf(arrayItem[pos].getUser().normalize(), ignoreCase = true) > -1)
                            {
                                //KIEDY JUŻ POSIADAMY TO NARZĘDZIE
                                infoBar.setText(con.getString(R.string.errAddMe))
                                infoBar.show()
                                return@launch
                            }
                            else
                            {
                                job!!.cancel()
                                job1!!.cancel()
                                job2!!.cancel()
                            }

                            //USTAWIENIE NOWEGO UŻYTKOWNIKA
                            restPost.getStart(HttpMethod.Patch, Edit, "${arrayItem[pos].getId()}/fields", """{"USERLookupId": "$idUser", "STATUSLookupId": "1", "NEXT_USERLookupId": null}""", contentT = ContentType.Application.Json)
                            //DODANIE DANYCH NARZĘDZIA I WYŚWIETLENIE KOMUNIKATU O POWODZENIU
                            infoBar.setText(con.getString(R.string.manualSucces)+" ${arrayItem[pos].getName()} | ${arrayItem[pos].getEwidence()} \n- UŻUTKOWNIK: $me")
                            infoBar.show()

                            delay(1000)
                            //ODŚWIERZENIE LISTY
                            withContext(Dispatchers.Main){
                                bind!!.hideButton.callOnClick()
                                //bind!!.exitSearch.callOnClick()
                            }

                            return@launch
                        }
                        catch (e: Exception)
                        {
                            Log.d("_MANUAL_ADD", e.message.toString())
                            errREST.show()

                            return@launch
                        }
                    }
                    return@setOnMenuItemClickListener true
                }
                //RĘCZNIE ODDAJ NARZĘDZIE
                R.id.manualDel ->
                {
/*                    tmpNextUser = -1
                    if(arrayItem[pos].getIdState() == 2)
                        return@setOnMenuItemClickListener true
*/
                    job4 = CoroutineScope(Dispatchers.IO).launch{
                            try {
                               /* //zmienna przechowująca konstrukcję okna powiadomień
                                val builder = AlertDialog.Builder(con)

                                //POBRANIE LISTY UŻYTKOWNIKÓW - jeżeli ta nie jest jeszcze pobrana
                                if(listUser.isNullOrEmpty())
                                {
                                    restPost.getStart(
                                        src = usrList,
                                        contentT = ContentType.Application.Json
                                    )
                                    val lists : JSONArray =
                                        JSONObject(restPost.getRecData()).getJSONArray("value") //pomocnicza lista przechowująca wynik zapytania z MS GRAPH

                                    //USUWAMY Z LISTY POZYCJE NIE BĘDĄCE OSOBĄ, CZYLI UŻYTKOWNICY WBUDOWANI
                                    for (i in lists.length() - 1 downTo 0) {
                                        val tList = lists.getJSONObject(i).getJSONObject("fields")
                                        if (!tList.has("EMail") || tList.getString("EMail").indexOf("@mazak.com.pl") == -1)
                                        {
                                            lists.remove(i)
                                        }
                                    }

                                    //STOWRZENIE LISTY OBIEKTÓW Z URZYTKOWNIKAMI
                                    val tmpListUserMutable = mutableListOf<UserList>() //lista pomocnicza dla pętli for
                                    for (j in lists.length() - 1 downTo 0)
                                    {
                                        val tmpItmObj = UserList()
                                        var tmpUserOrMag : List<CheckActivity.Magss>?
                                        Log.d("_MANUAL_DEL", "MAKE i: ${j - 1}")
                                        val items = lists.getJSONObject(lists.length() - j - 1)
                                            .getJSONObject("fields")
                                        tmpUserOrMag = mags.filter { it.user == items.getString("EMail") }

                                        if (tmpUserOrMag.isNullOrEmpty())
                                        {
                                            tmpItmObj.set(
                                                items.getInt("id"),
                                                items.getString("Title"),
                                                items.getString("EMail"))
                                        }
                                        else
                                        {
                                            tmpItmObj.set(
                                                items.getInt("id"),
                                                tmpUserOrMag.first().name,
                                                items.getString("EMail"))
                                        }
                                        tmpListUserMutable.add(tmpItmObj)
                                    }

                                    listUser = tmpListUserMutable.toList() //gotowa lista użytkowników
                                }

                                //UTWORZENIE I WYŚWIELTENIE OKNA WYBORU UŻYTKOWNIKA
                                with(builder)
                                {
                                    setTitle("Do kogo wysyłasz narzędzie?:")
                                    //akcja po zatwierdzeniu przyciskiem OK
                                    setPositiveButton(R.string.saveBox){_, _ ->
                                        return@setPositiveButton}
                                    //akcja po anulowaniu przyciskiem
                                    setNegativeButton(R.string.cancelBox){ _, _ -> tmpNextUser = -1; return@setNegativeButton}
                                    setView(R.layout.email_box)

                                    //w wątku głównym
                                    withContext(Dispatchers.Main)
                                    {
                                        //wyświetlenie okna komunikatu
                                        bindAC = builder.show()

                                        //podpięcie autocomplete pod pole tekstowe komunikatu
                                        val nxtUsrAdapter = MyAdapterList(bindAC.context, listUser)
                                        bindAC.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!.setAdapter(nxtUsrAdapter)

                                        //akcja po wybraniu elementu listy podpowiedzi
                                        bindAC.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!.setOnItemClickListener { _, view, _, _ ->
                                            //zapisujemy ID użytkownika do zmiennej
                                            tmpNextUser = view.findViewById<TextView>(R.id.user_id).text.toString().toInt()
                                            //wprowadzamy dla efektu wizualnego adres email w pole textowe
                                            bindAC.findViewById<AutoCompleteTextView>(R.id.nxtUserChoose)!!.setText(view.findViewById<TextView>(R.id.user_email).text)
                                        }
                                    }
                                }

                                while (bindAC.isShowing)
                                {
                                    println("tmpNextUser: $tmpNextUser")
                                    delay(250)
                                }
                                //JEŻELI NIE WYBRANO UŻYTKOWNIKA Z LISTY BĄDŹ ANULOWANO
                                if(tmpNextUser == -1)
                                    return@launch
*/

                                //POBRANIE NAZWY AKTUALNIE ZALOGOWANEGO UŻYTKOWNIKA
                                restPost.getStart(src = Me, contentT = ContentType.Application.Json)
                                val me = JSONObject(restPost.getRecData()).getString("mail")

                                //POBRANIE AKTUALNEGO STATUSU NARZĘDZIA
                                restPost.getStart(src = Edit, getData = "${arrayItem[pos].getId()}?\$select=id&\$expand=fields(\$select=STATUSLookupId)")
                                val tool = JSONObject(restPost.getRecData()).getJSONObject("fields").getInt("STATUSLookupId")

                                //STATUS 4-WYCOFANE 3-BADANIE 2-LOGISTYKA WIĘC NIE MOŻNA JUŻ ODDAĆ
                                when
                                {
                                    tool == 2 || tool == 3 || tool == 4 ->
                                    {
                                        infoBar.setText(con.getString(R.string.errStateDel))
                                        infoBar.show()
                                        return@launch
                                    }
                                    me.normalize().indexOf(arrayItem[pos].getUser().normalize(), ignoreCase = true) == -1 ->
                                    {
                                        //JEŻELI NARZĘDZIE NIE JEST W POSIADANIU UŻYTKOWNIKA NIE MOŻE GO ODDAĆ
                                        infoBar.setText(con.getString(R.string.ErrDelNoIsset))
                                        infoBar.show()
                                        return@launch
                                    }
                                    else ->
                                    {
                                        job!!.cancel()
                                        job1!!.cancel()
                                        job2!!.cancel()
                                    }
                                }


/*
                                //USTAWIENIE STATUSU LOGISTYKA
                                restPost.getStart(HttpMethod.Patch, Edit, "${arrayItem[pos].getId()}/fields", """{"STATUSLookupId": "2", "NEXT_USERLookupId": "$tmpNextUser"}""", contentT = ContentType.Application.Json)

                                //SPRAWDZAMY CZY OSOBA DO KTÓREJ WYSYŁAMY TO MAGAZYN NARZĘDZI
                                //tmpNextUser - kolejny user id
                                //listUser - lista użytkowników
                                //userOrMag - obiekt z danymi kolejnego urzytkownika
                                //tempNameNextUser - tymczasowa zmienna przechowująca nazwę kolejnego urzytkownika bądź magazynu

                                val userOrMag = listUser.filter { it.getId() == tmpNextUser }
                                val ifMags : List<CheckActivity.Magss> = mags.filter{ it.user == userOrMag.first().getEmail() }
                                var tempNameNextUser = userOrMag.first().getName()

                                if(!ifMags.isNullOrEmpty())
                                    tempNameNextUser = ifMags.first().name

                                infoBar.setText(con.getString(R.string.manualSucces)+" ${arrayItem[pos].getName()} | ${arrayItem[pos].getEwidence()} \n- LOGISTYKA: $tempNameNextUser")
                                infoBar.show()

                               */
                                val act = Intent(th, EmailBox::class.java)
                                act.putExtra("logToken", th.intent.getStringExtra("logToken"))
                                act.putExtra("idTool", arrayItem[pos].getId())
                                act.putExtra("titleTool", arrayItem[pos].getName())

                                startActivity(th, act, null)

                                //ODŚWIERZENIE LISTY
                                delay(1000)
                                withContext(Dispatchers.Main){bind!!.hideButton.callOnClick()}

                                return@launch
                            }
                            catch (e: Exception)
                            {
                                Log.d("_MANUAL_DEL", e.message.toString())
                                errREST.show()

                                return@launch
                            }
                        }
                    return@setOnMenuItemClickListener true
                }
            }

            return@setOnMenuItemClickListener true
        }

        popup.show()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MyViewHolder
    {
        // tworzymy layout artykułu oraz obiekt ViewHoldera
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item, viewGroup, false)

        // dla elementu listy ustawiamy obiekt OnClickListener
        //view.setOnLongClickListener {  }
        view.setOnClickListener { // odnajdujemy indeks klikniętego elementu
            v -> val position = mRecyclerView.getChildAdapterPosition(v)
            if(/*arrayItem[position].getIdState() != 2 &&*/ arrayItem[position].getIdState() != 3 && arrayItem[position].getIdState() != 4 && arrayItem[position].getIdState() != 0)
                //wykluczamy statusy: logistyka, kalibracja / badanie, wycofane, brak
                showPopupMenu(view, position, view.context)
        }

        // tworzymy i zwracamy obiekt ViewHolder
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: MyViewHolder, i: Int)
    {
        // uzupełniamy layout artykułu
        val itemsTool: ItemTool = this.arrayItem[i]
        (viewHolder as MyViewHolder?)!!.name.text = itemsTool.getName()
        viewHolder.features.text = itemsTool.getFeatures()
        when {
            itemsTool.getIdState() == 1 -> {
                val text = itemsTool.getState()+" : "+itemsTool.getUser()
                viewHolder.state.text = text
            }
            itemsTool.getIdState() == 2 -> {
                val tmpUserOrMag =
                    mags.filter { it.user.trim().lowercase().normalize() == (itemsTool.getNext().trim().lowercase().normalize()) }

                viewHolder.state.text = if (tmpUserOrMag.isEmpty()) {itemsTool.getState()+" : "+itemsTool.getNext()} else {tmpUserOrMag.first().name}
            }
            else -> viewHolder.state.text = itemsTool.getState()
        }
        viewHolder.cert.text = itemsTool.getCertificate()
        viewHolder.ewid.text = "${itemsTool.getEwidence()} | ${itemsTool.getEwidenceOld()}"
        viewHolder.serial.text = itemsTool.getSerial()
        val now = LocalDate.now()
        val exp = LocalDate.parse(itemsTool.getCertificate())
        val period = Period.between(now, exp)
        if(period.years <= 0 && period.months <= 0  && period.days <= 0 )  //jeżeli jest po czasie
            viewHolder.img.setImageResource(R.drawable.inputroundnok)
        else if((period.years > 0 || period.months > 0) || (period.years == 0  && period.months == 0 && period.days >= 30))   // jeżeli jest 30 przed czasem
            viewHolder.img.setImageResource(R.drawable.inputroundok)
        else
            viewHolder.img.setImageResource(R.drawable.inputroundnormal)

        if(period.years == 0 && period.months == 0 && (period.days in 1..99))
            viewHolder.number.text = period.get(ChronoUnit.DAYS).toString()
    }

    override fun getItemCount(): Int {
        return arrayItem.size
    }
}

