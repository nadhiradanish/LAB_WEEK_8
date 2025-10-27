package com.example.lab_week_8

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkInfo // optional, hanya jika kamu mau pakai info.state == WorkInfo.State.SUCCEEDED
import com.example.lab_week_8.worker.FirstWorker
import com.example.lab_week_8.worker.SecondWorker

class MainActivity : AppCompatActivity() {

    // WorkManager akan diinisialisasi nanti di onCreate
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inisialisasi WorkManager dengan context Activity yang sudah siap
        workManager = WorkManager.getInstance(this)

        // Menyesuaikan padding untuk edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Membuat constraint agar worker hanya jalan saat ada koneksi internet
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        // Membuat request untuk FirstWorker
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        // Membuat request untuk SecondWorker
        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        // Menjalankan urutan pekerjaan: FirstWorker lalu SecondWorker
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // Observasi status FirstWorker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        // Observasi status SecondWorker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")
                }
            }
    }

    // Membuat input data untuk worker
    private fun getIdInputData(idKey: String, idValue: String): Data =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    // Menampilkan hasil dalam bentuk Toast
    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
