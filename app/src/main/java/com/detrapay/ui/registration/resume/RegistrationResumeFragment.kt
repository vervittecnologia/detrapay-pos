package com.detrapay.ui.registration.resume

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.FragmentRegistrationOrderResumeBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.registration.discount_dialog.DiscountDialogFragment
import com.detrapay.ui.registration.resume.RegistrationResumeRecyclerViewAdapter.OnItemClickListener
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class RegistrationResumeFragment : Fragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationOrderResumeBinding
    private lateinit var adapter: RegistrationResumeRecyclerViewAdapter

    companion object {
        const val REQUEST_EXTERNAL_STORAGE = 1
        val PERMISSIONS_STORAGE = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE ,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationOrderResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()

        binding.registrationOrderResumeNextBtn.setOnClickListener {
            binding.root.findNavController()
                .navigate(R.id.action_resumeFragment_to_paymentMethodFragment)
            registrationViewModel.onResumeNext()
        }

        binding.registrationOrderResumePrintBtn.setOnClickListener {
            verifyStoragePermissions(requireActivity())
            val dir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DCIM)
            val path = takeScreenShot(dir, binding.orderResumeView)
            if (path != null) {
                registrationViewModel.printOrderResume(path)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Falha ao realizar impressão",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.registrationOrderDiscountBtn.visibility =
            if (registrationViewModel.canAddDiscount()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.registrationOrderDiscountBtn.setOnClickListener {
            val discountDialogFragment =
                DiscountDialogFragment(listener = object : DiscountDialogFragment.OnUpdateListener {
                    override fun onUpdate(itemPosition: Int?) {
                        updateItem(itemPosition)
                    }

                })
            discountDialogFragment.show(parentFragmentManager, "DiscountDialogFragment")
        }

        registrationViewModel.loggedUser()?.let{
            binding.sellerName.visibility = View.VISIBLE
            binding.storeName.visibility = View.VISIBLE

            binding.sellerName.text = "Vendedor: ${it.preferredEmployeeName}"
            binding.storeName.text = "Loja: ${it.displayName}"
        }

        binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
        adapter = RegistrationResumeRecyclerViewAdapter(
            registrationViewModel.simulationItems(),
            object : OnItemClickListener {
                override fun onRemoveDiscount(item: SimulationItem, itemPosition: Int) {
                    val position = registrationViewModel.removeDiscount(item)
                    updateItem(position)
                }

            })
        val recyclerView: RecyclerView = binding.rvOrderDetailed
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        registrationViewModel.orderResumePrintState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Success<String> -> {
                    binding.printProgressView.visibility = View.GONE
                    binding.registrationOrderResumePrintBtn.visibility = View.VISIBLE
//                    Toast.makeText(requireContext(), status.data, Toast.LENGTH_LONG ).show()
                }

                is UIState.Error -> {
                    binding.printProgressView.visibility = View.GONE
                    binding.registrationOrderResumePrintBtn.visibility = View.VISIBLE
//                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG ).show()
                }

                is UIState.Loading -> {
                    binding.printProgressView.visibility = View.VISIBLE
                    binding.registrationOrderResumePrintBtn.visibility = View.GONE
                }
            }
        })
    }

    private fun verifyStoragePermissions(activity: Activity) {
        val permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission1 != PackageManager.PERMISSION_GRANTED ||
            permission2 != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun updateItem(itemPosition: Int?) {
        if (itemPosition != null) {
            adapter.updateItem(registrationViewModel.simulationItems(), itemPosition)
            binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
        }
    }

    private fun updateResume() {
        adapter.swapData(registrationViewModel.simulationItems())
        binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
    }

    private fun takeScreenShot(dir: File?, view: View): String? {
        try {
            view.isDrawingCacheEnabled = true
            val bitmap = view.drawingCache
            val now = Date()
            val imageFile = File(dir, now.time.toString() + ".jpg")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return imageFile.toString();
        } catch (e: Throwable) {
            Logger.d(e.message.toString())
            return null
        }
    }
}