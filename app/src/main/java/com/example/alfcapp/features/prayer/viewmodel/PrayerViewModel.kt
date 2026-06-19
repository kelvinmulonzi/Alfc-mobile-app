package com.example.alfcapp.features.prayer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.alfcapp.data.prayer.PrayerCategory
import com.example.alfcapp.data.prayer.PrayerDto
import com.example.alfcapp.data.prayer.PrayerRepository
import com.example.alfcapp.data.prayer.PrayerVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Plain state-holder for the Prayer Wall screen. Kept Compose-friendly via
 * mutableStateOf so the screen can observe it directly without pulling in
 * lifecycle-viewmodel-compose; matches the rest of the app's pattern of
 * keeping feature state next to the screen.
 */
class PrayerViewModel(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    var prayers by mutableStateOf<List<PrayerDto>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var selectedCategory by mutableStateOf<PrayerCategory?>(null)
        private set

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                prayers = PrayerRepository.wall(selectedCategory)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                error = t.message ?: "Couldn't load prayer wall."
            } finally {
                loading = false
            }
        }
    }

    fun setCategory(category: PrayerCategory?) {
        if (selectedCategory == category) return
        selectedCategory = category
        refresh()
    }

    fun submit(
        body: String,
        category: PrayerCategory,
        visibility: PrayerVisibility,
        onSuccess: (PrayerDto) -> Unit,
    ) {
        if (submitting) return
        scope.launch {
            submitting = true
            error = null
            try {
                val created = PrayerRepository.submit(body, category, visibility)
                // Only public-wall posts appear in the list; private ones go to the
                // prayer team. Prepend so the submitter immediately sees their post.
                if (created.visibility == PrayerVisibility.PUBLIC_WALL) {
                    prayers = listOf(created) + prayers
                }
                onSuccess(created)
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                error = t.message ?: "Couldn't send your request."
            } finally {
                submitting = false
            }
        }
    }

    fun togglePray(id: Long) {
        scope.launch {
            try {
                val res = PrayerRepository.pray(id)
                prayers = prayers.map {
                    if (it.id == id) it.copy(prayCount = res.prayCount, prayedByMe = res.prayedByMe) else it
                }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                error = t.message ?: "Couldn't record that, please try again."
            }
        }
    }

    fun deleteMine(id: Long) {
        scope.launch {
            try {
                PrayerRepository.delete(id)
                prayers = prayers.filterNot { it.id == id }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                error = t.message ?: "Couldn't delete, please try again."
            }
        }
    }

    fun dismissError() {
        error = null
    }
}
