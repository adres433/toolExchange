package com.example.toolexchange


//-----------KLASA PRZECHOWUJĄCA NARZEDZIA -----------------
class ItemTool
{
    private var name : String = "Nazwa"
    private var features : String = "Cecha"
    private var serial : String = "SERIAL"
    private var certificate : String = "2021-12-31"
    private var nrEwidence : String = "P123/PL"
    private var nrEwidenceOld : String = "T123"
    private var user : String = "email@mazak.com.pl"
    private var state : String = "STATUS"
    private var phoneNr : String = "123456789"
    private var token : String = ""
    private var nextUser : String = "nxtemail@mazak.com.pl"
    private var idTool : Int = 0
    private var idState : Int = 0
    private var idUser : Int = 0

    fun init(
        pName : String,
        pFeatures : String,
        pSerial : String,
        pCertificate : String,
        pNrEwidence : String,
        pNrEwidenceOld : String,
        pUser : String,
        pState : String,
        pPhoneNr : String,
        pToken : String,
        pNextUser : String,
        pIdTool : Int,
        pIdState : Int,
        pIdUser : Int)
    {
        name = pName
        features = pFeatures
        serial = pSerial
        certificate = pCertificate
        nrEwidence = pNrEwidence
        nrEwidenceOld = pNrEwidenceOld
        user = pUser
        state = pState
        phoneNr = pPhoneNr
        token = pToken
        nextUser = pNextUser
        idTool = pIdTool
        idState = pIdState
        idUser = pIdUser

    }

    fun getName() : String
    {
        return name
    }

    fun getFeatures() : String
    {
        return features
    }

    fun getEwidence() : String
    {
        return "Nr ew: $nrEwidence"
    }

    fun getEwidenceOld() : String
    {
        return nrEwidenceOld
    }

    fun getSerial() : String
    {
        return "S/N:  $serial"
    }

    fun getUser() : String
    {
        return user
    }

    fun getToken() : String
    {
        return token
    }

    fun getIdState() : Int
    {
        return idState
    }

    fun getState() : String
    {
        return state
    }

    fun getPhone() : String
    {
        return phoneNr
    }

    fun getCertificate() : String
    {
        return certificate
    }

    fun getId() : Int
    {
        return idTool
    }

    fun getNext() : String
    {
        return nextUser
    }


}
//---------------------- END -------------------------------
