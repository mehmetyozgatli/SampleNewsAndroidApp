package com.myapps.wearenginepractice.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.myapps.wearenginepractice.R
import androidx.lifecycle.ViewModelProvider
import com.myapps.wearenginepractice.db.ArticleDatabase
import com.myapps.wearenginepractice.repository.NewsRepository
import com.myapps.wearenginepractice.ui.NewsViewModel
import com.myapps.wearenginepractice.ui.NewsViewModelProviderFactory
import kotlinx.android.synthetic.main.fragment_qrcode.*
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.monitor.MonitorClient
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.SendCallback
import java.nio.charset.StandardCharsets

class QrCodeFragment : Fragment(R.layout.fragment_qrcode) {

    private lateinit var viewModel: NewsViewModel

    private var p2pClient: P2pClient? = null
    private var monitorClient: MonitorClient? = null
    private var deviceClient: DeviceClient? = null

    private val deviceList: MutableList<Device> = mutableListOf()
    private var connectedDevice: Device? = null

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                val uri = data?.data

                userQrCodeImageView.setImageBitmap(viewModel.generateQrCode(requireContext(), uri!!))
                qrCodeValue.text = viewModel.convertPdfBitmapToQrCodeResult(requireContext(), uri)
                sendP2pMessage(viewModel.convertPdfBitmapToQrCodeResult(requireContext(), uri))
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)

        // Wearable App Information
        val peerPkgName = "com.myapps.wearenginepractices"
        val peerFingerPrint = "com.myapps.wearenginepractices_BELk4ejgPpN6GP2gM0298wTEaYETQnnZEruLwVIq3ao1024w41Vpb7faxlyN+nHqB0GsZ1V2WiCoTe4ykGGXzS0="

        p2pClient = HiWear.getP2pClient(requireContext())
        deviceClient = HiWear.getDeviceClient(requireContext())
        monitorClient = HiWear.getMonitorClient(requireContext())

        p2pClient?.setPeerPkgName(peerPkgName)
        p2pClient?.setPeerFingerPrint(peerFingerPrint)

        boundedAndConnectedDevice()

        uploadQrCode.setOnClickListener {
            val intent = Intent()
            intent.type = "application/pdf"
            intent.action = Intent.ACTION_GET_CONTENT
            resultLauncher.launch(intent)
        }

    }

    private fun sendP2pMessage(messageStr: String){

        val builder: Message.Builder = Message.Builder()
        builder.setPayload(messageStr.toByteArray(StandardCharsets.UTF_8))
        val sendMessage: Message = builder.build()

        val sendCallback: SendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {}
            override fun onSendProgress(progress: Long) {}
        }

        if (connectedDevice != null && connectedDevice?.isConnected == true) {
            p2pClient?.send(connectedDevice, sendMessage, sendCallback)
                ?.addOnSuccessListener {
                    Toast.makeText(requireContext(), "message sent successfully", Toast.LENGTH_LONG).show()
                }
                ?.addOnFailureListener {
                    Toast.makeText(requireContext(), "message not sent", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun boundedAndConnectedDevice(){
        deviceClient?.bondedDevices?.addOnSuccessListener(OnSuccessListener { devices ->
            if (devices == null || devices.size == 0) {
                Toast.makeText(
                    requireContext(), "getBondedDevices list is null or list size is 0",
                    Toast.LENGTH_LONG
                ).show()
                return@OnSuccessListener
            }
            deviceList.addAll(devices)

            if (deviceList.isNotEmpty()) {
                for (device in deviceList) {
                    if (device.isConnected) {
                        connectedDevice = device
                        Toast.makeText(
                            requireContext(), "getBondedDevices onSuccess! devices list size = "
                                    + devices.size, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        })?.addOnFailureListener {
            Toast.makeText(
                requireContext(), "getBondedDevices task submission error",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}