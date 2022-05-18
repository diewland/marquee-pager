package com.diewland.pager

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Handler
import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import java.util.*

class MarqueePager constructor(private val pager: ViewPager2) {

    private fun l(msg: String) { Log.d("MARQUEE", msg) }

    // default config
    private val jobDelay = 1_000L
    private val durationPerPage = 5_000L
    private val lastPageDelay = 1_000L

    // setup job
    private var timer: Timer? = null
    private var handler: Handler? = null
    private var animator: ValueAnimator? = null

    // main functions
    fun play(delay: Long=jobDelay,
             dpp: Long=durationPerPage,
             lpd: Long=lastPageDelay) {
        // make sure no marquee running
        stop()
        // calc job period
        val pageSize = getItemCount()
        val period = (pageSize * dpp) + lpd
        // make schedule
        handler = Handler()
        timer = Timer()
        timer!!.schedule(object: TimerTask() {
            override fun run() {
                handler?.post { playAnimation(dpp) }
            }
        }, delay, period)
        // info
        l("<start> page_size: $pageSize, job_period: $period")
    }
    fun stop() {
        // stop animation
        animator?.also {
            it.removeAllListeners()
            it.cancel()
        }
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
        // info
        l("<stop>")
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
    private fun playAnimation(dpp: Long) {
        val total = getItemCount()
        if (total <= 1)
            l("empty or single page, do nothing")
        else if (pager.currentItem > 0) // go to first page
            goTo(pager, 0, dpp)
        else // go to last page
            goTo(pager, total-1, dpp)
    }
    // https://stackoverflow.com/a/59235979/466693
    private fun goTo(pager: ViewPager2, item: Int, dpp: Long) {
        l("go to item: $item")
        if (!validateItem(item)) return

        // get current pager state
        val pagePxWidth = pager.width
        val currentItem = pager.currentItem
        val duration = getItemCount() * dpp

        var previousValue = 0
        val pxToDrag: Int = pagePxWidth * (item - currentItem)
        animator = ValueAnimator.ofInt(0, pxToDrag)
        animator?.also {
            it.addUpdateListener { valueAnimator ->
                val currentValue = valueAnimator.animatedValue as Int
                val currentPxToDrag = (currentValue - previousValue).toFloat()
                pager.fakeDragBy(-currentPxToDrag)
                previousValue = currentValue
            }
            it.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) { pager.beginFakeDrag() }
                override fun onAnimationEnd(animation: Animator?) { pager.endFakeDrag() }
                override fun onAnimationCancel(animation: Animator?) { /* Ignored */ }
                override fun onAnimationRepeat(animation: Animator?) { /* Ignored */ }
            })
            it.interpolator = null // linear
            it.duration = duration
            it.start()
        }
    }

}