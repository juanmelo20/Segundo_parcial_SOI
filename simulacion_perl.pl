#!C:\Strawberry\perl\bin\perl.exe

use strict;
use warnings;
use threads;
use Thread::Semaphore;
use Thread::Queue;

# Clase para representar un proceso
package Proceso;

sub new {
    my ($class, $id, $estado, $tiempo_ejecucion, $prioridad) = @_;
    my $self = {
        id => $id,
        estado => $estado,
        tiempo_ejecucion => $tiempo_ejecucion,
        prioridad => $prioridad
    };
    bless $self, $class;
    return $self;
}

1;

# Inicialización de recursos compartidos
my $cpu_semaphore = Thread::Semaphore->new(1);
my $memoria_semaphore = Thread::Semaphore->new(1);
my $cola = Thread::Queue->new();
my $memoria_disponible = 1024;
my %memoria_procesos;
my @globalProcesos;
my $idSecuencia=0;


# Función para ejecutar un proceso en la CPU
sub ejecutar_proceso {
    my ($proceso) = @_;

    
    if ($proceso->{estado} eq "Por ejecutar") {

        # Sección crítica: bloqueo de semáforo de CPU
        $cpu_semaphore->down(); # Bloquear el semáforo de CPU

        $proceso->{estado} = "Ejecutando";
        print "Proceso ", $proceso->{id}, " en ejecucion...\n";
        sleep($proceso->{tiempo_ejecucion}); # Simular la ejecución del proceso
        $proceso->{estado} = "Terminado";
        
        # Liberar recursos utilizados por el proceso
        liberar_memoria($proceso);

        print "\nProceso ", $proceso->{id}, " finalizado.\n";

        # Liberar la CPU para que otros procesos puedan ejecutarse
        $cpu_semaphore->up(); # Liberar el semáforo de CPU
    }
    if (@globalProcesos) {
            foreach my $proceso (@globalProcesos){
                
                if($proceso->{estado} eq "Por asignar memoria"){
                    print "\nProcesos en espera. Asignando memoria...\n";
                    asignar_memoria($proceso,int(rand(512)) + 1);
                    return menu_principal(); # Salir y mostrar el menú
                }
            }
    }
    menu_principal(); # Volver al menú principal




}


sub mostrar_procesos_creados {
    print "Procesos creados:\n";
    foreach my $proceso (@globalProcesos) {
        print "ID: $proceso->{id}, Estado: $proceso->{estado}, Tiempo de ejecucion: $proceso->{tiempo_ejecucion}, Prioridad: $proceso->{prioridad}\n";
    }
}


# Función para asignar memoria a un proceso
sub asignar_memoria {
    my ($proceso, $tamano) = @_;

    $memoria_semaphore->down(); # Bloquear el semáforo de memoria

    if ($memoria_disponible >= $tamano && $proceso->{estado} eq "Por asignar memoria") {
        $memoria_procesos{$proceso->{id}} = $tamano;
        $memoria_disponible -= $tamano;
        $proceso->{estado} = "Por ejecutar"; # Cambiar estado del proceso
        print "Memoria asignada al proceso ", $proceso->{id}, ": $tamano MB\n";
        push @globalProcesos, $proceso unless grep { $_->{id} == $proceso->{id} } @globalProcesos;
    } else {
        print "Memoria insuficiente para asignar al proceso ", $proceso->{id}, ". Esperando liberacion de memoria...\n";
        sleep(1);
        #$proceso->{estado} = "Por asignar memoria"; # Cambiar el estado del proceso
        push @globalProcesos, $proceso unless grep { $_->{id} == $proceso->{id} } @globalProcesos;
    }

    $memoria_semaphore->up(); # Liberar el semáforo de memoria
}

sub liberar_memoria {
    my ($proceso) = @_;

    $memoria_semaphore->down(); # Bloquear el semáforo de memoria

    if ($proceso->{estado} eq "Terminado" && exists $memoria_procesos{$proceso->{id}}) {
            my $tamano_liberado = delete $memoria_procesos{$proceso->{id}}; # Liberar memoria asociada al proceso
            $memoria_disponible += $tamano_liberado; # Incrementar la memoria disponible
            print "\nMemoria liberada del proceso ", $proceso->{id}, ": $tamano_liberado MB\n";
        }

    $memoria_semaphore->up(); # Liberar el semáforo de memoria
}




# Función para realizar operación de E/S
sub operacion_entrada_salida {
    my ($proceso) = @_;
    $proceso->{estado} = "Bloqueado"; # Cambiar estado a Bloqueado durante E/S
    print "Proceso ", $proceso->{id}, " en operación de entrada/salida.\n";
    sleep(2); # Simular operación de E/S
    $proceso->{estado} = "Listo"; # Cambiar estado a Listo después de E/S
}

# Función para generar procesos
sub generar_procesos {
    my ($cantidad) = @_;
    my @procesos;
    
    for (my $i = 1; $i <= $cantidad; $i++) {
        my $proceso = Proceso->new($idSecuencia+=1, "Por asignar memoria", int(rand(5)) + 1, int(rand(5)) + 1); # ID, estado inicial, tiempo de ejecución y prioridad aleatorios
        push @procesos, $proceso;
    }
    return @procesos;
}


# Algoritmo de planificación Round Robin con prioridad
sub planificar_rr {

    

    my ($lista_procesos) = @_;
    my $total_procesos = scalar @$lista_procesos;
    my $procesos_terminados = 0;

    

    # Ordenar la lista de procesos por prioridad (de mayor a menor)
    my @procesos_ordenados = sort { $b->{prioridad} <=> $a->{prioridad} } @$lista_procesos;

    my $hay_procesos_por_ejecutar = 0;

    foreach my $proceso (@procesos_ordenados) {
        if ($proceso->{estado} eq "Por ejecutar") {
            $hay_procesos_por_ejecutar = 1;
            last; # Salir del bucle al encontrar un proceso por ejecutar
        }
    }

    if ($hay_procesos_por_ejecutar) {
        while ($procesos_terminados < $total_procesos) {
            foreach my $proceso (@procesos_ordenados) {
                if ($proceso->{estado} eq "Por ejecutar") {
                    ejecutar_proceso($proceso);
                    $procesos_terminados++;
                } elsif ($proceso->{estado} eq "Bloqueado") {
                    operacion_entrada_salida($proceso);
                }
            }
        }
    } else {
        print "\nNo hay procesos por ejecutar.\n";
    }

    
}



# Algoritmo de planificación FCFS (First Come, First Served)
sub planificar_fcfs {
    my ($lista_procesos) = @_;
    foreach my $proceso (@$lista_procesos) {
        if ($proceso->{estado} eq "Listo") {
            ejecutar_proceso($proceso);
        }
    }
}




sub menu_principal {
    my $opcion;
    do {
        mostrar_menu();
        $opcion = <STDIN>;
        chomp($opcion);
        procesar_opcion($opcion);
    } while ($opcion != 6);
}


sub ver_estado_procesos {
    if (@globalProcesos) {
        print "\n========== Estado de los procesos ==========\n";
        foreach my $proceso (@globalProcesos) {
            print "Proceso ID: $proceso->{id}\n";
            print "Estado: $proceso->{estado}\n";
            print "Prioridad: $proceso->{prioridad}\n";
            if ($proceso->{estado} eq "Terminado") {
                my $tiempo_total = $proceso->{tiempo_ejecucion};
                print "Tiempo total de ejecucion: $tiempo_total segundos\n";
            }
            print "---------------------------------------------\n";
        }
    } else {
        print "\nNo hay procesos creados.\n";
    }
   
}


sub ver_cola_procesos {
    print "========== Cola de procesos ==========\n";
    my @cola_procesos;
    while (my $proceso = $cola->dequeue_nb()) {
        push @cola_procesos, $proceso;
    }

    if (@cola_procesos) {
        foreach my $proceso (@cola_procesos) {
            print "Proceso ", $proceso->{id}, "\n";
            $cola->enqueue($proceso);  # Volver a encolar el proceso
        }
    } else {
        print "La cola de procesos esta vacia.\n";
    }
    menu_principal();

}


sub ver_estado_memoria {
    print "========== Estado de la memoria ==========\n";
    print "Memoria disponible: $memoria_disponible MB\n";
    foreach my $proceso_id (keys %memoria_procesos) {
        print "Proceso $proceso_id: $memoria_procesos{$proceso_id} MB\n";
    }
    menu_principal();
}

sub mostrar_menu {
    print "\n======================= Menu =======================\n";
    print "|   1. Crear procesos                               |\n";
    print "|   2. Ejecutar procesos                            |\n";
    print "|   3. Ver estado de los procesos                   |\n";
    print "|   4. Ver estado de la memoria                     |\n";
    print "|   5. Salir                                        |\n";
    print "=====================================================\n";
    print "Ingrese su opcion: ";
    print "\n";
}


sub procesar_opcion {
    my ($opcion) = @_;
    if($opcion == 1){
        simulador();
    }elsif ($opcion == 2) {
        planificar_rr(\@globalProcesos);
    } elsif ($opcion == 3) {
        ver_estado_procesos();
    } elsif ($opcion == 4) {
        ver_estado_memoria();
    } elsif ($opcion == 5) {
        print "Saliendo del programa.\n";
        exit;  
    } else {
        print "Opcion invalida. Por favor, seleccione una opcion valida.\n";
    }
}


#MENUUU

# Simulación del sistema operativo
sub simulador {
        
        print "Cuantos procesos desea crear? ";
        my $numProcesos = <STDIN>;
        chomp($numProcesos); # Eliminar el salto de línea al final de la entrada
        @globalProcesos = (@globalProcesos, generar_procesos($numProcesos)); # Utilizar el valor de $numProcesos

        # Iterar sobre cada proceso y asignar memoria con pausas
        foreach my $proceso (@globalProcesos) {
            #print "Proceso ID: $proceso->{id}, Estado: $proceso->{estado}, Tiempo de ejecución: $proceso->{tiempo_ejecucion}, Prioridad: $proceso->{prioridad}\n";
            if($proceso->{estado} eq "Por asignar memoria"){
                asignar_memoria($proceso, int(rand(512)) + 1);
                sleep(1); # Pausa de 1 segundo entre cada asignación de memoria
            }
        }

        # Después de asignar memoria a todos los procesos, agregarlos a la cola
        foreach my $proceso (@globalProcesos) {
            $cola->enqueue($proceso);
        }

}

# Ejecutar el menú principal
menu_principal();


