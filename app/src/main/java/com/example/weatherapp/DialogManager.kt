package com.example.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import com.example.weatherapp.R


object DialogManager {
    fun locationSettingDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle("Enable Location")
        dialog.setMessage("Location disabled, do you want enable")
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES") { _, _ ->
            listener.onClick(null)
            dialog.dismiss()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO") { _, _ ->
            dialog.dismiss()
        }
        dialog.show()
    }

    fun searchByNameDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val customLayout = inflater.inflate(R.layout.search_city, null)
        builder.setView(customLayout)

        val dialog = builder.create()
        dialog.show()

        val edName = customLayout.findViewById<EditText>(R.id.edSearchCity)
        val bCancel = customLayout.findViewById<Button>(R.id.bCancel)
        val bOk = customLayout.findViewById<Button>(R.id.bOk)

        edName.requestFocus()
        bCancel.setOnClickListener {
            dialog.dismiss() // закрываем диалог
        }

        bOk.setOnClickListener {
            val cityName = edName.text.toString() // Получаем текст из EditText
            listener.onClick(cityName)
            dialog.dismiss()
        }

        dialog.show()
    }

    interface Listener {

        fun onClick(name: String?)
    }
}
