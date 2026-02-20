package com.detrapay.ui.registration.resume

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
            Manifest.permission.READ_EXTERNAL_STORAGE,
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
        setupUI()
    }

    private fun setupUI() {
        binding.registrationOrderResumeNextBtn.setOnClickListener {
            registrationViewModel.onResumeNext()
            findNavController().navigate(R.id.action_resumeFragment_to_paymentMethodFragment)
        }

        binding.btnMore.setOnClickListener {
            showOverflowMenu(it)
        }

        registrationViewModel.loggedUser()?.let {
            binding.dealershipName.text = "Concessionária: ${registrationViewModel.getDealershipName()}"
            binding.sellerName.text = "Vendedor: ${registrationViewModel.getSalesmanName()}"
        }

        registrationViewModel.simulationSimulation()?.let { sim ->
            binding.vehicleType.text = "Tipo: ${registrationViewModel.getVehicleTypeName(sim.vehicleTypeId)}"
            binding.vehicleValue.text = "Valor do Veículo: R$ ${sim.vehiclePrice}"
            binding.acquisitionDate.text = "Data de Aquisição: ${sim.billingDate}"
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
            
        binding.rvOrderDetailed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrderDetailed.adapter = adapter
    }

    private fun showOverflowMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        if (registrationViewModel.canAddDiscount()) {
            popup.menu.add(0, 1, 0, "Aplicar Desconto")
        }
        popup.menu.add(0, 2, 1, "Imprimir Resumo")
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> showDiscountDialog()
                2 -> printResume()
            }
            true
        }
        popup.show()
    }

    private fun showDiscountDialog() {
        val discountDialogFragment = DiscountDialogFragment(listener = object : DiscountDialogFragment.OnUpdateListener {
            override fun onUpdate(itemPosition: Int?) {
                updateItem(itemPosition)
            }
        })
        discountDialogFragment.show(parentFragmentManager, "DiscountDialogFragment")
    }

    private fun printResume() {
        verifyStoragePermissions(requireActivity())
        val dir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_DCIM)
        
        val printView = createPrintView()
        val path = captureViewForPrint(dir, printView)
        if (path != null) {
            registrationViewModel.printOrderResume(path)
        } else {
            Toast.makeText(requireContext(), "Falha ao realizar impressão", Toast.LENGTH_LONG).show()
        }
    }

    private fun createPrintView(): View {
        val container = android.widget.LinearLayout(requireContext())
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.layoutParams = ViewGroup.LayoutParams(binding.cvVehicleInfo.width, ViewGroup.LayoutParams.WRAP_CONTENT)
        container.setBackgroundColor(android.graphics.Color.WHITE)
        container.setPadding(20, 20, 20, 20)

        fun addTextView(text: String, isBold: Boolean = false) {
            val tv = android.widget.TextView(requireContext())
            tv.text = text.uppercase()
            tv.setTextColor(android.graphics.Color.BLACK)
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f)
            if (isBold) tv.typeface = android.graphics.Typeface.DEFAULT_BOLD
            container.addView(tv)
        }

        addTextView("CONCESSIONÁRIA: ${registrationViewModel.getDealershipName()}", true)
        addTextView("VENDEDOR: ${registrationViewModel.getSalesmanName()}")
        
        registrationViewModel.simulationSimulation()?.let { sim ->
            addTextView("TIPO: ${registrationViewModel.getVehicleTypeName(sim.vehicleTypeId)}")
            addTextView("VALOR DO VEÍCULO: R$ ${sim.vehiclePrice}")
            addTextView("DATA DE AQUISIÇÃO: ${sim.billingDate}")
        }

        val divider = View(requireContext())
        divider.layoutParams = android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
        divider.setBackgroundColor(android.graphics.Color.BLACK)
        val params = divider.layoutParams as android.widget.LinearLayout.LayoutParams
        params.setMargins(0, 15, 0, 15)
        container.addView(divider)

        registrationViewModel.simulationItems().forEach { item ->
            val price = "%,.2f".format(java.util.Locale("pt", "BR"), item.price)
            addTextView("${item.name}: R$ $price")
            if (item.discount != null && item.discount > 0) {
                val disc = "%,.2f".format(java.util.Locale("pt", "BR"), item.discount)
                addTextView("   DESCONTO: - R$ $disc")
            }
        }

        val totalDivider = View(requireContext())
        totalDivider.layoutParams = android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
        totalDivider.setBackgroundColor(android.graphics.Color.BLACK)
        val tParams = totalDivider.layoutParams as android.widget.LinearLayout.LayoutParams
        tParams.setMargins(0, 15, 0, 15)
        container.addView(totalDivider)

        addTextView("VALOR TOTAL: ${registrationViewModel.simulationTotalAmount()}", true)

        return container
    }

    private fun captureViewForPrint(dir: File?, view: View): String? {
        return try {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(view.layoutParams.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            view.draw(canvas)

            val now = Date()
            val imageFile = File(dir, "print_${now.time}.jpg")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
            outputStream.close()
            imageFile.toString()
        } catch (e: Throwable) {
            Logger.d("Capture Error: ${e.message}")
            null
        }
    }

    private fun setupObservers() {
        registrationViewModel.orderResumePrintState.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UIState.Success -> {
                    binding.printProgressView.visibility = View.GONE
                }
                is UIState.Error -> {
                    binding.printProgressView.visibility = View.GONE
                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_LONG).show()
                }
                is UIState.Loading -> {
                    binding.printProgressView.visibility = View.VISIBLE
                }
                is UIState.Idle -> {}
            }
        }
    }

    private fun verifyStoragePermissions(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
        }
    }

    private fun updateItem(itemPosition: Int?) {
        if (itemPosition != null) {
            adapter.updateItem(registrationViewModel.simulationItems(), itemPosition)
            binding.resumeTotalAmount.text = registrationViewModel.simulationTotalAmount()
        }
    }
}
