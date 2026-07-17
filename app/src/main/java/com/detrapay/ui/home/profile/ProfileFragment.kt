package com.detrapay.ui.home.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.Salesman
import com.detrapay.databinding.FragmentProfileBinding
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.ImageUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val homeViewModel: HomeViewModel by activityViewModels()
    private lateinit var salesmenAdapter: ProfileSalesmenAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeHomeState()
    }

    private fun setupRecyclerView() {
        salesmenAdapter = ProfileSalesmenAdapter()
        binding.salesmenList.layoutManager = LinearLayoutManager(requireContext())
        binding.salesmenList.adapter = salesmenAdapter
    }

    private fun observeHomeState() {
        homeViewModel.homeState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UIState.Success -> {
                    renderProfile(
                        companyName = state.data?.companyName.orEmpty(),
                        companyDocument = state.data?.companyDocument.orEmpty(),
                        companyLogoKey = state.data?.companyLogoKey,
                        salesmen = state.data?.salesmen.orEmpty()
                    )
                }

                else -> {
                    renderProfile(
                        companyName = "",
                        companyDocument = "",
                        companyLogoKey = null,
                        salesmen = emptyList()
                    )
                }
            }
        })
    }

    private fun renderProfile(
        companyName: String,
        companyDocument: String,
        companyLogoKey: String?,
        salesmen: List<Salesman>
    ) {
        binding.companyName.text = companyName.ifBlank {
            getString(R.string.home_default_company_name)
        }
        binding.companyDocument.text = getString(
            R.string.home_company_document,
            formatCnpj(companyDocument)
        )
        if (!companyLogoKey.isNullOrBlank()) {
            ImageUtils.loadImage(requireContext(), companyLogoKey, binding.companyLogo)
        } else {
            binding.companyLogo.setImageResource(R.drawable.icon)
        }
        salesmenAdapter.submitList(salesmen)
        binding.emptySalesmen.visibility = if (salesmen.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun formatCnpj(document: String): String {
        val numbers = document.filter(Char::isDigit)
        if (numbers.length != 14) return if (document.isBlank()) "-" else document

        return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
            "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
    }
}
