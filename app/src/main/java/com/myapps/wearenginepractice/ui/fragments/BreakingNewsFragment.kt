package com.myapps.wearenginepractice.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.monitor.MonitorClient
import com.huawei.wearengine.p2p.*
import com.myapps.wearenginepractice.R
import com.myapps.wearenginepractice.adapters.NewsAdapter
import com.myapps.wearenginepractice.db.ArticleDatabase
import com.myapps.wearenginepractice.repository.NewsRepository
import com.myapps.wearenginepractice.ui.NewsViewModel
import com.myapps.wearenginepractice.ui.NewsViewModelProviderFactory
import com.myapps.wearenginepractice.util.Resource
import kotlinx.android.synthetic.main.fragment_breaking_news.*
import java.util.*
import java.nio.charset.StandardCharsets


class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news) {

    private lateinit var viewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter

    private var p2pClient: P2pClient? = null
    private var monitorClient: MonitorClient? = null
    private var deviceClient: DeviceClient? = null

    private val deviceList: MutableList<Device> = mutableListOf()
    private var connectedDevice: Device? = null

    private val TAG = "BreakingNewsFragment"

    // Wear Engine Receiver
    private val receiver =
        Receiver { message ->
            if (message != null) {
                val data = String(message.data)

                val bundleNewsUrl = Bundle().apply {
                    putString("newsUrl", data)
                }
                findNavController().navigate(
                    R.id.action_breakingNewsFragment_to_articleFragment,
                    bundleNewsUrl
                )
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_breaking_news, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newsRepository = NewsRepository(ArticleDatabase(requireContext()))
        val viewModelProviderFactory = NewsViewModelProviderFactory(newsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(NewsViewModel::class.java)
        setupRecyclerView()

        // Wearable App Information
        val peerPkgName = "com.myapps.wearenginepractices"
        val peerFingerPrint = "com.myapps.wearenginepractices_BLZuSIOv61FbrIyDenjI5b0A0+pbnm0atCNzpIt0OcMom3AT0+I+dlQE743fc9VcU5tkb9G5Edy5budtpSYrX9g="

        p2pClient = HiWear.getP2pClient(requireContext())
        deviceClient = HiWear.getDeviceClient(requireContext())
        monitorClient = HiWear.getMonitorClient(requireContext())

        p2pClient?.setPeerPkgName(peerPkgName)
        p2pClient?.setPeerFingerPrint(peerFingerPrint)

        // Adapter OnClick
        newsAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("article", it)
            }
            findNavController().navigate(
                R.id.action_breakingNewsFragment_to_articleFragment,
                bundle
            )
        }


        viewModel.breakingNews.observe(viewLifecycleOwner, { response ->
            when (response) {
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let { newsResponse ->
                        newsAdapter.differ.submitList(newsResponse.articles)
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e(TAG, "An error occured: $message")
                    }
                }
                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.wear_engine_settings, menu)
    }

    private fun hideProgressBar() {
        paginationProgressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        paginationProgressBar.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter()
        rvBreakingNews.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(activity)

        }
    }

    /**
     * Toolbar Options Menu For Wear Engine Processes.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.checkAvailableDevices) {
            deviceClient?.hasAvailableDevices()?.addOnSuccessListener {
                if (it) {
                    Toast.makeText(requireContext(), "There is watch available", Toast.LENGTH_LONG)
                        .show()
                }
            }?.addOnFailureListener {
                it.message?.let { exception -> Log.i("availableDevice", exception) }
            }
        }

        if (item.itemId == R.id.queryWearableDevice) {
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

        if (item.itemId == R.id.pingBoundedDevice) {

            if (connectedDevice != null && connectedDevice?.isConnected == true) {

                p2pClient?.ping(connectedDevice) {
                    // Toast.makeText(requireContext(), "Ping Result", Toast.LENGTH_LONG).show()
                }
                    ?.addOnSuccessListener {
                        Toast.makeText(requireContext(), "Success Ping", Toast.LENGTH_LONG).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "Fail Ping", Toast.LENGTH_LONG).show()
                    }
            }

        }

        if (item.itemId == R.id.sendMessageToBoundedDevice){

            val messageStr = "Hello, Wear Engine!"
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

        if (item.itemId == R.id.registerReceiver){

            if (connectedDevice != null && connectedDevice?.isConnected == true) {
                p2pClient?.registerReceiver(connectedDevice, receiver)
                    ?.addOnFailureListener {
                        // Your phone app fails to receive messages or files.
                    }
                    ?.addOnSuccessListener {
                        Toast.makeText(requireContext(), "Receiver successfully", Toast.LENGTH_LONG).show()
                    }
            }

        }

        return super.onOptionsItemSelected(item)
    }

}