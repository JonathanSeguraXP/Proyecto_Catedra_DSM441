package com.example.the_white_smile.ui.treatments

import com.example.the_white_smile.data.LocalTreatmentRepository
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.testing.FakeTreatmentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de las pantallas del catalogo de tratamientos. */
class TreatmentsViewModelTest {

    private val sample = listOf(
        Treatment(
            id = "limpieza",
            name = "Limpieza dental",
            summary = "Resumen de la limpieza",
            description = "Descripción de la limpieza",
            durationMinutes = 45,
            recommendations = listOf("Recomendación uno")
        ),
        Treatment(
            id = "blanqueamiento",
            name = "Blanqueamiento",
            summary = "Resumen del blanqueamiento",
            description = "Descripción del blanqueamiento",
            durationMinutes = 60,
            recommendations = listOf("Recomendación dos")
        )
    )

    private val repository = FakeTreatmentRepository(sample)

    @Test
    fun `la lista expone los tratamientos del repositorio`() {
        val viewModel = TreatmentsViewModel(repository)

        assertEquals(sample, viewModel.uiState.value.treatments)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `el detalle encuentra el tratamiento por su identificador`() {
        val viewModel = TreatmentDetailViewModel(repository, "blanqueamiento")

        assertEquals(sample[1], viewModel.uiState.value.treatment)
    }

    @Test
    fun `el detalle con un identificador inexistente queda vacio`() {
        val viewModel = TreatmentDetailViewModel(repository, "no-existe")

        assertNull(viewModel.uiState.value.treatment)
    }

    @Test
    fun `el catalogo local tiene identificadores unicos y datos completos`() {
        val catalog = LocalTreatmentRepository.getTreatments()

        assertTrue(catalog.isNotEmpty())
        assertEquals(catalog.size, catalog.map { it.id }.toSet().size)
        catalog.forEach { treatment ->
            assertTrue(treatment.name.isNotBlank())
            assertTrue(treatment.summary.isNotBlank())
            assertTrue(treatment.description.isNotBlank())
            assertTrue(treatment.durationMinutes > 0)
            assertTrue(treatment.recommendations.isNotEmpty())
            assertTrue(treatment.recommendations.all { it.isNotBlank() })
        }
    }
}
