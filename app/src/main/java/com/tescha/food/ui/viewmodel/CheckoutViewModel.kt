package com.tescha.food.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tescha.food.FoodApp
import com.tescha.food.data.model.OrderItem
import com.tescha.food.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentState { IDLE, PROCESSING, CONFIRMED, ERROR }

data class CheckoutForm(
    val cardHolder: String = "",
    val cardNumber: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val cardHolderError: String? = null,
    val cardNumberError: String? = null,
    val expiryError: String? = null,
    val cvvError: String? = null,
)

class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val orderRepository = (application as FoodApp).container.orderRepository
    private val userRepository = (application as FoodApp).container.userRepository

    private val _form = MutableStateFlow(CheckoutForm())
    val form: StateFlow<CheckoutForm> = _form.asStateFlow()

    private val _paymentState = MutableStateFlow(PaymentState.IDLE)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    // Mensaje de error mostrado en el checkout (ej. saldo insuficiente)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Saldo restante tras un pago confirmado (para refrescar la sesión / billetera)
    private val _newBalance = MutableStateFlow<Double?>(null)
    val newBalance: StateFlow<Double?> = _newBalance.asStateFlow()

    // ── Actualización de campos ──────────────────────────────────────────────
    fun onCardHolder(v: String) { _form.value = _form.value.copy(cardHolder = v, cardHolderError = null) }

    fun onCardNumber(v: String) {
        val digits = v.filter { it.isDigit() }.take(16)
        val formatted = digits.chunked(4).joinToString(" ")
        _form.value = _form.value.copy(cardNumber = formatted, cardNumberError = null)
    }

    fun onExpiry(v: String) {
        val digits = v.filter { it.isDigit() }.take(4)
        val formatted = if (digits.length > 2) "${digits.take(2)}/${digits.drop(2)}" else digits
        _form.value = _form.value.copy(expiry = formatted, expiryError = null)
    }

    fun onCvv(v: String) {
        val digits = v.filter { it.isDigit() }.take(3)
        _form.value = _form.value.copy(cvv = digits, cvvError = null)
    }

    // ── Validación local (RNF-07) ────────────────────────────────────────────
    private fun validate(): Boolean {
        val f = _form.value
        var ok = true

        val holderError = if (f.cardHolder.isBlank()) "Ingresa el nombre del titular" else null
        val numberError = when {
            f.cardNumber.replace(" ", "").length != 16 -> "El número debe tener 16 dígitos"
            !luhn(f.cardNumber.replace(" ", ""))       -> "Número de tarjeta inválido"
            else -> null
        }
        val expiryError = if (!validExpiry(f.expiry)) "Fecha inválida o vencida (MM/AA)" else null
        val cvvError    = if (f.cvv.length != 3) "CVV debe tener 3 dígitos" else null

        _form.value = f.copy(
            cardHolderError = holderError,
            cardNumberError = numberError,
            expiryError = expiryError,
            cvvError = cvvError,
        )

        return listOf(holderError, numberError, expiryError, cvvError).all { it == null }
    }

    // RF-08: simulación asíncrona de respuesta bancaria (exactamente 4 segundos)
    // RNF-08: Coroutines → no bloquea la UI
    // Ahora valida saldo, registra el pedido en Supabase y descuenta el crédito del usuario.
    fun pay(summary: CartSummary, user: User?) {
        _errorMessage.value = null
        if (!validate()) return

        if (user == null) {
            _errorMessage.value = "Debes iniciar sesión para pagar"
            return
        }
        if (summary.items.isEmpty()) {
            _errorMessage.value = "Tu carrito está vacío"
            return
        }
        // Validación local de saldo (feedback inmediato; el descuento real se revalida en la BD)
        if (user.balance < summary.total) {
            _errorMessage.value = "Saldo insuficiente. Tu crédito es " +
                "$${String.format("%.2f", user.balance)} y el total es " +
                "$${String.format("%.2f", summary.total)}."
            return
        }

        val merchantId = summary.items.first().product.merchantId
        val orderItems = summary.items.map { OrderItem(it.product, it.quantity) }
        val shippingFee = summary.shippingFee ?: 25.0

        viewModelScope.launch {
            _paymentState.value = PaymentState.PROCESSING
            delay(4_000)                          // exactamente 4 segundos (RF-08)
            try {
                // 1. Registrar el pedido en la base de datos
                orderRepository.createOrder(
                    userId = user.id,
                    merchantId = merchantId,
                    items = orderItems,
                    deliveryAddress = "",
                    shippingFee = shippingFee,
                )
                // 2. Descontar el crédito de forma atómica (revalida fondos en la BD)
                _newBalance.value = userRepository.debitBalance(user.id, summary.total)
                _paymentState.value = PaymentState.CONFIRMED
            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "Error al procesar el pago", e)
                _errorMessage.value = "No se pudo completar el pago. Verifica tu saldo e inténtalo de nuevo."
                _paymentState.value = PaymentState.ERROR
            }
        }
    }

    fun reset() {
        _paymentState.value = PaymentState.IDLE
        _form.value = CheckoutForm()
        _errorMessage.value = null
        _newBalance.value = null
    }

    // ── Algoritmo de Luhn (RNF-07) ──────────────────────────────────────────
    private fun luhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.indices.reversed()) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    // Valida que la fecha MM/AA sea futura (RNF-07)
    private fun validExpiry(expiry: String): Boolean {
        val parts = expiry.split("/")
        if (parts.size != 2) return false
        val month = parts[0].toIntOrNull() ?: return false
        val year  = parts[1].toIntOrNull() ?: return false
        if (month < 1 || month > 12) return false
        val now = java.util.Calendar.getInstance()
        val currentYear  = now.get(java.util.Calendar.YEAR) % 100
        val currentMonth = now.get(java.util.Calendar.MONTH) + 1
        return year > currentYear || (year == currentYear && month >= currentMonth)
    }
}
