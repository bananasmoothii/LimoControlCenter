import * as THREE from 'three'
import { getPinForRobot, removePin, robotGoals, showPinAsBeingFollowed, unassignedPin } from '@/components/3D/pin_goal'
import { selectedRobot } from '@/components/3D/robots'
import type { Ref } from 'vue'
import { ref } from 'vue'

export const ignoreNextClick = ref(false)

export function click_handling(scene: THREE.Scene, camera: THREE.Camera, plane: THREE.Mesh, viewWidth: Ref<number>, viewHeight: Ref<number>) {
  let raycaster = new THREE.Raycaster()
  let mouse = new THREE.Vector2()
  const minDistanceToConsiderAsDrag = 6
  let startX: number, startY: number
  let threeAppWrapper = document.getElementById('three-app-wrapper')!
  threeAppWrapper.addEventListener('mousedown', (event) => {
    startX = event.clientX
    startY = event.clientY
  })
  threeAppWrapper.addEventListener('mouseup', (event) => {
    if (ignoreNextClick.value) {
      ignoreNextClick.value = false
      return
    }

    let distance = Math.sqrt(Math.pow(event.clientX - startX, 2) + Math.pow(event.clientY - startY, 2))
    if (distance < minDistanceToConsiderAsDrag) {
      let scrolledWrapperTop = threeAppWrapper.getBoundingClientRect().y
      if (window.innerWidth < 768) {
        scrolledWrapperTop += 16 // idk why this is needed
      }

      mouse.x = (event.clientX / viewWidth.value) * 2 - 1
      mouse.y = -((event.clientY - scrolledWrapperTop) / viewHeight.value) * 2 + 1
      raycaster.setFromCamera(mouse, camera)

      let intersectRobot = raycaster.intersectObjects(scene.children.filter((obj) => obj.name.startsWith('robot-')))
      if (intersectRobot.length > 0) {
        let obj = intersectRobot[0].object
        while (!obj.name.startsWith('robot-')) {
          if (obj.parent === null) {
            throw new Error('Could not find robot object')
          }
          obj = obj.parent
        }
        let clickedRobot = obj.name.split('-', 2)[1]
        if (unassignedPin?.parent) {
          let pinObj = getPinForRobot(clickedRobot, scene)
          pinObj.visible = true
          pinObj.position.copy(unassignedPin.position)
          removePin()
          showPinAsBeingFollowed(robotGoals[clickedRobot], false)
        } else {
          selectedRobot.value = clickedRobot
        }
        return
      }

      let intersectPlane = raycaster.intersectObject(plane)
      if (intersectPlane.length > 0) {
        let intersect = intersectPlane[0]
        let pinObj = getPinForRobot(selectedRobot.value, scene)
        pinObj.visible = true
        pinObj.position.copy(intersect.point)
        console.log('set pin to', intersect.point.x, intersect.point.y)
        if (selectedRobot.value) {
          showPinAsBeingFollowed(robotGoals[selectedRobot.value], false)
        }
      } else {
        if (unassignedPin?.parent) {
          removePin()
        }
      }

      selectedRobot.value = null
    }
  })
}