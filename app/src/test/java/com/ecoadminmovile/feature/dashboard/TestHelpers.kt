package com.ecoadminmovile.feature.dashboard

import com.ecoadminmovile.core.network.EcoAdminApi
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Utilidad para tests: crea una instancia de EcoAdminApi que lanza error en cualquier metodo.
 *
 * Conceptos demostrados:
 * - java.lang.reflect.Proxy: crea implementaciones dinamicas de interfaces en runtime.
 * - object: singleton en Kotlin (solo hay una instancia de ThrowingApi).
 * - Proxy.newProxyInstance: patron del JDK para crear implementaciones "on the fly".
 *   Recibe: ClassLoader, array de interfaces, InvocationHandler (lambda que maneja las llamadas).
 *
 * Por que: los Fakes heredan de DashboardRepository/TransfersRepository que requieren
 * un EcoAdminApi en el constructor. Como los fakes NUNCA llaman a la API real (sobreescriben
 * los metodos), pasamos este proxy que explotaria si se usara accidentalmente.
 * Asi detectamos bugs donde el fake olvido sobreescribir un metodo.
 */
object ThrowingApi {
    // Proxy dinamico: crea un EcoAdminApi que lanza en cualquier metodo invocado.
    // Esto garantiza que si un test llama accidentalmente a un metodo no-fake, falla claro.
    val instance: EcoAdminApi = Proxy.newProxyInstance(
        EcoAdminApi::class.java.classLoader,
        arrayOf(EcoAdminApi::class.java),
        InvocationHandler { _, method, _ ->
            throw NotImplementedError(
                "ThrowingApi: ${method.name} should not be called in tests. " +
                "Override the repository method in your Fake."
            )
        }
    ) as EcoAdminApi
}
