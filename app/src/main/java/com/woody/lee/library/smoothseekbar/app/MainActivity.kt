package com.woody.lee.library.smoothseekbar.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.woody.lee.library.smoothseekbar.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressPercent.onProgressChanged = { progress ->
            binding.textPercent.text = "$progress %"
        }
        binding.progressPercent.setProgress(50, false)

        binding.progressStar.onProgressChanged = { star ->
            binding.textStar.text = "$star Star"
        }
        binding.progressStar.setProgress(3, false)


        binding.progressMoney.onProgressChanged = { money ->
            binding.textMoney.text = "$money $"
        }
        binding.progressMoney.setProgress(50, false)

        binding.progressMonth.onProgressChanged = { month ->
            binding.textMonth.text = "${months[month]}"
        }
        binding.progressMonth.setProgress(6, false)

        binding.progressDay.onProgressChanged = { day ->
            binding.textDay.text = "${days[day]}"
        }
        binding.progressDay.setProgress(3, false)
    }
}
