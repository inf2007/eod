package com.singaporetech.eod

/**
 * Simple data object to record ML inference results.
 * @param label the name of the output class
 * @param confidence the probability of the recognition result
 */
data class InferenceOutput(val label:String, val confidence:Float) {

    override fun toString():String{
        return "$label / $probabilityString"
    }

    val probabilityString = String.format("%.1f%%", confidence * 100.0f)
}