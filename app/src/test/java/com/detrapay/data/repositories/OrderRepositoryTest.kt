package com.detrapay.data.repositories

import com.detrapay.data.Result
import com.detrapay.data.datasources.remote.DetrapayRemoteDataSource
import com.detrapay.data.model.Company
import com.detrapay.data.model.Dispatcher
import com.detrapay.data.model.LoggedInUser
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.OrderStatus
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.Simulation
import com.detrapay.data.model.SimulationCustomer
import com.detrapay.data.model.SimulationItem
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.SimulationSimulation
import com.detrapay.data.model.remote.CreateOrderRequest
import com.detrapay.data.model.remote.FlatCustomerResponse
import com.detrapay.data.model.remote.FlatOrderReceivableResponse
import com.detrapay.data.model.remote.FlatPaymentMethodResponse
import com.detrapay.data.model.remote.FlatSalesmanResponse
import com.detrapay.data.model.remote.FlatVehicleTypeResponse
import com.detrapay.data.model.remote.OrderResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderRepositoryTest {

    private val remoteDataSource = mockk<DetrapayRemoteDataSource>()
    private val authRepository = mockk<AuthRepository>()

    private lateinit var repository: OrderRepository

    @Before
    fun setUp() {
        repository = OrderRepository(
            detrapayRemoteDataSource = remoteDataSource,
            authRepository = authRepository,
        )
    }

    @Test
    fun `createOrder sends item discount as separate field`() = runTest {
        val requestSlot = slot<CreateOrderRequest>()
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.createOrder(capture(requestSlot)) } returns Result.Error(Exception("stop"))

        val result = repository.createOrder(
            simulation = simulation(),
            simulationPayments = listOf(payment()),
            salesmanId = 58,
        )

        assertTrue(result is Result.Error)
        assertEquals("1277.20", requestSlot.captured.simulation.totalPrice)
        assertEquals("400.00", requestSlot.captured.items?.last()?.price)
        assertEquals("50.00", requestSlot.captured.items?.last()?.discount)
        assertNull(requestSlot.captured.items?.first()?.discount)
        coVerify(exactly = 1) { remoteDataSource.createOrder(any()) }
    }

    @Test
    fun `createOrder supports empty receivables list`() = runTest {
        val requestSlot = slot<CreateOrderRequest>()
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.createOrder(capture(requestSlot)) } returns Result.Error(Exception("stop"))

        val result = repository.createOrder(
            simulation = simulation(),
            simulationPayments = emptyList(),
            salesmanId = 58,
        )

        assertTrue(result is Result.Error)
        assertTrue(requestSlot.captured.receivables.isEmpty())
        coVerify(exactly = 1) { remoteDataSource.createOrder(any()) }
    }

    @Test
    fun `getOrders parses summary contract response`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.getOrders(37, 35) } returns Result.Success(
            listOf(
                OrderResponse(
                    id = 123,
                    status = "paid",
                    createdAt = "2026-03-06T10:30:00Z",
                    billingDate = "2026-03-06",
                    currentAmount = 1299.9,
                    customerName = "Joao Silva",
                    customer = FlatCustomerResponse(
                        id = 88,
                        name = "Joao Silva",
                        cpfCnpj = "12345678901",
                        phoneNumber = "5511999999999",
                        email = "joao@email.com"
                    ),
                    vehicleTypeName = "Carro",
                    vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                    salesmanName = "Maria"
                )
            )
        )

        val result = repository.getOrders()

        assertTrue(result is Result.Success)
        val order = (result as Result.Success).data.single()
        assertEquals(123, order.id)
        assertEquals(OrderStatus.PAID, order.status)
        assertEquals("Joao Silva", order.customer.name)
        assertEquals("12345678901", order.customer.cpfCnpj)
        assertEquals("Carro", order.vehicleType.name)
        assertEquals("Maria", order.salesman?.name)
        assertEquals(1299.9, order.currentAmount, 0.0)
    }

    @Test
    fun `getOrders uses customerCpfCnpj from summary payload`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.getOrders(37, 35) } returns Result.Success(
            listOf(
                OrderResponse(
                    id = 286,
                    status = "pending",
                    createdAt = "2026-03-10T16:48:03.281768+00:00",
                    billingDate = "2026-03-09",
                    currentAmount = 1734.43,
                    customerName = "jose airtin",
                    customerCpfCnpj = "12345678901",
                    salesmanName = "Jose Ray Da Silva"
                )
            )
        )

        val result = repository.getOrders()

        assertTrue(result is Result.Success)
        val order = (result as Result.Success).data.single()
        assertEquals("12345678901", order.customer.cpfCnpj)
        assertEquals("jose airtin", order.customer.name)
    }

    @Test
    fun `getOrder fetches detailed payload after summary list cache`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.getOrders(37, 35) } returns Result.Success(
            listOf(
                OrderResponse(
                    id = 286,
                    status = "pending",
                    createdAt = "2026-03-10T16:48:03.281768+00:00",
                    billingDate = "2026-03-09",
                    currentAmount = 1734.43,
                    customerName = "jose airtin",
                    salesmanName = "Jose Ray Da Silva"
                )
            )
        )
        coEvery { remoteDataSource.getOrder(286) } returns Result.Success(
            OrderResponse(
                id = 286,
                status = "pending",
                createdAt = "2026-03-10T16:48:03.281768+00:00",
                billingDate = "2026-03-09",
                originalAmount = 1734.43,
                currentAmount = 1734.43,
                customer = FlatCustomerResponse(
                    id = 99,
                    name = "jose airtin",
                    cpfCnpj = "12345678901",
                    phoneNumber = "85999999999"
                ),
                salesman = FlatSalesmanResponse(id = 54, name = "Jose Ray Da Silva"),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Moto"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 1,
                        documentId = "rec-1",
                        status = "pending",
                        installments = 1,
                        amountOriginal = 1734.43,
                        amountFinal = 1734.43,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 10,
                            name = "Credito",
                            installments = 1,
                            interestTax = 0.0,
                            paymentType = "credit"
                        )
                    )
                )
            )
        )

        val summaryResult = repository.getOrders()
        assertTrue(summaryResult is Result.Success)

        val detailResult = repository.getOrder(286)

        assertTrue(detailResult is Result.Success)
        val order = (detailResult as Result.Success).data
        assertEquals("12345678901", order.customer.cpfCnpj)
        assertEquals(1734.43, order.originalAmount, 0.0)
        assertEquals(1, order.receivables.size)
        coVerify(exactly = 1) { remoteDataSource.getOrder(286) }
    }

    @Test
    fun `getReceivables returns receivables with their order context`() = runTest {
        coEvery { authRepository.getLoggedUser(false) } returns loggedUser()
        coEvery { remoteDataSource.getOrders(37, 35) } returns Result.Success(
            listOf(
                OrderResponse(
                    id = 286,
                    status = "pending",
                    createdAt = "2026-03-10T16:48:03.281768+00:00",
                    billingDate = "2026-03-09",
                    currentAmount = 1734.43,
                    customerName = "jose airtin",
                    salesmanName = "Jose Ray Da Silva"
                )
            )
        )
        coEvery { remoteDataSource.getOrder(286) } returns Result.Success(
            OrderResponse(
                id = 286,
                status = "pending",
                createdAt = "2026-03-10T16:48:03.281768+00:00",
                billingDate = "2026-03-09",
                originalAmount = 1734.43,
                currentAmount = 1734.43,
                customer = FlatCustomerResponse(
                    id = 99,
                    name = "jose airtin",
                    cpfCnpj = "12345678901",
                    phoneNumber = "85999999999"
                ),
                salesman = FlatSalesmanResponse(id = 54, name = "Jose Ray Da Silva"),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Moto"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 1,
                        documentId = "rec-1",
                        status = "pending",
                        installments = 1,
                        amountOriginal = 1734.43,
                        amountFinal = 1734.43,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 10,
                            name = "Credito",
                            installments = 1,
                            interestTax = 0.0,
                            paymentType = "credit"
                        )
                    )
                )
            )
        )

        val result = repository.getReceivables(forceRefresh = true)

        assertTrue(result is Result.Success)
        val item = (result as Result.Success).data.single()
        assertEquals(286, item.order.id)
        assertEquals("jose airtin", item.order.customer.name)
        assertEquals(1, item.receivable.id)
        assertEquals(OrderReceivableItemStatus.PENDING, item.receivable.status)
        coVerify(exactly = 1) { remoteDataSource.getOrder(286) }
    }

    @Test
    fun `payOrder uses updated order returned by backend without extra fetch`() = runTest {
        val receivable = receivable()
        val paymentData = mockk<com.detrapay.data.model.PaymentData>(relaxed = true) {
            io.mockk.every { date } returns "06/03/2026"
            io.mockk.every { time } returns "10:20:30"
            io.mockk.every { copy(date = any()) } answers { callOriginal() }
        }
        coEvery { remoteDataSource.payOrderReceivable(any(), any()) } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "paid",
                createdAt = "2026-03-06T10:30:00Z",
                billingDate = "2026-03-06",
                currentAmount = 1299.9,
                originalAmount = 1299.9,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 20,
                        documentId = "abc",
                        status = "paid",
                        installments = 1,
                        paymentDate = "2026-03-06T10:40:00Z",
                        amountOriginal = 1299.9,
                        amountFinal = 1299.9,
                        tax = 0.0,
                        cardLast4 = "1234",
                        cardHolder = "JOAO",
                        cardBrand = "VISA",
                        authorizationCode = "9999",
                        pixTxIdCode = null,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 1,
                            name = "Credito",
                            installments = 12,
                            interestTax = 0.02,
                            paymentType = "credit"
                        )
                    )
                )
            )
        )

        val result = repository.payOrder(123, receivable, paymentData)

        assertTrue(result is Result.Success)
        val order = (result as Result.Success).data
        assertEquals(OrderStatus.PAID, order.status)
        assertEquals("Maria", order.salesman?.name)
        assertEquals(1, order.receivables.size)
        assertEquals("9999", order.receivables.single().authorizationCode)
        coVerify(exactly = 1) { remoteDataSource.payOrderReceivable(any(), any()) }
        coVerify(exactly = 0) { remoteDataSource.getOrder(any()) }
    }

    @Test
    fun `payOrder refetches order details when confirm payment returns partial payload`() = runTest {
        val receivable = receivable()
        val paymentData = mockk<com.detrapay.data.model.PaymentData>(relaxed = true) {
            io.mockk.every { date } returns "06/03/2026"
            io.mockk.every { time } returns "10:20:30"
            io.mockk.every { copy(date = any()) } answers { callOriginal() }
        }
        coEvery { remoteDataSource.payOrderReceivable(any(), any()) } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "paid",
                customerName = "Joao Silva"
            )
        )
        coEvery { remoteDataSource.getOrder(123) } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "paid",
                createdAt = "2026-03-06T10:30:00Z",
                billingDate = "2026-03-06",
                currentAmount = 1299.9,
                originalAmount = 1299.9,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 20,
                        documentId = "abc",
                        status = "paid",
                        installments = 1,
                        paymentDate = "2026-03-06T10:40:00Z",
                        amountOriginal = 1299.9,
                        amountFinal = 1299.9,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 1,
                            name = "Dinheiro",
                            installments = 1,
                            interestTax = 0.0,
                            paymentType = "cash"
                        )
                    )
                )
            )
        )

        val result = repository.payOrder(123, receivable, paymentData)

        assertTrue(result is Result.Success)
        val order = (result as Result.Success).data
        assertEquals(1299.9, order.originalAmount, 0.0)
        assertEquals(1, order.receivables.size)
        assertEquals("Dinheiro", order.receivables.single().paymentMethod.name)
        coVerify(exactly = 1) { remoteDataSource.payOrderReceivable(any(), any()) }
        coVerify(exactly = 1) { remoteDataSource.getOrder(123) }
    }

    @Test
    fun `payOrder preserves manual amount final when formatting confirm payload`() = runTest {
        val payloadSlot = slot<PaymentData>()
        coEvery { remoteDataSource.payOrderReceivable(any(), capture(payloadSlot)) } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "paid",
                customerName = "Joao Silva"
            )
        )
        coEvery { remoteDataSource.getOrder(123) } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "paid",
                createdAt = "2026-03-06T10:30:00Z",
                billingDate = "2026-03-06",
                currentAmount = 100.0,
                originalAmount = 80.0,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 20,
                        documentId = "abc",
                        status = "paid",
                        installments = 1,
                        paymentDate = "2026-03-06T10:40:00Z",
                        amountOriginal = 80.0,
                        amountFinal = 100.0,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 1,
                            name = "Dinheiro",
                            installments = 1,
                            interestTax = 0.0,
                            paymentType = "cash"
                        )
                    )
                )
            )
        )

        repository.payOrder(
            123,
            receivable().copy(
                amountOriginal = 80.0,
                amountFinal = 80.0,
                paymentMethod = receivable().paymentMethod.copy(
                    installments = 1,
                    interestTax = 0.0,
                    paymentType = "cash"
                )
            ),
            PaymentData(
                date = "06/03/2026",
                time = "10:20:30",
                amountFinal = 100.0
            )
        )

        assertEquals("2026-03-06T10:20:30", payloadSlot.captured.date)
        assertNull(payloadSlot.captured.amountOriginal)
        assertEquals(100.0, payloadSlot.captured.amountFinal)
    }

    @Test
    fun `addPendingReceivable uses updated order returned by backend without extra fetch`() = runTest {
        coEvery {
            remoteDataSource.addOrderReceivable(
                orderId = 123,
                paymentMethodId = 168,
                amountOriginal = 1000.0,
                installments = 1,
                paymentDate = null
            )
        } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "pending",
                createdAt = "2026-03-10T16:30:00Z",
                billingDate = "2026-03-10",
                currentAmount = 1043.9,
                originalAmount = 1577.2,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 55,
                        documentId = "rec-55",
                        status = "pending",
                        installments = 1,
                        paymentDate = "2026-03-10",
                        amountOriginal = 1000.0,
                        amountFinal = 1043.9,
                        tax = 0.0439,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 168,
                            name = "Credito 1x",
                            installments = 1,
                            interestTax = 0.0439,
                            paymentType = "credit"
                        )
                    )
                )
            )
        )

        val result = repository.addPendingReceivable(
            orderId = 123,
            paymentMethod = payment().paymentMethod,
            amountOriginal = 1000.0
        )

        assertTrue(result is Result.Success)
        val order = (result as Result.Success).data
        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals(1, order.receivables.size)
        assertEquals(1043.9, order.currentAmount, 0.0)
        coVerify(exactly = 1) {
            remoteDataSource.addOrderReceivable(
                orderId = 123,
                paymentMethodId = 168,
                amountOriginal = 1000.0,
                installments = 1,
                paymentDate = null
            )
        }
        coVerify(exactly = 0) { remoteDataSource.getOrder(any()) }
    }

    @Test
    fun `addPendingReceivable keeps cash payment pending without auto confirmation`() = runTest {
        val cashMethod = PaymentMethod(
            id = 22,
            name = "Dinheiro",
            installments = 1,
            interestTax = 0.0,
            paymentType = "cash"
        )
        coEvery {
            remoteDataSource.addOrderReceivable(
                orderId = 123,
                paymentMethodId = 22,
                amountOriginal = 500.0,
                installments = 1,
                paymentDate = null
            )
        } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "pending",
                createdAt = "2026-03-10T16:30:00Z",
                billingDate = "2026-03-10",
                currentAmount = 500.0,
                originalAmount = 1577.2,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = listOf(
                    FlatOrderReceivableResponse(
                        id = 56,
                        documentId = "rec-56",
                        status = "pending",
                        installments = 1,
                        paymentDate = null,
                        amountOriginal = 500.0,
                        amountFinal = 500.0,
                        tax = 0.0,
                        paymentMethod = FlatPaymentMethodResponse(
                            id = 22,
                            name = "Dinheiro",
                            installments = 1,
                            interestTax = 0.0,
                            paymentType = "cash"
                        )
                    )
                )
            )
        )

        val result = repository.addPendingReceivable(
            orderId = 123,
            paymentMethod = cashMethod,
            amountOriginal = 500.0
        )

        assertTrue(result is Result.Success)
        assertEquals(OrderReceivableItemStatus.PENDING, (result as Result.Success).data.receivables.single().status)
        coVerify(exactly = 1) {
            remoteDataSource.addOrderReceivable(
                orderId = 123,
                paymentMethodId = 22,
                amountOriginal = 500.0,
                installments = 1,
                paymentDate = null
            )
        }
        coVerify(exactly = 0) { remoteDataSource.payOrderReceivable(any(), any()) }
    }

    @Test
    fun `cancelPendingReceivable allows paid cash receivable`() = runTest {
        val receivable = receivable(
            status = OrderReceivableItemStatus.PAID,
            paymentMethod = PaymentMethod(
                id = 2,
                name = "Dinheiro",
                installments = 1,
                interestTax = 0.0,
                paymentType = "cash"
            )
        )
        coEvery { remoteDataSource.deleteOrderReceivableItem("abc") } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "pending",
                createdAt = "2026-03-10T16:30:00Z",
                billingDate = "2026-03-10",
                currentAmount = 0.0,
                originalAmount = 1299.9,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = emptyList()
            )
        )

        val result = repository.cancelPendingReceivable(123, receivable)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { remoteDataSource.deleteOrderReceivableItem("abc") }
    }

    @Test
    fun `cancelPendingReceivable rejects paid credit receivable`() = runTest {
        val receivable = receivable(status = OrderReceivableItemStatus.PAID)

        val result = repository.cancelPendingReceivable(123, receivable)

        assertTrue(result is Result.Error)
        assertEquals(
            "Este pagamento nao pode ser excluido no status atual.",
            (result as Result.Error).exception.message
        )
        coVerify(exactly = 0) { remoteDataSource.deleteOrderReceivableItem(any()) }
    }

    @Test
    fun `cancelPendingReceivable allows paid pix receivable`() = runTest {
        val receivable = receivable(
            status = OrderReceivableItemStatus.PAID,
            paymentMethod = PaymentMethod(
                id = 3,
                name = "Pix",
                installments = 1,
                interestTax = 0.0,
                paymentType = "pix"
            )
        )
        coEvery { remoteDataSource.deleteOrderReceivableItem("abc") } returns Result.Success(
            OrderResponse(
                id = 123,
                status = "pending",
                createdAt = "2026-03-10T16:30:00Z",
                billingDate = "2026-03-10",
                currentAmount = 0.0,
                originalAmount = 1299.9,
                customer = FlatCustomerResponse(
                    id = 88,
                    name = "Joao Silva",
                    cpfCnpj = "12345678901"
                ),
                vehicleType = FlatVehicleTypeResponse(id = 3, name = "Carro"),
                salesman = FlatSalesmanResponse(id = 4, name = "Maria"),
                receivables = emptyList()
            )
        )

        val result = repository.cancelPendingReceivable(123, receivable)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { remoteDataSource.deleteOrderReceivableItem("abc") }
    }

    private fun simulation() = Simulation(
        customer = SimulationCustomer(
            cpfCnpj = "05257121352",
            name = "Joao Vitor Lima",
            whatsapp = "11987654321",
        ),
        simulation = SimulationSimulation(
            billingDate = "06/03/2026",
            vehiclePrice = "20000",
            vehicleDisposal = false,
            vehicleSpecialPlate = false,
            totalPrice = 1327.2,
            vehicleTypeId = 1,
        ),
        simulationItems = listOf(
            SimulationItem(id = 25, name = "PRIMEIRO EMPLACAMENTO VEICULOS NOVOS", discountAllowed = false, price = 289.74, discount = null),
            SimulationItem(id = 28, name = "CARTORIO", discountAllowed = false, price = 50.0, discount = null),
            SimulationItem(id = 2, name = "IPVA MOTOS ATE 125cc", discountAllowed = false, price = 200.0, discount = null),
            SimulationItem(id = 5, name = "CONFECCAO DA PLACA", discountAllowed = false, price = 230.0, discount = null),
            SimulationItem(id = 19, name = "EXPEDICAO DE CRV/CRLV", discountAllowed = false, price = 31.49, discount = null),
            SimulationItem(id = 3, name = "EMPLACAMENTO EXTERNO", discountAllowed = false, price = 125.97, discount = null),
            SimulationItem(id = 6, name = "DESPACHANTE", discountAllowed = true, price = 400.0, discount = 50.0),
        ),
    )

    private fun payment() = SimulationPayment(
        id = 1L,
        paymentMethod = PaymentMethod(
            id = 168,
            name = "Credito 1x",
            installments = 1,
            interestTax = 0.0439,
            paymentType = "credit",
        ),
        amountOriginal = "1000,00",
        amountFinal = "1043,90",
        installment = 1,
    )

    private fun receivable(
        status: OrderReceivableItemStatus = OrderReceivableItemStatus.PENDING,
        paymentMethod: PaymentMethod = PaymentMethod(
            id = 1,
            name = "Credito",
            installments = 12,
            interestTax = 0.02,
            paymentType = "credit"
        )
    ) = com.detrapay.data.model.OrderReceivableItem(
        id = 20,
        documentId = "abc",
        amountOriginal = 1299.9,
        amountFinal = 1299.9,
        installments = 1,
        status = status,
        paymentMethod = paymentMethod,
        paymentDate = null,
        refundDate = null,
        cardLast4 = null,
        cardHolder = null,
        tax = null,
        cardBrand = null,
        authorizationId = null,
        authorizationCode = null,
        pixTxIdCode = null
    )

    private fun loggedUser() = LoggedInUser(
        id = "user-1",
        sessionToken = "token",
        displayName = "Usuario",
        username = "user",
        cpfCnpj = "12345678000190",
        email = "user@example.com",
        companies = listOf(Company(id = 37, name = "Detrapay")),
        dispatchers = listOf(Dispatcher(id = 35, name = "Despachante")),
        salesmen = emptyList(),
    )
}
