package com.faridnia.mystrava.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.faridnia.mystrava.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        setContentView(R.layout.activity_main)


        return super.onCreateView(name, context, attrs)

    }
}