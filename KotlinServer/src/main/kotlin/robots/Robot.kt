package fr.bananasmoothii.limocontrolcenter.robots

class Robot(val id: String) {
    var lastPosString: String = "2.57,2.55,0"
    var lastReceivedKeepAlive: Long = 0
}