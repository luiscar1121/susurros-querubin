// app/src/test/java/com/sq/susurros/service/AudioStateManagerTest.kt
package com.sq.susurros.service

import com.sq.susurros.domain.state.PlaybackState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests para AudioStateManager — máquina de estados y timing de fades.
 *
 * Usa verificación de emisiones de StateFlow corriendo sobre coroutines-test.
 */
class AudioStateManagerTest {

    @Test
    fun `Estado inicial es Idle`() = runTest {
        val manager = AudioStateManager()
        assertEquals(PlaybackState.Idle, manager.state.value)
    }

    @Test
    fun `startListening transita a Listening`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 0L, timerMs = 3_600_000L)

        val state = manager.state.value
        assertTrue(state is PlaybackState.Listening)
        val listeningState = state as PlaybackState.Listening
        assertEquals(3_600_000L, listeningState.activeTimer)
        assertEquals(1.0f, listeningState.bookVolume)
        assertEquals(0.0f, listeningState.musicVolume)
    }

    @Test
    fun `Listening con tick emite estado actualizado con remaining decrementado`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 0L, timerMs = 3_600_000L)

        manager.onTick(elapsedMs = 60_000L)

        val state = manager.state.value
        assertTrue(state is PlaybackState.Listening)
        val listeningState = state as PlaybackState.Listening
        // 1 minuto transcurrido, book position avanza
        assertEquals(60_000L, listeningState.bookPosition)
    }

    @Test
    fun `Listening con 6s restantes transita a MusicFadeIn`() = runTest {
        val manager = AudioStateManager()
        // Timer de 10s para trigger rápido
        manager.startListening(bookPosition = 0L, timerMs = 10_000L)

        // 4s transcurridos -> 6s restantes -> trigger de MusicFadeIn
        manager.onTick(elapsedMs = 4_000L)

        val state = manager.state.value
        assertTrue(state is PlaybackState.MusicFadeIn)
    }

    @Test
    fun `MusicFadeIn con 10s completos transita a MusicPlaying`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 0L, timerMs = 10_000L)
        manager.onTick(elapsedMs = 4_000L) // entra a MusicFadeIn

        val state1 = manager.state.value
        assertTrue(state1 is PlaybackState.MusicFadeIn)

        // 10s de fade -> completar
        manager.onTick(elapsedMs = 10_000L)

        val state2 = manager.state.value
        assertTrue(state2 is PlaybackState.MusicPlaying)
    }

    @Test
    fun `MusicPlaying con 5s restantes transita a BookResume (crossfade)`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 5_000L, timerMs = 10_000L)
        manager.onTick(elapsedMs = 4_000L) // MusicFadeIn
        manager.onTick(elapsedMs = 10_000L) // MusicPlaying

        val state1 = manager.state.value
        assertTrue(state1 is PlaybackState.MusicPlaying)

        // 5s restantes -> BookResume
        manager.onTick(elapsedMs = 5_000L)

        val state2 = manager.state.value
        assertTrue(state2 is PlaybackState.BookResume)
    }

    @Test
    fun `BookResume con 5s completos vuelve a Listening (ciclo reinicia)`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 5_000L, timerMs = 10_000L)
        // Secuencia completa: Listening -> MusicFadeIn -> MusicPlaying -> BookResume -> Listening
        manager.onTick(elapsedMs = 4_000L)
        manager.onTick(elapsedMs = 10_000L)
        manager.onTick(elapsedMs = 5_000L) // BookResume

        val state1 = manager.state.value
        assertTrue(state1 is PlaybackState.BookResume)

        // 5s de crossfade completos -> Listening
        manager.onTick(elapsedMs = 5_000L)

        val state2 = manager.state.value
        assertTrue(state2 is PlaybackState.Listening)
        val listeningState = state2 as PlaybackState.Listening
        // El timer se reinicia
        assertEquals(10_000L, listeningState.activeTimer)
    }

    @Test
    fun `Listening agotado transita a Idle`() = runTest {
        val manager = AudioStateManager()
        manager.startListening(bookPosition = 0L, timerMs = 5_000L)

        // 5s pasados -> no hay trigger a fades (no llega a los 6s de trigger)
        manager.onTick(elapsedMs = 5_000L)

        val state = manager.state.value
        assertEquals(PlaybackState.Idle, state)
    }
}
