package com.detrapay.ui.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import com.detrapay.data.Result
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationCustomer
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.SimulationSimulation
import com.detrapay.data.model.VehicleType
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.repositories.AuthRepository
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.testing.MainDispatcherRule
import com.detrapay.testing.getOrAwaitValueMatching
import com.detrapay.ui.registration.payment_method.RegistrationPaymentMethodCreateOrderState
import com.detrapay.ui.state.UIState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegistrationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val registrationRepository = mockk<RegistrationRepository>()
    private val orderRepository = mockk<OrderRepository>()
    private val authRepository = mockk<AuthRepository>()
    private val salesmanRepository = mockk<SalesmanRepository>()
    private val plugPag = mockk<IPlugPagWrapper>(relaxed = true)

    private lateinit var viewModel: RegistrationViewModel

    @Before
    fun setUp() {
        viewModel = RegistrationViewModel(
            registrationRepository = registrationRepository,
            orderRepository = orderRepository,
            authRepository = authRepository,
            salesmanRepository = salesmanRepository,
            plugPag = plugPag,
        )
    }

    @Test
    fun `onOrderNext loads simulation and updates current screen`() {
        coEvery { authRepository.getLoggedUser(true) } returns loggedUser()
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { salesmanRepository.getSalesmen() } returns Result.Success(loggedUser().salesmen)
        coEvery {
            registrationRepository.simulate(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Result.Success(simulation())

        viewModel.onOrderNext(
            cpfCnpj = "12345678901",
            clientName = "Cliente",
            whatsapp = "11999999999",
            invoiceDate = "06/03/2026",
            vehicleValue = "10.000,00",
            vehicleTypeId = 2,
            disposalVehicle = false,
            specialPlate = false,
            salesmanId = 1,
        )

        val state = viewModel.orderDataState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        }
        val navigationState = viewModel.registrationState.getOrAwaitValueMatching {
            it.currentScreen == 2
        }

        assertTrue(state is UIState.Success)
        assertEquals(2, navigationState.currentScreen)
        assertEquals("R$ 150,00", viewModel.simulationTotalAmount())
    }

    @Test
    fun `addDiscountToSimulationItem updates total and remaining balance`() {
        arrangeSimulationLoaded()

        val targetItem = viewModel.simulationItems().first()

        val updatedPosition = viewModel.addDiscountToSimulationItem(targetItem, 10.0)
        val remainingBalance = viewModel.remainingBalanceLiveData.getOrAwaitValueMatching {
            it == 140.0
        }

        assertEquals(0, updatedPosition)
        assertEquals("R$ 140,00", viewModel.simulationTotalAmount())
        assertEquals(140.0, remainingBalance, 0.0)
        assertEquals(10.0, viewModel.simulationItems().first().discount ?: 0.0, 0.0)
    }

    @Test
    fun `createOrder allows payments below total amount`() {
        arrangeSimulationLoaded()
        viewModel.addPayment(payment(amountOriginal = "100,00", amountFinal = "100,00"))
        coEvery { orderRepository.createOrder(any(), any(), any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createOrder()

        val state = viewModel.paymentSelectionCreateOrderState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        }

        assertTrue(state is UIState.Success)
        coVerify(exactly = 1) { orderRepository.createOrder(any(), any(), 1) }
    }

    @Test
    fun `createOrder submits matching payments and publishes success`() {
        arrangeSimulationLoaded()
        viewModel.addDiscountToSimulationItem(viewModel.simulationItems().first(), 10.0)
        viewModel.addPayment(payment(amountOriginal = "140,00", amountFinal = "145,60"))

        val paymentsSlot = slot<List<SimulationPayment>>()
        coEvery { orderRepository.createOrder(any(), capture(paymentsSlot), any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createOrder()

        val state = viewModel.paymentSelectionCreateOrderState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        }

        assertTrue(state is UIState.Success)
        assertTrue(state.data is RegistrationPaymentMethodCreateOrderState)
        assertEquals(1, paymentsSlot.captured.size)
        assertEquals("140,00", paymentsSlot.captured.single().amountOriginal)
        assertEquals("145,60", paymentsSlot.captured.single().amountFinal)
        coVerify(exactly = 1) { orderRepository.createOrder(any(), any(), 1) }
    }

    @Test
    fun `createOrderWithoutPayments submits empty receivables list`() {
        arrangeSimulationLoaded()
        val paymentsSlot = slot<List<SimulationPayment>>()
        coEvery { orderRepository.createOrder(any(), capture(paymentsSlot), any()) } returns Result.Success(mockk(relaxed = true))

        viewModel.createOrderWithoutPayments()

        val state = viewModel.paymentSelectionCreateOrderState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        }

        assertTrue(state is UIState.Success)
        assertTrue(paymentsSlot.captured.isEmpty())
        coVerify(exactly = 1) { orderRepository.createOrder(any(), any(), 1) }
    }

    @Test
    fun `addPayment publishes saved payment id for detail list focus`() {
        val payment = payment(amountOriginal = "100,00", amountFinal = "100,00")

        viewModel.addPayment(payment)

        val savedId = viewModel.lastSavedPaymentId.getOrAwaitValueMatching { it == payment.id }
        assertEquals(payment.id, savedId)
    }

    @Test
    fun `updateSimulationPayment publishes updated payment id for detail list focus`() {
        val initialPayment = payment(amountOriginal = "100,00", amountFinal = "100,00")
        val updatedPayment = initialPayment.copy(amountFinal = "120,00")
        viewModel.addPayment(initialPayment)
        viewModel.consumeLastSavedPaymentId()

        viewModel.updateSimulationPayment(updatedPayment)

        val savedId = viewModel.lastSavedPaymentId.getOrAwaitValueMatching { it == updatedPayment.id }
        assertEquals(updatedPayment.id, savedId)
    }

    @Test
    fun `calculateFees caches result using normalized key`() {
        val response = mockk<com.detrapay.data.model.remote.CalculateFeesResponse>()
        coEvery {
            registrationRepository.calculateFees(100.004, "CrÃ©dito", "visa")
        } returns Result.Success(response)

        viewModel.calculateFees(100.004, "CrÃ©dito", "visa")

        val state = viewModel.calculateFeesState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        }
        val cached = viewModel.getCachedFees(100.0, "crÃ©dito", "VISA")

        assertTrue(state is UIState.Success)
        assertNotNull(cached)
        assertEquals(response, cached)
    }

    @Test
    fun `calculateFees returns local zero fee quote for cash without remote call`() {
        viewModel.calculateFees(100.0, "cash")

        val state = viewModel.calculateFeesState.getOrAwaitValueMatching {
            it is UIState.Success<*>
        } as UIState.Success<CalculateFeesResponse>

        val installment = state.data?.data?.firstOrNull()?.installments?.firstOrNull()
        assertNotNull(installment)
        assertEquals(1, installment?.installmentNumber)
        assertEquals("100,00", installment?.totalValue)
        assertEquals("0,00", installment?.interestValue)
        assertTrue(installment?.noInterest == true)
        coVerify(exactly = 0) { registrationRepository.calculateFees(any(), any(), any()) }
    }

    @Test
    fun `buildWhatsAppTargetPhone normalizes digits and preserves country code`() {
        assertEquals("5511999998888", viewModel.buildWhatsAppTargetPhone("(11) 99999-8888"))
        assertEquals("5511999998888", viewModel.buildWhatsAppTargetPhone("55 (11) 99999-8888"))
        assertEquals("5511987654321", viewModel.buildWhatsAppTargetPhone("55+11 98765-4321"))
    }

    @Test
    fun `buildWhatsAppMessage includes customer items discounts and total`() {
        arrangeResumeContextLoaded()
        viewModel.addDiscountToSimulationItem(viewModel.simulationItems().first(), 10.0)

        val message = viewModel.buildWhatsAppMessage()

        assertNotNull(message)
        assertTrue(message!!.contains("Cliente: Cliente"))
        assertTrue(message.contains("CPF/CNPJ: 12345678901"))
        assertTrue(message.contains("WhatsApp: 11999999999"))
        assertTrue(message.contains("Concessionaria: Detrapay"))
        assertTrue(message.contains("Vendedor: Vendedor"))
        assertTrue(message.contains("Tipo de veiculo: Carro"))
        assertTrue(message.contains("Valor do veiculo: R$ 10.000,00"))
        assertTrue(message.contains("Data de aquisicao: 06/03/2026"))
        assertTrue(message.contains("- Taxa base: R$ 100,00"))
        assertTrue(message.contains("Desconto: -R$ 10,00"))
        assertTrue(message.contains("Valor total final: R$ 140,00"))
    }

    @Test
    fun `buildWhatsAppSharePayload creates wa me link with encoded message`() {
        arrangeResumeContextLoaded()

        val payload = viewModel.buildWhatsAppSharePayload()

        assertNotNull(payload)
        assertEquals("5511999999999", payload!!.phone)
        assertTrue(payload.waMeLink.startsWith("https://wa.me/5511999999999?text="))
        assertTrue(payload.waMeLink.contains("Cliente%3A%20Cliente"))
        assertTrue(payload.waMeLink.contains("%0A"))
    }

    private fun arrangeSimulationLoaded() {
        coEvery { authRepository.getLoggedUser(true) } returns loggedUser()
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { salesmanRepository.getSalesmen() } returns Result.Success(loggedUser().salesmen)
        coEvery {
            registrationRepository.simulate(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Result.Success(simulation())

        viewModel.onOrderNext(
            cpfCnpj = "12345678901",
            clientName = "Cliente",
            whatsapp = "11999999999",
            invoiceDate = "06/03/2026",
            vehicleValue = "10.000,00",
            vehicleTypeId = 2,
            disposalVehicle = false,
            specialPlate = false,
            salesmanId = 1,
        )

        viewModel.orderDataState.getOrAwaitValueMatching { it is UIState.Success<*> }
    }

    private fun arrangeResumeContextLoaded() {
        coEvery { registrationRepository.loadVehicleTypes() } returns Result.Success(
            listOf(VehicleType(id = 2, name = "Carro"))
        )
        coEvery { authRepository.getLoggedUser(true) } returns loggedUser()
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { salesmanRepository.getSalesmen() } returns Result.Success(loggedUser().salesmen)
        coEvery {
            registrationRepository.simulate(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Result.Success(simulation())

        viewModel.loadOrderScreenContent()
        viewModel.orderInitialState.getOrAwaitValueMatching { it is UIState.Success<*> }

        viewModel.onOrderNext(
            cpfCnpj = "12345678901",
            clientName = "Cliente",
            whatsapp = "11999999999",
            invoiceDate = "06/03/2026",
            vehicleValue = "10.000,00",
            vehicleTypeId = 2,
            disposalVehicle = false,
            specialPlate = false,
            salesmanId = 1,
        )

        viewModel.orderDataState.getOrAwaitValueMatching { it is UIState.Success<*> }
    }

    private fun simulation() = Simulation(
        customer = SimulationCustomer(
            cpfCnpj = "12345678901",
            name = "Cliente",
            whatsapp = "11999999999",
        ),
        simulation = SimulationSimulation(
            billingDate = "06/03/2026",
            vehiclePrice = "10000.00",
            vehicleDisposal = false,
            vehicleSpecialPlate = false,
            totalPrice = 150.0,
            vehicleTypeId = 2,
        ),
        simulationItems = listOf(
            SimulationItem(id = 1, name = "Taxa base", discountAllowed = true, price = 100.0, discount = null),
            SimulationItem(id = 2, name = "Taxa adicional", discountAllowed = false, price = 50.0, discount = null),
        ),
    )

    private fun payment(amountOriginal: String, amountFinal: String) = SimulationPayment(
        id = 1L,
        paymentMethod = PaymentMethod(
            id = 7,
            name = "CartÃ£o de crÃ©dito",
            installments = 1,
            interestTax = 0.04,
            paymentType = "credito",
            isOnlinePayment = true,
        ),
        amountOriginal = amountOriginal,
        amountFinal = amountFinal,
        installment = 1,
    )

    private fun loggedUser() = LoggedInUser(
        id = "user-1",
        sessionToken = "token",
        displayName = "UsuÃ¡rio",
        username = "user",
        cpfCnpj = "12345678000190",
        email = "user@example.com",
        companies = listOf(Company(id = 10, name = "Detrapay")),
        dispatchers = listOf(Dispatcher(id = 20, name = "Despachante")),
        salesmen = listOf(Salesman(id = 1, name = "Vendedor")),
    )
}
