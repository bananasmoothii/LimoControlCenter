package fr.bananasmoothii.limocontrolcenter.robots

class Robot(val id: String) {
    var lastPosString: String = "0,0,0"
    var lastReceivedKeepAlive: Long = 0
}