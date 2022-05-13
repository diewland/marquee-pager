package com.diewland.pager

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Handler
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager2.widget.ViewPager2
import java.util.*

class MarqueePager constructor(private val pager: ViewPager2) {

    private fun l(msg: String) { Log.d("MARQUEE", msg) }
    private val interpolator = AccelerateDecelerateInterpolator()

    // default config
    private val jobDelay = 1_000L
    private val jobPeriod = 30_000L
    private val pagePeriod = 5_000L

    // setup job
    private var isRunning = false
    private var timer: Timer? = null
    private var handler: Handler? = null

    // main functions
    fun play(delay: Long=jobDelay,
             period: Long=jobPeriod,
             aniPeriod: Long=pagePeriod) {
        if (isRunning || pager.isFakeDragging) {
            l("job is running..")
            return
        }
        isRunning = true
        handler = Handler()
        timer = Timer()
        timer!!.schedule(object: TimerTask() {
            override fun run() {
                handler?.also { it.post { playAnimation(aniPeriod) } }
            }
        }, delay, period)
    }
    fun stop() {
        if (!isRunning || !pager.isFakeDragging) {
            l("job already stopped")
            return
        }
        isRunning = false
        // clean up
        timer?.also {
            it.cancel()
            it.purge()
        }
        handler?.also {
            it.removeCallbacksAndMessages(null)
        }
        // set null
        timer = null
        handler = null
    }

    // pager util
    private fun getItemCount(): Int {
        return pager.adapter?.itemCount ?: 0
    }
    private fun validateItem(item: Int): Boolean {
        if (item < 0) {
            l("[BREAK] invalid item")
            return false
        }
        return true
    }

    // animation
    private fun playAnimation(durationPerPage: Long) {
        val total = getItemCount()
        if (total <= 1)
            l("empty or single page, do nothing")
        else if (pager.currentItem > 0) // go to first page
            goTo(pager, 0, durationPerPage)
        else // go to last page
            goTo(pager, total-1, durationPerPage)
    }
    // https://stackoverflow.com/a/59235979/466693
    private fun goTo(pager: ViewPager2, item: Int, durationPerPage: Long) {
        l("go to item: $item")
        if (!validateItem(item)) return

        // get current pager state
        val pagePxWidth = pager.width
        val currentItem = pager.currentItem
        val duration = getItemCount() * durationPerPage

        val pxToDrag: Int = pagePxWidth * (item - currentItem)
        val animator = ValueAnimator.ofInt(0, pxToDrag)
        var previousValue = 0
        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            val currentPxToDrag = (currentValue - previousValue).toFloat()
            pager.fakeDragBy(-currentPxToDrag)
            previousValue = currentValue
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) { pager.beginFakeDrag() }
            override fun onAnimationEnd(animation: Animator?) { pager.endFakeDrag() }
            override fun onAnimationCancel(animation: Animator?) { /* Ignored */ }
            override fun onAnimationRepeat(animation: Animator?) { /* Ignored */ }
        })
        animator.interpolator = interpolator
        animator.duration = duration
        animator.start()
    }

}