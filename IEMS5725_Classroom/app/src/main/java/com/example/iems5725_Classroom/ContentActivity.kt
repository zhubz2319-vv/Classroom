package com.example.iems5725_Classroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.iems5725_Classroom.ui.theme.IEMS5725_ClassTheme

class ContentActivity : ComponentActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IEMS5725_ClassTheme {
                val courseName = intent.getStringExtra("course_name")
                val courseCode = intent.getStringExtra("course_code")
                CourseUI(courseName.toString(),courseCode.toString())
            }
        }
    }

}