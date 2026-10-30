package com.example.the_white_smile.domain

/**
 * Se lanza cuando otro paciente ocupa el horario justo antes de confirmar.
 * La transaccion de Firestore vuelve a leer los bloques y falla con esta
 * excepcion si alguno ya existe.
 */
class SlotConflictException :
    IllegalStateException("El horario seleccionado ya fue ocupado")
