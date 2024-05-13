import java.util.*
import java.util.concurrent.Semaphore

// Se importan las clases necesarias para trabajar con colecciones y semáforos.

// Clase para representar un proceso
data class Proceso(
    val id: Int, // Identificador único del proceso
    var estado: Estado, // Estado actual del proceso
    val tiempoEjecucion: Int, // Tiempo de ejecución del proceso (en segundos)
    val prioridad: Int // Prioridad del proceso (valor entre 1 y 5)
)

// Enumeración que define los diferentes estados que puede tener un proceso
enum class Estado {
    POR_ASIGNAR_MEMORIA, // El proceso está esperando para que se le asigne memoria
    POR_EJECUTAR, // El proceso está listo para ser ejecutado
    EJECUTANDO, // El proceso se está ejecutando actualmente
    TERMINADO, // El proceso ha finalizado su ejecución
    BLOQUEADO // El proceso está bloqueado debido a una operación de entrada/salida
}

// Semáforos para controlar el acceso a la CPU y la memoria
val cpuSemaphore = Semaphore(1) // Semáforo para la CPU (solo un proceso puede ejecutarse a la vez)
val memoriaSemaphore = Semaphore(1) // Semáforo para la memoria (solo un proceso puede acceder a la vez)

// Cola para almacenar los procesos listos para ser ejecutados
val cola = LinkedList<Proceso>()

// Variables para controlar la memoria disponible y la memoria asignada a cada proceso
var memoriaDisponible = 1024 // Memoria disponible (en MB)
val memoriaProcesos = mutableMapOf<Int, Int>() // Mapa que almacena la memoria asignada a cada proceso (ID del proceso -> Memoria asignada)

// Lista global para almacenar todos los procesos creados
val globalProcesos = mutableListOf<Proceso>()

// Variable para generar identificadores únicos para los procesos
var idSecuencia = 1

// Función para ejecutar un proceso en la CPU
fun ejecutarProceso(proceso: Proceso) {
    // Si el proceso está en estado "POR_EJECUTAR"
    if (proceso.estado == Estado.POR_EJECUTAR) {
        // Adquirir el semáforo de la CPU
        cpuSemaphore.acquire()

        // Cambiar el estado del proceso a "EJECUTANDO"
        proceso.estado = Estado.EJECUTANDO
        println("Proceso ${proceso.id} en ejecución...")

        // Simular la ejecución del proceso (dormir el hilo durante el tiempo de ejecución)
        Thread.sleep((proceso.tiempoEjecucion * 1000).toLong())

        // Cambiar el estado del proceso a "TERMINADO"
        proceso.estado = Estado.TERMINADO

        // Liberar la memoria asignada al proceso
        liberarMemoria(proceso)

        println("\nProceso ${proceso.id} finalizado.")

        // Liberar el semáforo de la CPU
        cpuSemaphore.release()
    }

    // Si hay procesos esperando para que se les asigne memoria
    if (globalProcesos.any { it.estado == Estado.POR_ASIGNAR_MEMORIA }) {
        println("\nProcesos en espera. Asignando memoria...")

        // Obtener el primer proceso en estado "POR_ASIGNAR_MEMORIA"
        val procesoEnEspera = globalProcesos.first { it.estado == Estado.POR_ASIGNAR_MEMORIA }

        // Asignar memoria al proceso
        asignarMemoria(procesoEnEspera, (1..512).random())

        // Volver al menú principal después de asignar memoria
        return menuPrincipal()
    }

    // Si no hay más procesos por ejecutar, volver al menú principal
    menuPrincipal()
}

// Función para mostrar los procesos creados
fun mostrarProcesosCreados() {
    println("Procesos creados:")
    // Iterar sobre cada proceso en la lista globalProcesos y mostrar su información
    globalProcesos.forEach {
        println("ID: ${it.id}, Estado: ${it.estado}, Tiempo de ejecución: ${it.tiempoEjecucion}, Prioridad: ${it.prioridad}")
    }
}

// Función para asignar memoria a un proceso
fun asignarMemoria(proceso: Proceso, tamano: Int) {
    // Adquirir el semáforo de memoria
    memoriaSemaphore.acquire()

    // Si hay suficiente memoria disponible y el proceso está en estado "POR_ASIGNAR_MEMORIA"
    if (memoriaDisponible >= tamano && proceso.estado == Estado.POR_ASIGNAR_MEMORIA) {
        // Asignar la memoria al proceso
        memoriaProcesos[proceso.id] = tamano
        // Restar la memoria asignada de la memoria disponible
        memoriaDisponible -= tamano
        // Cambiar el estado del proceso a "POR_EJECUTAR"
        proceso.estado = Estado.POR_EJECUTAR
        println("Memoria asignada al proceso ${proceso.id}: $tamano MB")

        // Agregar el proceso a la lista globalProcesos si no está presente
        if (proceso !in globalProcesos) globalProcesos.add(proceso)
    } else {
        // Si no hay suficiente memoria disponible, imprimir un mensaje y agregar el proceso a la lista globalProcesos si no está presente
        println("Memoria insuficiente para asignar al proceso ${proceso.id}. Esperando liberación de memoria...")
        if (proceso !in globalProcesos) globalProcesos.add(proceso)
    }

    // Liberar el semáforo de memoria
    memoriaSemaphore.release()
}

// Función para liberar la memoria asignada a un proceso
fun liberarMemoria(proceso: Proceso) {
    // Adquirir el semáforo de memoria
    memoriaSemaphore.acquire()

    // Si el proceso está en estado "TERMINADO" y tiene memoria asignada
    if (proceso.estado == Estado.TERMINADO && proceso.id in memoriaProcesos) {
        // Obtener el tamaño de la memoria asignada al proceso
        val tamanoLiberado = memoriaProcesos.remove(proceso.id)!!
        // Sumar la memoria liberada a la memoria disponible
        memoriaDisponible += tamanoLiberado
        println("\nMemoria liberada del proceso ${proceso.id}: $tamanoLiberado MB")
    }

    // Liberar el semáforo de memoria
    memoriaSemaphore.release()
}

// Función para simular una operación de entrada/salida
fun operacionEntradaSalida(proceso: Proceso) {
    // Cambiar el estado del proceso a "BLOQUEADO"
    proceso.estado = Estado.BLOQUEADO
    println("Proceso ${proceso.id} en operación de entrada/salida.")
    // Simular la operación de entrada/salida (dormir el hilo durante 2 segundos)
    Thread.sleep(2000)
    // Cambiar el estado del proceso a "POR_EJECUTAR" después de la operación de entrada/salida
    proceso.estado = Estado.POR_EJECUTAR
}

// Función para generar una lista de procesos
fun generarProcesos(cantidad: Int): List<Proceso> {
    val procesos = mutableListOf<Proceso>()
    for (i in 1..cantidad) {
        // Crear un nuevo proceso con un ID único, estado inicial "POR_ASIGNAR_MEMORIA", tiempo de ejecución y prioridad aleatorios
        val proceso = Proceso(
            id = idSecuencia++, // Incrementar el idSecuencia para obtener un ID único para cada proceso
            estado = Estado.POR_ASIGNAR_MEMORIA,
            tiempoEjecucion = (1..5).random(), // Tiempo de ejecución aleatorio entre 1 y 5 segundos
            prioridad = (1..5).random() // Prioridad aleatoria entre 1 y 5
        )
        procesos.add(proceso)
    }
    return procesos
}

// Algoritmo de planificación Round Robin con prioridad
fun planificarRR() {
    // Ordenar la lista de procesos por prioridad (de mayor a menor)
    val procesosOrdenados = globalProcesos.sortedByDescending { it.prioridad }
    // Verificar si hay procesos en estado "POR_EJECUTAR"
    val hayProcesosPorEjecutar = procesosOrdenados.any { it.estado == Estado.POR_EJECUTAR }

    // Si hay procesos por ejecutar
    if (hayProcesosPorEjecutar) {
        // Ejecutar procesos hasta que todos hayan terminado
        while (globalProcesos.any { it.estado != Estado.TERMINADO }) {
            for (proceso in procesosOrdenados) {
                // Si el proceso está en estado "POR_EJECUTAR", ejecutarlo
                if (proceso.estado == Estado.POR_EJECUTAR) {
                    ejecutarProceso(proceso)
                } else if (proceso.estado == Estado.BLOQUEADO) {
                    // Si el proceso está bloqueado, realizar la operación de entrada/salida
                    operacionEntradaSalida(proceso)
                }
            }
        }
    } else {
        println("\nNo hay procesos por ejecutar.")
    }
}

// Algoritmo de planificación FCFS (First Come, First Served)
fun planificarFCFS() {
    // Ejecutar todos los procesos en estado "POR_EJECUTAR" en el orden en que fueron creados
    globalProcesos.filter { it.estado == Estado.POR_EJECUTAR }.forEach { ejecutarProceso(it) }
}

// Función para mostrar el menú principal
fun menuPrincipal() {
    var opcion: Int?
    do {
        mostrarMenu()
        // Leer la opción ingresada por el usuario
        opcion = readLine()?.toIntOrNull()
        // Procesar la opción seleccionada
        procesarOpcion(opcion)
    } while (opcion != 6) // Continuar mostrando el menú hasta que se ingrese la opción 6 (Salir)
}

// Función para mostrar el estado de los procesos
fun verEstadoProcesos() {
    if (globalProcesos.isNotEmpty()) {
        println("\n========== Estado de los procesos ==========")
        globalProcesos.forEach {
            println("Proceso ID: ${it.id}")
            println("Estado: ${it.estado}")
            println("Prioridad: ${it.prioridad}")
            if (it.estado == Estado.TERMINADO) {
                println("Tiempo total de ejecución: ${it.tiempoEjecucion} segundos")
            }
            println("---------------------------------------------")
        }
    } else {
        println("\nNo hay procesos creados.")
    }
}

// Función para simular la creación de procesos
fun simulador() {
    print("Cuántos procesos desea crear? ")
    // Leer la cantidad de procesos a crear ingresada por el usuario
    val numProcesos = readLine()?.toIntOrNull() ?: 0

    // Generar la lista de nuevos procesos
    val nuevoProcesos = generarProcesos(numProcesos)
    // Agregar los nuevos procesos a la lista globalProcesos
    globalProcesos.addAll(nuevoProcesos)

    // Iterar sobre cada proceso en estado "POR_ASIGNAR_MEMORIA" y asignarle memoria
    globalProcesos.filter { it.estado == Estado.POR_ASIGNAR_MEMORIA }.forEach {
        asignarMemoria(it, (1..512).random()) // Asignar memoria entre 1 y 512 MB
        Thread.sleep(1000) // Pausa de 1 segundo entre cada asignación de memoria
    }

    // Agregar todos los procesos a la cola después de asignarles memoria
    cola.addAll(globalProcesos)
}

// Función para mostrar la cola de procesos
fun verColaProcesos() {
    println("========== Cola de procesos ==========")
    if (cola.isNotEmpty()) {
        // Recorrer la cola e imprimir el ID de cada proceso
        cola.forEach { println("Proceso ${it.id}") }
    } else {
        println("La cola de procesos está vacía.")
    }
    menuPrincipal()
}

// Función para mostrar el estado de la memoria
fun verEstadoMemoria() {
    println("========== Estado de la memoria ==========")
    println("Memoria disponible: $memoriaDisponible MB")
    // Recorrer el mapa memoriaProcesos e imprimir la memoria asignada a cada proceso
    memoriaProcesos.forEach { (procesoId, tamano) -> println("Proceso $procesoId: $tamano MB") }
    menuPrincipal()
}

// Función para mostrar el menú de opciones
fun mostrarMenu() {
    println("\n======================= Menú =======================")
    println("|   1. Crear procesos                               |")
    println("|   2. Ejecutar procesos                            |")
    println("|   3. Ver estado de los procesos                   |")
    println("|   4. Ver estado de la memoria                     |")
    println("|   5. Salir                                        |")
    println("=====================================================")
    print("Ingrese su opción: ")
}

// Función para procesar la opción seleccionada en el menú
fun procesarOpcion(opcion: Int?) {
    when (opcion) {
        1 -> simulador() // Crear nuevos procesos
        2 -> planificarRR() // Ejecutar procesos con el algoritmo Round Robin
        3 -> verEstadoProcesos() // Mostrar el estado de los procesos
        4 -> verEstadoMemoria() // Mostrar el estado de la memoria
        5 -> println("Saliendo del programa.") // Salir del programa
        else -> println("Opción inválida. Por favor, seleccione una opción válida.") // Opción inválida
    }
}

// Función principal
fun main() {
    menuPrincipal() // Mostrar el menú principal y iniciar el programa
}