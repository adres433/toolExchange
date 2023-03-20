package com.example.toolexchange

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import io.ktor.http.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URLEncoder


class DelActivity : AppCompatActivity()
{
    private lateinit var codeScannerDel: CodeScanner
    private var rest = DataExchange()
    private var tool : JSONObject? = null
    private var token = ""
    private var act : Intent? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_del)

        act = Intent(applicationContext, EmailBox::class.java)
        token = if(intent.hasExtra("logToken")) intent.getStringExtra("logToken")!! else ""
        val scannerViewDel = findViewById<CodeScannerView>(R.id.scannerViewDel)

        if(token.isNotEmpty())
        {
            act!!.putExtra("logToken", token)
            act!!.putExtra("src", 0x2d)
        }
        rest.init(applicationContext, intent.getStringExtra("logToken"))

        codeScannerDel = CodeScanner(this, scannerViewDel)
        codeScannerDel.camera = CodeScanner.CAMERA_BACK
        codeScannerDel.formats = CodeScanner.ALL_FORMATS
        codeScannerDel.autoFocusMode = AutoFocusMode.SAFE
        codeScannerDel.isAutoFocusEnabled = true
        codeScannerDel.isFlashEnabled = false
        codeScannerDel.isTouchFocusEnabled = true


        codeScannerDel.decodeCallback = DecodeCallback{
                runOnUiThread{

                   // val infoBar = Toast.makeText(this, getString(R.string.manualSucces), Toast.LENGTH_LONG)
                    val errREST = Toast.makeText(this, getString(R.string.errREST), Toast.LENGTH_LONG)
                    val errState = Toast.makeText(this, getString(R.string.errStateDel), Toast.LENGTH_LONG)
                    runBlocking {
                        launch {
                        try {
                            //POBRANIE DANYCH ZESKANOWANEGO KODU
                            rest.getStart(
                                HttpMethod.Get,
                                DataExchange.Source.Edit,
                                getData = "?\$filter=startswith(fields/QRCODE, '${
                                    URLEncoder.encode(
                                        it.text,
                                        "utf-8"
                                    )
                                }')&\$expand=fields(\$select=Title,id,STATUSLookupId)"
                            )
                            tool = JSONObject(rest.getRecData()).getJSONArray("value")
                                .getJSONObject(0).getJSONObject("fields")

                            //STATUS 4-WYCOFANE 3-BADANIE 2-LOGISTYKA
                            if (tool!!.getInt("STATUSLookupId") == 2 || tool!!.getInt("STATUSLookupId") == 3 || tool!!.getInt(
                                    "STATUSLookupId") == 4)
                            {
                                errState.show()
                                onResume()
                                return@launch
                            }

                            act!!.putExtra("idTool", tool!!.getInt("id"))
                            act!!.putExtra("titleTool", tool!!.getString("Title"))
                            startActivity(act)

                                finish()

                            }
                            catch (e: Exception)
                            {
                                Log.d("_DELETE", e.message.toString())
                                errREST.show()
                                finish()
                            }
                        }
                    }

                }
        }
        codeScannerDel.errorCallback = ErrorCallback{
            runOnUiThread {
                Toast.makeText(this, getString(R.string.cameraError)+it.message, Toast.LENGTH_SHORT).show()
            }
        }
        codeScannerDel.startPreview()
    }

    override fun onResume()
    {
        super.onResume()
        codeScannerDel.startPreview()
    }

    override fun onPause()
    {
        super.onPause()
        codeScannerDel.releaseResources()
    }
}