package fr.bananasmoothii.limocontrolcenter.threeD

import kotlinx.serialization.Serializable

@Serializable
data class Point2D(
    val x: Double,
    val y: Double
)

@Serializable
data class RobotGoals(
    val goals: Map<String, Point2D>
)
