package br.com.reciclaja.radar.ui

import br.com.reciclaja.radar.domain.model.Evaluation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBR = Locale("pt", "BR")
private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ptBR)
private val dayMonth = DateTimeFormatter.ofPattern("dd/MM", ptBR)

fun Long.asDateTime(): String = dateTime.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
fun Long.asDayMonth(): String = dayMonth.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
fun Double.fmt(decimals: Int = 1): String = String.format(ptBR, "%.${decimals}f", this)

fun Long.relativeAge(now: Long): String {
    val min = (now - this) / 60_000
    return when {
        min < 1 -> "agora"
        min < 60 -> "há $min min"
        min < 60 * 24 -> "há ${min / 60} h"
        else -> "há ${min / (60 * 24)} dias"
    }
}

/** Rótulo para quando não há avaliação possível (null = avaliado). */
fun Evaluation.unavailableLabel(): String? = when (this) {
    is Evaluation.StaleData -> "Desatualizado"
    is Evaluation.InsufficientData -> "Dados insuficientes"
    Evaluation.NoData -> "Sem dados"
    else -> null
}
