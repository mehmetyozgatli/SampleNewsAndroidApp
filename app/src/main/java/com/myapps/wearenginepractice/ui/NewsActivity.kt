package com.myapps.wearenginepractice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.myapps.wearenginepractice.R
import com.myapps.wearenginepractice.db.ArticleDatabase
import com.myapps.wearenginepractice.repository.NewsRepository
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : AppCompatActivity() {

    private lateinit var viewModel: NewsViewModel
    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private val REQUEST_EXTERNAL_STORAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        val newsRepository = NewsRepository(ArticleDatabase(this))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        bottomNavigationView.setupWithNavController(newsHostFragment.findNavController())

        initData()
        verifyStoragePermissions()
    }

    /**
     * Initialization: Obtain the authorization.
     */
    private fun initData() {
        val authCallback: AuthCallback = object : AuthCallback {
            override fun onOk(permissions: Array<Permission>) {
                Log.d("TAG", "getAuthClient onOk")
            }

            override fun onCancel() {
                Log.e("TAG", "getAuthClient onCancel")
            }
        }
        HiWear.getAuthClient(this)
            .requestPermission(authCallback, Permission.DEVICE_MANAGER, Permission.NOTIFY)
            .addOnSuccessListener {
                Log.d("TAG","getAuthClient onSuccess")
            }
            .addOnFailureListener {
                Log.d("TAG","getAuthClient onFailure")
            }
    }

    /**
     * Apply for the read permission on the external storage device.
     */
    private fun verifyStoragePermissions() {
        val permission = ActivityCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
