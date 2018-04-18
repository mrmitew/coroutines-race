package com.github.mrmitew.coroutinerace

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.RendezvousChannel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val TRIGGER_STOP_RACE = true
    }

    private val random: Random = Random()
    private val channel = RendezvousChannel<Boolean>()
    private lateinit var raceJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefreshLayout.setOnRefreshListener {
            cancelRace()
            raceJob = with(Job()) {
                launch(UI, parent = this) { startProgress(this, rangeSeekbar1) }
                launch(UI, parent = this) { startProgress(this, rangeSeekbar2) }
                launch(UI, parent = this) { startProgress(this, rangeSeekbar3) }
                this
            }
        }

        launch(UI) {
            for (status in channel) {
                if (status) {
                    cancelRace()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelRace()
    }

    private fun cancelRace() {
        swipeRefreshLayout.isRefreshing = false
        if (!::raceJob.isInitialized) return
        if (!raceJob.isCancelled) {
            raceJob.cancel()
        }
    }

    private suspend fun startProgress(coroutineScope: CoroutineScope, seekbar: SeekBar) {
        swipeRefreshLayout.isRefreshing = false
        seekbar.progress = 0

        while (seekbar.progress < seekbar.max && coroutineScope.isActive) {
            seekbar.progress += (1..10).random()
            delay(10)
        }

        channel.offer(TRIGGER_STOP_RACE)

        Toast.makeText(this, "${seekbar.tag} has won", Toast.LENGTH_SHORT).show()
    }

    private fun ClosedRange<Int>.random() = random.nextInt(endInclusive - start) + start
}
