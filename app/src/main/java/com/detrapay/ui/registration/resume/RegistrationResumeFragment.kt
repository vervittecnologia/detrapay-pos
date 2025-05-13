package com.detrapay.ui.registration.resume

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.SimulationItem
import com.detrapay.databinding.FragmentRegistrationOrderResumeBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.registration.discount_dialog.DiscountDialogFragment
import com.detrapay.ui.registration.resume.RegistrationResumeRecyclerViewAdapter.OnItemClickListener
import java.io.File
import java.io.FileOutputStream
import java.util.Date


class RegistrationResumeFragment : Fragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationOrderResumeBinding
    private lateinit var adapter: RegistrationResumeRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationOrderResumeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registrationOrderResumeNextBtn.setOnClickListener {
            binding.root.findNavController()
                .navigate(R.id.action_resumeFragment_to_paymentMethodFragment)
            registrationViewModel.onResumeNext()
        }

        binding.registrationOrderResumePrintBtn.setOnClickListener {
//            activity?.window?.decorView?.getRootView()?.let {
//                takeScreenShot(it)
//            }
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
                    override fun onUpdate() {
                        updateResume()
                    }

                })
            discountDialogFragment.show(parentFragmentManager, "DiscountDialogFragment")
        }

        binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
        adapter = RegistrationResumeRecyclerViewAdapter(
            registrationViewModel.simulationItems(),
            object : OnItemClickListener {
                override fun onRemoveDiscount(item: SimulationItem, itemPosition: Int) {
                    registrationViewModel.removeDiscount(item)
                    updateResume()
                }

            })
        val recyclerView: RecyclerView = binding.rvOrderDetailed
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        recyclerView.adapter = adapter
    }

    private fun updateResume() {
        adapter.swapData(registrationViewModel.simulationItems())
        binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
    }

    private fun takeScreenShot(view: View) {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            val mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg"
            val bitmap = createBitmap(view.width, view.height)
            val imageFile = File(mPath)
            val outputStream = FileOutputStream(imageFile)
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            openScreenshot(imageFile)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun openScreenshot(imageFile: File) {
        val intent = Intent()
        intent.setAction(Intent.ACTION_VIEW)
        val uri = Uri.fromFile(imageFile)
        intent.setDataAndType(uri, "image/*")
        startActivity(intent)
    }


}