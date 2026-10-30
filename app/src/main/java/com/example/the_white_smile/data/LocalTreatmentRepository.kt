package com.example.the_white_smile.data

import com.example.the_white_smile.domain.Treatment

/**
 * Catalogo de tratamientos incluido en la aplicacion.
 * Es contenido de ejemplo: al conectarlo a un backend, basta con crear
 * otra implementacion de [TreatmentRepository].
 */
object LocalTreatmentRepository : TreatmentRepository {

    override fun getTreatments(): List<Treatment> = CATALOG

    override fun getTreatment(id: String): Treatment? =
        CATALOG.firstOrNull { it.id == id }

    private val CATALOG = listOf(
        Treatment(
            id = "consulta-general",
            name = "Consulta general",
            summary = "Evaluación inicial de tu salud bucal.",
            description = "En la primera consulta revisamos el estado de tus dientes, " +
                "encías y mucosas. A partir de esta evaluación definimos un plan de " +
                "tratamiento y resolvemos tus dudas.",
            durationMinutes = 30,
            recommendations = listOf(
                "Trae tus radiografías o estudios previos, si los tienes.",
                "Indícanos los medicamentos que tomas y las alergias conocidas."
            ),
            priceCents = 1_500
        ),
        Treatment(
            id = "limpieza-dental",
            name = "Limpieza dental",
            summary = "Eliminación de placa y sarro para mantener encías sanas.",
            description = "La limpieza profesional remueve la placa bacteriana y el sarro " +
                "que el cepillado no alcanza. Ayuda a prevenir caries, gingivitis y " +
                "mal aliento.",
            durationMinutes = 45,
            recommendations = listOf(
                "Se recomienda realizarla cada seis meses.",
                "Evita alimentos muy fríos o calientes durante las primeras horas."
            ),
            priceCents = 3_000
        ),
        Treatment(
            id = "blanqueamiento",
            name = "Blanqueamiento",
            summary = "Aclara el tono de tus dientes de forma segura.",
            description = "Aplicamos un agente blanqueador controlado para reducir manchas " +
                "y devolver luminosidad al esmalte. El resultado varía según el tono " +
                "inicial y los hábitos de cada paciente.",
            durationMinutes = 60,
            recommendations = listOf(
                "Evita café, té, vino y tabaco durante las 48 horas posteriores.",
                "Consulta antes si tienes sensibilidad dental o restauraciones visibles."
            ),
            priceCents = 10_000
        ),
        Treatment(
            id = "ortodoncia",
            name = "Valoración de ortodoncia",
            summary = "Evaluación inicial para definir un plan de ortodoncia.",
            description = "Evaluamos la posición de tus dientes y tu mordida para definir " +
                "el tratamiento de ortodoncia más adecuado. El costo del tratamiento " +
                "completo se determina después de esta valoración.",
            durationMinutes = 45,
            recommendations = listOf(
                "Asiste a los controles periódicos.",
                "Mantén una higiene cuidadosa alrededor de los brackets."
            ),
            priceCents = 4_000
        ),
        Treatment(
            id = "endodoncia",
            name = "Endodoncia",
            summary = "Tratamiento de conducto para conservar una pieza dañada.",
            description = "La endodoncia elimina el tejido infectado del interior del " +
                "diente y lo sella para evitar la pérdida de la pieza y aliviar el dolor.",
            durationMinutes = 90,
            recommendations = listOf(
                "Acude si presentas dolor persistente o inflamación.",
                "Programa la restauración definitiva después del tratamiento."
            ),
            priceCents = 37_500
        ),
        Treatment(
            id = "extraccion",
            name = "Extracción",
            summary = "Retiro de una pieza dental que no puede conservarse.",
            description = "Realizamos la extracción de dientes muy dañados, retenidos o " +
                "con infección, con anestesia local y las indicaciones necesarias para " +
                "una recuperación adecuada.",
            durationMinutes = 30,
            recommendations = listOf(
                "No hagas esfuerzos ni enjuagues fuertes durante las primeras 24 horas.",
                "Aplica frío local y toma los medicamentos indicados."
            ),
            priceCents = 4_000
        ),
        Treatment(
            id = "implante-dental",
            name = "Implante dental",
            summary = "Reemplazo de una pieza perdida con una raíz artificial.",
            description = "El implante dental sustituye la raíz de un diente perdido " +
                "mediante una pieza de titanio que se integra al hueso. Luego se coloca " +
                "la corona definitiva.",
            durationMinutes = 120,
            recommendations = listOf(
                "Se requiere una valoración previa con estudios de imagen.",
                "El cuidado diario y los controles son clave para su duración."
            ),
            priceCents = 90_000
        ),
        Treatment(
            id = "revision-rutina",
            name = "Revisión de rutina",
            summary = "Control periódico para detectar problemas a tiempo.",
            description = "Revisamos dientes y encías, y confirmamos si necesitas una " +
                "limpieza o algún tratamiento adicional. Es la mejor forma de prevenir " +
                "complicaciones.",
            durationMinutes = 30,
            recommendations = listOf(
                "Asiste cada seis meses, aunque no tengas molestias.",
                "Aprovecha la visita para resolver tus dudas sobre higiene."
            ),
            priceCents = 1_000
        )
    )
}
