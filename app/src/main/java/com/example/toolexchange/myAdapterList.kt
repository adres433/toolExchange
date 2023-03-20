package com.example.toolexchange

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView

class MyAdapterList(private val mContext: Context, var mObjects: List<UserList>) : ArrayAdapter<UserList>(mContext, 0)
{
    var mObjectsFiltered = mObjects

    override fun getFilter(): Filter {
        return object : Filter()
        {
            override fun performFiltering(p0: CharSequence?): FilterResults
            {
                val endResult = FilterResults()
                val value = p0.toString().lowercase().trim().normalize()
                val results : List<UserList> = if(value.isEmpty()) mObjects

                else
                {
                    mObjects.filter {
                        it.getEmail().lowercase().trim().normalize().contains(value, true) || it.getName().lowercase().trim().normalize().contains(value, true)
                    }
                }
                endResult.values = results
                endResult.count = results.size
                return endResult
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?)
            {
                this@MyAdapterList.clear()
                mObjectsFiltered = p1!!.values as List<UserList>
                this@MyAdapterList.addAll(mObjectsFiltered)
                this@MyAdapterList.notifyDataSetChanged()
            }
        }
    }

    override fun getCount(): Int
    {
        return mObjectsFiltered.size
    }

    override fun getView(position: Int, pconvertView: View?, parent: ViewGroup): View
    {

        val convertView = LayoutInflater.from(mContext).inflate(R.layout.email_box_list,parent, false)

        val idUserList : TextView = convertView.findViewById(R.id.user_id)
        val nameUserList : TextView = convertView.findViewById(R.id.user_full_name)
        val emailUserList : TextView = convertView.findViewById(R.id.user_email)

        idUserList.text = mObjectsFiltered[position].getId().toString()
        nameUserList.text = mObjectsFiltered[position].getName()
        emailUserList.text = mObjectsFiltered[position].getEmail()

        return convertView!!
    }

}

class UserList
{
    private var id : Int
    private var name: String
    private var email: String

    init {
        id = 0
        name = ""
        email = ""
    }

    fun set(pId: Int, pName: String, pEmail: String)
    {
        id = pId
        name = pName
        email = pEmail
    }

    fun getId() : Int
    {
        return id
    }
    fun getName() : String
    {
        return name
    }
    fun getEmail() : String
    {
        return email
    }
}