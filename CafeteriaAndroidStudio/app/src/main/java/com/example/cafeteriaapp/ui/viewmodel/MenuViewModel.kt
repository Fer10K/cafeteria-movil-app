package com.example.cafeteriaapp.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeteriaapp.data.local.SessionManager
import com.example.cafeteriaapp.data.remote.RetrofitClient
import com.example.cafeteriaapp.data.repository.CarritoRepository
import com.example.cafeteriaapp.domain.model.OpcionExtraResponse
import com.example.cafeteriaapp.domain.model.ProductoModificado
import com.example.cafeteriaapp.domain.model.ProductoResponse
import com.example.cafeteriaapp.domain.model.toNetworkPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PedidoNetworkState {
    object Idle : PedidoNetworkState
    object Loading : PedidoNetworkState
    data class Success(val pedidoId: String) : PedidoNetworkState
    data class Error(val mensaje: String) : PedidoNetworkState
}
sealed interface MenuUiState {
    object Loading : MenuUiState
    data class Success(
        val todosLosProductos: List<ProductoResponse>,
        // Agrupados por nombre de categoría para la sección "Todo"
        val productosPorCategoria: Map<String, List<ProductoResponse>>
    ) : MenuUiState
    data class Error(val mensaje: String) : MenuUiState
}

class MenuViewModel: ViewModel() {
    var pedidoNetworkState by mutableStateOf<PedidoNetworkState>(PedidoNetworkState.Idle)
        private set

    private val _uiState = MutableStateFlow<MenuUiState>(MenuUiState.Loading)
    val uiState: StateFlow<MenuUiState> = _uiState

    init {
        cargarCatalogo()
    }
    var productoAEditar by mutableStateOf<ProductoResponse?>(null)
        private set

    // Lista mutable reactiva para los extras que el usuario selecciona EN TIEMPO REAL
    val extrasSeleccionadosTemporalmente = mutableStateListOf<OpcionExtraResponse>()

    // Cantidad de unidades del producto actual
    var cantidadTemporal by mutableStateOf(1)
        private set
    fun cargarCatalogo() {
        viewModelScope.launch {
            _uiState.value = MenuUiState.Loading
            try {
                // 🔍 PRINT CLAVE 2: Verificar intento de petición
                println("📡 [ANDROID DEBUG] Realizando GET a /productos...")

                val response = RetrofitClient.apiService.obtenerProductos()

                // 🔍 PRINT CLAVE 3: Ver código de respuesta HTTP
                println("📡 [ANDROID DEBUG] Código HTTP recibido: ${response}")

                if (response.isSuccessful) {
                    val cuerpo = response.body()


                    if (cuerpo != null) {

                        if (cuerpo.isNotEmpty()) {
                        }

                        val agrupados = cuerpo.groupBy { it.categoriaNombre ?: "Otros" }
                        _uiState.value = MenuUiState.Success(cuerpo, agrupados)
                    } else {
                        println("📡 [ANDROID DEBUG] El cuerpo llegó completamente NULL")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = MenuUiState.Error("Error al cargar el menú de la cafetería.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = MenuUiState.Error("Sin conexión con el servidor de la cafetería.")
            }
        }
    }
    fun abrirPersonalizacion(producto: ProductoResponse) {
        productoAEditar = producto
        cantidadTemporal = 1
        extrasSeleccionadosTemporalmente.clear()

        // Opcional: Preseleccionar opciones por defecto si min_seleccion == 1
        producto.gruposOpciones?.forEach { grupo ->
            if (grupo.minSeleccion == 1 && !grupo.opciones.isNullOrEmpty()) {
                // Preselecciona la primera opción del grupo (ej. Leche Entera)
                extrasSeleccionadosTemporalmente.add(grupo.opciones.first())
            }
        }
    }

    fun cerrarPersonalizacion() {
        productoAEditar = null
        extrasSeleccionadosTemporalmente.clear()
        cantidadTemporal = 1
    }

    fun incrementarCantidad() { cantidadTemporal++ }
    fun decrementarCantidad() { if (cantidadTemporal > 1) cantidadTemporal-- }

    // Lógica inteligente para Checkbox y RadioButton
    fun alternarOpcion(opcion: OpcionExtraResponse, maxSeleccion: Int, grupoId: Int, todasLasOpcionesDelGrupo: List<OpcionExtraResponse>) {
        if (maxSeleccion == 1) {
            // 🔘 COMPORTAMIENTO RADIO BUTTON: Quita cualquier otra opción de este grupo y pone la nueva
            extrasSeleccionadosTemporalmente.removeAll { elExtra ->
                todasLasOpcionesDelGrupo.any { it.id == elExtra.id }
            }
            extrasSeleccionadosTemporalmente.add(opcion)
        } else {
            // ☑️ COMPORTAMIENTO CHECKBOX: Si ya existe la quita, si no existe la agrega (respetando el máximo)
            if (extrasSeleccionadosTemporalmente.any { it.id == opcion.id }) {
                extrasSeleccionadosTemporalmente.removeAll { it.id == opcion.id }
            } else {
                val actualesDelGrupo = extrasSeleccionadosTemporalmente.count { elExtra ->
                    todasLasOpcionesDelGrupo.any { it.id == elExtra.id }
                }
                if (actualesDelGrupo < maxSeleccion) {
                    extrasSeleccionadosTemporalmente.add(opcion)
                }
            }
        }
    }

    fun agregarAlCarritoConfirmado() {
        val producto = productoAEditar ?: return

        val productoPersonalizadoFinal = ProductoModificado(
            productoBase = producto,
            extrasSeleccionados = extrasSeleccionadosTemporalmente.toList(),
            cantidad = cantidadTemporal
        )

        println("[CARRITO] Agregado: ${productoPersonalizadoFinal.productoBase.nombre} x${productoPersonalizadoFinal.cantidad}")
        println("[CARRITO] Precio Combinación: $${productoPersonalizadoFinal.precioTotal}")

        CarritoRepository.agregar(productoPersonalizadoFinal)
        cerrarPersonalizacion()
    }

    fun enviarPedidoAlServidor(metodoPago: String, onIdGenerado: (String) -> Unit) {
        val itemsCarrito = CarritoRepository.items
        if (itemsCarrito.isEmpty()) return

        // 1. Cambiamos el estado a cargando (para mostrar un spinner si es necesario)
        pedidoNetworkState = PedidoNetworkState.Loading

        viewModelScope.launch {
            try {

                val payload = itemsCarrito.toNetworkPayload(
                    usuarioId = "63f28ccd-9173-40b2-b919-01b784eb148f",
                    metodoPago = metodoPago
                )
                println("🚀 [FRONTEND] ENVIANDO PEDIDO A FASTAPI VIA NFC")
                println("👤 Usuario ID: ${payload.usuarioId}")
                println("💳 Método de Pago: ${payload.metodoPago}")
                println("📦 Cantidad de productos en el carrito: ${payload.items.size}")

                payload.items.forEachIndexed { index, item ->
                    println("   🛒 Producto [${index + 1}]: ${item.nombreProducto} (ID: ${item.productoId}) x${item.cantidad}")
                    println("      💰 Precio Base Unitario: $${item.precioUnitarioBase}")

                    if (item.extras.isNotEmpty()) {
                        println("      ➕ Extras seleccionados (${item.extras.size}):")
                        item.extras.forEach { extra ->
                            println("         - ${extra.nombreExtra} (ID: ${extra.extraId}) -> +$${extra.precioAdicional}")
                        }
                    } else {
                        println("      ➕ Sin extras seleccionados.")
                    }
                }
                println("🏁 [FRONTEND] FIN DEL PAYLOAD DE ENVÍO")

                val respuesta = RetrofitClient.apiService.crearPedido(payload)

                pedidoNetworkState = PedidoNetworkState.Success(respuesta.pedidoId)

                // Pasamos el ID del pedido a la UI para que inicie el Polling o el flujo correspondiente
                onIdGenerado(respuesta.pedidoId)

            } catch (e: Exception) {
                // 5. Si el servidor se cae, no hay internet o FastAPI tira un error 422/500
                pedidoNetworkState = PedidoNetworkState.Error(e.localizedMessage ?: "Error de conexión")
                println("❌ [API ERROR] No se pudo subir el pedido: ${e.message}")
            }
        }
    }
    fun resetPedidoState() {
        pedidoNetworkState = PedidoNetworkState.Idle
    }

    private val _gamificationUiState = MutableStateFlow<GamificationUiState>(GamificationUiState.Idle)
    val gamificationUiState: StateFlow<GamificationUiState> = _gamificationUiState.asStateFlow()

    fun cargarPerfilGamificacion(usuarioId: String) {
        viewModelScope.launch {
            // 🚀 CORRECCIÓN: Asignamos al nuevo estado de gamificación, no al del menú
            _gamificationUiState.value = GamificationUiState.Loading
            try {
                val response = RetrofitClient.apiService.obtenerPerfilGamificacion(usuarioId)

                if (response.isSuccessful && response.body() != null) {
                    _gamificationUiState.value = GamificationUiState.Success(data = response.body()!!)
                } else {
                    _gamificationUiState.value = GamificationUiState.Error("No se pudieron obtener tus estadísticas.")
                }
            } catch (e: Exception) {
                _gamificationUiState.value = GamificationUiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }
}