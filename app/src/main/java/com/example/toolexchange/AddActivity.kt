package com.example.toolexchange


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.google.android.material.snackbar.Snackbar
import io.ktor.http.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URLEncoder


class AddActivity : AppCompatActivity() {
    private lateinit var codeScannerAdd: CodeScanner
    private var rest = DataExchange()
    private var mTitle = ""
    private var infoBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        val scannerViewAdd = findViewById<CodeScannerView>(R.id.scannerViewAdd)
        if (!intent.hasExtra("logToken"))
            finish()

        infoBar = Snackbar.make(findViewById(R.id.mainLayout), "", Snackbar.LENGTH_LONG)
        rest.init(applicationContext, intent.getStringExtra("logToken"))

        codeScannerAdd = CodeScanner(this, scannerViewAdd)
        codeScannerAdd.camera = CodeScanner.CAMERA_BACK
        codeScannerAdd.formats = CodeScanner.ALL_FORMATS
        codeScannerAdd.autoFocusMode = AutoFocusMode.SAFE
        codeScannerAdd.isAutoFocusEnabled = true
        codeScannerAdd.isFlashEnabled = false
        codeScannerAdd.isTouchFocusEnabled = true

        codeScannerAdd.decodeCallback = DecodeCallback {
            runOnUiThread {

                val errREST = Toast.makeText(this, getString(R.string.errREST), Toast.LENGTH_LONG)
                val errState =
                    Toast.makeText(this, getString(R.string.errStateAdd), Toast.LENGTH_LONG)
                runBlocking {
                    launch {

                        try {
                            saveItem()
                            rest.getStart(
                                src = DataExchange.Source.Me,
                                contentT = ContentType.Application.Json
                            )
                            val me = JSONObject(rest.getRecData()).getString("mail")

                            rest.getStart(
                                src = DataExchange.Source.IdUser, getData = "'$me')"
                            )
                            val idUser =
                                JSONObject(rest.getRecData()).getJSONArray("value").getJSONObject(0)
                                    .getString("id")

                            rest.getStart(
                                src = DataExchange.Source.Edit,
                                getData = "?\$filter=startswith(fields/QRCODE, '${
                                    URLEncoder.encode(
                                        it.text,
                                        "utf-8"
                                    )
                                }')&\$expand=fields(\$select=Title,id,STATUSLookupId)"
                            )
                            val tool =
                                JSONObject(rest.getRecData()).getJSONArray("value").getJSONObject(0)
                                    .getJSONObject("fields")

                            //STATUS 4- WYCOFANE
                            if (tool.getInt("STATUSLookupId") == 4) {
                                errState.show()
                                finish()
                                return@launch
                            }

                            rest.getStart(
                                HttpMethod.Patch,
                                DataExchange.Source.Edit,
                                "${tool.getString("id")}/fields",
                                """{"USERLookupId": "$idUser", "NEXT_USERLookupId": null, "STATUSLookupId": "1"}""",
                                contentT = ContentType.Application.Json
                            )
                            mTitle = tool.getString("Title")
                            return@launch
                        }
                        catch (e: Exception)
                        {
                            Log.d("_ADD", e.message.toString())
                            errREST.show()
                            finish()
                        }
                    }
                }
                //codeScannerAdd.startPreview()
            }
        }
        codeScannerAdd.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.cameraError) + it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        codeScannerAdd.startPreview()
    }

    override fun onResume() {
        super.onResume()
        codeScannerAdd.startPreview()
    }

    override fun onPause() {
        super.onPause()
        codeScannerAdd.releaseResources()
    }

    private fun saveItem() {
        CoroutineScope(Dispatchers.Main).launch {
            while (mTitle == "")
            {
                println("Czekam na wstawienie danych")
                delay(250)
            }
            infoBar!!.setText(getString(R.string.manualSucces) + " $mTitle")
            infoBar!!.show()
            delay(1000)
            finish()
        }
    }
}